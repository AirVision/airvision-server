/*
 * AirVision
 *
 * Copyright (c) AirVision <https://www.github.com/AirVision>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
@file:Suppress("FunctionName", "NOTHING_TO_INLINE")

package io.github.airvision.util.json

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

fun pathOf(first: String, vararg more: String) = Path(listOf(first) + more.asList())

inline class Path(val parts: List<String>) {

  inline val size: Int
    get() = parts.size

  inline operator fun get(index: Int) = parts[index]

  inline operator fun plus(other: Path): Path = Path(parts + other.parts)

  inline operator fun plus(other: String): Path = Path(parts + other)

  inline fun isEmpty() = parts.isEmpty()

  override fun toString() = parts.joinToString(prefix = "[", postfix = "]")
}

fun JsonObject.getStringOrNull(key: String): String? =
    getPrimitiveOrNull(key)?.contentOrNull

fun JsonObject.getString(key: String): String =
    getPrimitive(key).content

fun JsonObject.getStringOrNull(path: Path): String? =
    getPrimitiveOrNull(path)?.contentOrNull

fun JsonObject.getString(path: Path): String =
    getPrimitive(path).content

fun JsonObject.getObject(path: Path): JsonObject =
    getValue(path) as? JsonObject ?: unexpectedJson(path, "JsonObject")

fun JsonObject.getArray(path: Path): JsonArray =
    getValue(path) as? JsonArray ?: unexpectedJson(path, "JsonArray")

fun JsonObject.getPrimitive(path: Path): JsonPrimitive =
    getValue(path) as? JsonPrimitive ?: unexpectedJson(path, "JsonPrimitive")

fun JsonObject.getObjectOrNull(path: Path): JsonObject? =
    get(path) as? JsonObject

fun JsonObject.getArrayOrNull(path: Path): JsonArray? =
    get(path) as? JsonArray

fun JsonObject.getPrimitiveOrNull(path: Path): JsonPrimitive? =
    get(path) as? JsonPrimitive

operator fun JsonObject.get(path: Path): JsonElement? {
  if (path.isEmpty())
    return null
  return get(path, 0)
}

fun JsonObject.getValue(path: Path): JsonElement =
    get(path) ?: throw NoSuchElementException("Path $path is missing in the object.")

private fun JsonObject.get(path: Path, index: Int): JsonElement? {
  val element = this[path[index]] ?: return null
  // Last index
  if (index == path.size - 1)
    return element
  // Not a JsonObject, so we cannot traverse more
  if (element !is JsonObject)
    return null
  return element.get(path, index + 1)
}

private fun unexpectedJson(path: Path, expected: String): Nothing =
    throw JsonException("Element at path $path is not a $expected")
