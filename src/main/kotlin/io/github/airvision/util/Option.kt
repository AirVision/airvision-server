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

import arrow.core.Option

inline fun <T> Option<T>.ifSome(ifSome: (T) -> Unit): Unit = fold({}, ifSome)
inline fun <T> Option<T>.ifNone(ifNone: () -> Unit): Unit = fold(ifNone, {})
