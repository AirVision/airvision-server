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

import kotlinx.serialization.CompositeDecoder
import kotlinx.serialization.Decoder
import kotlinx.serialization.SerialDescriptor

inline fun <R> Decoder.structure(desc: SerialDescriptor, fn: CompositeDecoder.() -> R): R {
  val decoder = beginStructure(desc)
  val result = decoder.fn()
  decoder.endStructure(desc)
  return result
}

inline fun <R> Decoder.collection(desc: SerialDescriptor, fn: CompositeDecoder.() -> R): R
    = structure(desc, fn)
