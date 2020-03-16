/*
 * AirVision
 *
 * Copyright (c) AirVision <https://www.github.com/AirVision>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
package io.github.airvision.rest

import io.github.airvision.AirVision
import io.github.airvision.service.AircraftService
import io.github.airvision.service.openflights.OpenFlights
import io.github.airvision.service.openskynetwork.OpenSkyNetwork
import io.github.airvision.service.openskynetwork.OsnCredentials
import io.github.airvision.service.AirportService
import io.github.airvision.service.openskynetwork.OsnAircraftService
import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.serialization.SerializationConverter
import io.ktor.util.pipeline.PipelineContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.stringify
import java.nio.file.Files
import java.nio.file.Paths

fun loadRestConfig(): Rest.Config {
  val json = Json {
    prettyPrint = true
  }
  val path = Paths.get("rest.json")
  return if (Files.exists(path)) {
    json.parse(Rest.Config.serializer(), Files.readAllLines(path).joinToString("\n"))
  } else {
    val config = Rest.Config()
    Files.newBufferedWriter(path).use { writer ->
      writer.write(json.stringify(config))
    }
    config
  }
}

class Rest(private val config: Config = loadRestConfig()) {

  @Serializable
  class Config(
      @SerialName("osn_credentials") val osnCredentials: OsnCredentials = OsnCredentials("", ""),
      @SerialName("visible_aircraft") val visibleAircraft: VisibleAircraft = VisibleAircraft()
  ) {

    /**
     * @property range The range in which aircraft's are valid to be selected, in degrees
     */
    @Serializable
    class VisibleAircraft(
        val range: Double = 4.0 // TODO: Find a good value
    )
  }

  /**
   * Setup of the server of the REST Web Service.
   */
  fun setup(application: Application) {
    val osn = OpenSkyNetwork(if (config.osnCredentials.username.isEmpty()) null else config.osnCredentials)
    val airportService = OpenFlights()
    val aircraftService = OsnAircraftService(osn)

    val context = RestContext(config, osn, aircraftService, airportService)

    // Build module
    application.apply {
      // Install Json Content Conversion
      install(ContentNegotiation) {
        register(ContentType.Application.Json, RestSerializationConverter(
            SerializationConverter(AirVision.json)))
      }

      routing {
        route("/v1") {
          get("/visible_aircraft") { handleVisibleAircraftRequest(context) }
          get("/aircrafts") { handleAircraftsRequest(context) }
          get("/aircraft") { handleAircraftRequest(context) }
          get("/aircraft_trajectory") { handleAircraftTrajectoryRequest(context) }
        }
      }
    }
  }
}

class RestContext(
    val config: Rest.Config,
    val osn: OpenSkyNetwork,
    val aircrafts: AircraftService,
    val airports: AirportService
)

typealias PipelineContext = PipelineContext<Unit, ApplicationCall>
