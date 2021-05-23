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

import io.github.airvision.serializer.InstantSerializer
import io.github.airvision.serializer.QuaterniondSerializer
import io.github.airvision.serializer.Vector2dSerializer
import io.github.airvision.serializer.Vector3dSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual

val AirVisionJson = Json {
  val module = SerializersModule {
    contextual(InstantSerializer)
    contextual(QuaterniondSerializer)
    contextual(Vector2dSerializer)
    contextual(Vector3dSerializer)
  }
  ignoreUnknownKeys = true
  serializersModule = module
  encodeDefaults = false
}
