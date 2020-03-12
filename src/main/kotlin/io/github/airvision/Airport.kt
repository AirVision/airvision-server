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

/**
 * Represents an airport.
 */
data class Airport(
    val id: Int,
    val icao: AirportIcao?,
    val iata: String?,
    val name: String,
    val city: String,
    val country: String,
    val position: GeodeticPosition
)
