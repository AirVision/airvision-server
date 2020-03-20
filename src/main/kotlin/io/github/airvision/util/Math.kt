/*
 * AirVision
 *
 * Copyright (c) AirVision <https://www.github.com/AirVision>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
@file:Suppress("NOTHING_TO_INLINE")

package io.github.airvision.util

/**
 * Converts from degrees to radians.
 */
inline fun degToRad(degrees: Double): Double =
    Math.toRadians(degrees)

/**
 * Converts from radians to degrees.
 */
inline fun radToDeg(radians: Double): Double =
    Math.toDegrees(radians)

/**
 * Converts from feet (ft) to meters (m).
 */
fun feetToMeters(feet: Double): Double =
    feet * 0.3048
