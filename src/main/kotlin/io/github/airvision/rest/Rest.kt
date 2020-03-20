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
import io.github.airvision.service.AircraftFlightService
import io.github.airvision.service.AircraftService
import io.github.airvision.service.AirportService
import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.http.ContentType
import io.ktor.request.ContentTransformationException
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.serialization.SerializationConverter
import io.ktor.util.pipeline.PipelineContext

class Rest(
    private val aircraftService: AircraftService,
    private val aircraftFlightService: AircraftFlightService,
    private val airportService: AirportService
) {

  /**
   * Setup of the server of the REST Web Service.
   */
  fun setup(application: Application) {
    val context = RestContext(aircraftService, aircraftFlightService, airportService)

    // Build module
    application.apply {
      // Install Json Content Conversion
      install(ContentNegotiation) {
        register(ContentType.Application.Json, RestSerializationConverter(
            SerializationConverter(AirVision.json)))
      }

      // Install error handling
      install(StatusPages) {
        exception<ContentTransformationException> { cause ->
          AirVision.logger.debug("Invalid request while handling ${call.request.local.uri}", cause)
          call.respond(error.badRequest("Invalid request${if (cause.message != null) ": $cause.message" else ""}"))
        }
        exception<Throwable> { cause ->
          AirVision.logger.error("Error while handling ${call.request.local.uri}", cause)
          call.respond(error.internalError())
        }
      }

      routing {
        route("/v1") {
          get("/visible_aircraft") { handleVisibleAircraftRequest(context) }
          get("/aircrafts") { handleAircraftsRequest(context) }
          get("/aircraft") { handleAircraftRequest(context) }
          get("/aircraft_flight") { handleAircraftFlightRequest(context) }
        }
      }
    }
  }
}

class RestContext(
    val aircraftService: AircraftService,
    val aircraftFlightService: AircraftFlightService,
    val airportService: AirportService
)

typealias PipelineContext = PipelineContext<Unit, ApplicationCall>
