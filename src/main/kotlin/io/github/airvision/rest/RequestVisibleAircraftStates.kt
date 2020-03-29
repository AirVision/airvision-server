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

import io.github.airvision.AircraftState
import io.github.airvision.Camera
import io.github.airvision.EnuTransform
import io.github.airvision.GeodeticBounds
import io.github.airvision.GeodeticPosition
import io.github.airvision.toEcefPosition
import io.github.airvision.toEcefTransform
import io.github.airvision.toViewPosition
import io.github.airvision.util.vector.min
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
    val states: List<AircraftState?>
)

suspend fun PipelineContext.handleVisibleAircraftRequest(context: RestContext) {
  val request = call.receive<VisibleAircraftRequest>()

  // https://www.scratchapixel.com/lessons/3d-basic-rendering/perspective-and-orthographic-projection-matrix/building-basic-perspective-projection-matrix

  val enuRotation = request.rotation.let { Quaterniond.fromAxesAnglesRad(it.x, it.y, it.z) }
  val enuTransform = EnuTransform(request.position, enuRotation)

  val transform = enuTransform.toEcefTransform()

  val maxFov = Vector2d(179.0, 179.0)
  val fov = min(maxFov, request.fov)

  val camera = Camera.ofPerspective(fov)
      .withTransform(transform)

  val visibleAircraftConfig = context.config.visibleAircraft

  val bounds = GeodeticBounds.ofCenterAndSize(request.position,
      Vector2d(visibleAircraftConfig.range, visibleAircraftConfig.range))
  val possibleStates = context.aircraftService.getAllWithin(bounds)

  var states = tryMatch(camera, possibleStates, request.aircrafts)
  if (states.count { it != null } != request.aircrafts.size) {
    // Try again with different error margins, to see if we get better results
    val marginSize = 5.0
    fun tryWithMargin(xModifier: Double, yModifier: Double): Boolean {
      var cameraWithMargin = Camera.ofPerspective(fov)
          .withTransform(transform)
      cameraWithMargin = cameraWithMargin.rotate(
          Quaterniond.fromAngleDegAxis(marginSize / 2.0 * xModifier, cameraWithMargin.yAxis))
      cameraWithMargin = cameraWithMargin.rotate(
          Quaterniond.fromAngleDegAxis(marginSize / 2.0 * yModifier, cameraWithMargin.xAxis))

      val statesForMargin = tryMatch(cameraWithMargin, possibleStates, request.aircrafts)
      if (statesForMargin.count { it != null } != request.aircrafts.size)
        return false
      // A better result was found
      states = statesForMargin
      return true
    }
    tryWithMargin(+1.0, 0.0) ||
        tryWithMargin(-1.0, 0.0) ||
        tryWithMargin(0.0, +1.0) ||
        tryWithMargin(0.0, -1.0) ||
        tryWithMargin(+1.0, +1.0) ||
        tryWithMargin(+1.0, -1.0) ||
        tryWithMargin(-1.0, +1.0) ||
        tryWithMargin(-1.0, -1.0)
  }

  call.respond(VisibleAircraftResponse(listOf()))
}

fun tryMatch(camera: Camera, states: Collection<AircraftState>, aircrafts: List<ImageAircraft>): List<AircraftState?> {
  val closestInView = states
      .map { state ->
        val position = state.position?.toEcefPosition()
            ?: return@map null // Position not known
        val viewPosition = position.toViewPosition(camera)
            ?: return@map null // Not within the camera view
        Triple(position, viewPosition, state)
      }
      .filterNotNull()
      // Sort by the aircraft closest to the camera first
      .sortedBy { (position, _, _) -> camera.transform.position.distanceSquared(position) }

  val closestLimited = closestInView.take(aircrafts.size)
  return aircrafts.asSequence()
      .map { aircraft ->
        // TODO: Utilize size, if needed.
        val result = closestLimited
            .minBy { (_, viewPosition, _) -> viewPosition.distanceSquared(aircraft.position) }
        result?.third
      }
      .toList()
}
