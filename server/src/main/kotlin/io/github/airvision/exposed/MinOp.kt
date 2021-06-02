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
import org.jetbrains.exposed.sql.ExpressionWithColumnType
import org.jetbrains.exposed.sql.IColumnType
import org.jetbrains.exposed.sql.QueryBuilder

private class MinOp<T>(
  private val expressions: List<Expression<T>>,
  override val columnType: IColumnType
) : ExpressionWithColumnType<T>() {

  override fun toQueryBuilder(queryBuilder: QueryBuilder) = queryBuilder {
    append("LEAST(")
    expressions.appendTo { +it }
    append(')')
  }
}

fun <T> min(
  first: ExpressionWithColumnType<T>,
  second: ExpressionWithColumnType<T>,
  vararg more: ExpressionWithColumnType<T>
): ExpressionWithColumnType<T> = MinOp(listOf(first, second) + more.toList(), first.columnType)
