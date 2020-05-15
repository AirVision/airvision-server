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

import io.github.airvision.AirVision
import io.github.airvision.AircraftState
import io.github.airvision.Camera
import io.github.airvision.EnuTransform
import io.github.airvision.GeodeticBounds
import io.github.airvision.GeodeticPosition
import io.github.airvision.toEcefPosition
import io.github.airvision.toEcefTransform
import io.github.airvision.toViewPosition
import io.github.airvision.util.ToStringHelper
import io.github.airvision.util.collections.poll
import io.github.airvision.util.math.radToDeg
import io.github.airvision.util.math.min
import io.ktor.application.call
import io.ktor.request.receive
import io.ktor.response.respond
import kotlinx.serialization.ContextualSerialization
import kotlinx.serialization.Serializable
import org.spongepowered.math.imaginary.Quaterniond
import org.spongepowered.math.vector.Vector2d
import org.spongepowered.math.vector.Vector2i
import java.time.Instant
import kotlin.math.acos
import kotlin.math.roundToInt

// https://github.com/AirVision/airvision-server/wiki/Rest-API#request-visible-aircraft

@Serializable
data class VisibleAircraftRequest(
    @ContextualSerialization val time: Instant,
    val position: GeodeticPosition,
    @ContextualSerialization val rotation: Quaterniond,
    @ContextualSerialization val fov: Vector2d,
    val aircrafts: List<ImageAircraft>
) {

  override fun toString() = ToStringHelper()
      .add("time", this.time.epochSecond)
      .add("position", this.position)
      .add("rotation", this.rotation)
      .add("fov", this.fov)
      .add("aircrafts", this.aircrafts.joinToString(separator = ",", prefix = "[", postfix = "]"))
      .toString()
}

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

  val enuTransform = EnuTransform(request.position, request.rotation)
  val transform = enuTransform.toEcefTransform()

  val maxFov = Vector2d(179.0, 179.0)
  val fov = min(maxFov, request.fov)

  val camera = Camera.ofPerspective(fov)
      .withTransform(transform)

  val visibleAircraftConfig = context.config.visibleAircraft

  val bounds = GeodeticBounds.ofCenterAndSize(request.position,
      Vector2d(visibleAircraftConfig.range, visibleAircraftConfig.range))
  AirVision.logger.debug("Bounds: $bounds")
  val possibleStates = context.aircraftService.getAllWithin(bounds, request.time)
      .also {
        AirVision.logger.debug("Candidates: ${it.size}")
        it.forEach { state ->
          if (state.icao24.address > 0xffff00) {
            AirVision.logger.debug("   Test Candidate: $state")
          }
        }
      }

  var states = tryMatch(camera, possibleStates, request.aircrafts)
  if (states.count { it != null } != request.aircrafts.size) {
    // Try again with slight alterations to the camera orientation,
    // rotate the camera in different directions and check for better
    // results, e.g. 5 degrees up, down, left, right, up-left, etc.
    val alteration = 5.0
    fun tryWithAlteration(xModifier: Double, yModifier: Double): Boolean {
      var alteredCamera = Camera.ofPerspective(fov)
          .withTransform(transform)
      val xAxis = alteredCamera.xAxis
      val yAxis = alteredCamera.yAxis
      alteredCamera = alteredCamera.rotate(
          Quaterniond.fromAngleDegAxis(alteration * xModifier, yAxis))
      alteredCamera = alteredCamera.rotate(
          Quaterniond.fromAngleDegAxis(alteration * yModifier, xAxis))

      val statesForMargin = tryMatch(alteredCamera, possibleStates, request.aircrafts)
      if (statesForMargin.count { it != null } != request.aircrafts.size)
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

fun tryMatch(camera: Camera, states: Collection<AircraftState>, aircrafts: List<ImageAircraft>): List<AircraftState?> {
  val closestInView = states
      .map { state ->
        val position = state.position?.toEcefPosition()
            ?: return@map null // Position not known
        if (state.icao24.address > 0xffff00) {
          AirVision.logger.debug("Test Aircraft in area: $state")

          val cameraDir = camera.zAxis.negate()
          println("    Camera dir: $cameraDir")
          val aircraftRelative = position.sub(camera.position)
          println("    Aircraft distance: ${aircraftRelative.length()}")
          val aircraftDir = position.sub(camera.position).normalize()
          println("    Aircraft dir: $aircraftDir")
          val rotation = Quaterniond.fromRotationTo(cameraDir, aircraftDir).normalize()
          val angle = radToDeg(2 * acos(rotation.w))
          AirVision.logger.info("  -> Angle: $angle")
        }
        val viewPosition = position.toViewPosition(camera)
            ?: return@map null // Not within the camera view
        if (state.icao24.address > 0xffff00) {
          AirVision.logger.debug("Test Aircraft in view: $viewPosition")
        }
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
