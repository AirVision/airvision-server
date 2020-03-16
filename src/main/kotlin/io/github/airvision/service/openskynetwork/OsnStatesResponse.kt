/*
 * AirVision
 *
 * Copyright (c) AirVision <https://www.github.com/AirVision>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
package io.github.airvision.service.openskynetwork

import kotlinx.serialization.ContextualSerialization
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class OsnStatesResponse(
    @ContextualSerialization val time: Instant,
    val states: List<OsnAircraft>?
)
