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
import io.github.airvision.AircraftIcao
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.stringify
import org.junit.jupiter.api.Test

@ImplicitReflectionSerializer
class RequestAircraftTest {

  @Test
  fun `get 0xa092fe`() = testApp {
    val icao = AircraftIcao.parse("a092fe")
    handleRequest(HttpMethod.Get, "/v1/aircraft") {
      addHeader(HttpHeaders.ContentType, "application/json")
      setBody(AirVision.json.stringify(AircraftRequest(icao)))
    }.apply {
      println(response.content)
    }
  }
}
