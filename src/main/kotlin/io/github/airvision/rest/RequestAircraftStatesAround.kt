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
import io.github.airvision.GeodeticPosition
import io.ktor.application.call
import io.ktor.request.receive
import io.ktor.response.respond
import kotlinx.serialization.ContextualSerialization
import kotlinx.serialization.Serializable
import org.spongepowered.math.vector.Vector2d

// https://github.com/AirVision/airvision-server/wiki/Rest-API#request-aircrafts

@Serializable
data class AircraftStatesAroundRequest(
    val position: GeodeticPosition,
    @ContextualSerialization val size: Vector2d
)

suspend fun PipelineContext.handleAircraftStatesAroundRequest(context: RestContext) {
  val request = call.receive<AircraftStatesAroundRequest>()

  val position = request.position
  val size = request.size

  val bounds = GeodeticBounds.ofCenterAndSize(position, size)
  val states = context.aircraftStateService.getAllWithin(bounds)

  call.respond(states)
}
