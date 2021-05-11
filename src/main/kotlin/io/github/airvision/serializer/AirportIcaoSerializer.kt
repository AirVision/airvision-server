/*
 * AirVision
 *
 * Copyright (c) AirVision <https://www.github.com/AirVision>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
package io.github.airvision.serializer

import io.github.airvision.AirportIcao
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * A serializer for [AirportIcao]s.
 */
object AirportIcaoSerializer : KSerializer<AirportIcao> {

  override val descriptor: SerialDescriptor =
      PrimitiveSerialDescriptor("AirportIcao", PrimitiveKind.STRING)

  override fun deserialize(decoder: Decoder): AirportIcao =
      AirportIcao(decoder.decodeString())

  override fun serialize(encoder: Encoder, value: AirportIcao) =
      encoder.encodeString(value.toString())
}
