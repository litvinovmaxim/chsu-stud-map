package ru.chsu.studentmap.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
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
 * Простой контейнер зависимостей без Hilt.
 */
class AppContainer(context: Context) {

    val sessionStore = SessionStore(context)

    private val tokenRef = AtomicReference<String?>(null)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch {
            sessionStore.session.collectLatest { session ->
                tokenRef.set(session.accessToken)
            }
        }
    }

    private val authInterceptor = Interceptor { chain ->
        val token = tokenRef.get()
        val request = if (token.isNullOrBlank()) {
            chain.request()
        } else {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }
        chain.proceed(request)
    }

    private val okHttp: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        // OSM через бэк может отвечать до 60с
        .readTimeout(90, TimeUnit.SECONDS)
        .addInterceptor(authInterceptor)
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BASIC
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            }
        )
        .build()

    val api: StudMapApi = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(okHttp)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(StudMapApi::class.java)

    val authRepository = AuthRepository(api, sessionStore)
    val placeRepository = PlaceRepository(api)
    val favoriteRepository = FavoriteRepository(api)
    val eventRepository = EventRepository(api)
}
