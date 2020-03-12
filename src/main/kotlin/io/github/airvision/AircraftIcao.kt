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

import io.github.airvision.serializer.AircraftIcaoSerializer
import kotlinx.serialization.Serializable

/**
 * Represents an identifier of aircrafts. Also known as the
 * ICAO 24 bit address used by aircraft's.
 *
 * @property address The 24 bit address
 */
@Serializable(with = AircraftIcaoSerializer::class)
class AircraftIcao(val address: Int) {

  init {
    check(this.address <= 0xffffff) {
      "Given address ${this.address.toString(16)} is more than 24 bits." }
  }

  /**
   * Converts this back to a string representation.
   */
  override fun toString() =
      this.address.toString(16).toUpperCase()

  companion object {

    /**
     * Parses a hexadecimal string as an [AircraftIcao].
     */
    fun parse(value: String): AircraftIcao {
      check(value.length <= 6)
      return AircraftIcao(value.toInt(16))
    }
  }
}
