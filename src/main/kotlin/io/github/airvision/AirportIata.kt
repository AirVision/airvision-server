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

import io.github.airvision.serializer.AirportIataSerializer
import kotlinx.serialization.Serializable

/**
 * Represents the IATA code which identifies airports.
 */
@Serializable(with = AirportIataSerializer::class)
data class AirportIata(val iata: String) {

  override fun toString() = this.iata
}
