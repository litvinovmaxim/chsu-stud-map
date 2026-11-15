package ru.chsu.studentmap.data.repository

import ru.chsu.studentmap.data.remote.StudMapApi
import ru.chsu.studentmap.data.remote.dto.EventDto

class EventRepository(private val api: StudMapApi) {

    /*
     * Получение мероприятий с бэкенда (GET /events/).
     */
    suspend fun getEvents(): List<EventDto> {
        return api.getEvents()
    }

    suspend fun searchEvents(search: String? = null, status: String? = null): List<EventDto> {
        return api.searchEvents(search, status)
    }

    suspend fun getEventById(eventId: Int): EventDto {
        return api.getEventById(eventId)
    }
}
