/*
 * AirVision
 *
 * Copyright (c) AirVision <https://www.github.com/AirVision>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
package io.github.airvision.util

import java.time.Instant
import kotlin.math.abs
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.milliseconds
import kotlin.time.seconds
import kotlin.time.toJavaDuration

fun Instant.toDouble(unit: DurationUnit): Double =
    Duration.convert(nano.toDouble(), DurationUnit.NANOSECONDS, unit) +
        Duration.convert(epochSecond.toDouble(), DurationUnit.SECONDS, unit)

fun Instant.minus(other: Instant): Duration =
    (toEpochMilli() - other.toEpochMilli()).milliseconds

fun Instant.minus(duration: Duration): Instant =
    minus(duration.toJavaDuration())

fun Duration.abs(): Duration = abs(inSeconds).seconds