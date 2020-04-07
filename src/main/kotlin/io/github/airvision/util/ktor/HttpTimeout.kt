/*
 * AirVision
 *
 * Copyright (c) AirVision <https://www.github.com/AirVision>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
package io.github.airvision.util.ktor

import io.ktor.client.features.HttpTimeout
import kotlin.time.Duration
import kotlin.time.milliseconds

var HttpTimeout.HttpTimeoutCapabilityConfiguration.requestTimeout: Duration?
  get() = requestTimeoutMillis?.milliseconds
  set(value) {
    requestTimeoutMillis = value?.inMilliseconds?.toLong()
  }
