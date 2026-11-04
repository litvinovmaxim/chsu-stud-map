package ru.chsu.studentmap.data.remote.dto

import com.google.gson.annotations.SerializedName

// ----- Auth (backend/src/routers/auth_router.py) -----

data class TokenResponseDto(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String? = null
)

data class MessageResponseDto(
    @SerializedName("status_code") val statusCode: Int? = null,
    @SerializedName("message") val message: String? = null
)

data class UserResponseDto(
    val id: Int,
    val name: String,
    val surname: String,
    val email: String
)

data class UserUpdateDto(
    val name: String? = null,
    val surname: String? = null,
    val email: String? = null,
    val password: String? = null
)

// ----- Places / OSM (GET /places/map/places) -----

data class OsmPlaceDto(
    val name: String,
    val address: String? = null,
    val latitude: Double,
    val longitude: Double,
    val category: String? = null,
    @SerializedName("osm_type") val osmType: String? = null,
    @SerializedName("osm_id") val osmId: Long? = null
)

// ----- Favorite places (GET/POST /favorites/places) -----

data class PlaceCreateDto(
    val name: String,
    val address: String? = null,
    val latitude: Double,
    val longitude: Double,
    val category: String? = null,
    @SerializedName("osm_type") val osmType: String? = null,
    @SerializedName("osm_id") val osmId: Long? = null
)

data class PlaceResponseDto(
    val id: Int,
    val name: String,
    val address: String? = null,
    val latitude: Double,
    val longitude: Double,
    val category: String? = null,
    @SerializedName("osm_type") val osmType: String? = null,
    @SerializedName("osm_id") val osmId: Long? = null
)

/**
 * GET /favorites/events/{user_id} сейчас отдаёт строки users_events (id_user + id_event).
 * TODO: когда event_router начнёт отдавать сами мероприятия — заменить на EventDto.
 */
data class FavoriteEventLinkDto(
    @SerializedName("id_user") val userId: Int? = null,
    @SerializedName("id_event") val eventId: Int? = null
)

/**
 * Модель под backend/src/models/event_model.py.
 * TODO: подключить, когда появится рабочий GET /events (файла event_router.py в backend нет — бэк не трогаем).
 */
data class EventDto(
    val id: Int,
    val title: String,
    val type: String? = null,
    val level: String? = null,
    @SerializedName("event_date") val eventDate: String? = null,
    val status: String? = null,
    val location: String? = null,
    val description: String? = null,
    val source: String? = null,
    // Локальное поле для вёрстки; бэк картинок пока не отдаёт
    val imageUrl: String? = null
)
