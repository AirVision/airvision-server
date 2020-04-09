/*
 * AirVision
 *
 * Copyright (c) AirVision <https://www.github.com/AirVision>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
package io.github.airvision.util.collections

fun <E> MutableCollection<E>.poll(n: Int): List<E> {
  return pollWhile(n) { true }
}

fun <E> MutableCollection<E>.pollWhile(n: Int, fn: (E) -> Boolean): List<E> {
  require(n >= 0) { "Requested element count $n is less than zero." }
  if (n == 0)
    return emptyList()
  return iterator().pollWhile(n, fn)
}

fun <E> MutableList<E>.pollLast(n: Int): List<E> {
  return pollLastWhile(n) { true }
}

fun <E> MutableList<E>.pollLastWhile(n: Int, fn: (E) -> Boolean): List<E> {
  require(n >= 0) { "Requested element count $n is less than zero." }
  if (n == 0)
    return emptyList()
  return reverseIterator().pollWhile(n, fn)
}

fun <E> MutableIterator<E>.poll(n: Int): List<E> {
  return pollWhile(n) { true }
}

fun <E> MutableIterator<E>.pollWhile(n: Int, fn: (E) -> Boolean): List<E> {
  require(n >= 0) { "Requested element count $n is less than zero." }
  if (n == 0 || !hasNext())
    return emptyList()
  var remaining = n
  val result = mutableListOf<E>()
  while (hasNext() && remaining > 0) {
    val value = next()
    if (!fn(value))
      continue
    remaining--
    result.add(value)
    remove()
  }
  return result
}

@JvmName("reverseMutableIterator")
fun <E> MutableList<E>.reverseIterator(): MutableIterator<E> {
  val it = listIterator(size)
  return object : MutableIterator<E> {
    override fun hasNext() = it.hasPrevious()
    override fun next() = it.previous()
    override fun remove() = it.remove()
  }
}

fun <E> List<E>.reverseIterator(): Iterator<E> {
  val it = listIterator(size)
  return object : Iterator<E> {
    override fun hasNext() = it.hasPrevious()
    override fun next() = it.previous()
  }
}
