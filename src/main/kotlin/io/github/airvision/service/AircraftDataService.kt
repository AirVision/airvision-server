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
import io.github.airvision.service.db.AircraftDataTable
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

  /**
   * Represents the latest aircraft data that was received.
   */
  private val lastAircraftData = Caffeine.newBuilder()
      // Also apply the executor to reduce context switches on cleanup
      .executor(updateDispatcher.asExecutor())
      .removalListener<AircraftIcao24, AircraftData> { _, value, cause ->
        if (value != null && cause == RemovalCause.EXPIRED) {
          // Process on the dispatcher
          GlobalScope.launch(updateDispatcher) {
            cleanupAircraft(value)
          }
        }
      }
      .expireAfterWrite(60.seconds.toJavaDuration())
      .build<AircraftIcao24, AircraftData>()

  private val channel = Channel<AircraftData>()
  private val mergeTime = 3.seconds
  private val validTime = 25.seconds

  private var job: Job? = null

  /**
   * The channel that can be used to send [AircraftData]
   * data to this service.
   */
  val sendChannel: SendChannel<AircraftData>
    get() = channel

  /**
   * Attempts to get [AircraftData] for the given [AircraftIcao24].
   */
  suspend fun getAircraft(icao24: AircraftIcao24, time: Instant? = null): AircraftData? {
    val aircraft = lastAircraftData.getIfPresent(icao24)
    if (aircraft != null && (time == null || aircraft.time.minus(time) < mergeTime))
      return aircraft
    val seconds = (time ?: Instant.now()).epochSecond
    return newSuspendedTransaction(getDispatcher, db = database) {
      AircraftDataTable
          .select { AircraftDataTable.icao24 eq icao24.address }
          .orderBy { abs(AircraftDataTable.time - seconds) }
          .andWhere {
            AircraftDataTable.time.between(
                seconds - validTime.inSeconds.toInt(),
                seconds + validTime.inSeconds.toInt())
          }
          .firstOrNull()
          ?.let { AircraftDataTable.fromRow(it) }
    }
  }

  /**
   * Attempts to get [AircraftData] for all the aircrafts. When [bounds] are specified,
   * they need to be located within the specific bounds.
   */
  suspend fun getAircrafts(bounds: GeodeticBounds? = null, time: Instant? = null): Collection<AircraftData> {
    val mapped = mutableMapOf<Int, AircraftData>()
    @Suppress("NAME_SHADOWING")
    val time = time ?: Instant.now()
    val now = Instant.now()
    fun addCachedValues(maxDiff: Duration) {
      if (now.minus(time) < maxDiff) {
        for ((_, value) in lastAircraftData.asMap()) {
          if (time.minus(value.time).absoluteValue < maxDiff && value.icao24.address !in mapped)
            mapped[value.icao24.address] = value
        }
      }
    }
    addCachedValues(mergeTime)
    newSuspendedTransaction(getDispatcher, db = database) {
      val seconds = time.epochSecond
      AircraftDataTable
          .selectAll()
          .distinctBy(AircraftDataTable.icao24)
          .orderBy { abs(AircraftDataTable.time - seconds) }
          .andWhere {
            AircraftDataTable.time.between(
                seconds - validTime.inSeconds.toInt(),
                seconds + validTime.inSeconds.toInt())
          }
          .let {
            if (bounds != null) {
              it.andWhere {
                AircraftDataTable.latitude.isNotNull() and AircraftDataTable.latitude.between(bounds.min.latitude, bounds.max.latitude) and
                    AircraftDataTable.longitude.isNotNull() and AircraftDataTable.longitude.between(bounds.min.longitude, bounds.max.longitude)
              }
            } else it
          }
          .forEach {
            val address = it[AircraftDataTable.icao24]
            if (address !in mapped)
              mapped[address] = AircraftDataTable.fromRow(it)
          }
    }
    addCachedValues(validTime)
    return mapped.values
  }

  /**
   * Converts the given [ResultRow] into a [AircraftData].
   */
  private fun AircraftDataTable.fromRow(it: ResultRow): AircraftData {
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
    return SimpleAircraftData(time = time, icao24 = icao24, onGround = onGround, position = position,
        callsign = callsign, velocity = velocity, verticalRate = verticalRate, heading = heading)
  }

  /**
   * Is called when for a while there wasn't any data received.
   */
  private suspend fun cleanupAircraft(data: AircraftData) {
    AircraftDataTable.insert(data)
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
          AircraftDataTable.cleanup()
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

  /**
   * Merges the two [AircraftData].
   */
  private fun AircraftData.merge(other: AircraftData): AircraftData {
    return other.withTime(time)
  }

  /**
   * Processes the [AircraftData].
   */
  private suspend fun process(data: AircraftData) {
    val lastData = lastAircraftData.getIfPresent(data.icao24)
    if (lastData != null) {
      val difference = data.time.minus(lastData.time)
      // Merge entries if the time is close enough, to fill missing data, etc.
      if (difference < mergeTime) {
        val merged = lastData.merge(data)
        lastAircraftData.put(merged.icao24, merged)
      } else {
        try {
          AircraftDataTable.insert(lastData)
        } catch (ex: Exception) {
          AirVision.logger.error("An error occurred while trying to insert an entry", ex)
          println("Difference: $difference")
        }
        lastAircraftData.put(data.icao24, data)
      }
    } else {
      lastAircraftData.put(data.icao24, data)
    }

    // TODO: Bulk insert?
  }

  private suspend fun AircraftDataTable.insert(data: AircraftData) {
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

  private suspend fun AircraftDataTable.cleanup() {
    val time = Instant.now().minus(1.hours)
    newSuspendedTransaction(updateDispatcher, db = database) {
      // For now, keep data up to and hour ago
      deleteWhere { AircraftDataTable.time less time.epochSecond }

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
