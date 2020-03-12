/*
 * AirVision
 *
 * Copyright (c) AirVision <https://www.github.com/AirVision>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
package io.github.airvision.rest.openskynetwork

import io.github.airvision.AircraftIcao
import io.github.airvision.rest.openskynetwork.serializer.OsnAircraftSerializer
import kotlinx.serialization.Serializable

@Serializable(with = OsnAircraftSerializer::class)
data class OsnAircraft(
    val icao: AircraftIcao,
    val callsign: String?,
    val originCountry: String,
    val timePosition: Int?,
    val lastContact: Int,
    val longitude: Float?,
    val latitude: Float?,
    val baroAltitude: Float?,
    val onGround: Boolean,
    val velocity: Float?,
    val trueTrack: Float?,
    val verticalRate: Float?,
    val sensors: IntArray?,
    val geoAltitude: Float?,
    val squawk: String?,
    val spi: Boolean,
    val positionSource: OsnPositionSource
)
