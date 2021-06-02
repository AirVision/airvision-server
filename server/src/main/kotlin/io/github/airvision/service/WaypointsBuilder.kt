/*
 * AirVision
 *
 * Copyright (c) AirVision <https://www.github.com/AirVision>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
package io.github.airvision.service

import io.github.airvision.Waypoint
import io.github.airvision.util.arrow.ifSome
import io.github.airvision.util.time.minus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import kotlin.math.abs
import kotlin.time.Duration

class WaypointsBuilder {

  private val builtWaypoints: MutableList<Waypoint> = mutableListOf()
  private var lastAddedState: AircraftStateData? = null
  private var lastState: AircraftStateData? = null
  private var departureWaypoint: Waypoint? = null
  private var departureTime: Instant? = null
  private var cachedWaypoints: MutableList<Waypoint>? = null
  private var prependedExternalWaypoints = false

  private val mutex = Mutex()

  /**
   * Gets the current list of [Waypoint]s.
   */
  suspend fun getWaypoints(): List<Waypoint>? {
    return mutex.withLock {
      var waypoints = cachedWaypoints
      if (waypoints == null) {
        waypoints = builtWaypoints.toMutableList()
        // Always add the departure airport, even if data from an external source
        // was prepended, this prevents issues in the case that the external source
        // provides incomplete data.
        val departureWaypoint = this.departureWaypoint
        if (departureWaypoint != null)
          waypoints.add(0, departureWaypoint)
        val lastState = this.lastState
        if (lastState?.position != null && lastState.position != waypoints.last().position)
          waypoints.add(Waypoint(lastState.time, lastState.position))
        cachedWaypoints = waypoints
      }
      // Also ignore a size of 1, you can't make
      // a path with just one point
      if (waypoints.size <= 1) null else waypoints
    }
  }

  /**
   * Appends aircraft state data.
   */
  suspend fun append(state: AircraftStateData) {
    mutex.withLock {
      doAppend(state)
    }
  }

  /**
   * Appends aircraft flight data.
   */
  suspend fun append(flight: AircraftFlightData, airportService: AirportService) {
    mutex.withLock {
      doAppend(flight, airportService)
    }
  }

  private suspend fun doAppend(flight: AircraftFlightData, airportService: AirportService) {
    if (lastState == null)
      return
    doAppendStartAirport(flight, airportService)
    flight.waypoints.ifSome { waypoints ->
      if (waypoints != null)
        doAppendWaypoints(waypoints)
    }
  }

  /**
   * Attempts to add external waypoints before the ones that are already
   * calculated by ourselves, in case the server gets restarted.
   */
  private fun doAppendWaypoints(waypoints: List<Waypoint>) {
    if (prependedExternalWaypoints)
      return
    val time = this.builtWaypoints.firstOrNull()?.time ?: Instant.now()
    val waypointsToAdd = waypoints
      .takeWhile { waypoint -> waypoint.time < time }
    builtWaypoints.addAll(0, waypointsToAdd)
    prependedExternalWaypoints = true
    cachedWaypoints = null
  }

  /**
   * Adds the start airport position, if needed.
   */
  private suspend fun doAppendStartAirport(
    flight: AircraftFlightData,
    airportService: AirportService
  ) {
    if (departureWaypoint != null && departureTime != null)
      return
    val airport = flight.departureAirport?.let { airportService.get(it) } ?: return
    val departureTime = flight.departureTime.orNull()
    val time = departureTime ?: builtWaypoints.firstOrNull()?.time ?: return
    departureWaypoint = Waypoint(time, airport.position)
    this.departureTime = departureTime
    cachedWaypoints = null
  }

  private fun reset() {
    lastState = null
    lastAddedState = null
    builtWaypoints.clear()
    cachedWaypoints = null
    departureWaypoint = null
    departureTime = null
    prependedExternalWaypoints = false
  }

  /**
   * Adds the aircraft state.
   */
  private fun doAppend(state: AircraftStateData) {
    if (state.onGround)
      return reset()

    // The same rules as the OpenSky Network apply

    // - The first point is set immediately after the aircraft’s expected departure, or after
    //   the network received the first position when the aircraft entered its reception range.
    // - The last point is set right before the aircraft’s expected arrival, or the aircraft
    //   left the networks reception range.
    // - There is a waypoint at least every 15 minutes when the aircraft is in-flight.
    // - A waypoint is added if the aircraft changes its track more than 2.5°.
    // - A waypoint is added if the aircraft changes altitude by more than 100m (~330ft).

    lastState = state
    cachedWaypoints = null

    val lastAddedState = this.lastAddedState
    if (lastAddedState == null ||
      (state.time - lastAddedState.time > Duration.minutes(15)) ||
      (state.heading != null && lastAddedState.heading != null &&
          abs(state.heading - lastAddedState.heading) > 2.5) ||
      (state.position != null && lastAddedState.position != null &&
          abs(state.position.altitude - lastAddedState.position.altitude) > 100)
    ) {
      val position = state.position ?: lastAddedState?.position
      if (position != null) {
        // If one of the conditions is met, a new point will be added to the waypoints
        val waypoint = Waypoint(state.time, position)
        builtWaypoints += waypoint
      }
      this.lastAddedState = state
    }
  }
}
