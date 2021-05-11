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

import io.github.airvision.AirportIata
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
object AirportIataSerializer : KSerializer<AirportIata> {

  override val descriptor: SerialDescriptor =
      PrimitiveSerialDescriptor("AirportIata", PrimitiveKind.STRING)

  override fun deserialize(decoder: Decoder): AirportIata =
      AirportIata(decoder.decodeString())

  override fun serialize(encoder: Encoder, value: AirportIata) =
      encoder.encodeString(value.toString())
}
