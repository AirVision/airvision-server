/*
 * AirVision
 *
 * Copyright (c) AirVision <https://www.github.com/AirVision>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
package io.github.airvision

import io.github.airvision.serializer.AircraftIcao24Serializer
import kotlinx.serialization.Serializable

/**
 * Represents an identifier of aircraft. Also known as the
 * ICAO 24 bit address used by aircraft.
 *
 * @property address The 24 bit address
 */
@Serializable(with = AircraftIcao24Serializer::class)
data class AircraftIcao24(val address: Int) {

  init {
    check(this.address <= 0xffffff) {
      "Given address ${this.address.toString(16)} is more than 24 bits."
    }
  }

  /**
   * Converts this back to a string representation.
   */
  override fun toString() = address.toString(16).uppercase()

  companion object {

    private const val maxLength = 6

    /**
     * Returns whether the given value is a valid ICAO 24 identifier.
     */
    fun isValid(value: String): Boolean =
      value.length in 1..maxLength && value.toIntOrNull(16) != null

    /**
     * Parses a hexadecimal string as an [AircraftIcao24].
     */
    fun parse(value: String): AircraftIcao24 {
      if (value.length !in 1..maxLength)
        invalidIdentifier(value)
      val address = value.toIntOrNull(16)
        ?: invalidIdentifier(value)
      return AircraftIcao24(address)
    }

    private fun invalidIdentifier(value: String): Nothing =
      throw IllegalArgumentException("Invalid ICAO 24 identifier: $value")
  }
}
