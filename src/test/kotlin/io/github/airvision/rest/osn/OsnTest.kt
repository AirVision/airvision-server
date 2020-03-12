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

import io.github.airvision.AircraftIcao
import io.github.airvision.rest.openskynetwork.OpenSkyNetwork
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
    val icao = AircraftIcao.parse("A808C4")
    println(osn.getAircraft(icao))
  }
}
