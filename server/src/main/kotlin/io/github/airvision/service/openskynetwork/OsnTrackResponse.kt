/*
 * AirVision
 *
 * Copyright (c) AirVision <https://www.github.com/AirVision>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
package io.github.airvision.service.openskynetwork

import io.github.airvision.AircraftIcao24
import io.github.airvision.service.openskynetwork.serializer.OsnTrackWaypointSerializer
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class OsnTrackResponse(
  @SerialName("icao24") val icao24: AircraftIcao24,
  @Contextual val startTime: Instant,
  @Contextual val endTime: Instant,
  val callsign: String?,
  val path: List<Waypoint>
) {

  @Serializable(with = OsnTrackWaypointSerializer::class)
  data class Waypoint(
    @Contextual val time: Instant,
    val latitude: Float?,
    val longitude: Float?,
    val baroAltitude: Float?,
    val heading: Float?,
    val onGround: Boolean
  )
}
