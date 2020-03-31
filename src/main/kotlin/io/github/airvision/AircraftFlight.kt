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

import kotlinx.serialization.ContextualSerialization
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class AircraftFlight(
    val icao24: AircraftIcao24,
    val code: String? = null,
    @SerialName("departure_airport") val departureAirport: Airport? = null,
    @SerialName("arrival_airport") val arrivalAirport: Airport? = null,
    @ContextualSerialization @SerialName("estimated_arrival_time") val estimatedArrivalTime: Instant? = null,
    val waypoints: List<Waypoint>? = null
)

@Serializable
data class Waypoint(
    @ContextualSerialization val time: Instant,
    val position: GeodeticPosition,
    @SerialName("on_ground") val onGround: Boolean
)
