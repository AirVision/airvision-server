/*
 * AirVision
 *
 * Copyright (c) LanternPowered <https://www.github.com/AirVision>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
package io.github.airvision.rest

import io.github.airvision.GeodeticPosition
import io.github.airvision.Icao24
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// https://github.com/AirVision/airvision-server/wiki/Rest-API#request-aircraft-trajectory

@Serializable
data class AircraftTrajectoryRequest(
    val icao24: Icao24
)

@Serializable
data class AircraftTrajectoryResponse(
    val icao24: Icao24,
    val points: List<TrajectoryPoint>
)

@Serializable
data class TrajectoryPoint(
    val time: Int,
    val position: GeodeticPosition,
    @SerialName("on_ground") val onGround: Boolean
)

suspend fun RestContext.handleAircraftTrajectoryRequest() {
  val request = call.receive<AircraftTrajectoryRequest>()

  // TODO: Respond with something

  call.respond(error.notFound())
}
