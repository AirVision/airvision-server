/*
 * AirVision
 *
 * Copyright (c) AirVision <https://www.github.com/AirVision>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
package io.github.airvision.service

import io.github.airvision.Aircraft
import io.github.airvision.AircraftIcao24
import io.github.airvision.GeodeticBounds
import java.time.Instant

interface AircraftService {

  suspend fun getAll(time: Instant? = null): List<Aircraft>

  suspend fun getAllWithin(bounds: GeodeticBounds, time: Instant? = null): List<Aircraft>

  suspend fun get(icao24: AircraftIcao24, time: Instant? = null): Aircraft?
}
