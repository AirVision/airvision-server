/*
 * AirVision
 *
 * Copyright (c) AirVision <https://www.github.com/AirVision>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
@file:Suppress("BlockingMethodInNonBlockingContext")

package io.github.airvision.util.ktor

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.head
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.HttpStatement
import io.ktor.http.isSuccess
import io.ktor.utils.io.errors.IOException
import io.ktor.utils.io.jvm.nio.copyTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.FileTime
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.Instant

private val logger = LoggerFactory.getLogger("http-client")

suspend fun HttpClient.downloadToFile(url: String, path: Path) {
  val statement = get<HttpStatement>(url)
  return statement.execute { response ->
    if (!response.status.isSuccess())
      throw IOException("Failed to download the file from: $url")
    // Update the Last-Modified field based on the remote time, if present
    val lastModified = try {
      response.lastModified
    } catch (e: Exception) {
      logger.error("An error occurred while downloading: $url", e)
      null
    }
    withContext(Dispatchers.IO) {
      response.content.copyTo(
        Files.newByteChannel(
          path,
          StandardOpenOption.CREATE,
          StandardOpenOption.WRITE
        )
      )
      if (lastModified != null)
        Files.setLastModifiedTime(path, FileTime.from(lastModified))
    }
  }
}

suspend fun HttpClient.downloadUpdateToFile(url: String, path: Path): Boolean {
  return withContext(Dispatchers.IO) {
    val fileLastModified =
      if (Files.exists(path)) Files.getLastModifiedTime(path).toInstant() else null
    val remoteLastModified = getLastModified(url)

    // File is already up-to-date
    if (fileLastModified != null && fileLastModified == remoteLastModified)
      return@withContext false

    val tmpFile = Files.createTempFile("tmp_download", "")
    downloadToFile(url, tmpFile)

    Files.deleteIfExists(path)
    Files.move(tmpFile, path)
    true
  }
}

private val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz")

/**
 * Gets the [Instant] at which the entry was last updated.
 */
private suspend fun HttpClient.getLastModified(url: String): Instant? {
  val statement = head<HttpStatement>(url)
  return statement.execute { response ->
    if (!response.status.isSuccess())
      return@execute null
    response.lastModified
  }
}

private val HttpResponse.lastModified: Instant?
  get() {
    val value = headers["Last-Modified"] ?: return null
    return try {
      dateFormat.parse(value).toInstant()
    } catch (e: ParseException) {
      throw IOException("Failed to parse Last-Modified field ($value)", e)
    }
  }
