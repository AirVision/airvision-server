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

import arrow.core.Option
import io.github.airvision.AircraftIcao24
import io.github.airvision.AirportIcao
import io.github.airvision.Waypoint
import java.time.Instant

data class AircraftFlightData(
    override val aircraftId: AircraftIcao24,
    override val time: Instant,
    val departureAirport: AirportIcao?,
    val departureTime: Option<Instant?>,
    val arrivalAirport: AirportIcao?,
    val estimatedArrivalTime: Option<Instant?>,
    val flightNumber: Option<String?>,
    val waypoints: Option<List<Waypoint>?>
) : AircraftData
