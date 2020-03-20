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
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder

fun Query.orderBy(vararg fns: SqlExpressionBuilder.() -> Expression<*>): Query {
  return orderBy(SortOrder.ASC, fns.asList())
}

fun Query.orderBy(order: SortOrder = SortOrder.ASC, vararg fns: SqlExpressionBuilder.() -> Expression<*>): Query {
  return orderBy(order, fns.asList())
}

fun Query.orderBy(order: SortOrder = SortOrder.ASC, fns: Iterable<SqlExpressionBuilder.() -> Expression<*>>): Query {
  val expressions = fns.map { it(SqlExpressionBuilder) to order }.toTypedArray()
  return orderBy(*expressions)
}

fun Query.orderBy(order: SortOrder = SortOrder.ASC, fn: SqlExpressionBuilder.() -> Expression<*>): Query {
  return orderBy(fn(SqlExpressionBuilder), order)
}
