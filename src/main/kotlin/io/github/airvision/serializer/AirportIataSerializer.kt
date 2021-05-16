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

import io.github.airvision.AirportIata
import io.github.airvision.AirportIcao
import io.github.airvision.util.serializer.ToStringSerializer

/**
 * A serializer for [AirportIcao]s.
 */
object AirportIataSerializer : ToStringSerializer<AirportIata>("AirportIata", ::AirportIata)
