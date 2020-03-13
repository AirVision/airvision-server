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

import io.github.airvision.AircraftIcao24
import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.PrimitiveDescriptor
import kotlinx.serialization.PrimitiveKind
import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.Serializer
import java.time.Instant

/**
 * A serializer for [AircraftIcao24]s.
 */
@Serializer(forClass = Instant::class)
object InstantSerializer : KSerializer<Instant> {

  override val descriptor: SerialDescriptor =
      PrimitiveDescriptor("Instant", PrimitiveKind.LONG)

  override fun deserialize(decoder: Decoder): Instant =
      Instant.ofEpochSecond(decoder.decodeLong())

  override fun serialize(encoder: Encoder, value: Instant) =
      encoder.encodeLong(value.epochSecond)
}
