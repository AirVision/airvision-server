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

import kotlinx.serialization.Serializable

@Serializable
data class AircraftModel(
    val icao24: AircraftIcao24,
    val name: String,
    val manufacturer: AircraftManufacturer?,
    val engine: AircraftEngine,
    val engineCount: Int,
    val type: Type
) {

  enum class Type {
    LandPlane,
    SeaPlane,
    Amphibian,
    Helicopter,
    /**
     * Dirigible, also known as Airship.
     */
    Dirigible
  }
}
