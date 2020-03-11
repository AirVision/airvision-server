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

import io.ktor.application.ApplicationCall
import io.ktor.features.ContentConverter
import io.ktor.http.ContentType
import io.ktor.request.ApplicationReceiveRequest
import io.ktor.util.pipeline.PipelineContext
import kotlinx.serialization.Serializable

class RestSerializationConverter(private val serializationConverter: ContentConverter) : ContentConverter {

  override suspend fun convertForReceive(context: PipelineContext<ApplicationReceiveRequest, ApplicationCall>) =
      this.serializationConverter.convertForReceive(context)

  override suspend fun convertForSend(context: PipelineContext<Any, ApplicationCall>, contentType: ContentType, value: Any): Any? {
    val content = when (value) {
      is Response.Success<*> -> mapOf("data" to value.data)
      is Response.Error<*> -> mapOf("error" to ErrorInfo(value.code.value, value.message))
      else -> value
    }
    return this.serializationConverter.convertForSend(context, contentType, content)
  }

  @Serializable
  data class ErrorInfo(
      val code: Int,
      val message: String? = null
  )
}
