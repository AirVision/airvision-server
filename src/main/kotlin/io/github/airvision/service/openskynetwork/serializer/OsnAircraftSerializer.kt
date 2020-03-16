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

import io.github.airvision.service.openskynetwork.OsnAircraft
import io.github.airvision.service.openskynetwork.OsnPositionSource
import io.github.airvision.serializer.InstantSerializer
import io.github.airvision.serializer.collection
import io.github.airvision.serializer.decodeIcao24
import io.github.airvision.serializer.decodeNullableFloat
import io.github.airvision.serializer.decodeNullableIntArray
import io.github.airvision.serializer.decodeNullableString
import io.github.airvision.util.toNullIfEmpty
import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.StructureKind
import kotlinx.serialization.builtins.nullable

object OsnAircraftSerializer : KSerializer<OsnAircraft> {

  override val descriptor: SerialDescriptor =
      SerialDescriptor("OsnAircraft", kind = StructureKind.LIST)

  private val positionSources = OsnPositionSource.values()
  private val nullableInstantSerializer = InstantSerializer.nullable

  override fun deserialize(decoder: Decoder): OsnAircraft {
    return decoder.collection(descriptor) {
      decodeElementIndex(descriptor)
      val icao24 = decodeIcao24(descriptor, 0)
      decodeElementIndex(descriptor)
      val callsign = decodeNullableString(descriptor, 1)?.trim()?.toNullIfEmpty()
      decodeElementIndex(descriptor)
      val originCountry = decodeStringElement(descriptor, 2)
      decodeElementIndex(descriptor)
      val timePosition = decodeNullableSerializableElement(descriptor, 3, nullableInstantSerializer)
      decodeElementIndex(descriptor)
      val lastContact = decodeSerializableElement(descriptor, 4, InstantSerializer)
      decodeElementIndex(descriptor)
      val longitude = decodeNullableFloat(descriptor, 5)
      decodeElementIndex(descriptor)
      val latitude = decodeNullableFloat(descriptor, 6)
      decodeElementIndex(descriptor)
      val baroAltitude = decodeNullableFloat(descriptor, 7)
      decodeElementIndex(descriptor)
      val onGround = decodeBooleanElement(descriptor, 8)
      decodeElementIndex(descriptor)
      val velocity = decodeNullableFloat(descriptor, 9)
      decodeElementIndex(descriptor)
      val trueTrack = decodeNullableFloat(descriptor, 10)
      decodeElementIndex(descriptor)
      val verticalRate = decodeNullableFloat(descriptor, 11)
      decodeElementIndex(descriptor)
      val sensors = decodeNullableIntArray(descriptor, 12)
      decodeElementIndex(descriptor)
      val geoAltitude = decodeNullableFloat(descriptor, 13)
      decodeElementIndex(descriptor)
      val squawk = decodeNullableString(descriptor, 14)
      decodeElementIndex(descriptor)
      val spi = decodeBooleanElement(descriptor, 15)
      decodeElementIndex(descriptor)
      val posSourceIndex = decodeIntElement(descriptor, 16)
      val posSource = if (posSourceIndex >= positionSources.size)
        OsnPositionSource.ADS_B else positionSources[posSourceIndex]
      OsnAircraft(icao24, callsign, originCountry, timePosition, lastContact, longitude, latitude, baroAltitude,
          onGround, velocity, trueTrack, verticalRate, sensors, geoAltitude, squawk, spi, posSource)
    }
  }

  override fun serialize(encoder: Encoder, value: OsnAircraft) =
      throw UnsupportedOperationException()
}
