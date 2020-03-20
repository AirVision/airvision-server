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

/**
 * @property name The name of the engine(s), if known
 */
@Serializable
data class AircraftEngine(
    val name: String?,
    val type: Type
) {

  enum class Type {
    Piston,
    Turboprop,
    TurboJet
  }
}
