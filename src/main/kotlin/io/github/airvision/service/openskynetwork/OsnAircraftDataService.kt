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
import io.ktor.client.features.ServerResponseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.time.seconds

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
      suspend fun handleTimeout() {
        AirVision.logger.debug("OSN: Timeout while trying to receive aircraft states.")
        delay(1.seconds)
      }
      try {
        val (_, states) = withTimeout(20000) { restService.getAircrafts() }
        AirVision.logger.debug("OSN: Received ${states?.size ?: 0} aircraft states.")
        if (states != null) {
          for (aircraft in states)
            dataSendChannel.send(aircraft)
        }
        delay(restService.rateLimit) // TODO: Possibly read faster?
      } catch (ex: TimeoutCancellationException) {
        handleTimeout()
      } catch (ex: SocketTimeoutException) {
        handleTimeout()
      } catch (ex: UnknownHostException) {
        handleTimeout()
      } catch (ex: ServerResponseException) {
        AirVision.logger.debug("OSN: ${ex.message ?: "Server error"}")
        delay(1.seconds)
      } catch (ex: Exception) {
        AirVision.logger.debug("Internal server error", ex)
        delay(1.seconds)
      }
    }
  }
}
