/*
 * AirVision
 *
 * Copyright (c) AirVision <https://www.github.com/AirVision>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
package io.github.airvision.service.openskynetwork

import com.github.benmanes.caffeine.cache.Caffeine
import io.github.airvision.AircraftFlight
import io.github.airvision.AircraftIcao24
import io.github.airvision.GeodeticPosition
import io.github.airvision.Waypoint
import io.github.airvision.service.AirportService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.await
import kotlin.time.Duration
import kotlin.time.toJavaDuration

class OsnAircraftFlightService(
  private val osnRestService: OsnRestService,
  private val airportService: AirportService
) {

  private val cache = Caffeine.newBuilder()
    .expireAfterWrite(Duration.seconds(20).toJavaDuration())
    .executor(Dispatchers.Default.asExecutor())
    .buildAsync<AircraftIcao24, AircraftFlight> { key, executor ->
      GlobalScope.async(executor.asCoroutineDispatcher()) {
        loadFlight(key)
      }.asCompletableFuture()
    }

  fun init() {
  }

  private suspend fun loadFlight(icao24: AircraftIcao24): AircraftFlight? {
    return coroutineScope {
      val trackTask = async { osnRestService.getTrack(icao24) }
      val flightTask = async { osnRestService.getFlight(icao24) }

      val flight = flightTask.await().orNull() ?: return@coroutineScope null

      val arrivalAirportTask = if (flight.estArrivalAirport != null) {
        async { airportService.get(flight.estArrivalAirport) }
      } else null

      val departureAirportTask = if (flight.estDepartureAirport != null) {
        async { airportService.get(flight.estDepartureAirport) }
      } else null

      val track = trackTask.await().orNull()
      val arrivalAirport = arrivalAirportTask?.await()
      val departureAirport = departureAirportTask?.await()
      val estimatedArrivalTime = flight.lastSeen

      val waypoints = if (track != null) {
        var latitude = track.path.firstOrNull { it.latitude != null }?.latitude
        var longitude = track.path.firstOrNull { it.longitude != null }?.longitude
        var altitude = track.path.firstOrNull { it.baroAltitude != null }?.baroAltitude ?: 0f
        if (latitude == null || longitude == null) {
          // No latitude or longitude is known?
          null
        } else {
          val waypoints = mutableListOf<Waypoint>()
          for (waypoint in track.path) {
            if (waypoint.latitude == null && waypoint.longitude == null)
              continue
            if (waypoint.latitude != null)
              latitude = waypoint.latitude
            if (waypoint.longitude != null)
              longitude = waypoint.longitude
            if (waypoint.baroAltitude != null)
              altitude = waypoint.baroAltitude
            val position = GeodeticPosition(
              latitude = latitude!!.toDouble(),
              longitude = longitude!!.toDouble(),
              altitude = altitude.toDouble()
            )
            waypoints += Waypoint(waypoint.time, position)
          }
          waypoints
        }
      } else null

      AircraftFlight(
        icao24 = icao24,
        arrivalAirport = arrivalAirport,
        departureAirport = departureAirport,
        estimatedArrivalTime = estimatedArrivalTime,
        waypoints = waypoints
      )
    }
  }

  suspend fun getFlight(icao24: AircraftIcao24): AircraftFlight? = cache.get(icao24).await()
}
