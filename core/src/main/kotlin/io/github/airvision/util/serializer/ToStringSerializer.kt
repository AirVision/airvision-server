/*
 * AirVision
 *
 * Copyright (c) AirVision <https://www.github.com/AirVision>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
package io.github.airvision.util.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

open class ToStringSerializer<T>(
    serialName: String,
    private val toValue: (String) -> T,
    private val toString: (T) -> String = { it.toString() },
) : KSerializer<T> {

  override val descriptor: SerialDescriptor =
      PrimitiveSerialDescriptor(serialName, PrimitiveKind.STRING)

  override fun deserialize(decoder: Decoder): T =
      toValue(decoder.decodeString())

  override fun serialize(encoder: Encoder, value: T) {
    encoder.encodeString(toString(value))
  }
}
