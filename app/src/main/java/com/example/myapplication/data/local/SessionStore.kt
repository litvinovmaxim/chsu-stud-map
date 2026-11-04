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

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "studmap_session")

/**
 * Локальная сессия: JWT и поля профиля.
 * TODO: GET /users/me — подтягивать имя после логина оттуда.
 */
class SessionStore(context: Context) {

    private val dataStore = context.applicationContext.dataStore

    val session: Flow<UserSession> = dataStore.data.map { prefs ->
        UserSession(
            accessToken = prefs[KEY_TOKEN],
            userId = prefs[KEY_USER_ID],
            name = prefs[KEY_NAME],
            surname = prefs[KEY_SURNAME],
            email = prefs[KEY_EMAIL]
        )
    }

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

data class UserSession(
    val accessToken: String?,
    val userId: Int?,
    val name: String?,
    val surname: String?,
    val email: String?
) {
    val isLoggedIn: Boolean get() = !accessToken.isNullOrBlank()

    val displayName: String
        get() {
            val full = listOfNotNull(name, surname).joinToString(" ").trim()
            return full.ifBlank { email ?: "Студент" }
        }
}
