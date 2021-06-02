/*
 * AirVision
 *
 * Copyright (c) AirVision <https://www.github.com/AirVision>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
package io.github.airvision

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Represents a snapshot of data of an aircraft.
 *
 * @property time The time at which the data was collected
 * @property icao24 The ICAO24 identifier
 * @property position The position, if known
 * @property velocity The velocity in meters per second, if known
 * @property verticalRate The vertical rate in meters per second, if known
 */
@Serializable
data class AircraftState(
  @Contextual val time: Instant,
  val icao24: AircraftIcao24,
  val position: GeodeticPosition? = null,
  val velocity: Double? = null,
  @SerialName("vertical_rate") val verticalRate: Double? = null,
  val heading: Double? = null,
  @SerialName("weight_category") val weightCategory: WeightCategory? = null
)
