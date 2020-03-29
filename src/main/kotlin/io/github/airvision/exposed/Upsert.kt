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
import org.jetbrains.exposed.sql.Index
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.transactions.TransactionManager

class UpsertStatement<T : Any> internal constructor(
    table: Table, conflictColumn: Column<*>? = null, conflictIndex: Index? = null
) : InsertStatement<T>(table, false) {

  private val indexName: String
  private val indexColumns: List<Column<*>>

  init {
    when {
      conflictIndex != null -> {
        val columns = conflictIndex.columns
        if (columns.size == 1) {
          indexName = columns[0].name
          indexColumns = columns
        } else {
          indexName = conflictIndex.indexName
          indexColumns = conflictIndex.columns
        }
      }
      conflictColumn != null -> {
        indexName = conflictColumn.name
        indexColumns = listOf(conflictColumn)
      }
      else -> throw IllegalArgumentException()
    }
  }

  override fun prepareSQL(transaction: Transaction) = buildString {
    append(super.prepareSQL(transaction))

    val dialect = transaction.db.vendor
    if (dialect == "postgresql") {
      append(" ON CONFLICT(")
      append(indexName)
      append(") DO UPDATE SET ")
      values.keys.filter { it !in indexColumns }.joinTo(this) { "${transaction.identity(it)}=EXCLUDED.${transaction.identity(it)}" }
    } else {
      append(" ON DUPLICATE KEY UPDATE ")
      values.keys.filter { it !in indexColumns }.joinTo(this) { "${transaction.identity(it)}=VALUES(${transaction.identity(it)})" }
    }
  }
}

fun <T : Table> T.upsert(body: T.(UpsertStatement<Number>) -> Unit): UpsertStatement<Number> {
  val index = indices.firstOrNull { it.unique }
      ?: throw IllegalStateException("There isn't a unique constraint in the table")
  return upsert(index, body)
}

fun <T : Table> T.upsert(index: Index, body: T.(UpsertStatement<Number>) -> Unit): UpsertStatement<Number> =
    UpsertStatement<Number>(this, conflictIndex = index).apply {
      body(this)
      execute(TransactionManager.current())
    }

fun <T : Table> T.upsert(column: Column<*>, body: T.(UpsertStatement<Number>) -> Unit): UpsertStatement<Number> =
    UpsertStatement<Number>(this, conflictColumn = column).apply {
      body(this)
      execute(TransactionManager.current())
    }
