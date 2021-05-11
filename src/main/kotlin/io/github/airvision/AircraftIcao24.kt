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
 * Represents an identifier of aircrafts. Also known as the
 * ICAO 24 bit address used by aircraft's.
 *
 * @property address The 24 bit address
 */
@Serializable(with = AircraftIcao24Serializer::class)
data class AircraftIcao24(val address: Int) {

  init {
    check(this.address <= 0xffffff) {
      "Given address ${this.address.toString(16)} is more than 24 bits." }
  }

  /**
   * Converts this back to a string representation.
   */
  override fun toString() = address.toString(16).uppercase()

  companion object {

    private val regex = "^[0-9A-Fa-f]{1,6}$".toRegex()

    /**
     * Returns whether the given value is a valid ICAO 24 identifier.
     */
    fun isValid(value: String): Boolean =
        regex.matches(value)

    /**
     * Parses a hexadecimal string as an [AircraftIcao24].
     */
    fun parse(value: String): AircraftIcao24 {
      check(isValid(value)) { "Invalid ICAO24 identifier: $value" }
      return AircraftIcao24(value.toInt(16))
    }
  }
}
