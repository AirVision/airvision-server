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

import io.github.airvision.AircraftIcao
import io.ktor.application.call
import io.ktor.request.receive
import io.ktor.response.respond
import kotlinx.serialization.Serializable

// https://github.com/AirVision/airvision-server/wiki/Rest-API#request-aircrafts

@Serializable
data class AircraftsRequest(
    val position: AircraftIcao
)

suspend fun RestContext.handleAircraftsRequest() {
  val request = call.receive<AircraftRequest>()

  // TODO: Respond with something

  call.respond(error.notFound())
}
