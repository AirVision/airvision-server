/*
 * AirVision
 *
 * Copyright (c) AirVision <https://www.github.com/AirVision>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
package io.github.airvision.rest.openskynetwork

import arrow.core.Either
import com.github.benmanes.caffeine.cache.AsyncLoadingCache
import com.github.benmanes.caffeine.cache.Caffeine
import io.github.airvision.AirVision
import io.github.airvision.AircraftIcao24
import io.github.airvision.GeodeticBounds
import io.ktor.client.HttpClient
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.get
import io.ktor.client.request.url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.await
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CompletableFuture

@Suppress("NON_APPLICABLE_CALL_FOR_BUILDER_INFERENCE")
class OpenSkyNetwork(
    credentials: OsnCredentials? = null
) {

  companion object {
    private const val RateLimitAnonymous = 10
    private const val RateLimit = 5
  }

  private val rateLimit = if (credentials != null) RateLimit else RateLimitAnonymous

  private val host = run {
    val base = "opensky-network.org"
    if (credentials == null) {
      base
    } else {
      "${credentials.username}:${credentials.password}@$base"
    }
  }

  private val client = HttpClient {
    install(JsonFeature) {
      serializer = KotlinxSerializer(AirVision.json)
    }
  }

  /**
   * Makes a http request to the OpenSky Network API.
   */
  private suspend inline fun <reified T> request(path: String, parameters: Map<String, Any> = mapOf()): T {
    return this.client.get {
      url(scheme = "https", host = host, path = "/api$path") {
        this.parameters.apply {
          parameters.forEach { (key, value) ->
            append(key, value.toString())
          }
        }
      }
    }
  }

  private val aircraftCache = buildCache<AircraftIcao24, OsnAircraft?> { requestAircraft(it) }
  private val aircraftTrackCache = buildCache<AircraftIcao24, OsnTrackResponse?> { requestAircraftTrack(it) }
  private val aircraftFlightCache = buildCache<AircraftIcao24, OsnFlight?> { requestAircraftFlight(it) }

  private fun <K, V> buildCache(loader: suspend (key: K) -> V): AsyncLoadingCache<K, V> {
    return Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofSeconds(this.rateLimit.toLong()))
        .executor(Dispatchers.Default.asExecutor())
        .buildAsync<K, V> { key, executor ->
          GlobalScope.async(executor.asCoroutineDispatcher()) {
            loader(key)
          }.asCompletableFuture()
        }
  }

  private suspend fun requestAircraft(icao24: AircraftIcao24, time: Int? = null): OsnAircraft? {
    val response = request<OsnStatesResponse>("/states/all", buildMap {
      put("icao24", icao24)
      if (time != null)
        put("time", time)
    })
    return response.states?.firstOrNull()
  }

  /**
   * Attempts to get the [OsnAircraft] object for the given
   * [AircraftIcao24] identifier and optional time.
   */
  suspend fun getAircraft(icao24: AircraftIcao24, time: Int? = null): OsnAircraft? {
    return if (time == null) {
      aircraftCache.get(icao24).await()
    } else {
      requestAircraft(icao24, time)
    }
  }

  /**
   * Attempts to get the [OsnAircraft] objects for the given [GeodeticBounds].
   */
  suspend fun getAircrafts(bounds: GeodeticBounds, time: Int? = null): List<OsnAircraft> {
    val max = bounds.max
    val min = bounds.min

    val response = request<OsnStatesResponse>("/states/all", buildMap {
      put("lamin", min.latitude)
      put("lamax", max.latitude)
      put("lomin", min.longitude)
      put("lomax", max.longitude)
      if (time != null)
        put("time", time)
    })

    // Cache the aircrafts, so we need less individual calls, only do this
    // for ones that don't use a time
    if (time == null && response.states != null) {
      for (aircraft in response.states)
        aircraftCache.put(aircraft.icao24, CompletableFuture.completedFuture(aircraft))
    }

    return response.states ?: listOf()
  }

  /**
   * Attempts to get all the [OsnAircraft] objects.
   */
  suspend fun getAircrafts(): List<OsnAircraft> {
    val response = request<OsnStatesResponse>("/states/all")

    // Cache the aircrafts, so we need less individual calls
    if (response.states != null) {
      for (aircraft in response.states)
        aircraftCache.put(aircraft.icao24, CompletableFuture.completedFuture(aircraft))
    }

    return response.states ?: listOf()
  }

  private suspend fun requestAircraftTrack(icao24: AircraftIcao24): OsnTrackResponse? {
    return Either.catch {
      request<OsnTrackResponse>("/tracks", mapOf(
          "icao24" to icao24,
          "time" to 0
      ))
    }.fold({
      it.printStackTrace()
      null
    }, {
      it
    })
  }

  /**
   * Attempts to get the current track for the given [AircraftIcao24].
   */
  suspend fun getTrack(icao24: AircraftIcao24): OsnTrackResponse? =
      aircraftTrackCache.get(icao24).await()

  /**
   * Attempts to get the current flight for the given [AircraftIcao24].
   */
  suspend fun getFlight(icao24: AircraftIcao24): OsnFlight? =
      aircraftFlightCache.get(icao24).await()

  private suspend fun requestAircraftFlight(icao24: AircraftIcao24): OsnFlight? {
    val now = Instant.now()
    val begin = now.minusMillis(5000)
    val end = now.plusMillis(5000)
    return requestAircraftFlights(icao24, begin, end).firstOrNull()
  }

  private suspend fun requestAircraftFlights(icao24: AircraftIcao24, beginTime: Instant, endTime: Instant): List<OsnFlight> {
    return Either.catch {
      request<List<OsnFlight>>("/flights/aircraft", mapOf(
          "icao24" to icao24,
          "begin" to beginTime.epochSecond,
          "end" to endTime.epochSecond
      ))
    }.fold({
      it.printStackTrace()
      emptyList()
    }, {
      it
    })
  }
}
