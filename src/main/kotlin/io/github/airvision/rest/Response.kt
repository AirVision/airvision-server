/*
 * AirVision
 *
 * Copyright (c) LanternPowered <https://www.github.com/AirVision>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
package io.github.airvision.rest

import io.ktor.http.HttpStatusCode

/**
 * Constructs a new [Response.Success] with the given [data].
 */
fun <T : Any> success(data: T): Response.Success<T>
    = Response.Success(data)

/**
 * Constructs a new [Response.Error] with the given [code] and optional [message].
 */
fun error(code: HttpStatusCode, message: String? = code.description): Response.Error<Any>
    = Response.Error(code, message)

@Suppress("ClassName")
object error {

  fun badRequest(message: String? = HttpStatusCode.BadRequest.description)
      = error(HttpStatusCode.BadRequest, message)

  fun notFound(message: String? = HttpStatusCode.NotFound.description)
      = error(HttpStatusCode.NotFound, message)
}

/**
 * Represents a response of the REST service. Will
 * always be either a [Success] or an [Error].
 */
sealed class Response<T : Any> {

  /**
   * Represents a success. The json data will be represented as:
   *
   * {
   *   "data": { ... }
   * }
   */
  data class Success<T : Any>(
      val data: T
  ) : Response<T>()

  /**
   * Represents an error. The json data will be represented as:
   *
   * {
   *   "error": {
   *     "code": "404",
   *     "message": "Some message, is optional."
   *   }
   * }
   */
  data class Error<T : Any>(
      val code: HttpStatusCode,
      val message: String? = null
  ) : Response<T>()
}
