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
import io.github.airvision.AirVision
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.TestApplicationResponse
import io.ktor.server.testing.withTestApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.getContextualOrDefault
import kotlinx.serialization.stringify
import kotlin.reflect.KClass

fun testApp(callback: TestApplicationEngine.() -> Unit) {
  val rest = Rest(Rest.Config())
  withTestApplication(rest::setup) {
    callback()
  }
}

inline fun <reified T : Any> TestApplicationResponse.parse(): Either<ErrorResponse, T> = parse(T::class)

fun <T : Any> TestApplicationResponse.parse(type: KClass<T>): Either<ErrorResponse, T> {
  val content = this.content ?: return Either.left(ErrorResponse(HttpStatusCode.NoContent))
  val json = Json.parseJson(content).jsonObject

  val error = json["error"]?.jsonObject
  return if (error != null) {
    val code = HttpStatusCode.fromValue(error["code"]!!.primitive.int)
    val message = error["message"]?.primitive?.toString()
    Either.left(ErrorResponse(code, message ?: code.description))
  } else {
    val serializer = AirVision.json.context.getContextualOrDefault(type)
    val value = AirVision.json.parse(serializer, Json.stringify(json["data"]!!))
    Either.right(value)
  }
}
