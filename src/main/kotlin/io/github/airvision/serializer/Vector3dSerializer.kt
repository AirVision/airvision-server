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
import io.github.airvision.util.math.component.z
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.spongepowered.math.vector.Vector3d

@Serializer(forClass = Vector3d::class)
object Vector3dSerializer : KSerializer<Vector3d> {

  override val descriptor: SerialDescriptor =
      buildSerialDescriptor("Vector3d", kind = StructureKind.LIST)

  override fun deserialize(decoder: Decoder): Vector3d {
    return decoder.collection(descriptor) {
      decodeElementIndex(descriptor)
      val x = decodeDoubleElement(descriptor, 0)
      decodeElementIndex(descriptor)
      val y = decodeDoubleElement(descriptor, 1)
      decodeElementIndex(descriptor)
      val z = decodeDoubleElement(descriptor, 2)
      Vector3d(x, y, z)
    }
  }

  override fun serialize(encoder: Encoder, value: Vector3d) {
    encoder.collection(descriptor, 3) {
      encodeDoubleElement(descriptor, 0, value.x)
      encodeDoubleElement(descriptor, 1, value.y)
      encodeDoubleElement(descriptor, 2, value.z)
    }
  }
}
