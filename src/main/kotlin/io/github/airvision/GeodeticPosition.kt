/*
 * AirVision
 *
 * Copyright (c) LanternPowered <https://www.github.com/AirVision>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
package io.github.airvision

import io.github.airvision.util.toString
import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.internal.ArrayListClassDesc
import kotlinx.serialization.internal.DoubleDescriptor
import kotlinx.serialization.list
import kotlinx.serialization.serializer
import kotlinx.serialization.withName
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
    val altitude: Double
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

  val n = A_POW2 / (sqrt(A_POW2 * cosLat * cosLat + B_POW2 * sinLat * sinLat))
  val x = (n + altitude) * cosLat * cosLon
  val y = (n + altitude) * cosLat * sinLon
  val z = (B_POW2_DIV_BY_A_POW2 * n + altitude) * sinLat

  return Vector3d(x, y, z)
}

/**
 * A serializer for [GeodeticPosition]s.
 */
@Serializer(forClass = GeodeticPosition::class)
object GeodeticPositionSerializer : KSerializer<GeodeticPosition> {

  private val doubleListSerializer = Double.serializer().list

  override val descriptor: SerialDescriptor =
      ArrayListClassDesc(DoubleDescriptor.withName("GeodeticPosition"))
      /*
      SerialDescriptor("GeodeticPosition", kind = StructureKind.LIST) {
        listDescriptor<Double>()
      }
      */

  override fun deserialize(decoder: Decoder): GeodeticPosition {
    val list = doubleListSerializer.deserialize(decoder)
    check(list.size == 2 || list.size == 3)
    return GeodeticPosition(list[0], list[1], if (list.size == 3) list[2] else 0.0)
  }

  override fun serialize(encoder: Encoder, value: GeodeticPosition) {
    val size = if (value.altitude != 0.0) 3 else 2
    @Suppress("NAME_SHADOWING")
    val encoder = encoder.beginCollection(descriptor, size)
    encoder.encodeDoubleElement(descriptor, 0, value.latitude)
    encoder.encodeDoubleElement(descriptor, 1, value.longitude)
    if (value.altitude != 0.0)
      encoder.encodeDoubleElement(descriptor, 2, value.altitude)
    encoder.endStructure(descriptor)
  }
}
