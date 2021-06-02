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

import io.github.airvision.AircraftIcao24
import io.github.airvision.GeodeticPosition
import io.github.airvision.service.AircraftStateData
import io.github.airvision.util.json.boolean
import io.github.airvision.util.json.doubleOrNull
import io.github.airvision.util.json.long
import io.github.airvision.util.json.string
import io.github.airvision.util.json.stringOrNull
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import java.time.Instant

@Serializer(forClass = AircraftStateData::class)
object OsnAircraftStateDataSerializer : KSerializer<AircraftStateData> {

  override val descriptor: SerialDescriptor =
    buildSerialDescriptor("OsnAircraft", kind = StructureKind.LIST)

  override fun deserialize(decoder: Decoder): AircraftStateData {
    val json = decoder.decodeSerializableValue(JsonArray.serializer())

    val aircraftId = AircraftIcao24.parse(json[0].string)
    val callsign = json[1].stringOrNull
    val time = Instant.ofEpochSecond(json[4].long)
    val longitude = json[5].doubleOrNull
    val latitude = json[6].doubleOrNull
    val baroAltitude = json[7].doubleOrNull
    val geoAltitude = json[13].doubleOrNull
    val velocity = json[9].doubleOrNull
    val verticalRate = json[11].doubleOrNull
    val heading = json[10].doubleOrNull
    val onGround = json[8].boolean
    val altitude = baroAltitude ?: geoAltitude ?: 0.0
    val position = if (latitude != null && longitude != null) {
      GeodeticPosition(latitude, longitude, altitude)
    } else null

    return AircraftStateData(
      aircraftId,
      time,
      position,
      velocity,
      onGround,
      verticalRate,
      heading,
      callsign
    )
  }

  override fun serialize(encoder: Encoder, value: AircraftStateData) =
    throw UnsupportedOperationException()
}
