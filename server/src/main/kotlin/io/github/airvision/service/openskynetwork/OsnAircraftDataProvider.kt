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
import io.github.airvision.service.AircraftStateData
import io.github.airvision.util.coroutines.delay
import io.github.airvision.util.ktor.Failure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlin.time.Duration

@Suppress("NON_APPLICABLE_CALL_FOR_BUILDER_INFERENCE")
class OsnAircraftDataProvider(
    private val dataSendChannel: SendChannel<AircraftStateData>,
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
      val result = restService.getStates()
      result.fold({ failure ->
        when (failure) {
          Failure.Timeout -> {
            AirVision.logger.debug("OSN: Timeout while trying to receive aircraft states.")
            delay(Duration.seconds(1))
          }
          is Failure.ErrorResponse -> {
            AirVision.logger.debug("OSN: ${failure.message}")
            delay(Duration.seconds(1))
          }
          is Failure.InternalError -> {
            AirVision.logger.debug("Internal server error", failure.exception)
            delay(Duration.seconds(1))
          }
        }
      }, { (_, states) ->
        AirVision.logger.debug("OSN: Received ${states?.size ?: 0} aircraft states.")
        if (states != null) {
          for (aircraft in states)
            dataSendChannel.send(aircraft)
        }
        delay(restService.rateLimit)
      })
    }
  }
}
