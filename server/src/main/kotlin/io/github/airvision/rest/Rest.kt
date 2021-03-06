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
import io.github.airvision.AirVisionJson
import io.github.airvision.service.AircraftInfoService
import io.github.airvision.service.AircraftService
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
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.util.pipeline.PipelineInterceptor
import kotlinx.serialization.SerializationException

class Rest(
  private val aircraftService: AircraftService,
  private val aircraftInfoService: AircraftInfoService,
  private val airportService: AirportService,
  private val config: AirVision.Config
) {

  /**
   * Setup of the server of the REST Web Service.
   */
  fun setup(application: Application) {
    val context = RestContext(aircraftService, aircraftInfoService, airportService, config)

    // Build module
    application.apply {
      val converter = RestSerializationConverter(AirVisionJson)

      // Install query parameter based Content Conversion
      installQueryParameterContentConversion(converter)

      // Install Json Content Conversion
      install(ContentNegotiation) {
        register(ContentType.Application.Json, converter)
      }

      // Install error handling
      install(StatusPages) {
        suspend fun PipelineContext.handleBadRequest(cause: Exception) {
          AirVision.logger.debug("Invalid request while handling ${call.request.local.uri}", cause)
          call.respond(
            HttpStatusCode.BadRequest, ErrorResponse(
              HttpStatusCode.BadRequest,
              "Invalid request${if (cause.message != null) ": ${cause.message}" else ""}"
            )
          )
        }
        exception<ContentTransformationException> { cause ->
          handleBadRequest(cause)
        }
        exception<SerializationException> { cause ->
          handleBadRequest(cause)
        }
        exception<ErrorResponseException> { cause ->
          val response = cause.response
          call.respond(response.code, response)
        }
        exception<Throwable> { cause ->
          AirVision.logger.error("Error while handling ${call.request.local.uri}", cause)
          call.respond(
            HttpStatusCode.InternalServerError,
            ErrorResponse(HttpStatusCode.InternalServerError)
          )
        }
        status(HttpStatusCode.NotFound) {
          call.respond(
            HttpStatusCode.NotFound, ErrorResponse(
              HttpStatusCode.NotFound,
              "Path not found: ${call.request.local.uri}"
            )
          )
        }
      }

      routing {
        route("/api/v1") {
          route("/aircraft") {
            route("/state") {
              postOrGet("/visible") { handleVisibleAircraftRequest(context) }
              postOrGet("/all") { handleAircraftStatesRequest(context) }
              postOrGet("/get") { handleRtAircraftRequest(context) }
            }
            postOrGet("/flight") { handleAircraftFlightRequest(context) }
            postOrGet("/info") { handleAircraftModelRequest(context) }
          }
        }
      }
    }
  }
}

private fun Route.postOrGet(path: String, body: PipelineInterceptor<Unit, ApplicationCall>) {
  route(path) {
    post(body)
    get(body)
  }
}

class RestContext(
  val aircraftService: AircraftService,
  val aircraftInfoService: AircraftInfoService,
  val airportService: AirportService,
  val config: AirVision.Config
)

typealias PipelineContext = io.ktor.util.pipeline.PipelineContext<Unit, ApplicationCall>
