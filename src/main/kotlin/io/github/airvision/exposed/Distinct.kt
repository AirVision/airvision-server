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

import org.jetbrains.exposed.sql.ExpressionWithColumnType
import org.jetbrains.exposed.sql.FieldSet
import org.jetbrains.exposed.sql.IColumnType
import org.jetbrains.exposed.sql.IntegerColumnType
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.QueryBuilder
import org.jetbrains.exposed.sql.Slice
import org.jetbrains.exposed.sql.SortOrder

/**
 * Creates a [FieldSet] where entries will be unique based on the columns.
 *
 * Only supported by a postgres database.
 */
fun Query.distinctBy(first: ExpressionWithColumnType<*>, vararg more: ExpressionWithColumnType<*>): Query =
    distinctBy(listOf(first to SortOrder.ASC) + more.asList().map { it to SortOrder.ASC })

/**
 * Creates a [FieldSet] where entries will be unique based on the columns.
 *
 * Only supported by a postgres database.
 */
fun Query.distinctBy(first: Pair<ExpressionWithColumnType<*>, SortOrder>, vararg more: Pair<ExpressionWithColumnType<*>, SortOrder>): Query =
    distinctBy(listOf(first) + more.asList())

/**
 * Creates a [Query] where entries will be unique based on the [columns].
 *
 * Only supported by a postgres database.
 */
fun Query.distinctBy(columns: Collection<Pair<ExpressionWithColumnType<*>, SortOrder>>): Query {
  if (columns.isEmpty())
    return this
  val orderBy = columns.toTypedArray()
  return adjustSlice {
    // Merge with another distinctBy operation, if it was applied before
    val (fields, expressions) = if (set.fields.firstOrNull() is DistinctBy) {
      val distinctOn = set.fields.first() as DistinctBy
      set.fields.subList(1, set.fields.size) to distinctOn.expressions
    } else set.fields to emptyList()
    val distinctOn = DistinctBy(expressions + columns.map { it.first })
    Slice(source, listOf(distinctOn) + fields)
  }.orderBy(*orderBy)
}

private class DistinctBy(
    val expressions: Collection<ExpressionWithColumnType<*>>
) : ExpressionWithColumnType<Int>() {

  override val columnType: IColumnType = IntegerColumnType()

  override fun toQueryBuilder(queryBuilder: QueryBuilder) = queryBuilder {
    append("DISTINCT ON (")
    appendJoined(expressions)
    append(") TRUE")
  }
}
