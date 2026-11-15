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
 * REST API контракт для взаимодействия с бэкендом StudMap (FastAPI).
 * Соответствует роутерам из backend/src/routers:
 * - auth_router.py (префикс /auth)
 * - user_router.py (префикс /users)
 * - favorite_place_router.py (префикс /favorites)
 * - place_router.py (префикс /places)
 */
interface StudMapApi {

    // ==========================================
    // Раздел: Аутентификация и регистрация (/auth)
    // ==========================================

    /**
     * Регистрация нового аккаунта.
     * Эндпоинт на бэкенде: @router.post("/register") в backend/src/routers/auth_router.py
     * Параметры name, surname, email, password объявлены в сигнатуре FastAPI как скалярные типы str,
     * поэтому FastAPI принимает их в Query параметрах URL: POST /auth/register?name=...&surname=...&email=...&password=...
     * В базе данных пароль хешируется через pbkdf2_sha256 (см. backend/src/services/auth_service.py).
     */
    @POST("auth/register")
    suspend fun register(
        @Query("name") name: String,
        @Query("surname") surname: String,
        @Query("email") email: String,
        @Query("password") password: String
    ): MessageResponseDto

    /**
     * Авторизация пользователя (вход).
     * Эндпоинт на бэкенде: @router.post("/login") в backend/src/routers/auth_router.py
     * Параметры передаются через Query (POST /auth/login?email=...&password=...).
     * При успешной аутентификации сервер возвращает JWT токен (access_token) со сроком жизни 30 минут.
     * В payload токена зашит sub: "<user_id>".
     */
    @POST("auth/login")
    suspend fun login(
        @Query("email") email: String,
        @Query("password") password: String
    ): TokenResponseDto

    /**
     * Завершение сессии (выход).
     * Эндпоинт на бэкенде: @router.post("/logout") в backend/src/routers/auth_router.py
     * Требует HTTP-заголовок Authorization: Bearer <access_token>.
     * Перехватчик authInterceptor в AppContainer автоматически подставляет этот заголовок.
     */
    @POST("auth/logout")
    suspend fun logout(
        @retrofit2.http.Header("Authorization") authHeader: String? = null
    ): MessageResponseDto

    // ==========================================
    // Раздел: Профиль пользователя (/users)
    // ==========================================

    /**
     * Обновление данных профиля пользователя.
     * Эндпоинт на бэкенде: @router.put("/update/{user_id}") в backend/src/routers/user_router.py
     * Требует заголовок Authorization: Bearer <access_token>.
     * В теле передаётся JSON (UserUpdateDto) с полями: name, surname, email, password.
     * Любое поле со значением null игнорируется бэкендом (exclude_unset=True, exclude_none=True).
     * 
     * Полезный приём: при отправке пустого UserUpdateDto() поля в БД не меняются,
     * но бэкенд возвращает полный актуальный объект UserResponseDto(id, name, surname, email).
     * Это позволяет загружать профиль пользователя после входа.
     */
    @PUT("users/update/{userId}")
    suspend fun updateUser(
        @Path("userId") userId: Int,
        @Body data: UserUpdateDto
    ): UserResponseDto

    // ==========================================
    // Раздел: Места и карта (/places, /favorites)
    // ==========================================

    /**
     * Список объектов OSM вокруг заданной гео-точки.
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

    // ==========================================
    // Раздел: Мероприятия (/events)
    // ==========================================

    @GET("events/")
    suspend fun getEvents(): List<EventDto>

    @GET("events/search")
    suspend fun searchEvents(
        @Query("search") search: String? = null,
        @Query("status") status: String? = null
    ): List<EventDto>

    @GET("events/{eventId}")
    suspend fun getEventById(@Path("eventId") eventId: Int): EventDto

    @GET("events/favorites/my")
    suspend fun getFavoriteEvents(): List<EventDto>

    @POST("events/{eventId}/favorite")
    suspend fun addFavoriteEvent(@Path("eventId") eventId: Int): Map<String, Boolean>

    @DELETE("events/{eventId}/favorite")
    suspend fun removeFavoriteEvent(@Path("eventId") eventId: Int): Map<String, Boolean>
}
