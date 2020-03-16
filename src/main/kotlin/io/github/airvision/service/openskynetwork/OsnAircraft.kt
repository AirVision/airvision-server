/*
 * AirVision
 *
 * Copyright (c) AirVision <https://www.github.com/AirVision>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
package io.github.airvision.service.openskynetwork

import io.github.airvision.AircraftIcao24
import io.github.airvision.service.openskynetwork.serializer.OsnAircraftSerializer
import kotlinx.serialization.ContextualSerialization
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable(with = OsnAircraftSerializer::class)
data class OsnAircraft(
    val icao24: AircraftIcao24,
    val callsign: String?,
    val originCountry: String,
    @ContextualSerialization val timePosition: Instant?,
    @ContextualSerialization val lastContact: Instant,
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
