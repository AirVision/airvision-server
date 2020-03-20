/*
 * AirVision
 *
 * Copyright (c) AirVision <https://www.github.com/AirVision>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
package io.github.airvision.service.openskynetwork

import arrow.core.Either
import io.github.airvision.AirVision
import io.github.airvision.AircraftIcao24
import io.github.airvision.GeodeticBounds
import io.ktor.client.HttpClient
import io.ktor.client.features.ClientRequestException
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.get
import io.ktor.client.request.url
import io.ktor.http.HttpStatusCode
import java.time.Instant
import kotlin.time.seconds

@Suppress("NON_APPLICABLE_CALL_FOR_BUILDER_INFERENCE")
class OsnRestService(credentials: OsnSettings = OsnSettings("", "")) {

  companion object {
    private const val RateLimitAnonymous = 10
    private const val RateLimit = 5
  }

  /**
   * The rate limit that requests can be done.
   */
  val rateLimit = (if (credentials.username.isNotEmpty()) RateLimit else RateLimitAnonymous).seconds

  private val host = run {
    val base = "opensky-network.org"
    if (credentials.username.isEmpty()) {
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

  /**
   * Attempts to get the [OsnAircraftData] object for the given
   * [AircraftIcao24] identifier and optional time.
   */
  suspend fun getAircraft(icao24: AircraftIcao24, time: Instant? = null): OsnAircraftsResponse {
    return request("/states/all", buildMap {
      put("icao24", icao24)
      if (time != null)
        put("time", time.epochSecond)
    })
  }

  /**
   * Attempts to get the [OsnAircraftData] objects for the given [GeodeticBounds].
   */
  suspend fun getAircrafts(bounds: GeodeticBounds, time: Instant? = null): OsnAircraftsResponse {
    val max = bounds.max
    val min = bounds.min

    return request("/states/all", buildMap {
      put("lamin", min.latitude)
      put("lamax", max.latitude)
      put("lomin", min.longitude)
      put("lomax", max.longitude)
      if (time != null)
        put("time", time.epochSecond)
    })
  }

  /**
   * Attempts to get all the [OsnAircraftData] objects.
   */
  suspend fun getAircrafts(time: Instant? = null): OsnAircraftsResponse {
    return request("/states/all", buildMap {
      if (time != null)
        put("time", time.epochSecond)
    })
  }

  /**
   * Attempts to get the current track for the given [AircraftIcao24].
   */
  suspend fun getTrack(icao24: AircraftIcao24): OsnTrackResponse? {
    return Either.catch {
      request<OsnTrackResponse>("/tracks", mapOf(
          "icao24" to icao24,
          "time" to 0
      ))
    }.fold({ cause ->
      if (cause is ClientRequestException && cause.response.status == HttpStatusCode.NotFound) {
        null
      } else {
        throw cause
      }
    }, { track ->
      track
    })
  }

  /**
   * Attempts to get the current flight for the given [AircraftIcao24].
   */
  suspend fun getFlight(icao24: AircraftIcao24): OsnFlight? {
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
    }.fold({ cause ->
      if (cause is ClientRequestException && cause.response.status == HttpStatusCode.NotFound) {
        emptyList()
      } else {
        throw cause
      }
    }, { flights ->
      flights
    })
  }
}
