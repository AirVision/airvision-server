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
import io.github.airvision.service.AircraftStateData
import io.github.airvision.service.openskynetwork.serializer.OsnAircraftStateDataSerializer
import io.github.airvision.util.arrow.flatMapLeft
import io.github.airvision.util.ktor.Failure
import io.github.airvision.util.ktor.requestTimeout
import io.github.airvision.util.ktor.tryToGet
import io.ktor.client.HttpClient
import io.ktor.client.features.HttpTimeout
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.url
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
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
    val json = Json {
      val module = SerializersModule {
        include(AirVision.json.context)
        contextual(OsnAircraftStateDataSerializer)
      }
      ignoreUnknownKeys = true
      serialModule = module
    }
    install(JsonFeature) {
      serializer = KotlinxSerializer(json)
    }
    install(HttpTimeout) {
      requestTimeout = 20.seconds
    }
  }

  /**
   * Makes a http request to the OpenSky Network API.
   */
  private suspend inline fun <reified T> request(
      path: String, parameters: Map<String, Any> = mapOf()): Either<Failure, T> {
    return this.client.tryToGet {
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
   * Attempts to get the [AircraftStateData] object for the given
   * [AircraftIcao24] identifier and optional time.
   */
  suspend fun getState(aircraftId: AircraftIcao24, time: Instant? = null): Either<Failure, OsnAircraftsResponse> {
    return request("/states/all", buildMap {
      put("icao24", aircraftId)
      if (time != null)
        put("time", time.epochSecond)
    })
  }

  /**
   * Attempts to get the [AircraftStateData] objects for the given [GeodeticBounds].
   */
  suspend fun getStates(bounds: GeodeticBounds, time: Instant? = null): Either<Failure, OsnAircraftsResponse> {
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
   * Attempts to get all the [AircraftStateData] objects.
   */
  suspend fun getStates(time: Instant? = null): Either<Failure, OsnAircraftsResponse> {
    return request("/states/all", buildMap {
      if (time != null)
        put("time", time.epochSecond)
    })
  }

  /**
   * Attempts to get the current track for the given [AircraftIcao24].
   */
  suspend fun getTrack(aircraftId: AircraftIcao24): Either<Failure, OsnTrackResponse?> {
    return request<OsnTrackResponse>("/tracks", mapOf(
        "icao24" to aircraftId,
        "time" to 0
    )).flatMapLeft { failure ->
      if (failure is Failure.ErrorResponse && failure.response.status == HttpStatusCode.NotFound) {
        Either.right(null)
      } else {
        Either.left(failure)
      }
    }
  }

  /**
   * Attempts to get the current flight for the given [AircraftIcao24].
   */
  suspend fun getFlight(aircraftId: AircraftIcao24): Either<Failure, OsnFlight?> {
    val now = Instant.now()
    val begin = now.minusMillis(5000)
    val end = now.plusMillis(5000)
    return requestAircraftFlights(aircraftId, begin, end).map { it.firstOrNull() }
  }

  private suspend fun requestAircraftFlights(
      aircraftId: AircraftIcao24, beginTime: Instant, endTime: Instant
  ) = request<List<OsnFlight>>("/flights/aircraft", mapOf(
      "icao24" to aircraftId,
      "begin" to beginTime.epochSecond,
      "end" to endTime.epochSecond
  )).flatMapLeft { failure ->
    if (failure is Failure.ErrorResponse && failure.response.status == HttpStatusCode.NotFound) {
      Either.right(emptyList())
    } else {
      Either.left(failure)
    }
  }
}
