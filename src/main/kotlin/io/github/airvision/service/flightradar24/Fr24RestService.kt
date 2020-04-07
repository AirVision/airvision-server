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

import arrow.core.Either
import arrow.core.Some
import io.github.airvision.AircraftIcao24
import io.github.airvision.AirportIata
import io.github.airvision.service.AirportService
import io.github.airvision.util.ktor.Failure
import io.github.airvision.util.ktor.requestTimeout
import io.github.airvision.util.ktor.tryToGet
import io.github.airvision.util.suspendedMap
import io.github.airvision.util.toNullIfEmpty
import io.ktor.client.HttpClient
import io.ktor.client.features.HttpTimeout
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.content
import kotlinx.serialization.json.contentOrNull
import java.time.Instant
import kotlin.time.seconds

class Fr24RestService(
    private val airportService: AirportService
) {

  private val baseUrl = "https://data-live.flightradar24.com"

  private val client = HttpClient {
    install(JsonFeature) {
      serializer = KotlinxSerializer(Json(JsonConfiguration.Default))
    }
    install(HttpTimeout) {
      requestTimeout = 20.seconds
    }
  }

  suspend fun getFlightData(): Either<Failure, List<FrAircraftFlightData>> {
    val result = client.tryToGet<JsonObject>(
        "$baseUrl/zones/fcgi/feed.js?gnd=1")
    val time = Instant.now()
    return result.suspendedMap { json ->
      json.entries
        .filter { (_, value) -> value is JsonArray }
        .mapNotNull { (key, value) -> value.jsonArray.toFlightData(key, time) }
    }
  }

  suspend fun getExtendedFlightData(id: String): Either<Failure, FrAircraftFlightData?> {
    val result = client.tryToGet<JsonObject>(
        "$baseUrl/clickhandler?flight=$id&version=1.5")
    return result.suspendedMap { json -> json.toExtendedFlightData() }
  }

  private suspend fun JsonObject.toExtendedFlightData(): FrAircraftFlightData? {
    return TODO()
  }

  private suspend fun JsonArray.toFlightData(id: String, time: Instant): FrAircraftFlightData? {
    val icao24Raw = this[0].content
    if (icao24Raw.isEmpty())
      return null
    val icao24 = AircraftIcao24.parse(icao24Raw)

    val code = Some(this[13].contentOrNull?.toNullIfEmpty())
    val departureAirportIata = this[11].contentOrNull?.toNullIfEmpty()?.let { AirportIata(it) }
    val arrivalAirportIata = this[12].contentOrNull?.toNullIfEmpty()?.let { AirportIata(it) }

    val departureAirport = departureAirportIata?.let { airportService.get(it) }
    val arrivalAirport = arrivalAirportIata?.let { airportService.get(it) }

    return FrAircraftFlightData(icao24, id, time, code, departureAirport?.icao, arrivalAirport?.icao)
  }
}
