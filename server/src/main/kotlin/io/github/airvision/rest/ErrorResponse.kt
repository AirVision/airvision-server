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

import io.ktor.http.HttpStatusCode

/**
 * Constructs a new [ErrorResponse] with the given [code] and optional [message].
 */
fun error(code: HttpStatusCode, message: String? = code.description): Nothing =
    throw ErrorResponseException(ErrorResponse(code, message))

@Suppress("ClassName")
object error {

  fun badRequest(message: String? = HttpStatusCode.BadRequest.description): Nothing =
      error(HttpStatusCode.BadRequest, message)

  fun notFound(message: String? = HttpStatusCode.NotFound.description): Nothing =
      error(HttpStatusCode.NotFound, message)

  fun internalError(message: String? = HttpStatusCode.InternalServerError.description): Nothing =
      error(HttpStatusCode.InternalServerError, message)
}

/**
 * Represents an error response of the REST service.
 *
 * The json data will be represented as:
 * {
 *   "error": {
 *     "code": "404",
 *     "message": "Some message, is optional."
 *   }
 * }
 */
class ErrorResponse(
    val code: HttpStatusCode,
    val message: String? = code.description
)

class ErrorResponseException(val response: ErrorResponse) : Exception()
