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

import arrow.core.Either
import io.github.airvision.AirVisionJson
import io.github.airvision.util.json.int
import io.github.airvision.util.json.primitive
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.TestApplicationResponse
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.reflect.KClass

fun testApp(callback: TestApplicationEngine.() -> Unit) {
  /*
  val rest = Rest(AirVision.Config())
  withTestApplication(rest::setup) {
    callback()
  }
  */
  // TODO
}

inline fun <reified T : Any> TestApplicationResponse.parse(): Either<ErrorResponse, T> =
  parse(T::class)

fun <T : Any> TestApplicationResponse.parse(type: KClass<T>): Either<ErrorResponse, T> {
  val content = content ?: return Either.Left(ErrorResponse(HttpStatusCode.NoContent))
  val json = Json.parseToJsonElement(content).jsonObject

  val error = json["error"]?.jsonObject
  return if (error != null) {
    val code = HttpStatusCode.fromValue(error.primitive("code").int)
    val message = error["message"]?.primitive?.toString()
    Either.Left(ErrorResponse(code, message ?: code.description))
  } else {
    val serializer = AirVisionJson.serializersModule.getContextual(type)!!
    val value = AirVisionJson.decodeFromString(serializer, Json.encodeToString(json["data"]!!))
    Either.Right(value)
  }
}
