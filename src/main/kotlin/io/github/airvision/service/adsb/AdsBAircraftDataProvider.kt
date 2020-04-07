/*
 * AirVision
 *
 * Copyright (c) AirVision <https://www.github.com/AirVision>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
package io.github.airvision.service.adsb

import arrow.core.Either
import com.fazecast.jSerialComm.SerialPort
import com.fazecast.jSerialComm.SerialPortDataListener
import com.fazecast.jSerialComm.SerialPortEvent
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.github.airvision.AirVision
import io.github.airvision.AircraftIcao24
import io.github.airvision.GeodeticPosition
import io.github.airvision.service.AircraftStateData
import io.github.airvision.service.SimpleAircraftStateData
import io.github.airvision.util.coroutines.delay
import io.github.airvision.util.time.toDouble
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import org.opensky.libadsb.ModeSDecoder
import org.opensky.libadsb.Position
import org.opensky.libadsb.PositionDecoder
import org.opensky.libadsb.msgs.AirbornePositionV0Msg
import org.opensky.libadsb.msgs.AirspeedHeadingMsg
import org.opensky.libadsb.msgs.AltitudeReply
import org.opensky.libadsb.msgs.CommBAltitudeReply
import org.opensky.libadsb.msgs.IdentificationMsg
import org.opensky.libadsb.msgs.LongACAS
import org.opensky.libadsb.msgs.ModeSReply
import org.opensky.libadsb.msgs.ShortACAS
import org.opensky.libadsb.msgs.SurfacePositionV0Msg
import org.opensky.libadsb.msgs.VelocityOverGroundMsg
import java.time.Duration
import java.time.Instant
import kotlin.time.DurationUnit
import kotlin.time.seconds
import org.opensky.libadsb.tools as AdsBTools

/**
 * @param dataSendChannel The channel that will be used to send [AircraftStateData] data to
 * @param receiverPosition The coordinates of the receiver, if known
 */
