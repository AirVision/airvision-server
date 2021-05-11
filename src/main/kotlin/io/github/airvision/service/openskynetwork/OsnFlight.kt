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
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class OsnFlight(
    val icao24: AircraftIcao24,
    @Contextual val firstSeen: Instant,
    val estDepartureAirport: AirportIcao?,
    @Contextual val lastSeen: Instant,
    val estArrivalAirport: AirportIcao?,
    val callsign: String?,
    val estDepartureAirportHorizDistance: Int,
    val estDepartureAirportVertDistance: Int,
    val estArrivalAirportHorizDistance: Int,
    val estArrivalAirportVertDistance: Int,
    val departureAirportCandidatesCount: Int,
    val arrivalAirportCandidatesCount: Int
)
