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

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.RemovalCause
import io.github.airvision.AirVision
import io.github.airvision.AircraftIcao24
import io.github.airvision.GeodeticBounds
import io.github.airvision.GeodeticPosition
import io.github.airvision.exposed.abs
import io.github.airvision.exposed.distinctBy
import io.github.airvision.exposed.orderBy
import io.github.airvision.exposed.upsert
import io.github.airvision.service.db.AircraftFlightTable
import io.github.airvision.service.db.AircraftStateTable
import io.github.airvision.util.delay
import io.github.airvision.util.time.minus
import io.github.airvision.util.time.plus
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.hours
import kotlin.time.minutes
import kotlin.time.seconds
import kotlin.time.toJavaDuration

/**
 * @property database The database
 * @property updateDispatcher The dispatcher used to write updates to the database
 * @property getDispatcher The dispatcher used to get data from the database
 */
class AircraftDataService(
    private val database: Database,
    private val updateDispatcher: CoroutineDispatcher,
    private val getDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

  private val lastAircraftStateData = Caffeine.newBuilder()
      // Also apply the executor to reduce context switches on cleanup
      .executor(updateDispatcher.asExecutor())
      .removalListener<AircraftIcao24, AircraftStateData> { _, value, cause ->
        if (value != null && cause == RemovalCause.EXPIRED) {
          // Process on the dispatcher
          GlobalScope.launch(updateDispatcher) {
            cleanupAircraft(value)
          }
        }
      }
      .expireAfterWrite(60.seconds.toJavaDuration())
      .build<AircraftIcao24, AircraftStateData>()

  private val lastAircraftFlightData = Caffeine.newBuilder()
      .expireAfterWrite(30.seconds.toJavaDuration())
      .build<AircraftIcao24, AircraftFlightData>()

  private val channel = Channel<AircraftData>()
  private val mergeTime = 3.seconds
  private val validTime = 25.seconds

  private var job: Job? = null

  /**
   * The channel that can be used to send [AircraftStateData]
   * data to this service.
   */
  val sendChannel: SendChannel<AircraftData>
    get() = channel

  /**
   * Attempts to get [AircraftStateData] for the given [AircraftIcao24].
   */
  suspend fun getState(aircraftId: AircraftIcao24, time: Instant? = null): AircraftStateData? {
    val aircraft = lastAircraftStateData.getIfPresent(aircraftId)
    if (aircraft != null && (time == null || aircraft.time - time < mergeTime))
      return aircraft
    @Suppress("NAME_SHADOWING")
    val time = time ?: Instant.now()
    return newSuspendedTransaction(getDispatcher, db = database) {
      AircraftStateTable
          .select { AircraftStateTable.aircraftId eq aircraftId }
          .orderBy { abs(AircraftStateTable.time - time) }
          .andWhere { AircraftStateTable.time.between(time - validTime, time + validTime) }
          .firstOrNull()
          ?.let { AircraftStateTable.fromRow(it) }
    }
  }

  /**
   * Attempts to get [AircraftStateData] for all the aircrafts. When [bounds] are specified,
   * they need to be located within the specific bounds.
   */
  suspend fun getStates(bounds: GeodeticBounds? = null, time: Instant? = null): Collection<AircraftStateData> {
    val mapped = mutableMapOf<AircraftIcao24, AircraftStateData>()
    @Suppress("NAME_SHADOWING")
    val time = time ?: Instant.now()
    val now = Instant.now()
    fun addCachedValues(maxDiff: Duration) {
      if (now - time < maxDiff) {
        for ((_, value) in lastAircraftStateData.asMap()) {
          if ((time - value.time).absoluteValue > maxDiff || value.aircraftId in mapped)
            continue
          if (bounds != null) {
            val position = value.position ?: continue
            if (position.latitude !in bounds.min.latitude..bounds.max.latitude ||
                position.longitude !in bounds.min.longitude..bounds.max.longitude)
              continue
          }
          mapped[value.aircraftId] = value
        }
      }
    }
    addCachedValues(mergeTime)
    newSuspendedTransaction(getDispatcher, db = database) {
      AircraftStateTable
          .selectAll()
          .distinctBy(AircraftStateTable.aircraftId)
          .orderBy { abs(AircraftStateTable.time - time) }
          .andWhere { AircraftStateTable.time.between(time - validTime, time + validTime) }
          .let {
            if (bounds != null) {
              it.andWhere {
                AircraftStateTable.latitude.isNotNull() and AircraftStateTable.latitude.between(bounds.min.latitude, bounds.max.latitude) and
                    AircraftStateTable.longitude.isNotNull() and AircraftStateTable.longitude.between(bounds.min.longitude, bounds.max.longitude)
              }
            } else it
          }
          .forEach {
            val aircraftId = it[AircraftStateTable.aircraftId]
            if (aircraftId !in mapped)
              mapped[aircraftId] = AircraftStateTable.fromRow(it)
          }
    }
    addCachedValues(validTime)
    return mapped.values
  }

  /**
   * Converts the given [ResultRow] into a [AircraftStateData].
   */
  private fun AircraftStateTable.fromRow(it: ResultRow): AircraftStateData {
    val icao24 = it[aircraftId]
    val time = it[time]
    val callsign = it[callsign]
    val latitude = it[latitude]
    val longitude = it[longitude]
    val altitude = it[altitude]
    val position = if (latitude != null && longitude != null && altitude != null) {
      GeodeticPosition(latitude, longitude, altitude)
    } else null
    val onGround = it[onGround]
    val velocity = it[velocity]
    val verticalRate = it[verticalRate]
    val heading = it[heading]
    return SimpleAircraftStateData(time = time, aircraftId = icao24, onGround = onGround, position = position,
        callsign = callsign, velocity = velocity, verticalRate = verticalRate, heading = heading)
  }

  /**
   * Is called when for a while there wasn't any data received.
   */
  private suspend fun cleanupAircraft(data: AircraftStateData) {
    AircraftStateTable.insert(data)
  }

  /**
   * Initializes the raw aircraft data processor.
   */
  fun init() {
    job = GlobalScope.launch(updateDispatcher) {
      val receive = launch {
        while (true) {
          process(channel.receive())
        }
      }
      val cleanup = launch {
        while (true) {
          AircraftStateTable.cleanup()
          cleanupFlightData()
          delay(1.minutes)
        }
      }
      receive.join()
      cleanup.join()
    }
  }

  fun shutdown() {
    job?.cancel()
    job = null
  }

  private suspend fun process(data: AircraftData) {
    if (data is AircraftStateData)
      process(data)
    if (data is AircraftFlightData)
      process(data)
  }

  /**
   * Merges the two [AircraftStateData].
   */
  private fun AircraftStateData.merge(other: AircraftStateData): AircraftStateData {
    return other.withTime(time)
  }

  /**
   * Processes the [AircraftStateData].
   */
  private suspend fun process(data: AircraftStateData) {
    val lastData = lastAircraftStateData.getIfPresent(data.aircraftId)
    if (lastData != null) {
      val difference = data.time - lastData.time
      // Merge entries if the time is close enough, to fill missing data, etc.
      if (difference < mergeTime) {
        val merged = lastData.merge(data)
        lastAircraftStateData.put(merged.aircraftId, merged)
      } else {
        try {
          AircraftStateTable.insert(lastData)
        } catch (ex: Exception) {
          AirVision.logger.error("An error occurred while trying to insert an entry", ex)
          println("Difference: $difference")
        }
        lastAircraftStateData.put(data.aircraftId, data)
      }
    } else {
      lastAircraftStateData.put(data.aircraftId, data)
    }
    // TODO: Bulk insert?
  }

  suspend fun getFlight(aircraftId: AircraftIcao24): AircraftFlightData? {
    return newSuspendedTransaction(getDispatcher) {
      AircraftFlightTable
          .select { AircraftFlightTable.aircraftId eq aircraftId }
          .map {
            val time = it[AircraftFlightTable.time]
            val code = it[AircraftFlightTable.code]
            val arrivalAirport = it[AircraftFlightTable.arrivalAirport]
            val departureAirport = it[AircraftFlightTable.departureAirport]
            val estimatedArrivalTime = it[AircraftFlightTable.estimatedArrivalTime]
            SimpleAircraftFlightData(aircraftId, time, code,
                departureAirport, arrivalAirport, estimatedArrivalTime)
          }
          .firstOrNull()
    }
  }

  /**
   * Processes changes in the flight destination and origin.
   */
  private suspend fun process(data: AircraftFlightData) {
    val lastData = lastAircraftFlightData.getIfPresent(data.aircraftId)
    if (lastData == null ||
        lastData.arrivalAirport != data.arrivalAirport ||
        lastData.departureAirport != data.departureAirport ||
        // Refresh every 10 minutes, so it doesn't get cleaned up
        (Instant.now() - lastData.time) > 10.minutes) {
      // Needs an update
      if (data.arrivalAirport == null && data.departureAirport == null) {
        // No flight, remove
        newSuspendedTransaction(updateDispatcher) {
          AircraftFlightTable.deleteWhere { AircraftFlightTable.aircraftId eq data.aircraftId }
        }
      } else {
        // A flight, update
        newSuspendedTransaction(updateDispatcher) {
          AircraftFlightTable.upsert {
            it[aircraftId] = data.aircraftId
            it[time] = data.time
            it[code] = data.code
            it[arrivalAirport] = data.arrivalAirport
            it[departureAirport] = data.departureAirport
            it[estimatedArrivalTime] = data.estimatedArrivalTime
          }
        }
      }
    }
  }

  private suspend fun cleanupFlightData() {
    // Remove all data older than 1 hour
    val time = Instant.now() - 1.hours
    newSuspendedTransaction(updateDispatcher) {
      AircraftFlightTable.deleteWhere { AircraftFlightTable.time less time }
    }
  }

  private suspend fun AircraftStateTable.insert(data: AircraftStateData) {
    newSuspendedTransaction(updateDispatcher, db = database) {
      insert {
        it[aircraftId] = data.aircraftId
        it[time] = data.time
        it[latitude] = data.position?.latitude
        it[longitude] = data.position?.longitude
        it[altitude] = data.position?.altitude
        it[onGround] = data.onGround
        it[verticalRate] = data.verticalRate
        it[heading] = data.heading
      }
    }
  }

  private suspend fun AircraftStateTable.cleanup() {
    val time = Instant.now() - 1.hours
    newSuspendedTransaction(updateDispatcher, db = database) {
      // For now, keep data up to and hour ago
      deleteWhere { AircraftStateTable.time less time }

      // TODO: Keep data for the current flight
      /*
      val entry = select { AircraftTable.icao24 eq icao24.address }
          .orderBy(time)
          .andWhere { onGround eq true }
          .firstOrNull()
      if (entry != null) {
        val time = entry[time]
        deleteWhere { (AircraftTable.icao24 eq icao24.address) and () }
      */
    }
  }
}
