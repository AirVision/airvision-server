/*
 * AirVision
 *
 * Copyright (c) AirVision <https://www.github.com/AirVision>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
package io.github.airvision.util.channel

import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ChannelIterator
import kotlinx.coroutines.channels.ValueOrClosed
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * A channel that only allows the same value to be
 * queued once at the same time.
 */
fun <E> Channel<E>.distinct(): Channel<E> = DistinctChannel(this) { it }

/**
 * A channel that only allows the same key produced by
 * [keyProvider] to be queued once at the same time.
 */
fun <E, K> Channel<E>.distinctBy(keyProvider: (E) -> K): Channel<E> = DistinctChannel(this, keyProvider)

@InternalCoroutinesApi
private class DistinctChannel<E, K>(val channel: Channel<E>, val keyProvider: (E) -> K) : Channel<E> by channel {

  private val keys = Collections.newSetFromMap(ConcurrentHashMap<K, Boolean>())

  override fun poll(): E? {
    val value = this.channel.poll()
    if (value != null)
      keys.remove(keyProvider(value))
    return value
  }

  override fun offer(element: E): Boolean {
    if (!keys.add(keyProvider(element)))
      return false
    return channel.offer(element)
  }

  override suspend fun send(element: E) {
    if (keys.add(keyProvider(element)))
      channel.send(element)
  }

  override fun iterator(): ChannelIterator<E> {
    val it = channel.iterator()
    return object : ChannelIterator<E> {
      override suspend fun hasNext() = it.hasNext()
      override fun next(): E {
        val value = it.next()
        keys.remove(keyProvider(value))
        return value
      }
    }
  }

  @ObsoleteCoroutinesApi
  @Deprecated(
      message = "Deprecated in favor of receiveOrClosed and receiveOrNull extension",
      level = DeprecationLevel.WARNING,
      replaceWith = ReplaceWith("receiveOrNull", "kotlinx.coroutines.channels.receiveOrNull")
  )
  override suspend fun receiveOrNull(): E? = receiveOrClosed().valueOrNull

  override suspend fun receive(): E {
    val value = channel.receive()
    keys.remove(keyProvider(value))
    return value
  }

  override suspend fun receiveOrClosed(): ValueOrClosed<E> {
    val value = channel.receiveOrClosed()
    if (value.isClosed)
      return value
    keys.remove(keyProvider(value.value))
    return value
  }
}
