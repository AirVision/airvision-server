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

import io.github.airvision.Icao24
import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.Serializer
import kotlinx.serialization.internal.StringDescriptor
import kotlinx.serialization.withName

/**
 * A serializer for [Icao24]s.
 */
@Serializer(forClass = Icao24::class)
object Icao24Serializer : KSerializer<Icao24> {

  override val descriptor: SerialDescriptor =
      StringDescriptor.withName("Icao24")
  // PrimitiveDescriptor("Icao24", PrimitiveKind.STRING)

  override fun deserialize(decoder: Decoder): Icao24 =
      Icao24.parse(decoder.decodeString())

  override fun serialize(encoder: Encoder, value: Icao24) =
      encoder.encodeString(value.toString())
}
