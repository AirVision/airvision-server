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
import io.github.airvision.GeodeticPosition
import io.github.airvision.Waypoint
import io.github.airvision.service.AircraftFlightData
import io.github.airvision.service.AircraftStateData
import java.time.Instant

class Fr24AircraftData(
    override val aircraftId: AircraftIcao24,
    val id: String,
    override val time: Instant,
    override val flightNumber: Option<String?>,
    override val arrivalAirport: AirportIcao?,
    override val departureAirport: AirportIcao?,
    override val position: GeodeticPosition?,
    override val velocity: Double?,
    override val onGround: Boolean,
    override val verticalRate: Double?,
    override val heading: Double?,
    override val callsign: String?
) : AircraftFlightData, AircraftStateData {

  override val estimatedArrivalTime: Option<Instant?> get() = None
  override val waypoints: Option<List<Waypoint>?>  get() = None
}
