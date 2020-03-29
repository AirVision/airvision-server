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

object AircraftStateTable : Table("aircraft_data") {
  val icao24 = integer("icao24").primaryKey(0)
  val callsign = varchar("callsign", 100).nullable()
  val time = long("time").primaryKey(1)
  val latitude = double("lat").nullable()
  val longitude = double("lon").nullable()
  val altitude = double("alt").nullable()
  val onGround = bool("on_ground")
  val velocity = double("velocity").nullable()
  val verticalRate = double("vert_rate").nullable()
  val heading = double("heading").nullable()

  init {
    uniqueIndex(icao24, time)
  }
}
