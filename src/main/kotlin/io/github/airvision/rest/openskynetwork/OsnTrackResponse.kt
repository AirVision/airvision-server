/*
 * AirVision
 *
 * Copyright (c) AirVision <https://www.github.com/AirVision>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
package io.github.airvision.rest.openskynetwork

import io.github.airvision.AircraftIcao
import io.github.airvision.rest.openskynetwork.serializer.OsnTrackWaypointSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OsnTrackResponse(
    @SerialName("icao24") val icao: AircraftIcao,
    val startTime: Int,
    val endTime: Int,
    val callsign: String?,
    val path: List<Waypoint>
) {

  @Serializable(with = OsnTrackWaypointSerializer::class)
  data class Waypoint(
      val time: Int,
      val latitude: Float?,
      val longitude: Float?,
      val baroAltitude: Float?,
      val trueTrack: Float?,
      val onGround: Boolean
  )
}
