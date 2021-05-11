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

import io.github.airvision.GeodeticPosition
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * A serializer for [GeodeticPosition]s.
 */
object GeodeticPositionSerializer : KSerializer<GeodeticPosition> {

  private val doubleListSerializer = ListSerializer(Double.serializer())

  override val descriptor: SerialDescriptor =
      buildSerialDescriptor("GeodeticPosition", kind = StructureKind.LIST)

  override fun deserialize(decoder: Decoder): GeodeticPosition {
    val list = doubleListSerializer.deserialize(decoder)
    check(list.size == 2 || list.size == 3)
    return GeodeticPosition(list[0], list[1], if (list.size == 3) list[2] else 0.0)
  }

  override fun serialize(encoder: Encoder, value: GeodeticPosition) {
    val size = if (value.altitude != 0.0) 3 else 2
    encoder.collection(descriptor, size) {
      encodeDoubleElement(descriptor, 0, value.latitude)
      encodeDoubleElement(descriptor, 1, value.longitude)
      if (value.altitude != 0.0)
        encodeDoubleElement(descriptor, 2, value.altitude)
    }
  }
}
