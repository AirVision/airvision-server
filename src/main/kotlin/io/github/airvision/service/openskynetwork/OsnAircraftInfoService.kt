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

import com.github.benmanes.caffeine.cache.AsyncLoadingCache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.util.CSVParseFormatException
import io.github.airvision.AirVision
import io.github.airvision.AircraftEngineType
import io.github.airvision.AircraftEngines
import io.github.airvision.AircraftIcao24
import io.github.airvision.AircraftInfo
import io.github.airvision.AircraftManufacturer
import io.github.airvision.WeightCategory
import io.github.airvision.exposed.upsert
import io.github.airvision.service.AircraftInfoService
import io.github.airvision.service.db.AircraftInfoTable
import io.github.airvision.service.db.AircraftManufacturerTable
import io.github.airvision.service.db.Entity
import io.github.airvision.util.coroutines.delay
import io.github.airvision.util.csv.suspendedOpen
import io.github.airvision.util.file.openStream
import io.github.airvision.util.ktor.downloadUpdateToFile
import io.github.airvision.util.notEmptyOrNull
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future
import kotlinx.coroutines.launch
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.time.Duration
import kotlin.time.days
import kotlin.time.minutes
import kotlin.time.seconds
import kotlin.time.toJavaDuration

class OsnAircraftInfoService(
    private val database: Database,
    private val updateDispatcher: CoroutineDispatcher,
    private val getDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AircraftInfoService {

  private val infoCache = buildCache(10.minutes) { aircraftId: AircraftIcao24 -> loadById(aircraftId) }

  private val client = HttpClient()
  private var job: Job? = null

  fun init() {
    val directory = Paths.get("downloads")
    if (!Files.exists(directory))
      Files.createDirectories(directory)

    val aircraftDbPath = directory.resolve("aircraft_database.csv")
    val manufacturerDbPath = directory.resolve("manufacturer_database.csv")

    val baseUrl = "https://opensky-network.org/datasets/metadata/"
    val aircraftDbUrl = "${baseUrl}aircraftDatabase.csv"
    val manufacturerDbUrl = "${baseUrl}doc8643Manufacturers.csv"

    job = GlobalScope.launch(getDispatcher) {
      while (true) {
        var success = try {
          AirVision.logger.info("Checking for manufacturer database updates...")
          var update = client.downloadUpdateToFile(manufacturerDbUrl, manufacturerDbPath)
          if (update)
            AirVision.logger.info("Successfully downloaded the manufacturer database.")
          if (!update) {
            update = newSuspendedTransaction {
              AircraftManufacturerTable.selectAll().count() == 0
            }
          }
          if (update) {
            AirVision.logger.info("Start updating the manufacturer database.")
            updateManufacturers(manufacturerDbPath.openStream())
            AirVision.logger.info("Finished updating the manufacturer database.")
          } else {
            AirVision.logger.info("The manufacturer database is already up-to-date.")
          }
          true
        } catch (e: Exception) {
          AirVision.logger.error("An error occurred while updating the manufacturer database.", e)
          false
        }
        success = if (success) {
          try {
            AirVision.logger.info("Checking for aircraft database updates...")
            var update = client.downloadUpdateToFile(aircraftDbUrl, aircraftDbPath)
            if (update)
              AirVision.logger.info("Successfully downloaded the aircraft database.")
            if (!update) {
              update = newSuspendedTransaction {
                AircraftInfoTable.selectAll().count() == 0
              }
            }
            if (update) {
              AirVision.logger.info("Start updating the aircraft database.")
              updateAircrafts(aircraftDbPath.openStream())
              AirVision.logger.info("Finished updating the aircraft database.")
            } else {
              AirVision.logger.info("The aircraft database is already up-to-date.")
            }
            true
          } catch (e: Exception) {
            AirVision.logger.error("An error occurred while updating the aircraft database.", e)
            false
          }
        } else false
        // Every day, check for aircraft updates, or in case of a
        // failure, try again in half an hour
        delay(if (success) 1.days else 30.minutes)
      }
    }
  }

  fun shutdown() {
    job?.cancel()
    job = null
  }

  private val codeToType = mapOf(
      'L' to AircraftInfo.Type.LandPlane,
      'S' to AircraftInfo.Type.SeaPlane,
      'A' to AircraftInfo.Type.Amphibian,
      'H' to AircraftInfo.Type.Helicopter,
      'D' to AircraftInfo.Type.Dirigible,
      'G' to AircraftInfo.Type.Glider
  )

  private val codeToEngineType = mapOf(
      'P' to AircraftEngineType.Piston,
      'T' to AircraftEngineType.Turboprop,
      'J' to AircraftEngineType.Jet
  )

  override suspend fun get(aircraftId: AircraftIcao24): AircraftInfo? =
      this.infoCache.get(aircraftId).await()

  private suspend fun loadById(aircraftId: AircraftIcao24): AircraftInfo? {
    return newSuspendedTransaction(getDispatcher, db = database) {
      AircraftInfoTable
          .select { AircraftInfoTable.aircraftId eq aircraftId }
          .map {
            val manufacturerId = it[AircraftInfoTable.manufacturer]
            val manufacturer =
                if (manufacturerId != null) getManufacturerById(manufacturerId)?.copy(code = null) else null
            val model = it[AircraftInfoTable.model]
            val owner = it[AircraftInfoTable.owner]

            val description = it[AircraftInfoTable.description]
            val type = it[AircraftInfoTable.type]

            val weightCategory = it[AircraftInfoTable.weightCategory]

            val engineCount = it[AircraftInfoTable.engineCount]
            val engineName = it[AircraftInfoTable.engineName]

            val engineType = if (description != null) codeToEngineType[description[2]] else null
            val engines =
                if (engineName == null && engineCount == null && engineType == null) null
                else AircraftEngines(engineType, engineCount, engineName)

            AircraftInfo(aircraftId, model, owner, manufacturer, engines, type, weightCategory)
          }
          .firstOrNull()
    }
  }

  /**
   * Attempts to get a manufacturer with the given [id].
   */
  private suspend fun getManufacturerById(id: EntityID<Int>): AircraftManufacturer? {
    return newSuspendedTransaction(getDispatcher, db = database) {
      AircraftManufacturerTable
          .select { AircraftManufacturerTable.id eq id }
          .map { AircraftManufacturerTable.rowToManufacturer(it) }
          .firstOrNull()
    }
  }

  private fun AircraftManufacturerTable.rowToManufacturer(row: ResultRow): AircraftManufacturer {
    val code = row[code]
    val country = row[country]
    val name = row[name]
    return AircraftManufacturer(code, name, country)
  }

  private fun <K, V> buildCache(
      expireDuration: Duration, fn: suspend (key: K) -> V
  ): AsyncLoadingCache<K, V> = Caffeine.newBuilder()
      .executor(getDispatcher.asExecutor())
      .expireAfterAccess(expireDuration.toJavaDuration())
      .buildAsync { key, executor ->
        GlobalScope.future(executor.asCoroutineDispatcher()) {
          fn(key)
        }
      }

  private inner class ManufacturerHelper {

    private val byName = buildCache<String, Entity<Int, AircraftManufacturer>?>(10.seconds) { loadByName(it) }
    private val byCode = buildCache<String, Entity<Int, AircraftManufacturer>?>(10.seconds) { loadByCode(it) }

    suspend fun getByCode(code: String): Entity<Int, AircraftManufacturer>? = byCode[code].await()
    suspend fun getByName(name: String): Entity<Int, AircraftManufacturer>? = byName[name].await()

    private suspend fun loadByCode(code: String): Entity<Int, AircraftManufacturer>? {
      return newSuspendedTransaction(getDispatcher, db = database) {
        AircraftManufacturerTable
            .select { AircraftManufacturerTable.code eq code }
            .map { Entity(it[AircraftManufacturerTable.id], AircraftManufacturerTable.rowToManufacturer(it)) }
            .firstOrNull()
      }
    }

    private suspend fun loadByName(name: String): Entity<Int, AircraftManufacturer>? {
      return newSuspendedTransaction(getDispatcher, db = database) {
        AircraftManufacturerTable
            .select { AircraftManufacturerTable.name eq name }
            .map { Entity(it[AircraftManufacturerTable.id], AircraftManufacturerTable.rowToManufacturer(it)) }
            .firstOrNull()
      }
    }

    suspend fun getOrInsert(data: CsvManufacturer): Entity<Int, AircraftManufacturer> {
      var entity = getByCode(data.code ?: data.name.toUpperCase())
      if (entity != null)
        return entity
      if (data.code == null) {
        // If there's no code, lookup by name
        entity = getByName(data.name)
        if (entity != null)
          return entity
      }
      // Create a new manufacturer
      entity = newSuspendedTransaction(updateDispatcher, db = database) {
        val uniqueCode = data.code ?: run {
          val v = data.name.toUpperCase()
          if (v.length <= 20) v else {
            UUID.nameUUIDFromBytes(v.toByteArray()).toString().substring(0, 20)
          }
        }
        val id = AircraftManufacturerTable
            .insertAndGetId {
              it[code] = uniqueCode
              it[name] = data.name
              it[country] = data.country
            }
        Entity(id, AircraftManufacturer(data.code, data.name, data.country))
      }
      val completed = CompletableFuture.completedFuture(entity)
      val (_, manufacturer) = entity
      byName.put(manufacturer.name, completed)
      val code = manufacturer.code
      if (code != null)
        byCode.put(code, completed)
      return entity
    }
  }

  private suspend fun updateAircrafts(inputStream: InputStream) {
    val manufacturers = ManufacturerHelper()
    csvReader().suspendedOpen(inputStream) {
      val header = readAllAsSequence().first()
          .map { it.toLowerCase() }
      val index = object {
        val aircraftId = header.indexOf("icao24")
        val manufacturerCode = header.indexOf("manufacturericao")
        val manufacturerName = header.indexOf("manufacturername")
        val model = header.indexOf("model")
        val description = header.indexOf("icaoaircrafttype")
        val owner = header.indexOf("owner")
        val engines = header.indexOf("engines")
        val categoryDescription = header.indexOf("categorydescription")
      }
      val enginesRegex = "(?:([0-9]+)\\s+x\\s+)?(.+)".toRegex()
      val descriptionRegex = "[LSAHDG][1-9][PTJ](/.+)?".toRegex()
      for (it in readAllAsSequence().filter { it.first().isNotEmpty() }) {
        val model = it[index.model]
        if (model.isBlank())
          continue

        val aircraftId = AircraftIcao24.parse(it[index.aircraftId])
        val manufacturerCode = it[index.manufacturerCode].notEmptyOrNull()
        val manufacturerName = it[index.manufacturerName].trim()
        val owner = it[index.owner].notEmptyOrNull()
        val categoryDescription = it[index.categoryDescription].toLowerCase()

        var description = it[index.description].notEmptyOrNull()
        if (description != null && !descriptionRegex.matches(description))
          description = null

        val type = if (!description.isNullOrEmpty()) {
          codeToType[description[0]]
        } else if (categoryDescription.contains("glider")) {
          AircraftInfo.Type.Glider
        } else null

        val manufacturer = if (manufacturerName.isBlank()) null else
          manufacturers.getOrInsert(CsvManufacturer(manufacturerCode, manufacturerName, null))

        val weightCategory = when {
          (!description.isNullOrEmpty() && description[0] == 'G') ||
              categoryDescription.contains("glider") ||
              categoryDescription.contains("ultralight") -> WeightCategory.Ultralight
          categoryDescription.contains("light") -> WeightCategory.Light
          categoryDescription.contains("small") -> WeightCategory.Normal
          categoryDescription.contains("large") -> WeightCategory.Heavy
          categoryDescription.contains("heavy") -> WeightCategory.VeryHeavy
          else -> null
        }

        var engineCount: Int? =
            if (description?.length ?: 0 > 1) description!![1].toString().toIntOrNull() else null
        var engineName: String? = null

        val enginesValue = it[index.engines]
        if (enginesValue.isNotBlank() && !enginesValue.contains("no engines", ignoreCase = true)) {
          val engineEntries = enginesValue
              .split("<br>")
              .asSequence()
              .filter { it.isNotBlank() }
              .map { it
                  .replace("&amp;", "&")
                  .replace("&nbsp;", " ")
                  .replace("\\s+".toRegex(), " ") // Replace duplicate spaces
              }
              .map {
                val result = enginesRegex.matchEntire(it) ?: error("Should never happen")
                val count = result.groups[1]?.value?.toInt() ?: 1
                EngineEntry(result.groups[2]!!.value, count)
              }
              .toList()
          if (engineEntries.size == 1) {
            val entry = engineEntries.first()
            engineCount = entry.count
            engineName = entry.name
          }
        }

        newSuspendedTransaction(updateDispatcher, db = database) {
          AircraftInfoTable.upsert(AircraftInfoTable.aircraftId) {
            it[AircraftInfoTable.aircraftId] = aircraftId
            it[AircraftInfoTable.model] = model
            it[AircraftInfoTable.type] = type
            it[AircraftInfoTable.description] = description
            it[AircraftInfoTable.engineName] = engineName
            it[AircraftInfoTable.engineCount] = engineCount
            it[AircraftInfoTable.manufacturer] = manufacturer?.id
            it[AircraftInfoTable.owner] = owner
            it[AircraftInfoTable.weightCategory] = weightCategory
          }
        }
      }
    }
  }

  private data class EngineEntry(
      val name: String,
      val count: Int
  )

  /**
   * Loads the manufacturer dataset file.
   */
  private suspend fun updateManufacturers(inputStream: InputStream) {
    val nameAndCountryRegex = "\\((.+)\\)\$".toRegex()
    csvReader().suspendedOpen(inputStream) {
      try {
        for (it in readAllAsSequence().drop(2).filter { it.isNotEmpty() }) {
          val code = it[0]
          val nameAndCountry = it[1].replace("\"", "")

          val match = nameAndCountryRegex.find(nameAndCountry)
          val (name, country) = if (match != null) {
            nameAndCountryRegex.replace(nameAndCountry, "").trim() to match.groupValues[1].trim()
          } else {
            nameAndCountry.trim() to null
          }

          newSuspendedTransaction(updateDispatcher, db = database) {
            AircraftManufacturerTable.upsert(AircraftManufacturerTable.code) {
              it[AircraftManufacturerTable.code] = code
              it[AircraftManufacturerTable.name] = name
              it[AircraftManufacturerTable.country] = country
            }
          }
        }
      } catch (e: CSVParseFormatException) {
        // The following error is thrown for the last empty line, currently a bug in the library
        // See: https://github.com/doyaaaaaken/kotlin-csv/issues/18
        // TODO
        if (e.message?.contains("must appear delimiter or line terminator after quote end") != true) {
          throw e
        }
      }
    }
  }

  private data class CsvManufacturer(
      val code: String?,
      val name: String,
      val country: String?
  )
}
