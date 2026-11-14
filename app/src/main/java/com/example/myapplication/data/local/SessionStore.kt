package ru.chsu.studentmap.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Делегат DataStore Preferences для хранения данных сессии пользователя на устройстве.
 * Файл настроек "studmap_session.preferences_pb" хранится в защищённом каталоге приложения.
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "studmap_session")

/**
 * Хранилище локальной сессии пользователя.
 * Отвечает за:
 * 1. Сохранение JWT токена авторизации (access_token)
 * 2. Сохранение идентификатора пользователя (user_id)
 * 3. Сохранение персональных данных (имя, фамилия, email)
 * 4. Очистку сессии при выходе из аккаунта (logout)
 */
class SessionStore(context: Context) {

    private val dataStore = context.applicationContext.dataStore

    /**
     * Реактивный поток (Flow) текущей сессии.
     * При любом изменении данных в DataStore все подписчики (UI, репозитории) автоматически получают актуальное состояние.
     */
    val session: Flow<UserSession> = dataStore.data.map { prefs ->
        UserSession(
            accessToken = prefs[KEY_TOKEN],
            userId = prefs[KEY_USER_ID],
            name = prefs[KEY_NAME],
            surname = prefs[KEY_SURNAME],
            email = prefs[KEY_EMAIL]
        )
    }

    /**
     * Сохраняет данные после успешной авторизации или регистрации.
     *
     * @param token JWT токен доступа, полученный от бэкенда при /auth/login
     * @param userId ID пользователя, извлечённый из JWT (claim "sub")
     * @param name Имя пользователя
     * @param surname Фамилия пользователя
     * @param email Адрес электронной почты
     */
    suspend fun saveLogin(
        token: String,
        userId: Int?,
        name: String?,
        surname: String?,
        email: String?
    ) {
        dataStore.edit { prefs ->
            prefs[KEY_TOKEN] = token
            if (userId != null) prefs[KEY_USER_ID] = userId else prefs.remove(KEY_USER_ID)
            if (!name.isNullOrBlank()) prefs[KEY_NAME] = name else prefs.remove(KEY_NAME)
            if (!surname.isNullOrBlank()) prefs[KEY_SURNAME] = surname else prefs.remove(KEY_SURNAME)
            if (!email.isNullOrBlank()) prefs[KEY_EMAIL] = email else prefs.remove(KEY_EMAIL)
        }
    }

    /**
     * Обновляет профильные данные без изменения токена.
     * Используется после вызова PUT /users/update/{userId}.
     */
    suspend fun updateProfile(
        name: String?,
        surname: String?,
        email: String?
    ) {
        dataStore.edit { prefs ->
            if (!name.isNullOrBlank()) prefs[KEY_NAME] = name
            if (!surname.isNullOrBlank()) prefs[KEY_SURNAME] = surname
            if (!email.isNullOrBlank()) prefs[KEY_EMAIL] = email
        }
    }

    /**
     * Очищает всю информацию о сессии (при выходе из системы).
     */
    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    companion object {
        private val KEY_TOKEN = stringPreferencesKey("access_token")
        private val KEY_USER_ID = intPreferencesKey("user_id")
        private val KEY_NAME = stringPreferencesKey("name")
        private val KEY_SURNAME = stringPreferencesKey("surname")
        private val KEY_EMAIL = stringPreferencesKey("email")
    }
}

/**
 * Модель данных активной сессии пользователя в приложении.
 *
 * @property accessToken JWT токен для заголовка Authorization: Bearer <token>
 * @property userId Целочисленный ID пользователя в базе данных бэкенда
 * @property name Имя студента
 * @property surname Фамилия студента
 * @property email Электронная почта
 */
data class UserSession(
    val accessToken: String?,
    val userId: Int?,
    val name: String?,
    val surname: String?,
    val email: String?
) {
    /** Пользователь считается авторизованным, если у него сохранён валидный токен */
    val isLoggedIn: Boolean get() = !accessToken.isNullOrBlank()

    /** Отображаемое имя пользователя (Имя Фамилия, либо email, либо дефолтная заглушка) */
    val displayName: String
        get() {
            val full = listOfNotNull(name, surname).filter { it.isNotBlank() }.joinToString(" ").trim()
            return full.ifBlank { email ?: "Студент" }
        }

    /** Инициалы пользователя для отображения на круглой аватарке (например, "ИИ") */
    val initials: String
        get() {
            val first = name?.trim()?.firstOrNull()?.uppercaseChar()
            val second = surname?.trim()?.firstOrNull()?.uppercaseChar()
            return when {
                first != null && second != null -> "$first$second"
                first != null -> "$first"
                !email.isNullOrBlank() -> email.take(1).uppercase()
                else -> "?"
            }
        }
}
