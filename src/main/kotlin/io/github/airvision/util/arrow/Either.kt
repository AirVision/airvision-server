/*
 * AirVision
 *
 * Copyright (c) AirVision <https://www.github.com/AirVision>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
package io.github.airvision.util.arrow

import arrow.core.Either
import arrow.core.Left
import arrow.core.Right

suspend fun <A, B, C> Either<A, B>.suspendedMap(f: suspend (B) -> C): Either<A, C> =
    suspendedFlatMap { Right(f(it)) }

suspend fun <A, B, C> Either<A, B>.suspendedFlatMap(f: suspend (B) -> Either<A, C>): Either<A, C> {
  return when (this) {
    is Either.Right -> f(b)
    is Either.Left -> this
  }
}

suspend fun <A, B, C> Either<A, B>.suspendedMapLeft(f: suspend (A) -> C): Either<C, B> =
    suspendedFlatMapLeft { Left(f(it)) }

suspend fun <A, B, C> Either<A, B>.suspendedFlatMapLeft(f: suspend (A) -> Either<C, B>): Either<C, B> {
  return when (this) {
    is Either.Right -> this
    is Either.Left -> f(a)
  }
}

fun <A, B, C> Either<A, B>.flatMapLeft(f: (A) -> Either<C, B>): Either<C, B> {
  return when (this) {
    is Either.Right -> this
    is Either.Left -> f(a)
  }
}
