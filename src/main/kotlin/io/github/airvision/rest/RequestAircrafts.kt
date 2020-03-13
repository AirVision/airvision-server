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
data class AircraftsRequest(
    val position: GeodeticPosition,
    @ContextualSerialization val size: Vector2d
)

suspend fun PipelineContext.handleAircraftsRequest(context: RestContext) {
  val request = call.receive<AircraftsRequest>()

  val position = request.position
  val size = request.size

  // TODO: Check whether OSK bounds supports like min=150 to max=-30

  var minLatitude = position.latitude - size.x / 2
  var maxLatitude = position.latitude + size.x / 2

  var minLongitude = position.longitude - size.y / 2
  var maxLongitude = position.longitude + size.y / 2

  if (minLatitude < -85)
    minLatitude += 85
  if (maxLatitude > 85)
    maxLatitude -= 85

  if (minLongitude < -180)
    minLongitude += 180
  if (maxLongitude > 180)
    maxLongitude -= 180

  val bounds = GeodeticBounds(
      GeodeticPosition(minLatitude, minLongitude),
      GeodeticPosition(maxLatitude, maxLongitude))

  val aircrafts = context.osn.getAircrafts(bounds)
  call.respond(aircrafts)
}
