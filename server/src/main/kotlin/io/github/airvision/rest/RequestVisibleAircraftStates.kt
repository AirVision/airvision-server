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
import io.github.airvision.Transform
import io.github.airvision.toEcefPosition
import io.github.airvision.toEcefTransform
import io.github.airvision.toViewPosition
import io.github.airvision.util.ToStringHelper
import io.github.airvision.util.collections.poll
import io.github.airvision.util.math.max
import io.github.airvision.util.math.min
import io.github.airvision.util.math.component.x
import io.github.airvision.util.math.component.y
import io.ktor.application.call
import io.ktor.request.receive
import io.ktor.response.respond
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.spongepowered.math.imaginary.Quaterniond
import org.spongepowered.math.vector.Vector2d
import org.spongepowered.math.vector.Vector2i
import org.spongepowered.math.vector.Vector3d
import java.time.Instant
import kotlin.math.max
import kotlin.math.roundToInt

// https://github.com/AirVision/airvision-server/wiki/Rest-API#request-visible-aircraft

@Serializable
data class VisibleAircraftRequest(
  @Contextual val time: Instant,
  val position: GeodeticPosition,
  @Contextual val rotation: Quaterniond,
  @SerialName("rotation_accuracy") val rotationAccuracy: Double?,
  @Contextual val fov: Vector2d,
  @SerialName("aircrafts") val detections: List<Detection>
) {

  override fun toString() = ToStringHelper()
    .add("time", this.time.epochSecond)
    .add("position", this.position)
    .add("rotation", this.rotation)
    .add("fov", this.fov)
    .add("detections", this.detections.joinToString(separator = ",", prefix = "[", postfix = "]"))
    .toString()
}

@Serializable
data class Detection(
  @Contextual val position: Vector2d,
  @Contextual val size: Vector2d
)

@Serializable
data class VisibleAircraftResponse(
  val states: List<AircraftState?>
)

class NarrowResult(
  val fov: Vector2d,
  val transform: Transform,
  val detections: List<Detection>
)

private fun narrowCameraView(
  fov: Vector2d,
  transform: Transform,
  detections: Collection<Detection>
): NarrowResult {
  // Narrow the FOV to the aircraft that are actually visible in the image.
  // This should prevent some edge cases where a center aircraft is detected,
  // but there's an aircraft visible near the edge of the screen which wasn't
  // detected, but is closer.

  val margin = 0.1

  // The minimum size a detection is allowed to have
  val minDetectionSize = Vector2d(0.18, 0.18)

  fun Detection.min(): Vector2d = position.sub(max(minDetectionSize, size.div(2.0)))
  fun Detection.max(): Vector2d = position.add(max(minDetectionSize, size.div(2.0)))

  val minDetectionX = (detections.minOf { it.min().x } - margin).coerceIn(0.0, 1.0) - 0.5
  val minDetectionY = (detections.minOf { it.min().y } - margin).coerceIn(0.0, 1.0) - 0.5
  val maxDetectionX = (detections.maxOf { it.max().x } + margin).coerceIn(0.0, 1.0) - 0.5
  val maxDetectionY = (detections.maxOf { it.max().y } + margin).coerceIn(0.0, 1.0) - 0.5

  val factorX = maxDetectionX - minDetectionX
  val factorY = maxDetectionY - minDetectionY

  val cameraCenterOffsetX = (minDetectionX + maxDetectionX) / 2.0
  val cameraCenterOffsetY = (minDetectionY + maxDetectionY) / 2.0

  var newFov = fov.mul(factorX, factorY)
  // A FOV greater or equal than 180 degrees isn't supported.
  newFov = min(Vector2d(179.0, 179.0), newFov)

  // + is rotate up, - is rotate down
  val rotateFovXAxis = (fov.y / 2.0) * cameraCenterOffsetY
  // + is rotate left, - is rotate right
  val rotateFovYAxis = (fov.x / 2.0) * -cameraCenterOffsetX

  var rotation = transform.rotation

  val xAxis = rotation.rotate(Vector3d.UNIT_X)
  val yAxis = rotation.rotate(Vector3d.UNIT_Y)

  rotation = rotation.mul(Quaterniond.fromAngleDegAxis(rotateFovYAxis, yAxis))
  rotation = rotation.mul(Quaterniond.fromAngleDegAxis(rotateFovXAxis, xAxis))

  val newDetections = detections
    .map { detection ->
      detection.copy(
        position = detection.position.sub(minDetectionX, minDetectionY),
        size = detection.size.div(factorX, factorY)
      )
    }

  return NarrowResult(newFov, transform.copy(rotation = rotation), newDetections)
}

