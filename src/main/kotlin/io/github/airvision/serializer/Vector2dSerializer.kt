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
import org.spongepowered.math.vector.Vector2d

@Serializer(forClass = Vector2d::class)
object Vector2dSerializer : KSerializer<Vector2d> {

  override val descriptor: SerialDescriptor =
      ArrayListClassDesc(DoubleDescriptor.withName("Vector2d"))
      /*
      SerialDescriptor("Vector2d", kind = StructureKind.LIST) {
        listDescriptor<Double>()
      }
      */

  override fun deserialize(decoder: Decoder): Vector2d {
    @Suppress("NAME_SHADOWING")
    val decoder = decoder.beginStructure(descriptor)
    val x = decoder.decodeDoubleElement(descriptor, 0)
    val y = decoder.decodeDoubleElement(descriptor, 1)
    decoder.endStructure(descriptor)
    return Vector2d(x, y)
  }

  override fun serialize(encoder: Encoder, value: Vector2d) {
    @Suppress("NAME_SHADOWING")
    val encoder = encoder.beginCollection(descriptor, 2)
    encoder.encodeDoubleElement(descriptor, 0, value.x)
    encoder.encodeDoubleElement(descriptor, 1, value.y)
    encoder.endStructure(descriptor)
  }
}
