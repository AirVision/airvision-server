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
import io.github.airvision.AirportIcao
import io.github.airvision.GeodeticPosition
import io.github.airvision.service.AircraftStateData
import io.github.airvision.service.openskynetwork.serializer.OsnAircraftSerializer
import kotlinx.serialization.ContextualSerialization
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable(with = OsnAircraftSerializer::class)
data class OsnAircraftData(
    override val icao24: AircraftIcao24,
    override val callsign: String?,
    val originCountry: String,
    @ContextualSerialization val timePosition: Instant?,
    @ContextualSerialization override val time: Instant,
    val longitude: Float?,
    val latitude: Float?,
    val baroAltitude: Float?,
    override val onGround: Boolean,
    override val velocity: Double?,
    override val heading: Double?,
    override val verticalRate: Double?,
    val sensors: IntArray?,
    val geoAltitude: Float?,
    val squawk: String?,
    val spi: Boolean,
    val positionSource: OsnPositionSource
) : AircraftStateData {

  override val position: GeodeticPosition?
    get() = if (latitude != null && longitude != null) {
      val altitude = (baroAltitude ?: geoAltitude ?: 0.0).toDouble()
      GeodeticPosition(latitude.toDouble(), longitude.toDouble(), altitude)
    } else null
}
