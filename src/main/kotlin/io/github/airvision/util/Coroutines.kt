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

import kotlin.time.Duration

suspend fun delay(millis: Long) =
    kotlinx.coroutines.delay(millis)

suspend fun delay(duration: Duration) =
    kotlinx.coroutines.delay(duration.toLongMilliseconds())
