/*
 * AirVision
 *
 * Copyright (c) AirVision <https://www.github.com/AirVision>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
package io.github.airvision.service.db

import io.github.airvision.exposed.aircraftIcao24
import io.github.airvision.exposed.airportIcao
import io.github.airvision.exposed.epochSecond
import org.jetbrains.exposed.sql.Table

object AircraftFlightTable : Table("aircraft_flight") {
  val aircraftId = aircraftIcao24("aircraft_id").primaryKey()
  val time = epochSecond("time")
  val number = varchar("number", 30).nullable()
  val departureAirport = airportIcao("origin").nullable()
  val departureTime = epochSecond("departure_time").nullable()
  val arrivalAirport = airportIcao("destination").nullable()
  val estimatedArrivalTime = epochSecond("est_arrival_time").nullable()

  init {
    uniqueIndex(aircraftId)
  }
}
