/*
 * AirVision
 *
 * Copyright (c) AirVision <https://www.github.com/AirVision>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
package io.github.airvision.service.flightradar24

import io.github.airvision.AircraftIcao24
import io.github.airvision.AirportIata
import io.github.airvision.service.AircraftFlightData
import io.github.airvision.service.AirportService
import io.github.airvision.service.SimpleAircraftFlightData
import io.github.airvision.util.toNullIfEmpty
import io.ktor.client.HttpClient
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.get
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.content
import kotlinx.serialization.json.contentOrNull
import java.time.Instant

class Fr24RestService(
    private val airportService: AirportService
) {

  private val client = HttpClient {
    install(JsonFeature) {
      serializer = KotlinxSerializer(Json(JsonConfiguration.Default))
    }
  }

  suspend fun getFlightData(): List<AircraftFlightData> {
    val json = client.get<JsonObject>(
        "https://data-live.flightradar24.com/zones/fcgi/feed.js?gnd=1")
    val time = Instant.now()
    return json.values
        .filterIsInstance<JsonArray>()
        .mapNotNull { element -> element.jsonArray.toFlightData(time) }
  }

  private suspend fun JsonArray.toFlightData(time: Instant): AircraftFlightData? {
    val icao24Raw = this[0].content
    if (icao24Raw.isEmpty())
      return null
    val icao24 = AircraftIcao24.parse(icao24Raw)

    val code = this[13].contentOrNull?.toNullIfEmpty()
    val departureAirportIata = this[11].contentOrNull?.toNullIfEmpty()?.let { AirportIata(it) }
    val arrivalAirportIata = this[12].contentOrNull?.toNullIfEmpty()?.let { AirportIata(it) }

    val departureAirport = departureAirportIata?.let { airportService.get(it) }
    val arrivalAirport = arrivalAirportIata?.let { airportService.get(it) }

    return SimpleAircraftFlightData(icao24, time, code, departureAirport?.icao, arrivalAirport?.icao)
  }
}
