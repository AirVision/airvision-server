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
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.request.ApplicationReceivePipeline
import io.ktor.request.ApplicationReceiveRequest
import io.ktor.request.ContentTransformationException
import io.ktor.request.header
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.util.pipeline.PipelineContext
import io.ktor.util.pipeline.PipelineInterceptor
import io.ktor.utils.io.ByteReadChannel
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecodingException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class Rest(
    private val aircraftStateService: AircraftStateService,
    private val aircraftInfoService: AircraftInfoService,
    private val aircraftFlightService: AircraftFlightService,
    private val airportService: AirportService,
    private val config: AirVision.Config
) {

  /**
   * Setup of the server of the REST Web Service.
   */
  fun setup(application: Application) {
    val context = RestContext(aircraftStateService, aircraftInfoService,
        aircraftFlightService, airportService, config)

    // Build module
    application.apply {
      val converter = RestSerializationConverter(AirVision.json)

      // Install query parameter based Content Conversion
      receivePipeline.intercept(ApplicationReceivePipeline.Transform) {
        val contentType = call.request.header(HttpHeaders.ContentType)?.let { ContentType.parse(it) }
        if (contentType != null || subject.type == ByteReadChannel::class) {
          // Just proceed
          proceed()
          return@intercept
        }

        val parameters = call.request.queryParameters
        val map = parameters.entries()
            .associate { (key, value) ->
              val element = if (value.size == 1) {
                JsonPrimitive(value[0])
              } else {
                JsonArray(value.map { entry -> JsonPrimitive(entry) })
              }
              key to element
            }
        val json = JsonObject(map)
        val content = Json.stringify(JsonObject.serializer(), json)

        val converted = converter.convertForReceive(this, content)
        proceedWith(ApplicationReceiveRequest(subject.typeInfo, converted, reusableValue = true))
      }

      // Install Json Content Conversion
      install(ContentNegotiation) {
        register(ContentType.Application.Json, converter)
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
    val aircraftStateService: AircraftStateService,
    val aircraftInfoService: AircraftInfoService,
    val aircraftFlightService: AircraftFlightService,
    val airportService: AirportService,
    val config: AirVision.Config
)

typealias PipelineContext = PipelineContext<Unit, ApplicationCall>
