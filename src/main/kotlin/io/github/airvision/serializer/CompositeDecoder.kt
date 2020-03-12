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
import kotlinx.serialization.internal.IntArraySerializer
import kotlinx.serialization.internal.nullable
import kotlinx.serialization.serializer

private val nullableStringSerializer = String.serializer().nullable
private val nullableIntSerializer = Int.serializer().nullable
private val nullableFloatSerializer = Float.serializer().nullable
private val nullableIntArraySerializer = IntArraySerializer.nullable

fun CompositeDecoder.decodeIcao24(desc: SerialDescriptor, index: Int) =
    decodeSerializableElement(desc, index, AircraftIcaoSerializer)

fun CompositeDecoder.decodeNullableString(desc: SerialDescriptor, index: Int) =
    decodeSerializableElement(desc, index, nullableStringSerializer)

fun CompositeDecoder.decodeNullableInt(desc: SerialDescriptor, index: Int) =
    decodeSerializableElement(desc, index, nullableIntSerializer)

fun CompositeDecoder.decodeNullableFloat(desc: SerialDescriptor, index: Int) =
    decodeSerializableElement(desc, index, nullableFloatSerializer)

fun CompositeDecoder.decodeIntArray(desc: SerialDescriptor, index: Int) =
    decodeSerializableElement(desc, index, IntArraySerializer)

fun CompositeDecoder.decodeNullableIntArray(desc: SerialDescriptor, index: Int) =
    decodeSerializableElement(desc, index, nullableIntArraySerializer)

inline fun <R> Decoder.structure(desc: SerialDescriptor, fn: CompositeDecoder.() -> R): R {
  val decoder = beginStructure(desc)
  val result = decoder.fn()
  decoder.endStructure(desc)
  return result
}

inline fun <R> Decoder.collection(desc: SerialDescriptor, fn: CompositeDecoder.() -> R): R
    = structure(desc, fn)
