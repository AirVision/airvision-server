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

import io.github.airvision.Aircraft
import io.github.airvision.AircraftIcao24
import io.github.airvision.GeodeticBounds
import io.github.airvision.service.adsb.AdsBService
import io.github.airvision.service.openskynetwork.OsnAircraftDataService
import io.github.airvision.service.openskynetwork.OsnRestService
import kotlinx.coroutines.asCoroutineDispatcher
import org.jetbrains.exposed.sql.Database
import java.time.Instant
import java.util.concurrent.Executors

class AircraftService(
    private val database: Database,
    private val osnRestService: OsnRestService
) {

  private lateinit var dataService: AircraftDataService
  private lateinit var adsBService: AdsBService
  private lateinit var osnAircraftDataService: OsnAircraftDataService

  private val databaseUpdateDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

  fun init() {
    val dataService = AircraftDataService(database, databaseUpdateDispatcher).also { dataService = it }
    dataService.init()

    val adsBService = AdsBService(dataService.sendChannel).also { adsBService = it }
    adsBService.init()

    val osnAircraftDataService = OsnAircraftDataService(dataService.sendChannel, osnRestService).also { osnAircraftDataService = it }
    osnAircraftDataService.init()
  }

  fun shutdown() {
    dataService.shutdown()
    adsBService.shutdown()
    osnAircraftDataService.shutdown()
  }

  suspend fun getAll(time: Instant? = null): Collection<Aircraft> {
    return dataService.getAircrafts(time = time).map { it.toAircraft() }
  }

  suspend fun getAllWithin(bounds: GeodeticBounds, time: Instant? = null): Collection<Aircraft> {
    return dataService.getAircrafts(bounds = bounds, time = time).map { it.toAircraft() }
  }

  suspend fun get(icao24: AircraftIcao24, time: Instant? = null): Aircraft? {
    return dataService.getAircraft(icao24, time)?.toAircraft()
  }
}
