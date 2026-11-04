package ru.chsu.studentmap.data.repository

import ru.chsu.studentmap.data.remote.StudMapApi
import ru.chsu.studentmap.data.remote.dto.OsmPlaceDto

/**
 * Корпуса/места с бэка (OSM через FastAPI).
 * Карта этим репозиторием НЕ пользуется — у неё локальный sampleMapPoints.
 */
class PlaceRepository(private val api: StudMapApi) {

    // Координаты кампуса ЧГУ, как на карте в OsmMapView
    companion object {
        const val CAMPUS_LAT = 59.1245
        const val CAMPUS_LON = 37.9275
        const val DEFAULT_RADIUS_M = 4000
    }

    suspend fun getCampusPlaces(category: String, radius: Int = DEFAULT_RADIUS_M): List<OsmPlaceDto> {
        return api.getMapPlaces(
            latitude = CAMPUS_LAT,
            longitude = CAMPUS_LON,
            radius = radius,
            categories = listOf(category)
        )
    }
}
