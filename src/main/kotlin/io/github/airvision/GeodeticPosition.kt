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

import io.github.airvision.serializer.GeodeticPositionSerializer
import io.github.airvision.util.toString
import kotlinx.serialization.Serializable
import org.spongepowered.math.vector.Vector3d
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Represents a geodetic position.
 *
 * @property latitude The latitude (in degrees)
 * @property longitude The longitude (in degrees)
 * @property altitude The altitude (in meters)
 */
@Serializable(with = GeodeticPositionSerializer::class)
data class GeodeticPosition(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0
) {

  override fun toString() = toString {
    "latitude" to latitude
    "longitude" to longitude
    if (altitude != 0.0)
      "altitude" to altitude
  }
}

private const val A = 6378137.0 // equatorial radius [m]
private const val B = 6356752.3 // polar radius [m]

private const val A_POW2 = A * A
private const val B_POW2 = B * B

private const val B_POW2_DIV_BY_A_POW2 = B_POW2 / A_POW2

/**
 * Converts the [GeodeticPosition] to an ECEF (earth-centered, earth-fixed)
 * position, the unit of each component (x, y, z) is in meters.
 */
fun GeodeticPosition.toEcefPosition(): Vector3d {
  // https://en.wikipedia.org/wiki/Geographic_coordinate_conversion#From_geodetic_to_ECEF_coordinates

  val radLat = Math.toRadians(latitude)
  val radLon = Math.toRadians(longitude)

  val sinLat = sin(radLat)
  val cosLat = cos(radLat)
  val sinLon = sin(radLon)
  val cosLon = cos(radLon)

  val n = A_POW2 / sqrt(A_POW2 * cosLat * cosLat + B_POW2 * sinLat * sinLat)
  val x = (n + altitude) * cosLat * cosLon
  val y = (n + altitude) * cosLat * sinLon
  val z = (B_POW2_DIV_BY_A_POW2 * n + altitude) * sinLat

  return Vector3d(x, y, z)
}
