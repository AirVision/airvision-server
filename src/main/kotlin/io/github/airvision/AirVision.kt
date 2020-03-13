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

import io.github.airvision.rest.Rest
import io.github.airvision.serializer.InstantSerializer
import io.github.airvision.serializer.QuaterniondSerializer
import io.github.airvision.serializer.Vector2dSerializer
import io.github.airvision.serializer.Vector3dSerializer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.apache.logging.log4j.io.IoBuilder

/**
 * The entry point of the application.
 */
fun main() {
  // Setup Log4j
  System.setOut(IoBuilder.forLogger(AirVision.logger).setLevel(Level.INFO).buildPrintStream());
  System.setErr(IoBuilder.forLogger(AirVision.logger).setLevel(Level.ERROR).buildPrintStream());

  // TODO: Make port configurable

  val rest = Rest()

  // Start the REST Web Service
  embeddedServer(Netty, module = rest::setup).start()

  // Start collecting ADS-B data
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
    serialModule = module
    encodeDefaults = false
  }
}
