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

import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Encoder

inline fun <R> Encoder.structure(desc: SerialDescriptor, fn: CompositeEncoder.() -> R): R {
  val encoder = beginStructure(desc)
  val result = encoder.fn()
  encoder.endStructure(desc)
  return result
}

inline fun <R> Encoder.collection(
  desc: SerialDescriptor,
  size: Int,
  fn: CompositeEncoder.() -> R
): R {
  val encoder = beginCollection(desc, size)
  val result = encoder.fn()
  encoder.endStructure(desc)
  return result
}
