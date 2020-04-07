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
import io.github.airvision.util.channel.distinct
import io.github.airvision.util.coroutines.delay
import io.github.airvision.util.ktor.Failure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
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

  private suspend fun read() = coroutineScope {
    AirVision.logger.info("FR24: Started reading with rate limit $rateLimit")

    val extendedDataQueue = Channel<String>().distinct()

    val readEntriesJob = launch {
      while (true) {
        val result = restService.getData()
        result.fold({ failure ->
          when (failure) {
            Failure.Timeout -> AirVision.logger.debug("FR24: Timeout while trying to receive aircraft states.")
            is Failure.ErrorResponse -> AirVision.logger.debug("FR24: ${failure.message}")
            is Failure.InternalError -> AirVision.logger.debug("Internal server error", failure.exception)
          }
          delay(1.seconds)
        }, { entries ->
          AirVision.logger.debug("FR24: Received ${entries.size} entries of aircraft flight data.")
          for (entry in entries) {
            extendedDataQueue.offer(entry.id)
          }
          for (entry in entries) {
            dataSendChannel.send(entry)
          }
          delay(rateLimit)
        })
      }
    }

    /*
    val readExtendedDataJob = launch {
      while (true) {
        val id = extendedDataQueue.receive()

        val result = restService.getExtendedFlightData(id)
        result.fold({ failure ->
          when (failure) {
            Failure.Timeout -> AirVision.logger.debug("FR24: Timeout while trying to get extended flight data for $id.")
            is Failure.ErrorResponse -> AirVision.logger.debug("FR24: ${failure.message}")
            is Failure.InternalError -> AirVision.logger.debug("Internal server error", failure.exception)
          }
        }, { data ->
          if (data != null)
            dataSendChannel.send(data)
        })
      }
    }
    readExtendedDataJob.join()
    */

    readEntriesJob.join()
  }
}
