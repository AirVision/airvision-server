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

import io.github.airvision.rest.setupRest
import io.github.airvision.serializer.InstantSerializer
import io.github.airvision.serializer.QuaterniondSerializer
import io.github.airvision.serializer.Vector2dSerializer
import io.github.airvision.serializer.Vector3dSerializer
import io.ktor.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual

/**
 * The entry point of the application.
 */
fun main() {
  // TODO: Make port configurable

  // Start the REST Web Service
  embeddedServer(Netty, module = Application::setupRest).start()

  // Start collecting ADS-B data
}

object AirVision {

  @Suppress("EXPERIMENTAL_API_USAGE")
  val json = Json {
    val module = SerializersModule {
      contextual(InstantSerializer)
      contextual(QuaterniondSerializer)
      contextual(Vector2dSerializer)
      contextual(Vector3dSerializer)
    }
    serialModule = module
    encodeDefaults = false
  }
}
