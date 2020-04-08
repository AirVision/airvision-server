/*
 * AirVision
 *
 * Copyright (c) AirVision <https://www.github.com/AirVision>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
package io.github.airvision.util.ktor

import arrow.core.Either
import io.ktor.client.HttpClient
import io.ktor.client.features.HttpRequestTimeoutException
import io.ktor.client.features.ResponseException
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.utils.EmptyContent
import io.ktor.http.DEFAULT_PORT
import kotlinx.coroutines.TimeoutCancellationException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

suspend inline fun <reified R> HttpClient.tryToGet(
    urlString: String,
    crossinline block: HttpRequestBuilder.() -> Unit = {}
): Either<Failure, R> = tryToHandle {
  get(urlString, block)
}

suspend inline fun <reified R> HttpClient.tryToGet(
    scheme: String = "http", host: String = "localhost", port: Int = DEFAULT_PORT,
    path: String = "/",
    body: Any = EmptyContent,
    crossinline block: HttpRequestBuilder.() -> Unit = {}
): Either<Failure, R> = tryToHandle {
  get(scheme, host, port, path, body, block)
}

suspend fun <R> tryToHandle(fn: suspend () -> R): Either<Failure, R> {
  return try {
    Either.right(fn())
  } catch (ex: Exception) {
    val failure = when (ex) {
      is TimeoutCancellationException,
      is SocketTimeoutException,
      // Is thrown by the HttpTimeout feature
      is HttpRequestTimeoutException,
      // Lets assume that this is also a timeout exception, since the host couldn't be found
      is UnknownHostException -> Failure.Timeout
      // Something just went wrong with getting a valid response
      is ResponseException -> Failure.ErrorResponse(ex.response)
      // Something else went wrong on our end
      else -> Failure.InternalError(ex)
    }
    Either.left(failure)
  }
}

sealed class Failure {

  object Timeout : Failure()

  data class ErrorResponse(val response: HttpResponse) : Failure() {

    val message: String
      get() = "${response.call.request.url}: ${response.status}"
  }

  data class InternalError(val exception: Exception) : Failure()
}
