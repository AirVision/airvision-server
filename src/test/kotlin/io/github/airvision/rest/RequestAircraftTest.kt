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

import io.github.airvision.Aircraft
import io.github.airvision.AircraftIcao24
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import org.junit.jupiter.api.Test

class RequestAircraftTest {

  @Test
  fun `get A808C4`() = testApp {
    val icao24 = AircraftIcao24.parse("A808C4")
    handleRequest(HttpMethod.Get, "/v1/aircraft") {
      addHeader(HttpHeaders.ContentType, "application/json")
      setBody("""{
        "icao24": "$icao24"
      }""".trimIndent())
    }.apply {
      val info = response.parse<Aircraft>()
      assert(info.isRight())
      info.fold({}, {
        assert(it.icao24 == icao24)
      })
    }
  }
}
