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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AircraftInfo(
  val icao24: AircraftIcao24,
  val model: String,
  val owner: String? = null,
  val manufacturer: AircraftManufacturer? = null,
  val engines: AircraftEngines? = null,
  val type: Type? = null,
  @SerialName("weight_category") val weightCategory: WeightCategory? = null
) {

  enum class Type {
    LandPlane,
    SeaPlane,
    Amphibian,
    Helicopter,

    /**
     * Dirigible, also known as Airship.
     */
    Dirigible,
    Glider, // Gyrocopter
  }
}
