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

data class AircraftStateData(
  override val aircraftId: AircraftIcao24,
  override val time: Instant,
  val position: GeodeticPosition? = null,
  val velocity: Double? = null,
  val onGround: Boolean = false,
  val verticalRate: Double? = null,
  val heading: Double? = null,
  val callsign: String? = null
) : AircraftData
