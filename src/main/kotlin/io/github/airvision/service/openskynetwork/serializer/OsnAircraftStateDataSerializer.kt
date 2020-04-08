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
import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.Serializer
import kotlinx.serialization.StructureKind
import kotlinx.serialization.decode
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.content
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.long
import java.time.Instant

@Serializer(forClass = AircraftStateData::class)
object OsnAircraftStateDataSerializer : KSerializer<AircraftStateData> {

  override val descriptor: SerialDescriptor =
      SerialDescriptor("OsnAircraft", kind = StructureKind.LIST)

  override fun deserialize(decoder: Decoder): AircraftStateData {
    val json = decoder.decode(JsonArray.serializer())

    val aircraftId = AircraftIcao24.parse(json[0].content)
    val callsign = json[1].contentOrNull
    val time = Instant.ofEpochSecond(json[4].long)
    val longitude = json[5].contentOrNull?.toDoubleOrNull()
    val latitude = json[6].contentOrNull?.toDoubleOrNull()
    val baroAltitude = json[7].contentOrNull?.toDoubleOrNull()
    val geoAltitude = json[13].contentOrNull?.toDoubleOrNull()
    val velocity = json[9].contentOrNull?.toDoubleOrNull()
    val verticalRate = json[11].contentOrNull?.toDoubleOrNull()
    val heading = json[10].contentOrNull?.toDoubleOrNull()
    val onGround = json[8].boolean
    val altitude = baroAltitude ?: geoAltitude ?: 0.0
    val position = if (latitude != null && longitude != null) {
      GeodeticPosition(latitude, longitude, altitude)
    } else null

    return AircraftStateData(aircraftId, time, position, velocity, onGround, verticalRate, heading, callsign)
  }

  override fun serialize(encoder: Encoder, value: AircraftStateData) =
      throw UnsupportedOperationException()
}
