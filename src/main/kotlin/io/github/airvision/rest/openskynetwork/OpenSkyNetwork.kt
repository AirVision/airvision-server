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
import io.github.airvision.AircraftIcao
import io.github.airvision.GeodeticBounds
import io.ktor.client.HttpClient
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.await
import java.time.Duration
import java.util.concurrent.CompletableFuture

class OpenSkyNetwork(
    credentials: OsnCredentials? = null
) {

  companion object {
    private const val RateLimitAnonymous = 10
    private const val RateLimit = 5
  }

  private val rateLimit = if (credentials != null) RateLimit else RateLimitAnonymous

  private val baseUrl = run {
    val base = "opensky-network.org/api"
    val https = "https://"
    if (credentials == null) {
      "$https$base"
    } else {
      "$https${credentials.username}:${credentials.password}@$base"
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
    val params = parameters.entries.joinToString(separator = "&")
    var url = baseUrl + path
    if (params.isNotEmpty())
      url += "?$params"
    return this.client.get(baseUrl + path)
  }

  private val aircraftCache = buildCache(::requestAircraft)
  private val aircraftTrackCache = buildCache(::requestAircraftTrack)

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

  private suspend fun requestAircraft(icao: AircraftIcao): OsnAircraft? {
    val response = request<OsnStatesResponse>("/states/all", mapOf(
        "icao24" to icao
    ))
    return response.states.firstOrNull()
  }

  /**
   * Attempts to get the [OsnAircraft] object for the given [AircraftIcao] identifier.
   */
  suspend fun getAircraft(icao: AircraftIcao): OsnAircraft? {
    return aircraftCache.get(icao).await()
  }

  /**
   * Attempts to get the [OsnAircraft] objects for the given [GeodeticBounds].
   */
  suspend fun getAircrafts(bounds: GeodeticBounds): List<OsnAircraft> {
    val max = bounds.max
    val min = bounds.min

    val response = request<OsnStatesResponse>("/states/all", mapOf(
        "lamin" to min.latitude,
        "lamax" to max.latitude,
        "lomin" to min.longitude,
        "lomax" to max.longitude
    ))

    // Cache the aircrafts, so we need less individual calls
    for (aircraft in response.states)
      aircraftCache.put(aircraft.icao, CompletableFuture.completedFuture(aircraft))

    return response.states
  }

  /**
   * Attempts to get all the [OsnAircraft] objects.
   */
  suspend fun getAircrafts(): List<OsnAircraft> {
    val response = request<OsnStatesResponse>("/states/all")

    // Cache the aircrafts, so we need less individual calls
    for (aircraft in response.states)
      aircraftCache.put(aircraft.icao, CompletableFuture.completedFuture(aircraft))

    return response.states
  }

  private suspend fun requestAircraftTrack(icao: AircraftIcao): OsnTrackResponse? {
    return Either.catch {
      request<OsnTrackResponse>("/tracks", mapOf(
          "icao24" to icao,
          "time" to 0
      ))
    }.fold({
      it.printStackTrace()
      null
    }, {
      it
    })
  }

  suspend fun getAircraftTrack(icao: AircraftIcao): OsnTrackResponse? =
      aircraftTrackCache.get(icao).await()
}
