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
import kotlin.time.Duration
import kotlin.time.seconds

class Fr24AircraftDataProvider(
    private val dataSendChannel: SendChannel<AircraftData>,
    private val restService: Fr24RestService,
    private val rateLimit: Duration = 5.seconds
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

  // TODO: Less duplication
  private suspend fun read() {
    AirVision.logger.info("FR24: Started reading with rate limit $rateLimit")
    while (true) {
      suspend fun handleTimeout() {
        AirVision.logger.debug("FR24: Timeout while trying to receive aircraft states.")
        delay(1.seconds)
      }
      try {
        val entries = withTimeout(20000) { restService.getFlightData() }
        AirVision.logger.debug("FR24: Received ${entries.size} entries of aircraft flight data.")
        for (entry in entries)
          dataSendChannel.send(entry)
        delay(rateLimit)
      } catch (ex: TimeoutCancellationException) {
        handleTimeout()
      } catch (ex: SocketTimeoutException) {
        handleTimeout()
      } catch (ex: UnknownHostException) {
        handleTimeout()
      } catch (ex: ServerResponseException) {
        AirVision.logger.debug("FR24: ${ex.message ?: "Server error"}")
        delay(1.seconds)
      } catch (ex: Exception) {
        AirVision.logger.debug("Internal server error", ex)
        delay(1.seconds)
      }
    }
  }
}
