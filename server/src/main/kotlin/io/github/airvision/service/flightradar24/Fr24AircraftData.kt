/*
 * AirVision
 *
 * Copyright (c) AirVision <https://www.github.com/AirVision>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
package io.github.airvision.service.flightradar24

import io.github.airvision.AircraftIcao24
import io.github.airvision.service.AircraftFlightData
import io.github.airvision.service.AircraftStateData

class Fr24AircraftData(
  val aircraftId: AircraftIcao24,
  val state: AircraftStateData,
  val flightId: String,
  val flight: AircraftFlightData
)
