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
import kotlinx.serialization.ContextualSerialization
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class OsnTrackResponse(
    @SerialName("icao24") val icao24: AircraftIcao24,
    @ContextualSerialization val startTime: Instant,
    @ContextualSerialization val endTime: Instant,
    val callsign: String?,
    val path: List<Waypoint>
) {

  @Serializable(with = OsnTrackWaypointSerializer::class)
  data class Waypoint(
      @ContextualSerialization val time: Instant,
      val latitude: Float?,
      val longitude: Float?,
      val baroAltitude: Float?,
      val trueTrack: Float?,
      val onGround: Boolean
  )
}
