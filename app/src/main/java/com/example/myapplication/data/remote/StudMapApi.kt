package ru.chsu.studentmap.data.remote

import ru.chsu.studentmap.data.remote.dto.EventDto
import ru.chsu.studentmap.data.remote.dto.FavoriteEventLinkDto
import ru.chsu.studentmap.data.remote.dto.MessageResponseDto
import ru.chsu.studentmap.data.remote.dto.OsmPlaceDto
import ru.chsu.studentmap.data.remote.dto.PlaceCreateDto
import ru.chsu.studentmap.data.remote.dto.PlaceResponseDto
import ru.chsu.studentmap.data.remote.dto.TokenResponseDto
import ru.chsu.studentmap.data.remote.dto.UserResponseDto
import ru.chsu.studentmap.data.remote.dto.UserUpdateDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Контракт FastAPI из backend/src/app.py.
 * Префиксы: /auth, /users, /places, /favorites, /events.
 */
interface StudMapApi {

    // --- Auth: параметры как Query, потому что на бэке это обычные аргументы функции, не Body ---

    @POST("auth/register")
    suspend fun register(
        @Query("name") name: String,
        @Query("surname") surname: String,
        @Query("email") email: String,
        @Query("password") password: String
    ): MessageResponseDto

    @POST("auth/login")
    suspend fun login(
        @Query("email") email: String,
        @Query("password") password: String
    ): TokenResponseDto

    @POST("auth/logout")
    suspend fun logout(): MessageResponseDto

    @PUT("users/update/{userId}")
    suspend fun updateUser(
        @Path("userId") userId: Int,
        @Body data: UserUpdateDto
    ): UserResponseDto

    /**
     * Список объектов OSM вокруг точки.
     * Категории бэка: buildings, dormitories, sports, cafes, libraries, shops, pharmacies.
     */
    @GET("places/map/places")
    suspend fun getMapPlaces(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("radius") radius: Int = 4000,
        @Query("categories") categories: List<String>
    ): List<OsmPlaceDto>

    @POST("favorites/places")
    suspend fun addFavoritePlace(@Body place: PlaceCreateDto): PlaceResponseDto

    @GET("favorites/places")
    suspend fun getFavoritePlaces(): List<PlaceResponseDto>

    @DELETE("favorites/places/{placeId}")
    suspend fun removeFavoritePlace(@Path("placeId") placeId: Int): MessageResponseDto

    @POST("favorites/events/{userId}/{eventId}")
    suspend fun addFavoriteEvent(
        @Path("userId") userId: Int,
        @Path("eventId") eventId: Int
    ): FavoriteEventLinkDto

    @GET("favorites/events/{userId}")
    suspend fun getFavoriteEvents(@Path("userId") userId: Int): List<FavoriteEventLinkDto>

    @DELETE("favorites/events/{userId}/{eventId}")
    suspend fun removeFavoriteEvent(
        @Path("userId") userId: Int,
        @Path("eventId") eventId: Int
    ): MessageResponseDto

    /**
     * TODO: эндпоинт заложен в app.py (`/events`), но routers/event_router.py в репозитории нет.
     * Вызов оставлен, чтобы UI был готов; при 404/ошибке клиент покажет локальные моки.
     */
    @GET("events")
    suspend fun getEvents(): List<EventDto>
}
