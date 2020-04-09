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
import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.StructureKind
import kotlinx.serialization.decode
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.long
import java.time.Instant

object OsnTrackWaypointSerializer : KSerializer<OsnTrackResponse.Waypoint> {

  override val descriptor: SerialDescriptor =
      SerialDescriptor("OsnTrackWaypoint", kind = StructureKind.LIST)

  override fun deserialize(decoder: Decoder): OsnTrackResponse.Waypoint {
    val json = decoder.decode(JsonArray.serializer())

    val time = Instant.ofEpochSecond(json[0].long)
    val latitude = json[1].floatOrNull
    val longitude = json[2].floatOrNull
    val baroAltitude = json[3].floatOrNull
    val heading = json[4].floatOrNull
    val onGround = json[5].boolean

    return OsnTrackResponse.Waypoint(time, latitude, longitude, baroAltitude, heading, onGround)
  }

  override fun serialize(encoder: Encoder, value: OsnTrackResponse.Waypoint) =
      throw UnsupportedOperationException()

}
