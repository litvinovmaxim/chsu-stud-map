package ru.chsu.studentmap.data.repository

import ru.chsu.studentmap.data.remote.StudMapApi
import ru.chsu.studentmap.data.remote.dto.OsmPlaceDto
import ru.chsu.studentmap.data.remote.dto.PlaceCreateDto
import ru.chsu.studentmap.data.remote.dto.PlaceResponseDto

class FavoriteRepository(private val api: StudMapApi) {

    suspend fun getPlaces(): List<PlaceResponseDto> = api.getFavoritePlaces()

    suspend fun addPlace(place: OsmPlaceDto): PlaceResponseDto {
        return api.addFavoritePlace(
            PlaceCreateDto(
                name = place.name,
                address = place.address,
                latitude = place.latitude,
                longitude = place.longitude,
                category = place.category,
                osmType = place.osmType,
                osmId = place.osmId
            )
        )
    }

    suspend fun removePlace(placeId: Int) {
        api.removeFavoritePlace(placeId)
    }

    suspend fun getEventIds(userId: Int): Set<Int> {
        return api.getFavoriteEvents(userId).mapNotNull { it.eventId }.toSet()
    }

    suspend fun addEvent(userId: Int, eventId: Int) {
        api.addFavoriteEvent(userId, eventId)
    }

    suspend fun removeEvent(userId: Int, eventId: Int) {
        api.removeFavoriteEvent(userId, eventId)
    }
}
