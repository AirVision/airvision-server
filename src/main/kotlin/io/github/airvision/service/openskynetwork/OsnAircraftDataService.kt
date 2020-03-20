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

import io.github.airvision.AirVision
import io.github.airvision.service.AircraftData
import io.github.airvision.util.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import java.net.SocketTimeoutException

@Suppress("NON_APPLICABLE_CALL_FOR_BUILDER_INFERENCE")
class OsnAircraftDataService(
    private val dataSendChannel: SendChannel<AircraftData>,
    private val restService: OsnRestService
) {

  private var job: Job? = null

  fun init() {
    job = GlobalScope.launch(Dispatchers.IO) {
      read()
    }
  }

  fun shutdown() {
    job?.cancel()
    job = null
  }

  private suspend fun read() {
    AirVision.logger.info("OSN: Started reading with rate limit ${restService.rateLimit}")
    while (true) {
      try {
        val (_, aircrafts) = restService.getAircrafts()
        AirVision.logger.debug("OSN: Received data for ${aircrafts?.size ?: 0} aircraft's.")
        if (aircrafts != null) {
          for (aircraft in aircrafts)
            dataSendChannel.send(aircraft)
        }
      } catch (ex: SocketTimeoutException) {
        AirVision.logger.debug("OSN: Timeout while trying to receive data for aircraft's.")
      }
      delay(restService.rateLimit) // TODO: Possibly read faster?
    }
  }
}
