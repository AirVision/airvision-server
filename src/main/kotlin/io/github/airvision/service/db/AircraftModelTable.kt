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

object AircraftModelTable : Table("aircraft_model") {
  val id = integer("id").autoIncrement().primaryKey()
  val icao24 = integer("icao24")
  // L1P, L2P, ... includes engine type, engine count, aircraft type
  val description = varchar("desc", 10)
  // The name of the engine
  val engine = varchar("engine", 100).nullable()
  // The manufacturer, if known
  val manufacturer = (integer("manf_id") references AircraftManufacturerTable.id).nullable()

  init {
    uniqueIndex(icao24)
  }
}
