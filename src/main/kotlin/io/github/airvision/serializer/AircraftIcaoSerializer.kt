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

import io.github.airvision.AircraftIcao
import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.Serializer
import kotlinx.serialization.internal.StringDescriptor
import kotlinx.serialization.withName

/**
 * A serializer for [AircraftIcao]s.
 */
@Serializer(forClass = AircraftIcao::class)
object AircraftIcaoSerializer : KSerializer<AircraftIcao> {

  override val descriptor: SerialDescriptor =
      StringDescriptor.withName("AircraftIdentifier")
  // PrimitiveDescriptor("Icao24", PrimitiveKind.STRING)

  override fun deserialize(decoder: Decoder): AircraftIcao =
      AircraftIcao.parse(decoder.decodeString())

  override fun serialize(encoder: Encoder, value: AircraftIcao) =
      encoder.encodeString(value.toString())
}
