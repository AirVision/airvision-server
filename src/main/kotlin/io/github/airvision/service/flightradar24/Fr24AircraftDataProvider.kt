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
import io.github.airvision.util.time.minus
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.minutes
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

    val extendedDataQueue = Channel<String>(Channel.UNLIMITED).distinct()

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
          AirVision.logger.debug("FR24: Received ${entries.size} aircraft states and flight data.")
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

    val readExtendedDataJob = launch {
      val lastUpdateTime = mutableMapOf<String, Instant>()

      while (true) {
        val id = extendedDataQueue.receive()
        if (id in lastUpdateTime)
          continue

        val result = restService.getExtendedFlightData(id)
        result.fold({ failure ->
          when (failure) {
            Failure.Timeout -> AirVision.logger.debug("FR24: Timeout while trying to get extended flight data for $id.")
            is Failure.ErrorResponse -> {
              if (failure.response.status != HttpStatusCode.PaymentRequired)
                AirVision.logger.debug("FR24: ${failure.message}")
            }
            is Failure.InternalError -> { AirVision.logger.debug("Internal server error", failure.exception) }
          }
        }, { data ->
          if (data != null) {
            dataSendChannel.send(data)
            AirVision.logger.debug("FR24: Received extended flight data for ${data.aircraftId}")
          }
          lastUpdateTime[id] = Instant.now()
        })

        val now = Instant.now()
        lastUpdateTime.values.removeIf { it - now > 5.minutes }
      }
    }

    readEntriesJob.join()
    readExtendedDataJob.join()
  }
}
