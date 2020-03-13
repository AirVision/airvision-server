/*
 * AirVision
 *
 * Copyright (c) AirVision <https://www.github.com/AirVision>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
package io.github.airvision.rest.openflights

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import io.github.airvision.Airport
import io.github.airvision.AirportIata
import io.github.airvision.AirportIcao
import io.github.airvision.GeodeticPosition
import io.github.airvision.util.mapIfNotNull
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlin.time.hours

class OpenFlights {

  companion object {
    private val AirportsReadInvalidation = 24.hours.toLongMilliseconds()
  }

  @Volatile private var airportCache: AirportCache? = null

  /**
   * Gets all the known [Airport]s.
   */
  suspend fun getAirports(): Collection<Airport> =
      this.getAirportsCache().map.values

  /**
   * Gets the airport for the given [AirportIcao], if it exists.
   */
  suspend fun getAirport(icao: AirportIcao): Airport? =
      this.getAirportsCache().map[icao]

  /**
   * Gets all the known [Airport]s mapped by their id.
   */
  private suspend fun getAirportsCache(): AirportCache {
    var cache = this.airportCache
    if (cache == null || System.currentTimeMillis() - cache.readTime > AirportsReadInvalidation) {
      val airports = requestAirports()
      val map = airports
          .associateBy { it.icao }
      cache = AirportCache(map,
          System.currentTimeMillis())
      this.airportCache = cache
    }
    return cache
  }

  private class AirportCache(
      val map: Map<AirportIcao, Airport>,
      val readTime: Long
  )

  private suspend fun requestAirports(): List<Airport> {
    val client = HttpClient()
    val content = client.get<String>(
        "https://raw.githubusercontent.com/jpatokal/openflights/master/data/airports.dat")
    return csvReader()
        .readAll(content)
        .asSequence()
        .map { row -> decodeAirport(row) }
        .filter { airport -> airport != null }
        .map { airport -> airport!! }
        .toList()
  }

  private fun decodeAirport(list: List<String>): Airport? {
    val icao = list[5].nullable().mapIfNotNull { AirportIcao(it) } ?: return null
    // val id = list[0].toInt()
    val name = list[1]
    val city = list[2]
    val country = list[3]
    val iata = list[4].nullable().mapIfNotNull { AirportIata(it) }!!
    val latitude = list[6].toDouble()
    val longitude = list[7].toDouble()
    // convert from foot to meters
    val altitude = list[8].nullable().mapIfNotNull { it.toDouble() * 0.3048 } ?: 0.0
    val position = GeodeticPosition(latitude, longitude, altitude)
    // val timezone = list[9].toDoubleOrNull()
    // val dst = list[10]
    return Airport(icao, iata, name, city, country, position)
  }

  private fun String.nullable() = if (this == "\\N") null else this
}
