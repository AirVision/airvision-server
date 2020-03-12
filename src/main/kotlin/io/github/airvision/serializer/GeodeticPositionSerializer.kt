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
import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.Serializer
import kotlinx.serialization.internal.ArrayListClassDesc
import kotlinx.serialization.internal.DoubleDescriptor
import kotlinx.serialization.list
import kotlinx.serialization.serializer
import kotlinx.serialization.withName

/**
 * A serializer for [GeodeticPosition]s.
 */
@Serializer(forClass = GeodeticPosition::class)
object GeodeticPositionSerializer : KSerializer<GeodeticPosition> {

  private val doubleListSerializer = Double.serializer().list

  override val descriptor: SerialDescriptor =
      ArrayListClassDesc(DoubleDescriptor.withName("GeodeticPosition"))
  /*
  SerialDescriptor("GeodeticPosition", kind = StructureKind.LIST) {
    listDescriptor<Double>()
  }
  */

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
