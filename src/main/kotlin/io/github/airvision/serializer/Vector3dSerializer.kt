/*
 * AirVision
 *
 * Copyright (c) LanternPowered <https://www.github.com/AirVision>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
package io.github.airvision.serializer

import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.Serializer
import kotlinx.serialization.internal.ArrayListClassDesc
import kotlinx.serialization.internal.DoubleDescriptor
import kotlinx.serialization.withName
import org.spongepowered.math.vector.Vector3d

@Serializer(forClass = Vector3d::class)
object Vector3dSerializer : KSerializer<Vector3d> {

  override val descriptor: SerialDescriptor =
      ArrayListClassDesc(DoubleDescriptor.withName("Vector3d"))
      /*
      SerialDescriptor("Vector3d", kind = StructureKind.LIST) {
        listDescriptor<Double>()
      }
      */

  override fun deserialize(decoder: Decoder): Vector3d {
    @Suppress("NAME_SHADOWING")
    val decoder = decoder.beginStructure(descriptor)
    val x = decoder.decodeDoubleElement(descriptor, 0)
    val y = decoder.decodeDoubleElement(descriptor, 1)
    val z = decoder.decodeDoubleElement(descriptor, 2)
    decoder.endStructure(descriptor)
    return Vector3d(x, y, z)
  }

  override fun serialize(encoder: Encoder, value: Vector3d) {
    @Suppress("NAME_SHADOWING")
    val encoder = encoder.beginCollection(descriptor, 3)
    encoder.encodeDoubleElement(descriptor, 0, value.x)
    encoder.encodeDoubleElement(descriptor, 1, value.y)
    encoder.encodeDoubleElement(descriptor, 2, value.z)
    encoder.endStructure(descriptor)
  }
}
