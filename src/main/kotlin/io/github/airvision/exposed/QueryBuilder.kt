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

import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.QueryBuilder

fun QueryBuilder.appendJoined(expressions: Iterable<Expression<*>>, separator: String = ", ") {
  val it = expressions.iterator()
  if (!it.hasNext())
    return
  append(it.next())
  while (it.hasNext()) {
    append(separator)
    append(it)
  }
}
