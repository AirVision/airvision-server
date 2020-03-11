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
import org.spongepowered.math.imaginary.Quaterniond

@Serializer(forClass = Quaterniond::class)
object QuaterniondSerializer : KSerializer<Quaterniond> {

  override val descriptor: SerialDescriptor = ArrayListClassDesc(DoubleDescriptor.withName("Quaterniond"))
      /*
      SerialDescriptor("Quaterniond", kind = StructureKind.LIST) {
        listDescriptor<Double>()
      }
      */

  override fun deserialize(decoder: Decoder): Quaterniond {
    @Suppress("NAME_SHADOWING")
    val decoder = decoder.beginStructure(descriptor)
    val x = decoder.decodeDoubleElement(descriptor, 0)
    val y = decoder.decodeDoubleElement(descriptor, 1)
    val z = decoder.decodeDoubleElement(descriptor, 2)
    val w = decoder.decodeDoubleElement(descriptor, 3)
    decoder.endStructure(descriptor)
    return Quaterniond(x, y, z, w)
  }

  override fun serialize(encoder: Encoder, value: Quaterniond) {
    @Suppress("NAME_SHADOWING")
    val encoder = encoder.beginCollection(descriptor, 4)
    encoder.encodeDoubleElement(descriptor, 0, value.x)
    encoder.encodeDoubleElement(descriptor, 1, value.y)
    encoder.encodeDoubleElement(descriptor, 2, value.z)
    encoder.encodeDoubleElement(descriptor, 3, value.w)
    encoder.endStructure(descriptor)
  }
}
