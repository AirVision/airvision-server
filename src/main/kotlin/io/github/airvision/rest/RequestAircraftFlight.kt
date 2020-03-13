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
import kotlinx.serialization.ContextualSerialization
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

// https://github.com/AirVision/airvision-server/wiki/Rest-API#request-aircraft-trajectory

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

  // TODO: Respond with something

  call.respond(error.notFound())
}
