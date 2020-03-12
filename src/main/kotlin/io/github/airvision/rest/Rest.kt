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

/**
 * Setup of the server of the REST Web Service.
 */
fun Application.setupRest() {
  // Install Json Content Conversion
  install(ContentNegotiation) {
    register(ContentType.Application.Json, RestSerializationConverter(
        SerializationConverter(AirVision.json)))
  }

  routing {
    route("/v1") {
      get("/visible_aircraft") { handleVisibleAircraftRequest() }
      get("/aircrafts") { handleAircraftsRequest() }
      get("/aircraft") { handleAircraftRequest() }
      get("/aircraft_trajectory") { handleAircraftTrajectoryRequest() }
    }
  }
}

typealias RestContext = PipelineContext<Unit, ApplicationCall>
