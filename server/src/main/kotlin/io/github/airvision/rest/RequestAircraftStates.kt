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

import io.github.airvision.GeodeticBounds
import io.ktor.application.call
import io.ktor.request.receive
import io.ktor.response.respond
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.Instant

// https://github.com/AirVision/airvision-server/wiki/Rest-API#request-aircrafts

@Serializable
class AircraftStatesRequest(
    @Contextual val time: Instant? = null,
    val bounds: GeodeticBounds? = null
)

suspend fun PipelineContext.handleAircraftStatesRequest(context: RestContext) {
  val request = call.receive<AircraftStatesRequest>()
  val states = if (request.bounds != null) {
    context.aircraftService.getAllWithin(request.bounds, request.time)
  } else {
    context.aircraftService.getAll(request.time)
  }
  call.respond(states)
}
