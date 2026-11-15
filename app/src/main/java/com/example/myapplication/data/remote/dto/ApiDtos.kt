package ru.chsu.studentmap.data.remote.dto

import com.google.gson.annotations.SerializedName

// Авторизация

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

// Места

data class OsmPlaceDto(
    val name: String,
    val address: String? = null,
    val latitude: Double,
    val longitude: Double,
    val category: String? = null,
    @SerializedName("osm_type") val osmType: String? = null,
    @SerializedName("osm_id") val osmId: Long? = null
)

// Избранные места

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

data class OrganizerDto(
    val id: Int,
    val name: String? = null,
    val surname: String? = null,
    val email: String? = null,
    @SerializedName("phone_number") val phoneNumber: String? = null
)

/**
 * Модель под backend/src/schemas/event_schema.py (EventResponse).
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
    val organizers: List<OrganizerDto>? = null,
    @SerializedName("is_favorite") val isFavorite: Boolean? = false,
    val imageUrl: String? = null
)
