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

import io.ktor.application.ApplicationCall
import io.ktor.features.ContentConverter
import io.ktor.http.ContentType
import io.ktor.request.ApplicationReceiveRequest
import io.ktor.util.pipeline.PipelineContext
import kotlinx.serialization.Serializable

/**
 * Serialize the object either as a success or an error. Success data is nested in a "data"
 * property, while an error will be nested in a "error" property.
 */
class RestSerializationConverter(private val serializationConverter: ContentConverter) : ContentConverter {

  override suspend fun convertForReceive(context: PipelineContext<ApplicationReceiveRequest, ApplicationCall>) =
      this.serializationConverter.convertForReceive(context)

  override suspend fun convertForSend(context: PipelineContext<Any, ApplicationCall>, contentType: ContentType, value: Any): Any? {
    val content = if (value is ErrorResponse) {
      mapOf("error" to ErrorInfo(value.code.value, value.message))
    } else {
      mapOf("data" to value)
    }
    return this.serializationConverter.convertForSend(context, contentType, content)
  }

  @Serializable
  data class ErrorInfo(
      val code: Int,
      val message: String? = null
  )
}
