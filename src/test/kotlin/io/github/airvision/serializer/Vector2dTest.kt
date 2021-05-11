/*
 * AirVision
 *
 * Copyright (c) AirVision <https://www.github.com/AirVision>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
package io.github.airvision.serializer

import io.github.airvision.AirVision
import io.github.airvision.util.math.component.x
import io.github.airvision.util.math.component.y
import kotlinx.serialization.encodeToString
import org.junit.jupiter.api.Test
import org.spongepowered.math.vector.Vector2d
import kotlin.test.assertEquals

class Vector2dTest {

  @Test
  fun serialization() {
    val p = Vector2d(23.654782, 94256.22563)
    assertEquals("[${p.x},${p.y}]", AirVision.json.encodeToString(p))
  }

  @Test
  fun deserialization() {
    val p = Vector2d(23.654782, 94256.22563)
    val json = AirVision.json.parseToJsonElement("[23.654782, 94256.22563]")
    assertEquals(p, AirVision.json.decodeFromJsonElement(Vector2dSerializer, json))
  }
}
