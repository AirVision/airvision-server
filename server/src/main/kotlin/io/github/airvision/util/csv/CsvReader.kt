/*
 * AirVision
 *
 * Copyright (c) AirVision <https://www.github.com/AirVision>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
package io.github.airvision.util.csv

import com.github.doyaaaaaken.kotlincsv.client.CsvFileReader
import com.github.doyaaaaaken.kotlincsv.client.CsvReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

suspend fun <T> CsvReader.suspendedOpen(
  inputStream: InputStream,
  read: suspend CsvFileReader.() -> T
): T {
  return withContext(Dispatchers.IO) {
    openAsync(inputStream) {
      read()
    }
  }
}
