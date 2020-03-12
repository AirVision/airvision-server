/*
 * AirVision
 *
 * Copyright (c) AirVision <https://www.github.com/AirVision>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
package io.github.airvision.rest.openskynetwork.serializer

import io.github.airvision.rest.openskynetwork.OsnTrackResponse
import io.github.airvision.serializer.decodeNullableFloat
import io.github.airvision.serializer.structure
import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.Serializer
import kotlinx.serialization.internal.ArrayListClassDesc
import kotlinx.serialization.internal.StringDescriptor
import kotlinx.serialization.withName

@Serializer(forClass = OsnTrackResponse.Waypoint::class)
object OsnTrackWaypointSerializer : KSerializer<OsnTrackResponse.Waypoint> {

  override val descriptor: SerialDescriptor =
      ArrayListClassDesc(StringDescriptor.withName("OsnTrackWaypoint"))

  override fun deserialize(decoder: Decoder): OsnTrackResponse.Waypoint {
    return decoder.structure(descriptor) {
      decodeElementIndex(descriptor)
      val time = decodeIntElement(descriptor, 0)
      decodeElementIndex(descriptor)
      val latitude = decodeNullableFloat(descriptor, 1)
      decodeElementIndex(descriptor)
      val longitude = decodeNullableFloat(descriptor, 2)
      decodeElementIndex(descriptor)
      val baroAltitude = decodeNullableFloat(descriptor, 3)
      decodeElementIndex(descriptor)
      val trueTrack = decodeNullableFloat(descriptor, 4)
      decodeElementIndex(descriptor)
      val onGround = decodeBooleanElement(descriptor, 5)
      OsnTrackResponse.Waypoint(time, latitude, longitude, baroAltitude, trueTrack, onGround)
    }
  }

  override fun serialize(encoder: Encoder, obj: OsnTrackResponse.Waypoint) =
      throw UnsupportedOperationException()

}
