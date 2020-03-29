/*
 * AirVision
 *
 * Copyright (c) AirVision <https://www.github.com/AirVision>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
package io.github.airvision.service.openflights

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import io.github.airvision.Airport
import io.github.airvision.AirportIata
import io.github.airvision.AirportIcao
import io.github.airvision.GeodeticPosition
import io.github.airvision.service.AirportService
import io.github.airvision.util.feetToMeters
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.hours
import kotlin.time.minutes

class OpenFlightsAirportService : AirportService {

  companion object {
    private val AirportsReadInvalidation = 24.hours.toLongMilliseconds()
  }

  @Volatile private var airportCache: AirportCache? = null
  private val mutex = Mutex()

  override suspend fun getAll(): Collection<Airport> =
      getAirportsCache().byIcao.values

  override suspend fun get(icao: AirportIcao): Airport? =
      getAirportsCache().byIcao[icao]

  override suspend fun get(iata: AirportIata): Airport? =
      getAirportsCache().byIata[iata]

  // TODO: Update periodically to reduce delay when updating

  /**
   * Gets all the known [Airport]s mapped by their id.
   */
  private suspend fun getAirportsCache(): AirportCache {
    var cache = airportCache
    if (cache == null || cache.needsUpdate()) {
      // Make sure only one request is made for the data
      mutex.withLock {
        cache = airportCache
        if (cache == null || cache!!.needsUpdate()) {
          cache = try {
            loadCache()
          } catch (ex: Exception) {
            if (cache == null)
              throw ex
            AirportCache(cache!!.byIcao, cache!!.byIata,
                System.currentTimeMillis() + 1.minutes.toLongMilliseconds())
          }
          airportCache = cache
        }
      }
    }
    return cache!!
  }

  private suspend fun loadCache(): AirportCache {
    val airports = requestAirports()
    val byIcao = airports.associateBy { it.icao }
    val byIata = airports.associateBy { it.iata }
    return AirportCache(byIcao, byIata, System.currentTimeMillis() + AirportsReadInvalidation)
  }

  private class AirportCache(
      val byIcao: Map<AirportIcao, Airport>,
      val byIata: Map<AirportIata, Airport>,
      val invalidationTime: Long
  )

  private fun AirportCache.needsUpdate() = System.currentTimeMillis() > invalidationTime

  private suspend fun requestAirports(): List<Airport> {
    val client = HttpClient()
    val content = client.get<String>(
        "https://raw.githubusercontent.com/jpatokal/openflights/master/data/airports.dat")
    return csvReader()
        .readAll(content)
        .asSequence()
        .map { row -> decodeAirport(row) }
        .filterNotNull()
        .toList()
  }

  private fun decodeAirport(list: List<String>): Airport? {
    val icao = list[5].nullable()?.let { AirportIcao(it) } ?: return null
    val iata = list[4].nullable()?.let { AirportIata(it) } ?: return null
    // val id = list[0].toInt()
    val name = list[1]
    val city = list[2]
    val country = list[3]
    val latitude = list[6].toDouble()
    val longitude = list[7].toDouble()
    val altitude = list[8].nullable()?.let { feetToMeters(it.toDouble()) } ?: 0.0
    val position = GeodeticPosition(latitude, longitude, altitude)
    // val timezone = list[9].toDoubleOrNull()
    // val dst = list[10]
    return Airport(icao, iata, name, city, country, position)
  }

  private fun String.nullable() = if (this == "\\N") null else this
}
