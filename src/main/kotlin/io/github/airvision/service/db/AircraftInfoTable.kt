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

import io.github.airvision.AircraftInfo
import io.github.airvision.WeightCategory
import io.github.airvision.exposed.aircraftIcao24
import org.jetbrains.exposed.sql.Table

object AircraftInfoTable : Table("aircraft_info") {
  val aircraftId = aircraftIcao24("aicraft_id")
  val model = varchar("model", 500)
  // The type, can overlap with description
  val type = enumeration("type", AircraftInfo.Type::class).nullable()
  // L1P, L2P, ... includes engine type, engine count, aircraft type
  val description = varchar("description", 10).nullable()
  // The name of the engine
  val engineName = varchar("engine_name", 1000).nullable()
  // The amount of engines
  val engineCount = integer("engine_count").nullable()
  // The manufacturer, if known
  val manufacturer = reference("manufacturer_id", AircraftManufacturerTable).nullable()
  // The owner
  val owner = varchar("owner", 1000).nullable()
  // The weight category
  val weightCategory = enumeration("weight_category", WeightCategory::class).nullable()

  init {
    uniqueIndex(aircraftId)
  }
}
