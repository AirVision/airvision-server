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

import io.github.airvision.AircraftIcao24
import io.github.airvision.AirportIcao
import java.time.Instant

interface AircraftFlightData : AircraftData {
  val flightDestination: AirportIcao?
  val flightOrigin: AirportIcao?
}

data class SimpleAircraftFlightData(
    override val icao24: AircraftIcao24,
    override val time: Instant,
    override val flightOrigin: AirportIcao?,
    override val flightDestination: AirportIcao?
) : AircraftFlightData
