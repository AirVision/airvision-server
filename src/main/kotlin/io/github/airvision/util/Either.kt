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

import arrow.core.Either
import arrow.core.Right

@Suppress("UNCHECKED_CAST")
suspend fun <A, B, C> Either<A, B>.suspendedMap(f: suspend (B) -> C): Either<A, C> =
    suspendedFlatMap { Right(f(it)) }

@Suppress("UNCHECKED_CAST")
suspend fun <A, B, C> Either<A, B>.suspendedFlatMap(f: suspend (B) -> Either<A, C>): Either<A, C> {
  return when (this) {
    is Either.Right -> f(b)
    is Either.Left -> this
  }
}
