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

import arrow.core.None
import arrow.core.Option
import io.github.airvision.AircraftIcao24
import io.github.airvision.AirportIcao
import io.github.airvision.Waypoint
import java.time.Instant

interface AircraftFlightData : AircraftData {
  val arrivalAirport: AirportIcao?
  val departureAirport: AirportIcao?
  val estimatedArrivalTime: Option<Instant?>
  val flightNumber: Option<String?>
  val waypoints: Option<List<Waypoint>?>

  fun copy(
      aircraftId: AircraftIcao24 = this.aircraftId,
      time: Instant = this.time,
      arrivalAirport: AirportIcao? = this.arrivalAirport,
      departureAirport: AirportIcao? = this.departureAirport,
      estimatedArrivalTime: Option<Instant?> = this.estimatedArrivalTime,
      flightNumber: Option<String?> = this.flightNumber,
      waypoints: Option<List<Waypoint>?> = this.waypoints
  ): AircraftFlightData = SimpleAircraftFlightData(
      aircraftId, time, flightNumber, departureAirport, arrivalAirport, estimatedArrivalTime, waypoints)
}

data class SimpleAircraftFlightData(
    override val aircraftId: AircraftIcao24,
    override val time: Instant,
    override val flightNumber: Option<String?>,
    override val departureAirport: AirportIcao?,
    override val arrivalAirport: AirportIcao?,
    override val estimatedArrivalTime: Option<Instant?> = None,
    override val waypoints: Option<List<Waypoint>?> = None
) : AircraftFlightData
