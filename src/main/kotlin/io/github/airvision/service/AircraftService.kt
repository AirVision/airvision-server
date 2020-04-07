/*
 * AirVision
 *
 * Copyright (c) AirVision <https://www.github.com/AirVision>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
package io.github.airvision.service

import io.github.airvision.AircraftFlight
import io.github.airvision.AircraftState
import io.github.airvision.AircraftIcao24
import io.github.airvision.GeodeticBounds
import io.github.airvision.service.adsb.AdsBAircraftDataProvider
import io.github.airvision.service.flightradar24.Fr24AircraftDataProvider
import io.github.airvision.service.flightradar24.Fr24RestService
import io.github.airvision.service.openskynetwork.OsnAircraftDataProvider
import io.github.airvision.service.openskynetwork.OsnAircraftFlightService
import io.github.airvision.service.openskynetwork.OsnRestService
import io.github.airvision.service.test.TestAircraftDataProvider
import kotlinx.coroutines.CoroutineDispatcher
import org.jetbrains.exposed.sql.Database
import java.time.Instant

class AircraftService(
    private val database: Database,
    private val databaseUpdateDispatcher: CoroutineDispatcher,
    private val osnRestService: OsnRestService,
    private val fr24RestService: Fr24RestService,
    private val airportService: AirportService
) {

  private lateinit var dataService: AircraftDataService
  private lateinit var adsBService: AdsBAircraftDataProvider
  private lateinit var osnAircraftFlightService: OsnAircraftFlightService
  private lateinit var osnAircraftDataProvider: OsnAircraftDataProvider
  private lateinit var fr24AircraftDataProvider: Fr24AircraftDataProvider
  private lateinit var testAircraftDataProvider: TestAircraftDataProvider

  fun init() {
    val dataService = AircraftDataService(database, databaseUpdateDispatcher)
        .also { dataService = it }
    dataService.init()

    val adsBService = AdsBAircraftDataProvider(dataService.sendChannel)
        .also { adsBService = it }
    adsBService.init()

    val osnAircraftDataService = OsnAircraftDataProvider(dataService.sendChannel, osnRestService)
        .also { osnAircraftDataProvider = it }
    osnAircraftDataService.init()

    val fr24RestService = Fr24AircraftDataProvider(dataService.sendChannel, fr24RestService)
        .also { fr24AircraftDataProvider = it }
    fr24RestService.init()

    val testAircraftDataProvider = TestAircraftDataProvider(dataService.sendChannel)
        .also { testAircraftDataProvider = it }
    testAircraftDataProvider.init()

    val osnAircraftFlightService = OsnAircraftFlightService(osnRestService, airportService)
        .also { osnAircraftFlightService = it }
    osnAircraftFlightService.init()
  }

  fun shutdown() {
    dataService.shutdown()
    adsBService.shutdown()
    osnAircraftDataProvider.shutdown()
    fr24AircraftDataProvider.shutdown()
    testAircraftDataProvider.shutdown()
  }

  suspend fun getAll(time: Instant? = null): Collection<AircraftState> {
    return dataService.getStates(time = time).map { it.toAircraft() }
  }

  suspend fun getAllWithin(bounds: GeodeticBounds, time: Instant? = null): Collection<AircraftState> {
    return dataService.getStates(bounds = bounds, time = time).map { it.toAircraft() }
  }

  suspend fun get(aircraftId: AircraftIcao24, time: Instant? = null): AircraftState? {
    return dataService.getState(aircraftId, time)?.toAircraft()
  }

  private fun AircraftStateData.toAircraft(): AircraftState {
    return AircraftState(time = time, icao24 = aircraftId, onGround = onGround, velocity = velocity,
        position = position, heading = heading, verticalRate = verticalRate)
  }

  suspend fun getFlight(aircraftId: AircraftIcao24): AircraftFlight? {
    var flight = osnAircraftFlightService.getFlight(aircraftId)
    if (flight?.departureAirport != null && flight.arrivalAirport != null)
      return flight
    val data = dataService.getFlight(aircraftId)
    if (data != null) {
      val departureAirport = data.departureAirport?.let { airportService.get(it) }
      val arrivalAirport = data.arrivalAirport?.let { airportService.get(it) }

      if (flight != null) {
        if (flight.departureAirport == null && departureAirport != null)
          flight = flight.copy(departureAirport = departureAirport)
        if (flight.arrivalAirport == null && arrivalAirport != null)
          flight = flight.copy(arrivalAirport = arrivalAirport)
        if (flight.number == null && data.flightNumber.orNull() != null)
          flight = flight.copy(number = data.flightNumber.orNull())
      } else {
        flight = AircraftFlight(aircraftId, data.flightNumber.orNull(), departureAirport,
            arrivalAirport, null, null)
      }
    }
    return flight
  }
}
