/*
 * AirVision
 *
 * Copyright (c) AirVision <https://www.github.com/AirVision>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
package io.github.airvision.service.test

import io.github.airvision.AirVision
import io.github.airvision.AircraftIcao24
import io.github.airvision.GeodeticPosition
import io.github.airvision.service.AircraftStateData
import io.github.airvision.service.SimpleAircraftStateData
import io.github.airvision.util.coroutines.delay
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.list
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.Instant
import kotlin.time.seconds

class TestAircraftDataProvider(
    private val dataSendChannel: SendChannel<AircraftStateData>
) {

  private lateinit var entries: List<TestDataEntry>
  private var job: Job? = null

  fun init() {
    val text = this::class.java.getResourceAsStream("/test/test_aircraft_states.json").use {
      val reader = BufferedReader(InputStreamReader(it))
      reader.readText()
    }
    entries = Json.parse(TestDataEntry.serializer().list, text)
    job = GlobalScope.launch {
      while (true) {
        val time = Instant.now()
        AirVision.logger.debug("TEST: Received ${entries.size} test aircraft states.")
        entries.forEach { entry ->
          dataSendChannel.send(SimpleAircraftStateData(time, entry.icao24, position = entry.position))
        }
        delay(5.seconds)
      }
    }
  }

  fun shutdown() {
    job?.cancel()
    job = null
  }
}

@Serializable
data class TestDataEntry(
    val icao24: AircraftIcao24,
    val model: String,
    val position: GeodeticPosition
)
