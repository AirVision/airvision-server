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

// https://github.com/AirVision/airvision-server/wiki/Rest-API#request-aircraft

@Serializable
data class AircraftRequest(
    val icao24: AircraftIcao24
)

suspend fun PipelineContext.handleAircraftRequest(context: RestContext) {
  val request = call.receive<AircraftRequest>()

  val aircraft = context.aircraftService.get(request.icao24)
  if (aircraft == null) {
    call.respond(error.notFound("Aircraft with icao24 ${request.icao24} not found"))
    return
  }
  if (aircraft.velocity == null || aircraft.position == null) {
    call.respond(error.notFound("Aircraft with icao24 ${request.icao24} has incomplete data"))
    return
  }
  call.respond(aircraft)
}
