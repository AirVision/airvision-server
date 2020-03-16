/*
 * AirVision
 *
 * Copyright (c) AirVision <https://www.github.com/AirVision>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
package io.github.airvision.rest.osn

import io.github.airvision.AircraftIcao24
import io.github.airvision.GeodeticBounds
import io.github.airvision.GeodeticPosition
import io.github.airvision.service.openskynetwork.OpenSkyNetwork
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class OsnTest {

  private val osn = OpenSkyNetwork()

  // @Test
  fun `get all aircrafts`() = runBlocking {
    osn.getAircrafts().asSequence().take(10).forEach { println(it) }
  }

  @Test
  fun `get aircraft`() = runBlocking {
    val icao = AircraftIcao24.parse("A808C4")
    println(osn.getAircraft(icao))
  }

  @Test
  fun `get aircraft's in bounds`(): Unit = runBlocking {
    val bounds = GeodeticBounds(
        min = GeodeticPosition(-26.0, -47.0),
        max = GeodeticPosition(-18.0, -38.0)
    )
    // https://opensky-network.org/api/states/all?lamin=-26.0&lamax=-18.0&lomin=-47.0&lomax=-38.0
    val aircrafts = osn.getAircrafts(bounds)
    aircrafts.forEach { println(it) }
  }
}