class AdsBAircraftDataProvider(
    private val dataSendChannel: SendChannel<AircraftStateData>,
    receiverPosition: GeodeticPosition? = null
) {

  private data class AdsBCacheEntry(
      val data: SimpleAircraftStateData,
      val positionDecoder: PositionDecoder = PositionDecoder(),
      val positionUpdateTime: Instant? = null
  )

  private val positionInvalidateDelay = 45.seconds.toLongMilliseconds()
  private val receiverPosition = receiverPosition?.run { Position(latitude, longitude, altitude) }

  /**
   * Checks whether the position is still valid.
   */
  private val AdsBCacheEntry.isPositionValid get() = data.position != null &&
      Instant.now().toEpochMilli() - positionUpdateTime!!.toEpochMilli() <= positionInvalidateDelay

  private val adsBDataCache: Cache<AircraftIcao24, AdsBCacheEntry> = Caffeine.newBuilder()
      .expireAfterWrite(Duration.ofMinutes(15))
      .build()

  private val modeSDecoder = ModeSDecoder()
  private var serialPort: SerialPort? = null
  private var job: Job? = null

  fun init() {
    val result = startReading()

    result.fold({ reason ->
      AirVision.logger.warn("ADS-B setup failed: $reason")
      null
    }, {
      serialPort = it
    })

    job = startConnectJob()
  }

  fun shutdown() {
    job?.cancel()
    job = null
  }

  /**
   * Starts a job that attempts to reconnect the [SerialPort]
   * if it closed for some reason.
   */
  private fun startConnectJob(): Job {
    return GlobalScope.launch(Dispatchers.IO) {
      while (true) {
        delay(10.seconds)
        var serialPort = this@AdsBAircraftDataProvider.serialPort
        if (serialPort == null || serialPort.isOpen) {
          serialPort = startReading().fold({ null }, { it })
          this@AdsBAircraftDataProvider.serialPort = serialPort
        }
        // Reconnect failed, so let's wait a bit longer
        if (serialPort == null)
          delay(15.seconds)
      }
    }
  }

  private fun startReading(): Either<String, SerialPort> {
    val serialPort = SerialPort.getCommPorts()
        // TODO: Filter based on description? Somehow find the device.
        .firstOrNull()
        ?: return Either.left("couldn't find serial port.")
    serialPort.addDataListener(object : SerialPortDataListener {
      override fun getListeningEvents() = SerialPort.LISTENING_EVENT_DATA_RECEIVED
      override fun serialEvent(event: SerialPortEvent) = receiveData(event.receivedData)
    })
    if (!serialPort.openPort())
      return Either.left("couldn't open serial port.")
    return Either.right(serialPort)
  }

  private fun receiveData(receivedData: ByteArray) {
    val time = Instant.now()
    val message = modeSDecoder.decode(receivedData)

    GlobalScope.launch {
      val data = receiveMessage(time, message)
      // Send the data to the processor
      dataSendChannel.send(data)
    }
  }

  private fun receiveMessage(time: Instant, message: ModeSReply): AircraftStateData {
    val aircraftId = AircraftIcao24(message.transponderAddress)
    val entry = adsBDataCache.get(aircraftId) { AdsBCacheEntry(SimpleAircraftStateData(time, aircraftId)) }!!

    val (data, positionDecoder, positionUpdateTime) = entry

    var newPositionUpdateTime = positionUpdateTime
    var position = data.position
    var onGround = data.onGround
    var callsign = data.callsign
    var velocity = data.velocity
    var verticalRate = data.verticalRate
    var heading = data.heading

    if (AdsBTools.isZero(message.parity) || message.checkParity()) {
      val msgPosition = when (message) {
        is AirbornePositionV0Msg -> {
          onGround = false
          if (receiverPosition != null) {
            positionDecoder.decodePosition(receiverPosition, message)
          } else {
            positionDecoder.decodePosition(message)
          }
        }
        is SurfacePositionV0Msg -> {
          onGround = true
          val doubleTime = time.toDouble(DurationUnit.SECONDS)
          if (receiverPosition != null) {
            positionDecoder.decodePosition(doubleTime, receiverPosition, message, null)
          } else {
            positionDecoder.decodePosition(doubleTime, message)
          }
        }
        else -> null
      }

      val newPosition = msgPosition?.asGeodeticPosition()
      if (newPosition != null || !entry.isPositionValid) {
        position = newPosition
        newPositionUpdateTime = time
      }

      when (message) {
        is IdentificationMsg -> {
          callsign = String(message.identity)
        }
        is VelocityOverGroundMsg -> {
          if (message.hasVelocityInfo()) {
            velocity = AdsBTools.knots2MetersPerSecond(message.velocity)
            heading = message.heading
          }
          if (message.hasVerticalRateInfo())
            verticalRate = AdsBTools.feetPerMinute2MetersPerSecond(message.verticalRate)
          // TODO: Also invalidate after a bit of time?
        }
        is AirspeedHeadingMsg -> {
          if (message.hasVerticalRateInfo())
            verticalRate = AdsBTools.feetPerMinute2MetersPerSecond(message.verticalRate)
          if (message.hasHeadingStatusFlag())
            heading = message.heading
        }
      }
    } else if (message.downlinkFormat != 17.toByte()) { // CRC failed
      val airborne = when (message) {
        is ShortACAS -> message.isAirborne
        is LongACAS -> message.isAirborne
        else -> null
      }
      if (airborne != null)
        onGround = !airborne
      val altitude = when (message) {
        is ShortACAS -> message.altitude
        is AltitudeReply -> message.altitude
        is LongACAS -> message.altitude
        is CommBAltitudeReply -> message.altitude
        else -> null
      }
      if (altitude != null)
        position = position?.copy(altitude = AdsBTools.feet2Meters(altitude.toDouble()))
    }

    val newData = data.copy(time = time, onGround = onGround, position = position,
        callsign = callsign, velocity = velocity, verticalRate = verticalRate, heading = heading)
    adsBDataCache.put(data.aircraftId, entry.copy(data = newData, positionUpdateTime = newPositionUpdateTime))
    return newData
  }

  private fun Position.asGeodeticPosition(): GeodeticPosition? {
    val latitude = latitude
    val longitude = longitude
    if (latitude == null || longitude == null || !isReasonable)
      return null
    val altitude = if (altitude != null) AdsBTools.feet2Meters(altitude) else 0.0
    return GeodeticPosition(latitude, longitude, altitude)
  }
}
