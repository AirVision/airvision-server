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

import io.github.airvision.GeodeticPosition
import io.github.airvision.AircraftIcao24
import io.github.airvision.Airport
import io.ktor.application.call
import io.ktor.request.receive
import io.ktor.response.respond
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.ContextualSerialization
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

// https://github.com/AirVision/airvision-server/wiki/Rest-API#request-aircraft-flight

@Serializable
data class AircraftTrajectoryRequest(
    val icao24: AircraftIcao24
)

@Serializable
data class AircraftFlightResponse(
    val icao24: AircraftIcao24,
    @SerialName("departure_airport") val departureAirport: Airport?,
    @SerialName("arrival_airport") val arrivalAirport: Airport?,
    @ContextualSerialization @SerialName("estimated_arrival_time") val estimatedArrivalTime: Instant?,
    val track: List<Waypoint>?
)

@Serializable
data class Waypoint(
    @ContextualSerialization val time: Instant,
    val position: GeodeticPosition,
    @SerialName("on_ground") val onGround: Boolean
)

suspend fun PipelineContext.handleAircraftTrajectoryRequest(context: RestContext) {
  val request = call.receive<AircraftTrajectoryRequest>()
  val icao24 = request.icao24

  val response = coroutineScope {
    val trackTask = async { context.osn.getTrack(icao24) }
    val flightTask = async { context.osn.getFlight(icao24) }

    val flight = flightTask.await() ?: return@coroutineScope error.notFound("Couldn't find a flight for $icao24")

    val arrivalAirportTask = if (flight.estArrivalAirport != null) {
      async { context.airports.get(flight.estArrivalAirport) }
    } else null

    val departureAirportTask = if (flight.estDepartureAirport != null) {
      async { context.airports.get(flight.estDepartureAirport) }
    } else null

    val track = trackTask.await()
    val arrivalAirport = arrivalAirportTask?.await()
    val departureAirport = departureAirportTask?.await()
    val estimatedArrivalTime = flight.lastSeen

    val waypoints = if (track != null) {
      var latitude = track.path.firstOrNull { it.latitude != null }?.latitude
      var longitude = track.path.firstOrNull { it.longitude != null }?.longitude
      var altitude = track.path.firstOrNull { it.baroAltitude != null }?.baroAltitude ?: 0.0
      if (latitude == null || longitude == null) {
        // No latitude or longitude is known?
        null
      } else {
        val waypoints = mutableListOf<Waypoint>()
        for (waypoint in track.path) {
          if (waypoint.latitude == null && waypoint.longitude == null)
            continue
          if (waypoint.latitude != null)
            latitude = waypoint.latitude
          if (waypoint.longitude != null)
            longitude = waypoint.longitude
          if (waypoint.baroAltitude != null)
            altitude = waypoint.baroAltitude
          val position = GeodeticPosition(latitude!!.toDouble(), longitude!!.toDouble(), altitude.toDouble())
          waypoints += Waypoint(waypoint.time, position, waypoint.onGround)
        }
        waypoints
      }
    } else null

    AircraftFlightResponse(icao24 = icao24, arrivalAirport = arrivalAirport, departureAirport = departureAirport,
        estimatedArrivalTime = estimatedArrivalTime, track = waypoints)
  }

  call.respond(response)
}
