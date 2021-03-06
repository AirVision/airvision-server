/*
 * AirVision
 *
 * Copyright (c) AirVision <https://www.github.com/AirVision>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
package io.github.airvision.serializer

import io.github.airvision.AircraftIcao24
import io.github.airvision.util.serializer.ToStringSerializer

/**
 * A serializer for [AircraftIcao24]s.
 */
object AircraftIcao24Serializer :
  ToStringSerializer<AircraftIcao24>("AircraftIcao24", AircraftIcao24::parse)
