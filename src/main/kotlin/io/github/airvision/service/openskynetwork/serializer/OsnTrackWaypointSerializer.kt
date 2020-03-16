/*
 * AirVision
 *
 * Copyright (c) AirVision <https://www.github.com/AirVision>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
package io.github.airvision.service.openskynetwork.serializer

import io.github.airvision.service.openskynetwork.OsnTrackResponse
import io.github.airvision.serializer.InstantSerializer
import io.github.airvision.serializer.decodeNullableFloat
import io.github.airvision.serializer.structure
import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.StructureKind

object OsnTrackWaypointSerializer : KSerializer<OsnTrackResponse.Waypoint> {

  override val descriptor: SerialDescriptor =
      SerialDescriptor("OsnTrackWaypoint", kind = StructureKind.LIST)

  override fun deserialize(decoder: Decoder): OsnTrackResponse.Waypoint {
    return decoder.structure(descriptor) {
      decodeElementIndex(descriptor)
      val time = decodeSerializableElement(descriptor, 0, InstantSerializer)
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

  override fun serialize(encoder: Encoder, value: OsnTrackResponse.Waypoint) =
      throw UnsupportedOperationException()

}