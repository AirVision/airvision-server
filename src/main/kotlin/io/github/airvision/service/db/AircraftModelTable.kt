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

import org.jetbrains.exposed.dao.IntIdTable

object AircraftModelTable : IntIdTable("aircraft_model") {
  val icao24 = integer("icao24")
  val name = varchar("name", 500)
  // L1P, L2P, ... includes engine type, engine count, aircraft type
  val description = varchar("type", 10).nullable()
  // The name of the engine
  val engines = varchar("engine", 1000).nullable()
  // The manufacturer, if known
  val manufacturer = reference("manufacturer", AircraftManufacturerTable).nullable()
  // The owner
  val owner = varchar("owner", 1000).nullable()

  init {
    uniqueIndex(icao24)
  }
}
