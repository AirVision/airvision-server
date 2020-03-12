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
import io.github.airvision.Icao24
import io.ktor.application.call
import io.ktor.request.receive
import io.ktor.response.respond
import kotlinx.serialization.Serializable

// https://github.com/AirVision/airvision-server/wiki/Rest-API#request-aircraft

@Serializable
data class AircraftRequest(
    val icao24: Icao24
)

suspend fun RestContext.handleAircraftRequest() {
  val request = call.receive<AircraftRequest>()

  // TODO: Respond with something

  // call.respond(error.badRequest())
  call.respond(AircraftInfo(0, Icao24(0x052f),
      GeodeticPosition(0.0, 0.0, 0.0), 0.0, true))
}
