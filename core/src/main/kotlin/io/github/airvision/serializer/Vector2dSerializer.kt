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

import io.github.airvision.util.math.component.x
import io.github.airvision.util.math.component.y
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.spongepowered.math.vector.Vector2d

@Serializer(forClass = Vector2d::class)
object Vector2dSerializer : KSerializer<Vector2d> {

  override val descriptor: SerialDescriptor =
      buildSerialDescriptor("Vector2d", kind = StructureKind.LIST)

  override fun deserialize(decoder: Decoder): Vector2d {
    return decoder.collection(descriptor) {
      decodeElementIndex(descriptor)
      val x = decodeDoubleElement(descriptor, 0)
      decodeElementIndex(descriptor)
      val y = decodeDoubleElement(descriptor, 1)
      Vector2d(x, y)
    }
  }

  override fun serialize(encoder: Encoder, value: Vector2d) {
    encoder.collection(descriptor, 2) {
      encodeDoubleElement(descriptor, 0, value.x)
      encodeDoubleElement(descriptor, 1, value.y)
    }
  }
}
