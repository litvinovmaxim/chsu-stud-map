package ru.chsu.studentmap.data.repository

import ru.chsu.studentmap.data.remote.StudMapApi
import ru.chsu.studentmap.data.remote.dto.EventDto
import ru.chsu.studentmap.data.remote.dto.OsmPlaceDto
import ru.chsu.studentmap.data.remote.dto.PlaceCreateDto
import ru.chsu.studentmap.data.remote.dto.PlaceResponseDto

class FavoriteRepository(private val api: StudMapApi) {

    // Загружаем список избранных мест с сервера
    suspend fun getPlaces(): List<PlaceResponseDto> = api.getFavoritePlaces()

    // Добавляем точку в избранные места
    suspend fun addPlace(place: PlaceCreateDto): PlaceResponseDto {
        return api.addFavoritePlace(place)
    }

    // Добавляем объект кампуса ЧГУ в избранные места
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

    // Удаляем место из избранного по id
    suspend fun removePlace(placeId: Int) {
        api.removeFavoritePlace(placeId)
    }

    // Получаем все сохранённые мероприятия пользователя
    suspend fun getFavoriteEvents(): List<EventDto> {
        return api.getFavoriteEvents()
    }

    // Достаем только айдишники сохранённых мероприятий (чтобы подсветить звёздочки)
    suspend fun getEventIds(): Set<Int> {
        return runCatching {
            api.getFavoriteEvents().map { it.id }.toSet()
        }.getOrDefault(emptySet())
    }

    suspend fun getEventIds(userId: Int): Set<Int> = getEventIds()

    // Добавляем мероприятие в избранное
    suspend fun addEvent(eventId: Int) {
        api.addFavoriteEvent(eventId)
    }

    suspend fun addEvent(userId: Int, eventId: Int) = addEvent(eventId)

    // Удаляем мероприятие из избранного
    suspend fun removeEvent(eventId: Int) {
        api.removeFavoriteEvent(eventId)
    }

    suspend fun removeEvent(userId: Int, eventId: Int) = removeEvent(eventId)
}
