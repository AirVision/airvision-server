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
import io.github.airvision.util.json.boolean
import io.github.airvision.util.json.floatOrNull
import io.github.airvision.util.json.long
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import java.time.Instant

object OsnTrackWaypointSerializer : KSerializer<OsnTrackResponse.Waypoint> {

  override val descriptor: SerialDescriptor =
    buildSerialDescriptor("OsnTrackWaypoint", kind = StructureKind.LIST)

  override fun deserialize(decoder: Decoder): OsnTrackResponse.Waypoint {
    val json = decoder.decodeSerializableValue(JsonArray.serializer())

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
