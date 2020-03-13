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
import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.PrimitiveDescriptor
import kotlinx.serialization.PrimitiveKind
import kotlinx.serialization.SerialDescriptor

/**
 * A serializer for [AirportIcao]s.
 */
object AirportIcaoSerializer : KSerializer<AirportIcao> {

  override val descriptor: SerialDescriptor =
      PrimitiveDescriptor("AirportIcao", PrimitiveKind.STRING)

  override fun deserialize(decoder: Decoder): AirportIcao =
      AirportIcao(decoder.decodeString())

  override fun serialize(encoder: Encoder, value: AirportIcao) =
      encoder.encodeString(value.toString())
}
