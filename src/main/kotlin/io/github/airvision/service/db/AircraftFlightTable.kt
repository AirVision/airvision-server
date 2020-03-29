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

import org.jetbrains.exposed.sql.Table

object AircraftFlightTable : Table("aircraft_flight") {
  val icao24 = integer("icao24").primaryKey()
  val time = long("time")
  val origin = varchar("origin", 30).nullable()
  val destination = varchar("destination", 30).nullable()

  init {
    uniqueIndex(icao24)
  }
}
