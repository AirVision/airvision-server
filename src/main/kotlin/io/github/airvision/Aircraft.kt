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

import kotlinx.serialization.ContextualSerialization
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Represents a snapshot of data of an aircraft.
 *
 * @property time The time at which the data was collected
 * @property icao24 The ICAO24 identifier
 * @property position The position, if known
 * @property velocity The velocity, if known
 * @property onGround Whether the aircraft is on the ground
 * @property index Only used for rest responses
 */
@Serializable
data class Aircraft(
    @ContextualSerialization val time: Instant,
    val icao24: AircraftIcao24,
    val position: GeodeticPosition?,
    val velocity: Double?,
    @SerialName("on_ground") val onGround: Boolean,
    val index: Int? = null
) {

  fun withIndex(index: Int) = copy(index = index)
}
