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
import org.jetbrains.exposed.sql.ColumnType
import org.jetbrains.exposed.sql.IColumnType
import java.sql.ResultSet

/**
 * Transforms the type of the [Column] to another one.
 */
fun <O, T> Column<O>.transform(to: (O) -> T, from: (T) -> O): Column<T> {
  return table.replaceColumn(this, Column(table, name, TransformedColumnType(columnType, from, to)))
}

@Suppress("UNCHECKED_CAST")
private class TransformedColumnType<O, T>(
  private val original: IColumnType,
  private val toOriginal: (T) -> O,
  private val toTransformed: (O) -> T
) : ColumnType() {

  override fun sqlType(): String = original.sqlType()

  override fun valueToDB(value: Any?): Any? {
    if (value is RawTransformed<*>) {
      return valueToDB(value.value)
    }
    if (original.nullable && !nullable) {
      return original.valueToDB(toOriginal(value as T))
    }
    return super.valueToDB(value)
  }

  override fun valueFromDB(value: Any): Any {
    if (value is RawTransformed<*>) {
      return toTransformed(value.value as O) as Any
    }
    return toTransformed(value as O) as Any
  }

  override fun valueToString(value: Any?): String {
    if (value is RawTransformed<*>) {
      return valueToString(value.value)
    }
    if (original.nullable && !nullable) {
      return original.valueToString(toOriginal(value as T))
    }
    return super.valueToString(value)
  }

  override fun readObject(rs: ResultSet, index: Int): Any? {
    val value = super.readObject(rs, index)
    if (original.nullable && !nullable && value == null) {
      return RawTransformed(toTransformed(null as O))
    }
    return value
  }

  override fun notNullValueToDB(value: Any): Any {
    if (value is RawTransformed<*>) {
      return valueToDB(value.value)!!
    }
    return original.notNullValueToDB(toOriginal(value as T) as Any)
  }

  override fun nonNullValueToString(value: Any): String {
    if (value is RawTransformed<*>) {
      return valueToString(value.value)
    }
    return original.nonNullValueToString(toOriginal(value as T) as Any)
  }

  private class RawTransformed<T>(val value: T)
}
