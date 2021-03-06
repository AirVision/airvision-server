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

import io.github.airvision.util.math.degToRad
import org.spongepowered.math.imaginary.Quaterniond
import org.spongepowered.math.matrix.Matrix3d
import org.spongepowered.math.vector.Vector3d
import kotlin.math.cos
import kotlin.math.sin

/**
 * Represents a transform within a ENU coordinate system.
 *
 * @property position The geodetic position
 * @property rotation The rotation
 */
data class EnuTransform(
  val position: GeodeticPosition,
  val rotation: Quaterniond
)

/**
 * Converts the [EnuTransform] to an ECEF (earth-centered, earth-fixed)
 * transform, the unit of each position component (x, y, z) is in meters.
 */
fun EnuTransform.toEcefTransform(): Transform {
  val ecefPosition = position.toEcefPosition()

  val enuToEcefMatrix = position.getEnuToEcefRotationMatrix()
  val ecefRotation = Quaterniond.fromRotationTo(
    Vector3d.UNIT_Z, enuToEcefMatrix.transform(rotation.rotate(Vector3d.UNIT_Z)))

  return Transform(ecefPosition, ecefRotation)
}

/**
 * Gets a rotation matrix that can be used to convert from an ENU coordinate system
 * at the target position to the ECEF coordinate system.
 */
private fun GeodeticPosition.getEnuToEcefRotationMatrix(): Matrix3d {
  val radLat = degToRad(latitude)
  val radLon = degToRad(longitude)

  val sinLat = sin(radLat)
  val cosLat = cos(radLat)
  val sinLon = sin(radLon)
  val cosLon = cos(radLon)

  // https://en.wikipedia.org/wiki/Geographic_coordinate_conversion#From_ENU_to_ECEF

  return Matrix3d.from(
    -sinLon, -sinLat * cosLon, cosLat * cosLon,
    cosLon, -sinLat * sinLon, cosLat * sinLon,
    0.0, cosLat, sinLat
  )
}
