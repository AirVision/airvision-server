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

import io.github.airvision.util.math.component.x
import io.github.airvision.util.math.component.y
import kotlinx.serialization.Serializable
import org.spongepowered.math.vector.Vector2d

@Serializable
data class GeodeticBounds(
  val min: GeodeticPosition,
  val max: GeodeticPosition
) {

  operator fun contains(position: GeodeticPosition): Boolean {
    val containsLatitude = { latitude: Double ->
      latitude >= min.latitude && latitude <= max.latitude
    }
    val containsLongitude = { longitude: Double ->
      if (min.longitude <= max.longitude) {
        longitude >= min.longitude && longitude <= max.longitude
      } else {
        longitude >= min.longitude || longitude <= max.longitude
      }
    }
    return containsLatitude(position.latitude) &&
        containsLongitude(position.longitude)
  }

  companion object {

    private const val latitudeMax = 90
    private const val latitudeRange = latitudeMax * 2

    private const val longitudeMax = 180
    private const val longitudeRange = longitudeMax * 2

    fun ofCenterAndSize(position: GeodeticPosition, size: Vector2d): GeodeticBounds {
      var minLatitude = position.latitude - size.x / 2
      var maxLatitude = position.latitude + size.x / 2

      var minLongitude = position.longitude - size.y / 2
      var maxLongitude = position.longitude + size.y / 2

      if (minLatitude < -latitudeMax)
        minLatitude += latitudeRange
      if (maxLatitude > latitudeMax)
        maxLatitude -= latitudeRange

      if (minLongitude < -longitudeMax)
        minLongitude += longitudeRange
      if (maxLongitude > longitudeMax)
        maxLongitude -= longitudeRange

      return GeodeticBounds(
        GeodeticPosition(minLatitude, minLongitude),
        GeodeticPosition(maxLatitude, maxLongitude)
      )
    }
  }
}
