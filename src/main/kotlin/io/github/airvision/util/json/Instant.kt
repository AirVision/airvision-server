/*
 * AirVision
 *
 * Copyright (c) AirVision <https://www.github.com/AirVision>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
package io.github.airvision.util.json

import kotlinx.serialization.json.JsonElement
import java.time.Instant

val JsonElement.instant: Instant
  get() = Instant.ofEpochSecond(long)

val JsonElement.instantOrNull: Instant?
  get() = longOrNull?.let { Instant.ofEpochSecond(it) }
