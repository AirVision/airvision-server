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

import io.github.airvision.serializer.AirportIcaoSerializer
import kotlinx.serialization.Serializable

/**
 * Represents the ICAO code which identifies airports.
 */
@Serializable(with = AirportIcaoSerializer::class)
data class AirportIcao(val icao: String) {

  init {
    check(icao.length <= MaxLength) { "Airport icao \"$icao\" is too long." }
  }

  override fun toString() = this.icao

  companion object {
     const val MaxLength = 4
  }
}
