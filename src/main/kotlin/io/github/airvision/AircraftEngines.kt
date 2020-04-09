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

import kotlinx.serialization.Serializable

@Serializable
data class AircraftEngines(
    val type: AircraftEngineType? = null,
    val count: Int? = null,
    val entries: List<AircraftEnginesEntry>? = null
)

/**
 * @property name The name of the engine(s), if known
 * @property count The count of engines
 */
@Serializable
data class AircraftEnginesEntry(
    val name: String,
    val count: Int? = null
)
