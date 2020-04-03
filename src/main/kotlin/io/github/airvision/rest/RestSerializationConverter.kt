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

import io.github.airvision.AirVision
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.features.ContentConverter
import io.ktor.features.UnsupportedMediaTypeException
import io.ktor.http.ContentType
import io.ktor.request.ApplicationReceiveRequest
import io.ktor.request.contentCharset
import io.ktor.serialization.SerializationConverter
import io.ktor.util.pipeline.PipelineContext
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.readText
import io.ktor.utils.io.readRemaining
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ArraySerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.jvm.jvmErasure

/**
 * Serialize the object either as a success or an error. Success data is nested in a "data"
 * property, while an error will be nested in a "error" property.
 */
class RestSerializationConverter(
    private val json: Json
) : ContentConverter {

  private val jsonContentConverter = SerializationConverter(json)

  private fun serializerByTypeInfo(type: KType): KSerializer<*> {
    val classifierClass = type.classifier as? KClass<*>
    if (classifierClass != null && classifierClass.java.isArray) {
      return arraySerializer(type)
    }
    return serializer(type)
  }

  @Suppress("UNCHECKED_CAST")
  private fun arraySerializer(type: KType): KSerializer<*> {
    val elementType = type.arguments[0].type ?: error("Array<*> is not supported")
    val elementSerializer = serializerByTypeInfo(elementType)
    return ArraySerializer(elementType.jvmErasure as KClass<Any>, elementSerializer as KSerializer<Any>)
  }

  override suspend fun convertForReceive(context: PipelineContext<ApplicationReceiveRequest, ApplicationCall>): Any? {
    val request = context.subject
    val channel = request.value as? ByteReadChannel ?: return null
    val charset = context.call.request.contentCharset() ?: Charsets.UTF_8
    val contentPacket = channel.readRemaining()
    val content = contentPacket.readText(charset)
    AirVision.logger.debug("REQ: $content")
    return convertForReceive(context, content)
  }

  fun convertForReceive(context: PipelineContext<ApplicationReceiveRequest, ApplicationCall>, content: String): Any {
    val serializer = serializerByTypeInfo(context.subject.typeInfo)
    return json.parse(serializer, content) ?: throw UnsupportedMediaTypeException(ContentType.Application.Json)
  }

  override suspend fun convertForSend(context: PipelineContext<Any, ApplicationCall>, contentType: ContentType, value: Any): Any? {
    val content = if (value is ErrorResponse) {
      mapOf("error" to ErrorInfo(value.code.value, value.message))
    } else {
      mapOf("data" to value)
    }
    return jsonContentConverter.convertForSend(context, contentType, content)
  }

  @Serializable
  data class ErrorInfo(
      val code: Int,
      val message: String? = null
  )
}
