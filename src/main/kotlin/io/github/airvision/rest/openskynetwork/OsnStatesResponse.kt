/*
 * AirVision
 *
 * Copyright (c) AirVision <https://www.github.com/AirVision>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
package io.github.airvision.rest.openskynetwork

import kotlinx.serialization.Serializable

@Serializable
data class OsnStatesResponse(
    val time: Int,
    val states: List<OsnAircraft>
)
