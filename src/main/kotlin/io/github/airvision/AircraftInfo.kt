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
data class AircraftInfo(
    val icao24: AircraftIcao24,
    val name: String,
    val description: String?,
    val owner: String?,
    val manufacturer: AircraftManufacturer?,
    val engines: AircraftEngines?,
    val type: Type?
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
