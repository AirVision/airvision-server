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

// https://github.com/AirVision/airvision-server/wiki/Rest-API#request-aircraft-model

@Serializable
data class AircraftModelRequest(
  val icao24: AircraftIcao24
)

suspend fun PipelineContext.handleAircraftModelRequest(context: RestContext) {
  val request = call.receive<AircraftModelRequest>()
  val info = context.aircraftInfoService.get(request.icao24)
    ?: error.notFound("Couldn't find information for ${request.icao24}")
  call.respond(info)
}
