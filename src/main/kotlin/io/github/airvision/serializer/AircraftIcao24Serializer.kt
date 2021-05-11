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
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * A serializer for [AircraftIcao24]s.
 */
object AircraftIcao24Serializer : KSerializer<AircraftIcao24> {

  override val descriptor: SerialDescriptor =
      PrimitiveSerialDescriptor("AircraftIcao", PrimitiveKind.STRING)

  override fun deserialize(decoder: Decoder): AircraftIcao24 =
      AircraftIcao24.parse(decoder.decodeString())

  override fun serialize(encoder: Encoder, value: AircraftIcao24) =
      encoder.encodeString(value.toString())
}
