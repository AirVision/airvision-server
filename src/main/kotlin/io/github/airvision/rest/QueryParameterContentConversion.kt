/*
 * AirVision
 *
 * Copyright (c) AirVision <https://www.github.com/AirVision>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
package io.github.airvision.rest

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.request.ApplicationReceivePipeline
import io.ktor.request.ApplicationReceiveRequest
import io.ktor.request.header
import io.ktor.utils.io.ByteReadChannel
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonLiteral
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject

private sealed class JsonTreeElement

private class JsonTreeArray(val entries: List<JsonTreeElement>) : JsonTreeElement() {
  override fun toString() = this.entries.joinToString(separator = ",", prefix = "[", postfix = "]")
}

private class JsonTreePrimitive(val value: String) : JsonTreeElement() {
  override fun toString(): String = "\"$value\""
}

private object JsonTreeNull : JsonTreeElement() {
  override fun toString(): String = "null"
}

private class JsonTree(val map: MutableMap<String, JsonTreeElement> = mutableMapOf()) : JsonTreeElement() {

  fun put(key: String, value: JsonTreeElement) {
    val index = key.indexOf('.')
    if (index != -1) {
      val element = map.computeIfAbsent(key.substring(0, index)) { JsonTree() }
      if (element is JsonTree) {
        element.put(key.substring(index + 1), value)
      }
    } else {
      map[key] = value
    }
  }

  override fun toString() = this.map.entries.joinToString(
      separator = ",", prefix = "{", postfix = "}") { entry -> "\"${entry.key}\":${entry.value}" }
}

private fun JsonElement.toTreeElement(): JsonTreeElement {
  return when (this) {
    is JsonLiteral -> JsonTreePrimitive(content)
    JsonNull -> JsonTreeNull
    is JsonObject -> JsonTree(this.content.entries.associate { it.key to it.value.toTreeElement() }.toMutableMap())
    is JsonArray -> JsonTreeArray(this.content.map { it.toTreeElement() })
  }
}

private fun parse(value: String): JsonTreeElement {
  return if (value.isNotEmpty() && value.first() == '[' && value.last() == ']') {
    Json.parse(JsonArray.serializer(), value).toTreeElement()
  } else if (value.isNotEmpty() && value.first() == '{' && value.last() == '}') {
    Json.parse(JsonObject.serializer(), value).toTreeElement()
  } else {
    JsonTreePrimitive(value)
  }
}

fun Application.installQueryParameterContentConversion(converter: RestSerializationConverter) {
  // Install query parameter based Content Conversion
  receivePipeline.intercept(ApplicationReceivePipeline.Transform) {
    val contentType = call.request.header(HttpHeaders.ContentType)?.let { ContentType.parse(it) }
    if (contentType != null || subject.type == ByteReadChannel::class) {
      // Just proceed
      proceed()
      return@intercept
    }

    val parameters = call.request.queryParameters
    val tree = JsonTree()

    parameters.entries()
        .forEach { (key, value) ->
          val element = if (value.size == 1) {
            parse(value[0])
          } else {
            JsonTreeArray(value.map { entry -> parse(entry) })
          }
          tree.put(key, element)
        }

    val content = tree.toString()
    val converted = converter.convertForReceive(this, content)
    proceedWith(ApplicationReceiveRequest(subject.typeInfo, converted, reusableValue = true))
  }
}
