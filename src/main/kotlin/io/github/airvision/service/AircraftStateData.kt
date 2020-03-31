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
import io.github.airvision.GeodeticPosition
import java.time.Instant

interface AircraftStateData : AircraftData {
  val position: GeodeticPosition?
  val velocity: Double?
  val onGround: Boolean
  val verticalRate: Double?
  val heading: Double?
  val callsign: String?

  fun withTime(time: Instant) = SimpleAircraftStateData(time, aircraftId, callsign,
      onGround, position, velocity, verticalRate, heading)
}

data class SimpleAircraftStateData(
    override val time: Instant,
    override val aircraftId: AircraftIcao24,
    override val callsign: String? = null,
    override val onGround: Boolean = false,
    override val position: GeodeticPosition? = null,
    override val velocity: Double? = null,
    override val verticalRate: Double? = null,
    override val heading: Double? = null
) : AircraftStateData
