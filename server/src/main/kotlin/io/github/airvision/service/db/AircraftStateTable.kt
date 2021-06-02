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
import io.github.airvision.exposed.epochSecond
import org.jetbrains.exposed.sql.Table

object AircraftStateTable : Table("aircraft_data") {
  val aircraftId = aircraftIcao24("aircraft_id").primaryKey(0)
  val callsign = varchar("callsign", 100).nullable()
  val time = epochSecond("time").primaryKey(1)
  val latitude = double("lat").nullable()
  val longitude = double("lon").nullable()
  val altitude = double("alt").nullable()
  val onGround = bool("on_ground")
  val velocity = double("ground_speed").nullable()
  val verticalRate = double("vertical_speed").nullable()
  val heading = double("heading").nullable()

  init {
    uniqueIndex(aircraftId, time)
  }
}
