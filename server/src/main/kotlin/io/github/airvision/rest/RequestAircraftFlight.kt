/*
 * AirVision
 *
 * Copyright (c) AirVision <https://www.github.com/AirVision>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
package io.github.airvision.rest

import io.github.airvision.AircraftIcao24
import io.ktor.application.call
import io.ktor.request.receive
import io.ktor.response.respond
import kotlinx.serialization.Serializable

// https://github.com/AirVision/airvision-server/wiki/Rest-API#request-aircraft-flight

@Serializable
data class AircraftFlightRequest(
  val icao24: AircraftIcao24
)

suspend fun PipelineContext.handleAircraftFlightRequest(context: RestContext) {
  val request = call.receive<AircraftFlightRequest>()
  val icao24 = request.icao24

  val response = context.aircraftService.getFlight(icao24)
    ?: error.notFound("Couldn't find a flight for $icao24")
  call.respond(response)
}
