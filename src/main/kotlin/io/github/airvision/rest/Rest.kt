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
import io.github.airvision.service.AircraftInfoService
import io.github.airvision.service.AircraftStateService
import io.github.airvision.service.AirportService
import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.request.ContentTransformationException
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.serialization.SerializationConverter
import io.ktor.util.pipeline.PipelineContext
import kotlinx.serialization.json.JsonDecodingException

class Rest(
    private val aircraftStateService: AircraftStateService,
    private val aircraftInfoService: AircraftInfoService,
    private val aircraftFlightService: AircraftFlightService,
    private val airportService: AirportService
) {

  /**
   * Setup of the server of the REST Web Service.
   */
  fun setup(application: Application) {
    val context = RestContext(aircraftStateService, aircraftInfoService, aircraftFlightService, airportService)

    // Build module
    application.apply {
      // Install Json Content Conversion
      install(ContentNegotiation) {
        register(ContentType.Application.Json, RestSerializationConverter(
            SerializationConverter(AirVision.json)))
      }

      // Install error handling
      install(StatusPages) {
        suspend fun PipelineContext<Unit, ApplicationCall>.handleBadRequest(cause: Exception) {
          AirVision.logger.debug("Invalid request while handling ${call.request.local.uri}", cause)
          call.respond(HttpStatusCode.BadRequest, error.badRequest(
              "Invalid request${if (cause.message != null) ": $cause.message" else ""}"))
        }
        exception<ContentTransformationException> { cause ->
          handleBadRequest(cause)
        }
        exception<JsonDecodingException> { cause ->
          handleBadRequest(cause)
        }
        exception<Throwable> { cause ->
          AirVision.logger.error("Error while handling ${call.request.local.uri}", cause)
          call.respond(HttpStatusCode.InternalServerError, error.internalError())
        }
        status(HttpStatusCode.NotFound) {
          call.respond(HttpStatusCode.NotFound, error.notFound("Path not found: ${call.request.local.uri}"))
        }
      }

      routing {
        route("/api/v1") {
          route("/aircraft") {
            route("/state") {
              get("/visible") { handleVisibleAircraftRequest(context) }
              get("/all") { handleAircraftStatesRequest(context) }
              get("/all-around") { handleAircraftStatesAroundRequest(context) }
              get("/get") { handleRtAircraftRequest(context) }
            }
            get("/flight") { handleAircraftFlightRequest(context) }
            get("/info") { handleAircraftModelRequest(context) }
          }
        }
      }
    }
  }
}

class RestContext(
    val aircraftStateService: AircraftStateService,
    val aircraftInfoService: AircraftInfoService,
    val aircraftFlightService: AircraftFlightService,
    val airportService: AirportService
)

typealias PipelineContext = PipelineContext<Unit, ApplicationCall>
