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

object AircraftManufacturerTable : Table("aircraft_manufacturer") {
  val id = integer("id").autoIncrement().primaryKey()
  val code = varchar("code", 30).nullable()
  val name = varchar("name", 100)
  val country = varchar("country", 100).nullable()
}
