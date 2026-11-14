package ru.chsu.studentmap.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import ru.chsu.studentmap.BuildConfig
import ru.chsu.studentmap.data.local.SessionStore
import ru.chsu.studentmap.data.remote.StudMapApi
import ru.chsu.studentmap.data.repository.AuthRepository
import ru.chsu.studentmap.data.repository.EventRepository
import ru.chsu.studentmap.data.repository.FavoriteRepository
import ru.chsu.studentmap.data.repository.PlaceRepository
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Инициализирует и связывает между собой:
 * Хранилище сессий
 * HTTP-клиент OkHttpClient с авторизацией JWT
 * Сетевой клиент для общения с бэкендом
 */
class AppContainer(context: Context) {

    // Хранилище токена и локального профиля пользователя
    val sessionStore = SessionStore(context)

    // Быстрый потокобезопасный кэш токена в памяти для перехватчика OkHttp
    private val tokenRef = AtomicReference<String?>(null)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        // Фоновая подписка на обновления сессии для поддержания tokenRef в актуальном состоянии
        scope.launch {
            sessionStore.session.collectLatest { session ->
                tokenRef.set(session.accessToken)
            }
        }
    }

    /**
     * Перехватчик (Interceptor), который автоматически добавляет заголовок
     * `Authorization: Bearer <token>` ко всем исходящим сетевым запросам, если пользователь авторизован
     */
    private val authInterceptor = Interceptor { chain ->
        // Проверяем токен в кэше памяти; если он ещё не успел прочитаться из DataStore, считываем синхронно
        var token = tokenRef.get()
        if (token.isNullOrBlank()) {
            token = runBlocking {
                try {
                    sessionStore.session.first().accessToken
                } catch (_: Exception) {
                    null
                }
            }
            if (!token.isNullOrBlank()) {
                tokenRef.set(token)
            }
        }

        val request = if (token.isNullOrBlank()) {
            // Если токена нет — отправляем запрос без заголовка авторизации (например, login или register)
            chain.request()
        } else {
            // Добавляем Bearer токен в заголовок Authorization
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }
        chain.proceed(request)
    }

    /**
     * Настройка HTTP-клиента:
     * authInterceptor: подставляет JWT-токен
     * HttpLoggingInterceptor: логирует отправляемые URL, заголовки и ответы в Logcat
     */
    private val okHttp: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .addInterceptor(authInterceptor)
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            }
        )
        .build()


    // Запросы к бекенду
    val api: StudMapApi = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(okHttp)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(StudMapApi::class.java)

    /**
     * Профил: регистрация, вход
     */
    val authRepository = AuthRepository(
        api = api,
        sessionStore = sessionStore,
        onTokenUpdated = { newToken -> tokenRef.set(newToken) }
    )

    val placeRepository = PlaceRepository(api)
    val favoriteRepository = FavoriteRepository(api)
    val eventRepository = EventRepository(api)
}
