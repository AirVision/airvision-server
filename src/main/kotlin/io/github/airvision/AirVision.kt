/*
 * AirVision
 *
 * Copyright (c) AirVision <https://www.github.com/AirVision>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
package io.github.airvision

import com.zaxxer.hikari.HikariDataSource
import io.github.airvision.rest.Rest
import io.github.airvision.serializer.InstantSerializer
import io.github.airvision.serializer.QuaterniondSerializer
import io.github.airvision.serializer.Vector2dSerializer
import io.github.airvision.serializer.Vector3dSerializer
import io.github.airvision.service.AircraftService
import io.github.airvision.service.db.AircraftFlightTable
import io.github.airvision.service.db.AircraftStateTable
import io.github.airvision.service.db.AircraftManufacturerTable
import io.github.airvision.service.db.AircraftInfoTable
import io.github.airvision.service.db.DatabaseSettings
import io.github.airvision.service.flightradar24.Fr24RestService
import io.github.airvision.service.openflights.OpenFlightsAirportService
import io.github.airvision.service.openskynetwork.OsnAircraftFlightService
import io.github.airvision.service.openskynetwork.OsnAircraftInfoService
import io.github.airvision.service.openskynetwork.OsnRestService
import io.github.airvision.service.openskynetwork.OsnSettings
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.stringify
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.apache.logging.log4j.io.IoBuilder
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.Executors

/**
 * The entry point of the application.
 */
fun main() {
  // Setup Log4j
  System.setOut(IoBuilder.forLogger(AirVision.logger).setLevel(Level.INFO).buildPrintStream())
  System.setErr(IoBuilder.forLogger(AirVision.logger).setLevel(Level.ERROR).buildPrintStream())

  // TODO: Make port configurable

  // Generate config or load it
  val json = Json {
    prettyPrint = true
  }
  val path = Paths.get("config.json")
  val config = if (Files.exists(path)) {
    json.parse(AirVision.Config.serializer(), Files.readAllLines(path).joinToString("\n"))
  } else {
    val config = AirVision.Config()
    Files.newBufferedWriter(path).use { writer ->
      writer.write(json.stringify(config))
    }
    AirVision.logger.info("The configuration file was generated, please configure it now and restart.")
    return
  }

  // Initialize the database
  val dataSource = HikariDataSource().apply {
    jdbcUrl = config.database.url
    username = config.database.username
    password = config.database.password
  }
  val database = Database.connect(dataSource)
  transaction(db = database) {
    SchemaUtils.createMissingTablesAndColumns(
        AircraftFlightTable,
        AircraftManufacturerTable,
        AircraftInfoTable,
        AircraftStateTable)
  }
  AirVision.logger.info("Successfully connected to the database.")
  val databaseUpdateDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

  // Initialize the airports service
  val airportService = OpenFlightsAirportService()

  // Initialize the FlightRadar24 rest service
  val fr24 = Fr24RestService(airportService)

  // Initialize the OpenSky Network rest service
  val osn = OsnRestService(config.osn)

  // Initialize the OpenSky Network aircraft info database service
  val aircraftInfoService = OsnAircraftInfoService(database, databaseUpdateDispatcher)
  aircraftInfoService.init()

  // Initialize the aircraft service
  val aircraftService = AircraftService(database, databaseUpdateDispatcher, osn, fr24, airportService)
  aircraftService.init()

  // Initialize the aircraft flight info service
  val osnAircraftFlightService = OsnAircraftFlightService(osn, airportService)
  osnAircraftFlightService.init()

  // Initialize the rest service
  val rest = Rest(aircraftService, aircraftInfoService, airportService, config)
  val restConfig = config.rest

  AirVision.logger.info("Starting the REST Server, bound to ${restConfig.host}:${restConfig.port}")
  embeddedServer(Netty, port = restConfig.port, host = restConfig.host, module = rest::setup).start()
}

object AirVision {

  val logger: Logger = LogManager.getLogger(this::class.simpleName)

  @Suppress("EXPERIMENTAL_API_USAGE")
  val json = Json {
    val module = SerializersModule {
      contextual(InstantSerializer)
      contextual(QuaterniondSerializer)
      contextual(Vector2dSerializer)
      contextual(Vector3dSerializer)
    }
    ignoreUnknownKeys = true
    serialModule = module
    encodeDefaults = false
  }

  @Serializable
  class Config(
      @SerialName("rest-server") val rest: Rest = Rest(),
      @SerialName("open-sky-network") val osn: OsnSettings =
          OsnSettings("", ""),
      @SerialName("database") val database: DatabaseSettings =
          DatabaseSettings("jdbc:postgresql://localhost/airvision", "airvision", "password"),
      @SerialName("visible-aircraft") val visibleAircraft: VisibleAircraft =
          VisibleAircraft()
  ) {

    @Serializable
    class Rest(
        val host: String = "0.0.0.0",
        val port: Int = 80
    )

    /**
     * @property range The range in which aircraft's are valid to be selected, in degrees
     */
    @Serializable
    class VisibleAircraft(
        val range: Double = 4.0 // TODO: Find a good value
    )
  }
}
