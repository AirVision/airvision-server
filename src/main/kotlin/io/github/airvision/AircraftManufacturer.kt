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

/**
 * Represents an aircraft manufacturer.
 */
@Serializable
data class AircraftManufacturer(
    val code: String? = null,
    val name: String,
    val country: String? = null
)
