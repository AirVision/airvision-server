/*
 * AirVision
 *
 * Copyright (c) LanternPowered <https://www.github.com/AirVision>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
package io.github.airvision.serializer

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Test
import org.spongepowered.math.vector.Vector2d
import kotlin.test.assertEquals

class Vector2dTest {

  @Test
  fun serialization() {
    val p = Vector2d(23.654782, 94256.22563)
    //assertEquals("[${p.x},${p.y}]", Json.toJson(Vector2dSerializer, p).toString())
  }

  @Test
  fun deserialization() {
    val v = listOf(23.654782, 94256.22563).map { JsonPrimitive(it) }
    val p = Vector2d(23.654782, 94256.22563)
    //assertEquals(p, Json.fromJson(Vector2dSerializer, JsonArray(v)))
  }
}