suspend fun PipelineContext.handleVisibleAircraftRequest(context: RestContext) {
  val request = call.receive<VisibleAircraftRequest>()

  // https://www.scratchapixel.com/lessons/3d-basic-rendering/perspective-and-orthographic-projection-matrix/building-basic-perspective-projection-matrix

  val enuTransform = EnuTransform(request.position, request.rotation)
  val narrow = narrowCameraView(request.fov, enuTransform.toEcefTransform(), request.detections)

  val fov = narrow.fov
  val detections = narrow.detections
  val transform = narrow.transform

  val camera = Camera.ofPerspective(fov).withTransform(transform)
  val visibleAircraftConfig = context.config.visibleAircraft

  val bounds = GeodeticBounds.ofCenterAndSize(
    request.position, Vector2d(visibleAircraftConfig.range, visibleAircraftConfig.range))
  val possibleStates = context.aircraftService.getAllWithin(bounds, request.time)

  var states = tryMatch(camera, possibleStates, detections)
  if (states.count { it != null } != detections.size) {
    // Try again with slight alterations to the camera orientation,
    // rotate the camera in different directions and check for better
    // results, e.g. 10 degrees up, down, left, right, up-left, etc.
    val alteration =
      if (request.rotationAccuracy == null) 10.0 else max(5.0, request.rotationAccuracy)

    fun tryWithAlteration(xModifier: Double, yModifier: Double): Boolean {
      var alteredCamera = Camera.ofPerspective(fov)
        .withTransform(transform)
      val xAxis = alteredCamera.xAxis
      val yAxis = alteredCamera.yAxis
      alteredCamera = alteredCamera.rotate(
        Quaterniond.fromAngleDegAxis(alteration * xModifier, yAxis)
      )
      alteredCamera = alteredCamera.rotate(
        Quaterniond.fromAngleDegAxis(alteration * yModifier, xAxis)
      )

      val statesForMargin = tryMatch(alteredCamera, possibleStates, detections)
      if (statesForMargin.count { it != null } != detections.size)
        return false
      // A better result was found
      states = statesForMargin
      return true
    }
    tryWithAlteration(+1.0, 0.0) ||
        tryWithAlteration(-1.0, 0.0) ||
        tryWithAlteration(0.0, +1.0) ||
        tryWithAlteration(0.0, -1.0) ||
        tryWithAlteration(+1.0, +1.0) ||
        tryWithAlteration(+1.0, -1.0) ||
        tryWithAlteration(-1.0, +1.0) ||
        tryWithAlteration(-1.0, -1.0)
  }
  call.respond(VisibleAircraftResponse(states))
}

fun tryMatch(
  camera: Camera,
  states: Collection<AircraftState>,
  aircrafts: List<Detection>
): List<AircraftState?> {
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
    .sortedBy { (position, _, _) -> camera.position.distanceSquared(position) }

  val closestMutable = closestInView.toMutableList()
  val used = mutableSetOf<AircraftState>()
  return aircrafts
    .withIndex()
    // We need to make groups for image sizes, sometimes, most most likely when
    // aircraft's are far away, the sizes will be almost the same, so we can no
    // longer depend on the distance, in this case we need to depend on the screen
    // positions. The groups make sure that the images are in the groups where the
    // size is more or less the same.
    .groupBy { entry ->
      val (_, aircraft) = entry

      // 0.0 to 1.0 range
      // group everything by difference sections in x and y
      val maxDifference = 0.1 // TODO: Adjust this value, if needed
      val position = aircraft.position

      val xGroup = (position.x / maxDifference).roundToInt()
      val yGroup = (position.y / maxDifference).roundToInt()
      Vector2i(xGroup, yGroup)
    }
    .asSequence()
    // The biggest images have priority, which means they're closer,
    // the closest are also first in the closestLimited list
    .sortedBy { (group, _) -> -(group.x * group.y) }
    .map { (_, entries) ->
      // In every group, the closest aircraft based on the screen position will be
      // used, when in different groups, the distance will be mainly used
      val closest = closestMutable.poll(entries.size)
      entries.map { entry ->
        val (index, aircraft) = entry
        val result = closest
          .asSequence()
          .sortedBy { (_, viewPosition, _) -> viewPosition.distanceSquared(aircraft.position) }
          // Only returns true the first time it was matched
          .filter { (_, _, state) -> used.add(state) }
          .firstOrNull()
        index to result?.third
      }
    }
    .flatten()
    // Bring back the original ordering
    .sortedBy { (index, _) -> index }
    // Extract only the state
    .map { (_, state) -> state }
    .toList()
}
