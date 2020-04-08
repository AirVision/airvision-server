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
import arrow.core.None
import arrow.core.Some
import io.github.airvision.AircraftIcao24
import io.github.airvision.AirportIata
import io.github.airvision.AirportIcao
import io.github.airvision.GeodeticPosition
import io.github.airvision.Waypoint
import io.github.airvision.service.AircraftFlightData
import io.github.airvision.service.AircraftStateData
import io.github.airvision.service.AirportService
import io.github.airvision.util.feetPerMinuteToMetersPerSecond
import io.github.airvision.util.feetToMeters
import io.github.airvision.util.json.getPrimitiveOrNull
import io.github.airvision.util.json.getStringOrNull
import io.github.airvision.util.json.instantOrNull
import io.github.airvision.util.json.pathOf
import io.github.airvision.util.knotsToMetersPerSecond
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

  suspend fun getData(): Either<Failure, List<Fr24AircraftData>> {
    val result = client.tryToGet<JsonObject>(
        "$baseUrl/zones/fcgi/feed.js?gnd=1")
    val time = Instant.now()
    return result.suspendedMap { json ->
      json.entries
        .filter { (_, value) -> value is JsonArray }
        .mapNotNull { (key, value) -> value.jsonArray.toFlightData(key, time) }
    }
  }

  suspend fun getExtendedFlightData(id: String): Either<Failure, AircraftFlightData?> {
    val result = client.tryToGet<JsonObject>(
        "$baseUrl/clickhandler?flight=$id&version=1.5")
    return result.map { json -> json.toExtendedFlightData() }
  }

  private object ExtendedDataPaths {
    val flightNumber = pathOf("identification", "number", "default")
    val aircraftId = pathOf("aircraft", "hex")
    val estimatedArrivalTime = pathOf("time", "estimated", "arrival")
    val realArrivalTime = pathOf("time", "real", "arrival")
    val departureAirport = pathOf("airport", "origin", "code", "icao")
    val arrivalAirport = pathOf("airport", "destination", "code", "icao")
  }

  private fun JsonObject.toExtendedFlightData(): AircraftFlightData? {
    val rawAircraftId = getStringOrNull(ExtendedDataPaths.aircraftId) ?: return null

    val aircraftId = AircraftIcao24.parse(rawAircraftId)
    val flightNumber = getStringOrNull(ExtendedDataPaths.flightNumber)

    val estimatedArrivalTime = getPrimitiveOrNull(ExtendedDataPaths.estimatedArrivalTime)?.instantOrNull
        ?: getPrimitiveOrNull(ExtendedDataPaths.realArrivalTime)?.instantOrNull
    val departureAirport = getStringOrNull(ExtendedDataPaths.departureAirport)?.let { AirportIcao(it) }
    val arrivalAirport = getStringOrNull(ExtendedDataPaths.arrivalAirport)?.let { AirportIcao(it) }

    val trailArray = getArrayOrNull("trail")
    val waypoints = trailArray?.content
        ?.map { it.jsonObject }
        ?.map { json ->
          val time = Instant.ofEpochSecond(json.getPrimitive("ts").long)
          val latitude = json.getPrimitive("lat").double
          val longitude = json.getPrimitive("lng").double
          val altitude = json.getPrimitive("alt").double.feetToMeters()
          Waypoint(time, GeodeticPosition(latitude, longitude, altitude))
        }

    return AircraftFlightData(aircraftId, Instant.now(),
        departureAirport, arrivalAirport, Some(estimatedArrivalTime), Some(flightNumber), Some(waypoints))
  }

  private suspend fun JsonArray.toFlightData(id: String, receiveTime: Instant): Fr24AircraftData? {
    val rawAircraftId = this[0].content
    if (rawAircraftId.isEmpty())
      return null

    val aircraftId = AircraftIcao24.parse(rawAircraftId)
    val callsign = this[16].contentOrNull?.toNullIfEmpty()

    val latitude = this[1].contentOrNull?.toDoubleOrNull()
    val longitude = this[2].contentOrNull?.toDoubleOrNull()
    val altitude = this[4].contentOrNull?.toDoubleOrNull()?.feetToMeters()

    val position = if (latitude != null && longitude != null) {
      GeodeticPosition(latitude, longitude, altitude ?: 0.0)
    } else null

    val time = this[10].contentOrNull?.toLongOrNull()?.let { Instant.ofEpochSecond(it) } ?: receiveTime
    val heading = this[3].contentOrNull?.toDoubleOrNull()
    val velocity = this[5].contentOrNull?.toDoubleOrNull()?.knotsToMetersPerSecond()
    val verticalRate = this[15].contentOrNull?.toDoubleOrNull()?.feetPerMinuteToMetersPerSecond()
    val onGround = this[14].contentOrNull?.toIntOrNull() != 1

    val flightNumber = Some(this[13].contentOrNull?.toNullIfEmpty())
    val departureAirportIata = this[11].contentOrNull?.toNullIfEmpty()?.let { AirportIata(it) }
    val arrivalAirportIata = this[12].contentOrNull?.toNullIfEmpty()?.let { AirportIata(it) }

    val departureAirport = departureAirportIata?.let { airportService.get(it) }
    val arrivalAirport = arrivalAirportIata?.let { airportService.get(it) }

    val flightData = AircraftFlightData(aircraftId, time,
        departureAirport?.icao, arrivalAirport?.icao, None, flightNumber, None)
    val stateData = AircraftStateData(aircraftId, time,
        position, velocity, onGround, verticalRate, heading, callsign)

    return Fr24AircraftData(aircraftId, stateData, id, flightData)
  }
}
