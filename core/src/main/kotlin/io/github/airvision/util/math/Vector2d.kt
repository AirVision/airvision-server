/*
 * AirVision
 *
 * Copyright (c) AirVision <https://www.github.com/AirVision>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
package io.github.airvision.util.math

import io.github.airvision.util.math.component.x
import io.github.airvision.util.math.component.y
import org.spongepowered.math.vector.Vector2d
import kotlin.math.max
import kotlin.math.min

fun min(a: Vector2d, b: Vector2d): Vector2d =
  Vector2d(min(a.x, b.x), min(a.y, b.y))

fun max(a: Vector2d, b: Vector2d): Vector2d =
  Vector2d(max(a.x, b.x), max(a.y, b.y))
