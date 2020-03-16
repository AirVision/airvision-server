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

import io.github.airvision.Aircraft
import io.github.airvision.AircraftIcao24
import io.github.airvision.GeodeticBounds
import io.github.airvision.GeodeticPosition
import io.github.airvision.service.AircraftService
import java.time.Instant

class OsnAircraftService(private val osn: OpenSkyNetwork) : AircraftService {

  private fun OsnAircraft.mapAircraft(): Aircraft {
    val position = if (latitude != null && longitude != null) {
      val altitude = (baroAltitude ?: geoAltitude ?: 0.0).toDouble()
      GeodeticPosition(latitude.toDouble(), longitude.toDouble(), altitude)
    } else null
    return Aircraft(time = lastContact, icao24 = icao24, onGround = onGround,
        velocity = velocity?.toDouble(), position = position)
  }

  override suspend fun getAll(time: Instant?) =
      this.osn.getAircrafts(time?.epochSecond?.toInt()).map { o -> o.mapAircraft() }

  override suspend fun getAllWithin(bounds: GeodeticBounds, time: Instant?) =
      this.osn.getAircrafts(bounds, time?.epochSecond?.toInt()).map { o -> o.mapAircraft() }

  override suspend fun get(icao24: AircraftIcao24, time: Instant?) =
      this.osn.getAircraft(icao24)?.mapAircraft()
}
