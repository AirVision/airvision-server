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
  require(n >= 0) { "Requested element count $n is less than zero." }
  if (isEmpty())
    return emptyList()
  return iterator().poll(n)
}

fun <E> MutableIterator<E>.poll(n: Int): List<E> {
  require(n >= 0) { "Requested element count $n is less than zero." }
  if (n == 0 || !hasNext())
    return emptyList()
  if (n == 1)
    return listOf(next())
  var remaining = n
  val result = mutableListOf<E>()
  while (hasNext() && remaining > 0) {
    remaining--
    result.add(next())
    remove()
  }
  return result
}

fun <E> MutableCollection<E>.pollWhile(fn: (E) -> Boolean): List<E> {
  if (isEmpty())
    return emptyList()
  return iterator().pollWhile(fn)
}

fun <E> MutableIterator<E>.pollWhile(fn: (E) -> Boolean): List<E> {
  if (!hasNext())
    return emptyList()
  val result = mutableListOf<E>()
  while (hasNext()) {
    val value = next()
    if (!fn(value))
      break
    result.add(value)
    remove()
  }
  return result
}

fun <E> MutableList<E>.pollLast(n: Int): List<E> {
  require(n >= 0) { "Requested element count $n is less than zero." }
  if (isEmpty() || n == 0)
    return emptyList()
  if (n == 1)
    return listOf(removeLast())
  if (n >= size) {
    val list = toList()
    clear()
    return list
  }
  val list = ArrayList<E>(n)
  val index = size - n
  for (i in 0 until n)
    list.add(removeAt(index))
  return list
}

fun <E> MutableList<E>.pollLastWhile(fn: (E) -> Boolean): List<E> {
  if (isEmpty())
    return emptyList()
  val it = listIterator(size)
  while (it.hasPrevious()) {
    if (!fn(it.previous())) {
      it.next()
      val expectedSize = size - it.nextIndex()
      if (expectedSize == 0)
        return emptyList()
      if (expectedSize == 0)
        return listOf(removeLast())
      val list = ArrayList<E>(expectedSize)
      while (it.hasNext()) {
        add(it.next())
        it.remove()
      }
      return list
    }
  }
  val list = toList()
  clear()
  return list
}

@JvmName("reverseMutableIterator")
fun <E> MutableList<E>.reverseIterator(): MutableListIterator<E> {
  val it = listIterator(size)
  return object : MutableListIterator<E> {
    override fun hasNext() = it.hasPrevious()
    override fun next() = it.previous()
    override fun nextIndex() = it.previousIndex()
    override fun remove() = it.remove()
    override fun hasPrevious() = it.hasNext()
    override fun previous() = it.next()
    override fun previousIndex() = it.nextIndex()
    override fun set(element: E) = it.set(element)
    override fun add(element: E) {
      it.add(element)
      // Adding a element makes it the new
      // previous value, so skip that one
      it.previous()
    }
  }
}

fun <E> List<E>.reverseIterator(): ListIterator<E> {
  val it = listIterator(size)
  return object : ListIterator<E> {
    override fun hasNext() = it.hasPrevious()
    override fun next() = it.previous()
    override fun nextIndex() = it.previousIndex()
    override fun hasPrevious() = it.hasNext()
    override fun previous() = it.next()
    override fun previousIndex() = it.nextIndex()
  }
}
