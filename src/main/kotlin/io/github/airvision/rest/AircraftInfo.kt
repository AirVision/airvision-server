/*
 * AirVision
 *
 * Copyright (c) AirVision <https://www.github.com/AirVision>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
package io.github.airvision.rest

import io.github.airvision.GeodeticPosition
import io.github.airvision.AircraftIcao24
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AircraftInfo(
    val time: Int,
    val icao24: AircraftIcao24,
    val position: GeodeticPosition,
    val velocity: Double,
    @SerialName("on_ground") val onGround: Boolean,
    val index: Int? = null
)
