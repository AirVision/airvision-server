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

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import java.io.InputStream

class OsnAircraftModelService {

  /**
   * Loads the CSV files and updates the database.
   */
  private fun processCsv(inputStream: InputStream, types: List<CsvAircraftType>, manufacturers: List<CsvManufacturer>) {
    csvReader().open(inputStream) {
      readAllAsSequence()
          .forEach {

          }
    }
  }

  /**
   * Loads the types dataset file.
   */
  // https://opensky-network.org/datasets/metadata/doc8643AircraftTypes.csv
  private fun loadTypes(inputStream: InputStream): List<CsvAircraftType> {
    return csvReader().open(inputStream) {
      readAllAsSequence()
          .drop(1)
          .map {
            val description = it[1]
            val designator = it[2]
            val manufacturer = it[5]
            val name = it[6]
            val wtc = it[7]
            CsvAircraftType(description, designator, manufacturer, name, wtc)
          }
          .toList()
    }
  }

  /**
   * Loads the manufacturer dataset file.
   */
  // https://opensky-network.org/datasets/metadata/doc8643Manufacturers.csv
  private fun loadManufacturers(inputStream: InputStream): List<CsvManufacturer> {
    val nameAndCountryRegex = "\\((.+)\\)\$".toRegex()
    return csvReader().open(inputStream) {
      readAllAsSequence()
          .drop(2)
          .map {
            val code = it[0]
            val nameAndCountry = it[1].replace("\"", "")

            val match = nameAndCountryRegex.find(nameAndCountry)
            val (name, country) = if (match != null) {
              nameAndCountryRegex.replace(nameAndCountry, "") to match.groupValues[1]
            } else {
              nameAndCountry to null
            }

            CsvManufacturer(code, name, country)
          }
          .toList()
    }
  }

  private data class CsvAircraftType(
      val description: String,
      val designator: String,
      val manufacturer: String,
      val name: String,
      val wtc: String
  )

  private data class CsvManufacturer(
      val code: String,
      val name: String,
      val country: String?
  )
}
