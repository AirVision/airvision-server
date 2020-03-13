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

import io.github.airvision.Camera
import io.github.airvision.EnuTransform
import io.github.airvision.GeodeticPosition
import io.github.airvision.toEcefTransform
import io.ktor.application.call
import io.ktor.request.receive
import io.ktor.response.respond
import kotlinx.serialization.ContextualSerialization
import kotlinx.serialization.Serializable
import org.spongepowered.math.imaginary.Quaterniond
import org.spongepowered.math.vector.Vector2d
import org.spongepowered.math.vector.Vector3d
import java.time.Instant

// https://github.com/AirVision/airvision-server/wiki/Rest-API#request-visible-aircraft

@Serializable
data class VisibleAircraftRequest(
    @ContextualSerialization val time: Instant,
    val position: GeodeticPosition,
    @ContextualSerialization val rotation: Vector3d,
    @ContextualSerialization val fov: Vector2d,
    val aircrafts: List<ImageAircraft>
)

@Serializable
data class ImageAircraft(
    @ContextualSerialization val position: Vector2d,
    @ContextualSerialization val size: Vector2d
)

@Serializable
data class VisibleAircraftResponse(
    val aircrafts: List<AircraftInfo>
)

suspend fun PipelineContext.handleVisibleAircraftRequest(context: RestContext) {
  val request = call.receive<VisibleAircraftRequest>()

  // https://www.scratchapixel.com/lessons/3d-basic-rendering/perspective-and-orthographic-projection-matrix/building-basic-perspective-projection-matrix
  // TODO: rotation must be modified based on Figure 3 orientation
  val enuRotation = request.rotation.let { Quaterniond.fromAxesAnglesRad(it.x, it.y, it.z) }
  val enuTransform = EnuTransform(request.position, enuRotation)

  val transform = enuTransform.toEcefTransform()
  val camera = Camera.ofPerspective(request.fov).withTransform(transform)

  // TODO: Analyse things, and respond with something

  call.respond(VisibleAircraftResponse(listOf()))
}
