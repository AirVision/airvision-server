/*
 * AirVision
 *
 * Copyright (c) AirVision <https://www.github.com/AirVision>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
package io.github.airvision.util.json

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.float
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull

val JsonElement.primitive: JsonPrimitive get() = jsonPrimitive
val JsonElement.primitiveOrNull: JsonPrimitive? get() = this as? JsonPrimitive

fun JsonObject.primitive(key: String): JsonPrimitive =
  (get(key) ?: missingPath(key)).jsonPrimitive

val JsonElement.int: Int get() = primitive.int
val JsonElement.intOrNull: Int? get() = primitiveOrNull?.intOrNull
val JsonElement.long: Long get() = primitive.long
val JsonElement.longOrNull: Long? get() = primitiveOrNull?.longOrNull
val JsonElement.double: Double get() = primitive.double
val JsonElement.doubleOrNull: Double? get() = primitiveOrNull?.doubleOrNull
val JsonElement.float: Float get() = primitive.float
val JsonElement.floatOrNull: Float? get() = primitiveOrNull?.floatOrNull
val JsonElement.boolean: Boolean get() = primitive.boolean
val JsonElement.booleanOrNull: Boolean? get() = primitiveOrNull?.booleanOrNull
val JsonElement.string: String get() = primitive.content
val JsonElement.stringOrNull: String? get() = primitiveOrNull?.contentOrNull
