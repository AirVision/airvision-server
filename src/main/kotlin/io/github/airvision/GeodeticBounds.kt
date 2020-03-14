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

import org.spongepowered.math.vector.Vector2d

data class GeodeticBounds(
    val min: GeodeticPosition,
    val max: GeodeticPosition
) {

  companion object {

    fun ofCenterAndSize(position: GeodeticPosition, size: Vector2d): GeodeticBounds {
      var minLatitude = position.latitude - size.x / 2
      var maxLatitude = position.latitude + size.x / 2

      var minLongitude = position.longitude - size.y / 2
      var maxLongitude = position.longitude + size.y / 2

      if (minLatitude < -85)
        minLatitude += 85
      if (maxLatitude > 85)
        maxLatitude -= 85

      if (minLongitude < -180)
        minLongitude += 180
      if (maxLongitude > 180)
        maxLongitude -= 180

      return GeodeticBounds(
          GeodeticPosition(minLatitude, minLongitude),
          GeodeticPosition(maxLatitude, maxLongitude))
    }
  }
}
