/*
 * AirVision
 *
 * Copyright (c) AirVision <https://www.github.com/AirVision>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
package io.github.airvision.rest.of

import io.github.airvision.service.openflights.OpenFlightsAirportService
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class OfTest {

  @Test
  fun `get all airports`() = runBlocking {
    val of = OpenFlightsAirportService()
    of.getAll().asSequence().take(10).forEach { println(it) }
  }
}
