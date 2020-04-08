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

import org.spongepowered.math.vector.Vector2d
import java.lang.Double.min

fun min(a: Vector2d, b: Vector2d): Vector2d =
    Vector2d(min(a.x, b.x), min(a.y, b.y))
