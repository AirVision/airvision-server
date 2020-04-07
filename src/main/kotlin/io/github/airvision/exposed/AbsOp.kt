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

private class AbsOp<T>(
    private val expression: Expression<T>,
    override val columnType: IColumnType
): ExpressionWithColumnType<T>() {

  override fun toQueryBuilder(queryBuilder: QueryBuilder) = queryBuilder { append("ABS(", expression, ')') }
}

val <T> ExpressionWithColumnType<T>.abs: ExpressionWithColumnType<T>
  get() = AbsOp(this, columnType)
