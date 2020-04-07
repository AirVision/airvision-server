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
import io.github.airvision.util.coroutines.delay
import io.github.airvision.util.ktor.Failure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
      val mutex = Mutex()

      val queueMap = mutableMapOf<String, FrAircraftFlightData>()
      val queue = ArrayDeque<String>()

      suspend fun queue(data: FrAircraftFlightData) {
        mutex.withLock {
          if (queueMap.putIfAbsent(data.id, data) == null)
            queue.add(data.id)
        }
      }

      suspend fun poll(): FrAircraftFlightData? {
        return mutex.withLock {
          if (queue.isNotEmpty()) {
            val id = queue.removeFirst()
            queueMap.remove(id)
          } else null
        }
      }

      val result = restService.getFlightData()
      result.fold({ failure ->
        when (failure) {
          Failure.Timeout -> {
            AirVision.logger.debug("FR24: Timeout while trying to receive aircraft states.")
            delay(1.seconds)
          }
          is Failure.ErrorResponse -> {
            AirVision.logger.debug("FR24: ${failure.message}")
            delay(1.seconds)
          }
          is Failure.InternalError -> {
            AirVision.logger.debug("Internal server error", failure.exception)
            delay(1.seconds)
          }
        }
      }, { entries ->
        AirVision.logger.debug("FR24: Received ${entries.size} entries of aircraft flight data.")
        for (entry in entries) {
          dataSendChannel.send(entry)
          queue(entry)
        }
      })
      delay(rateLimit)
    }
  }
}
