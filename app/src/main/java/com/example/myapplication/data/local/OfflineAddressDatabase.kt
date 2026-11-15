package ru.chsu.studentmap.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.location.Location
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.chsu.studentmap.sampleMapPoints
import java.io.File
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// Моделm для найденного места (название + адрес)
data class ResolvedPlaceInfo(
    val title: String? = null,
    val address: String
)

// Результат поиска из базы SQLite
data class OfflineAddressResult(
    val street: String,
    val houseNumber: String,
    val name: String?,
    val latitude: Double,
    val longitude: Double,
    val distanceMeters: Float
) {
    // Склеиваем улицу и номер дома: "Советский пр-кт, д. 8"
    fun formattedAddress(): String {
        return if (houseNumber.isNotBlank()) {
            if (houseNumber.startsWith("д.", ignoreCase = true) || houseNumber.startsWith("стр.", ignoreCase = true)) {
                "$street, $houseNumber"
            } else {
                "$street, д. $houseNumber"
            }
        } else {
            street
        }
    }
}

// Локальная база адресов
object OfflineAddressDatabase {
    private const val TAG = "OfflineAddressDb"
    private const val DB_NAME = "addresses.sqlite"
    private const val ASSETS_PATH = "addresses.sqlite"

    @Volatile
    private var database: SQLiteDatabase? = null

    // Открываем базу. При первом запуске копируем её из assets в файлы приложения
    @Synchronized
    private fun getDatabase(context: Context): SQLiteDatabase? {
        database?.let {
            if (it.isOpen) return it
        }

        val dbFile = File(context.filesDir, DB_NAME)
        try {
            // Если файла ещё нет в памяти телефона — копируем из ассетов
            if (!dbFile.exists() || dbFile.length() == 0L) {
                context.assets.open(ASSETS_PATH).use { input ->
                    context.openFileOutput(DB_NAME, Context.MODE_PRIVATE).use { output ->
                        input.copyTo(output)
                    }
                }
                Log.d(TAG, "База адресов успешно распакована: ${dbFile.length()} байт")
            }

            val db = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY)
            database = db
            return db
        } catch (e: Exception) {
            Log.e(TAG, "Не удалось открыть офлайн-базу адресов: ${e.message}", e)
            return null
        }
    }

    // Считаем примерное расстояние в метрах между двумя координатами
    private fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    // Главная функция: определяем что за место по координатам клика
    suspend fun resolvePlace(context: Context, latitude: Double, longitude: Double): ResolvedPlaceInfo? =
        withContext(Dispatchers.IO) {
            // 1. Сначала проверяем: вдруг кликнули прямо в корпус ЧГУ или общежитие
            val campusMatch = sampleMapPoints.firstOrNull { point ->
                distanceMeters(latitude, longitude, point.latitude, point.longitude) <= 50.0
            }
            if (campusMatch?.address != null) {
                return@withContext ResolvedPlaceInfo(
                    title = campusMatch.title,
                    address = campusMatch.address
                )
            }

            // 2. Если не ЧГУ, ищем ближайший дом в нашей офлайн-базе Череповца
            val offline = findNearest(context, latitude, longitude)
            if (offline != null) {
                return@withContext ResolvedPlaceInfo(
                    title = offline.name,
                    address = offline.formattedAddress()
                )
            }

            // Если ничего рядом не нашли
            return@withContext null
        }

    // Упрощённо возвращаем только строчку с адресом
    suspend fun resolveAddress(context: Context, latitude: Double, longitude: Double): String? {
        return resolvePlace(context, latitude, longitude)?.address
    }

    // Ищем в sqlite ближайший дом в радиусе 80 метров
    fun findNearest(
        context: Context,
        latitude: Double,
        longitude: Double,
        maxRadiusMeters: Float = 80f
    ): OfflineAddressResult? {
        val db = getDatabase(context) ?: return null

        // Ограничиваем квадрат поиска по координатам, чтобы не шерстить всю базу
        val deltaLat = (maxRadiusMeters / 111139.0) * 1.3
        val deltaLon = (maxRadiusMeters / (111139.0 * cos(Math.toRadians(latitude)))) * 1.3

        val minLat = latitude - deltaLat
        val maxLat = latitude + deltaLat
        val minLon = longitude - deltaLon
        val maxLon = longitude + deltaLon

        // Быстрый запрос по диапазону координат
        val query = """
            SELECT street, house_number, name, lat, lon
            FROM addresses
            WHERE lat BETWEEN ? AND ?
              AND lon BETWEEN ? AND ?
        """.trimIndent()

        val results = FloatArray(1)
        var bestResult: OfflineAddressResult? = null
        var minDistance = maxRadiusMeters

        try {
            val cursor = db.rawQuery(
                query,
                arrayOf(
                    minLat.toString(),
                    maxLat.toString(),
                    minLon.toString(),
                    maxLon.toString()
                )
            )

            cursor.use { c ->
                val streetIdx = c.getColumnIndex("street")
                val houseIdx = c.getColumnIndex("house_number")
                val nameIdx = c.getColumnIndex("name")
                val latIdx = c.getColumnIndex("lat")
                val lonIdx = c.getColumnIndex("lon")

                while (c.moveToNext()) {
                    val rowLat = c.getDouble(latIdx)
                    val rowLon = c.getDouble(lonIdx)

                    Location.distanceBetween(latitude, longitude, rowLat, rowLon, results)
                    val dist = results[0]

                    if (dist < minDistance) {
                        val street = if (streetIdx >= 0) c.getString(streetIdx) ?: "" else ""
                        val house = if (houseIdx >= 0) c.getString(houseIdx) ?: "" else ""
                        val name = if (nameIdx >= 0) c.getString(nameIdx) else null

                        if (street.isNotBlank() || house.isNotBlank()) {
                            minDistance = dist
                            bestResult = OfflineAddressResult(
                                street = street,
                                houseNumber = house,
                                name = name?.takeIf { it.isNotBlank() },
                                latitude = rowLat,
                                longitude = rowLon,
                                distanceMeters = dist
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка выполнения запроса адреса: ${e.message}", e)
        }

        return bestResult
    }
}
