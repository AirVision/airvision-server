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

object AircraftManufacturerTable : IntIdTable("aircraft_manufacturer") {
  val code = varchar("code", 100)
  val name = varchar("name", 500)
  val country = varchar("country", 100).nullable()

  init {
    uniqueIndex(code)
  }
}
