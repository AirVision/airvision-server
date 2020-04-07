/*
 * AirVision
 *
 * Copyright (c) AirVision <https://www.github.com/AirVision>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
package io.github.airvision.service.flightradar24

import arrow.core.None
import arrow.core.Option
import io.github.airvision.AircraftIcao24
import io.github.airvision.AirportIcao
import io.github.airvision.Waypoint
import io.github.airvision.service.AircraftFlightData
import java.time.Instant

class FrAircraftFlightData(
    override val aircraftId: AircraftIcao24,
    val id: String,
    override val time: Instant,
    override val number: Option<String?>,
    override val arrivalAirport: AirportIcao?,
    override val departureAirport: AirportIcao?,
    override val estimatedArrivalTime: Option<Instant?> = None,
    override val waypoints: Option<List<Waypoint>?> = None
) : AircraftFlightData
