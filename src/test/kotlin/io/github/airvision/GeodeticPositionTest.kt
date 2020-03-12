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

import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.stringify
import org.junit.jupiter.api.Test
import org.spongepowered.math.vector.Vector3d
import kotlin.test.assertEquals

@ImplicitReflectionSerializer
class GeodeticPositionTest {

  @Test
  fun `serialization with 3 values`() {
    val p = GeodeticPosition(23.654782, 94256.22563, 69.55)
    assertEquals("[${p.latitude},${p.longitude},${p.altitude}]", Json.stringify(p))
  }

  @Test
  fun `serialization with 2 values`() {
    val p = GeodeticPosition(23.654782, 94256.22563, 0.0)
    assertEquals("[${p.latitude},${p.longitude}]", Json.stringify(p))
  }

  @Test
  fun `deserialization with 3 values`() {
    val v = listOf(23.654782, 94256.22563, 69.55).map { JsonPrimitive(it) }
    val p = GeodeticPosition(23.654782, 94256.22563, 69.55)
    //assertEquals(p, Json.fromJson(JsonArray(v)))
  }

  @Test
  fun `deserialization with 2 values`() {
    val v = listOf(23.654782, 94256.22563).map { JsonPrimitive(it) }
    val p = GeodeticPosition(23.654782, 94256.22563, 0.0)
    //assertEquals(p, Json.fromJson(JsonArray(v)))
  }

  @Test
  fun `to ecef position 1`() {
    // Schriek
    val p = GeodeticPosition(51.030677, 4.675180, 10.0)
    assert(p.toEcefPosition().div(1000.0).distance(Vector3d(4006.011, 327.607, 4935.699)) < 0.01)
  }

  @Test
  fun `to ecef position 2`() {
    // Belo Horizonte
    val p = GeodeticPosition(-20.004401, -43.918436, 10.0)
    assert(p.toEcefPosition().div(1000.0).distance(Vector3d(4318.855, -4158.805, -2168.158)) < 0.01)
  }

}
