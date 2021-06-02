/*
 * AirVision
 *
 * Copyright (c) AirVision <https://www.github.com/AirVision>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
package io.github.airvision.exposed

import io.github.airvision.AircraftIcao24
import io.github.airvision.AirportIcao
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table

fun Table.airportIcao(name: String): Column<AirportIcao> {
  return varchar(name, AirportIcao.MaxLength).transform(
    to = { value -> AirportIcao(value) },
    from = { value -> value.icao })
}

fun Table.aircraftIcao24(name: String): Column<AircraftIcao24> {
  return integer(name).transform(
    to = { value -> AircraftIcao24(value) },
    from = { value -> value.address })
}
