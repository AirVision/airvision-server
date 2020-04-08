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

import io.github.airvision.Airport
import io.github.airvision.AirportIata
import io.github.airvision.AirportIcao

/**
 * Represents a service that can provide information about [Airport]s.
 */
interface AirportService {

  /**
   * Gets all the known [Airport]s.
   */
  suspend fun getAll(): Collection<Airport>

  /**
   * Gets the airport for the given [AirportIcao], if it exists.
   */
  suspend fun get(icao: AirportIcao): Airport?

  /**
   * Gets the airport for the given [AirportIata], if it exists.
   */
  suspend fun get(iata: AirportIata): Airport?
}
