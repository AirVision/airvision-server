/*
 * AirVision
 *
 * Copyright (c) AirVision <https://www.github.com/AirVision>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
package io.github.airvision.util.time

import java.time.Instant
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toJavaDuration

fun Instant.toDouble(unit: DurationUnit): Double =
  Duration.convert(nano.toDouble(), DurationUnit.NANOSECONDS, unit) +
      Duration.convert(epochSecond.toDouble(), DurationUnit.SECONDS, unit)

operator fun Instant.minus(other: Instant): Duration =
  Duration.milliseconds((toEpochMilli() - other.toEpochMilli()))

operator fun Instant.minus(duration: Duration): Instant =
  minus(duration.toJavaDuration())

operator fun Instant.plus(duration: Duration): Instant =
  plus(duration.toJavaDuration())
