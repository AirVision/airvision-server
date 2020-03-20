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
import org.jetbrains.exposed.sql.FieldSet
import org.jetbrains.exposed.sql.IColumnType
import org.jetbrains.exposed.sql.QueryBuilder
import org.jetbrains.exposed.sql.Table

private class DistinctOn<T>(
    private val expressions: Collection<ExpressionWithColumnType<*>>
) : ExpressionWithColumnType<T>() {

  override val columnType: IColumnType = expressions.first().columnType

  override fun toQueryBuilder(queryBuilder: QueryBuilder) = queryBuilder {
    append("DISTINCT ON (")
    appendJoined(expressions)
    append(") ")
    append(expressions.first())
  }
}

/**
 * Creates a [FieldSet] where entries will be unique based on the columns.
 *
 * Only supported by a postgres database.
 */
fun Table.distinctBy(first: ExpressionWithColumnType<*>, vararg more: ExpressionWithColumnType<*>): FieldSet =
    distinctBy(listOf(first) + more.asList())

/**
 * Creates a [FieldSet] where entries will be unique based on the [columns].
 *
 * Only supported by a postgres database.
 */
fun Table.distinctBy(columns: Collection<ExpressionWithColumnType<*>>): FieldSet {
  if (columns.isEmpty())
    return slice(this.columns)
  val firstColumn = columns.first()
  val sliceColumns = mutableListOf<Expression<*>>()
  sliceColumns.add(DistinctOn<Any>(columns))
  for (column in columns) {
    if (column != firstColumn)
      sliceColumns += column
  }
  return slice(sliceColumns)
}
