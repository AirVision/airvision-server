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

import io.github.airvision.serializer.Icao24Serializer
import kotlinx.serialization.Serializable

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
