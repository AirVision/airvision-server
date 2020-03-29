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
import io.github.airvision.AirportIcao
import io.github.airvision.GeodeticBounds
import io.github.airvision.GeodeticPosition
import io.github.airvision.exposed.abs
import io.github.airvision.exposed.distinctBy
import io.github.airvision.exposed.orderBy
import io.github.airvision.exposed.upsert
import io.github.airvision.service.db.AircraftStateTable
import io.github.airvision.service.db.AircraftFlightTable
import io.github.airvision.util.delay
import io.github.airvision.util.time.minus
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
  suspend fun getState(icao24: AircraftIcao24, time: Instant? = null): AircraftStateData? {
    val aircraft = lastAircraftStateData.getIfPresent(icao24)
    if (aircraft != null && (time == null || aircraft.time - time < mergeTime))
      return aircraft
    val seconds = (time ?: Instant.now()).epochSecond
    return newSuspendedTransaction(getDispatcher, db = database) {
      AircraftStateTable
          .select { AircraftStateTable.icao24 eq icao24.address }
          .orderBy { abs(AircraftStateTable.time - seconds) }
          .andWhere {
            AircraftStateTable.time.between(
                seconds - validTime.inSeconds.toInt(),
                seconds + validTime.inSeconds.toInt())
          }
          .firstOrNull()
          ?.let { AircraftStateTable.fromRow(it) }
    }
  }

  /**
   * Attempts to get [AircraftStateData] for all the aircrafts. When [bounds] are specified,
   * they need to be located within the specific bounds.
   */
  suspend fun getStates(bounds: GeodeticBounds? = null, time: Instant? = null): Collection<AircraftStateData> {
    val mapped = mutableMapOf<Int, AircraftStateData>()
    @Suppress("NAME_SHADOWING")
    val time = time ?: Instant.now()
    val now = Instant.now()
    fun addCachedValues(maxDiff: Duration) {
      if (now - time < maxDiff) {
        for ((_, value) in lastAircraftStateData.asMap()) {
          if ((time - value.time).absoluteValue < maxDiff && value.icao24.address !in mapped)
            mapped[value.icao24.address] = value
        }
      }
    }
    addCachedValues(mergeTime)
    newSuspendedTransaction(getDispatcher, db = database) {
      val seconds = time.epochSecond
      AircraftStateTable
          .selectAll()
          .distinctBy(AircraftStateTable.icao24)
          .orderBy { abs(AircraftStateTable.time - seconds) }
          .andWhere {
            AircraftStateTable.time.between(
                seconds - validTime.inSeconds.toInt(),
                seconds + validTime.inSeconds.toInt())
          }
          .let {
            if (bounds != null) {
              it.andWhere {
                AircraftStateTable.latitude.isNotNull() and AircraftStateTable.latitude.between(bounds.min.latitude, bounds.max.latitude) and
                    AircraftStateTable.longitude.isNotNull() and AircraftStateTable.longitude.between(bounds.min.longitude, bounds.max.longitude)
              }
            } else it
          }
          .forEach {
            val address = it[AircraftStateTable.icao24]
            if (address !in mapped)
              mapped[address] = AircraftStateTable.fromRow(it)
          }
    }
    addCachedValues(validTime)
    return mapped.values
  }

  /**
   * Converts the given [ResultRow] into a [AircraftStateData].
   */
  private fun AircraftStateTable.fromRow(it: ResultRow): AircraftStateData {
    val icao24 = AircraftIcao24(it[icao24])
    val time = Instant.ofEpochSecond(it[time])
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
    return SimpleAircraftStateData(time = time, icao24 = icao24, onGround = onGround, position = position,
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
          delay(1.minutes)
          AircraftStateTable.cleanup()
          cleanupFlightData()
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
    val lastData = lastAircraftStateData.getIfPresent(data.icao24)
    if (lastData != null) {
      val difference = data.time - lastData.time
      // Merge entries if the time is close enough, to fill missing data, etc.
      if (difference < mergeTime) {
        val merged = lastData.merge(data)
        lastAircraftStateData.put(merged.icao24, merged)
      } else {
        try {
          AircraftStateTable.insert(lastData)
        } catch (ex: Exception) {
          AirVision.logger.error("An error occurred while trying to insert an entry", ex)
          println("Difference: $difference")
        }
        lastAircraftStateData.put(data.icao24, data)
      }
    } else {
      lastAircraftStateData.put(data.icao24, data)
    }
    // TODO: Bulk insert?
  }

  suspend fun getFlight(icao24: AircraftIcao24): AircraftFlightData? {
    return newSuspendedTransaction {
      AircraftFlightTable
          .select { AircraftFlightTable.icao24 eq icao24.address }
          .map {
            val time = Instant.ofEpochSecond(it[AircraftFlightTable.time])
            val destination = it[AircraftFlightTable.destination]?.let { icao -> AirportIcao(icao) }
            val origin = it[AircraftFlightTable.origin]?.let { icao -> AirportIcao(icao) }
            SimpleAircraftFlightData(icao24, time, origin, destination)
          }
          .firstOrNull()
    }
  }

  /**
   * Processes changes in the flight destination and origin.
   */
  private suspend fun process(data: AircraftFlightData) {
    val lastData = lastAircraftFlightData.getIfPresent(data.icao24)
    if (lastData == null ||
        lastData.flightDestination != data.flightDestination ||
        lastData.flightOrigin != data.flightOrigin ||
        // Refresh every 10 minutes, so it doesn't get cleaned up
        (Instant.now() - lastData.time) > 10.minutes) {
      // Needs an update
      if (data.flightDestination == null && data.flightOrigin == null) {
        // No flight, remove
        newSuspendedTransaction {
          AircraftFlightTable.deleteWhere { AircraftFlightTable.icao24 eq data.icao24.address }
        }
      } else {
        // A flight, update
        newSuspendedTransaction {
          AircraftFlightTable.upsert {
            it[icao24] = data.icao24.address
            it[time] = data.time.epochSecond
            it[destination] = data.flightDestination?.icao
            it[origin] = data.flightOrigin?.icao
          }
        }
      }
    }
  }

  private suspend fun cleanupFlightData() {
    // Remove all data older than 1 hour
    val time = Instant.now() - 1.hours
    newSuspendedTransaction {
      AircraftFlightTable.deleteWhere { AircraftFlightTable.time less time.epochSecond }
    }
  }

  private suspend fun AircraftStateTable.insert(data: AircraftStateData) {
    newSuspendedTransaction(updateDispatcher, db = database) {
      insert {
        it[icao24] = data.icao24.address
        it[time] = data.time.epochSecond
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
    val time = Instant.now().minus(1.hours)
    newSuspendedTransaction(updateDispatcher, db = database) {
      // For now, keep data up to and hour ago
      deleteWhere { AircraftStateTable.time less time.epochSecond }

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
