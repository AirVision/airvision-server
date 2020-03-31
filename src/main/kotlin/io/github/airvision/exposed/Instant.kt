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

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import java.time.Instant

/**
 * Creates a [Column] that of [Instant]s where the value is seconds since epoch.
 */
fun Table.epochSecond(name: String): Column<Instant> {
  return long(name).transform(
      to = { value -> Instant.ofEpochSecond(value) },
      from = { value -> value.epochSecond })
}

/**
 * Creates a [Column] that of [Instant]s where the value is milliseconds since epoch.
 */
fun Table.epochMilli(name: String): Column<Instant> {
  return long(name).transform(
      to = { value -> Instant.ofEpochMilli(value) },
      from = { value -> value.toEpochMilli() })
}
