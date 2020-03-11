/*
 * AirVision
 *
 * Copyright (c) LanternPowered <https://www.github.com/AirVision>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
package io.github.airvision

import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.internal.StringDescriptor
import kotlinx.serialization.withName

/**
 * Represents a 24 bit address used by aircraft's.
 *
 * @property address The 24 bit address
 */
@Serializable(with = Icao24Serializer::class)
class Icao24(val address: Int) {

  init {
    check(this.address <= 0xffffff) { "Given address ${this.address.toString(16)} is more than 24 bits." }
  }

  /**
   * Converts this back to a string representation.
   */
  override fun toString() =
      this.address.toString(16).toUpperCase()

  companion object {

    /**
     * Parses a hexadecimal string as a ICAO 24 address.
     */
    fun parse(value: String): Icao24 {
      check(value.length <= 6)
      return Icao24(value.toInt(16))
    }
  }
}

/**
 * A serializer for [Icao24]s.
 */
@Serializer(forClass = Icao24::class)
object Icao24Serializer : KSerializer<Icao24> {

  override val descriptor: SerialDescriptor =
      StringDescriptor.withName("Icao24")
      // PrimitiveDescriptor("Icao24", PrimitiveKind.STRING)

  override fun deserialize(decoder: Decoder): Icao24 =
      Icao24.parse(decoder.decodeString())

  override fun serialize(encoder: Encoder, value: Icao24) =
      encoder.encodeString(value.toString())
}
