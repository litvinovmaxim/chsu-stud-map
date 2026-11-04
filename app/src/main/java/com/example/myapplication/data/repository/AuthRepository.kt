package ru.chsu.studentmap.data.repository

import kotlinx.coroutines.flow.first
import ru.chsu.studentmap.data.local.SessionStore
import ru.chsu.studentmap.data.remote.StudMapApi
import ru.chsu.studentmap.data.remote.decodeJwtUserId
import ru.chsu.studentmap.data.remote.dto.UserUpdateDto

class AuthRepository(
    private val api: StudMapApi,
    private val sessionStore: SessionStore
) {
    suspend fun register(name: String, surname: String, email: String, password: String) {
        api.register(name, surname, email, password)
        // После регистрации сразу логинимся — бэк не возвращает токен на /register
        login(email, password, name, surname)
    }

    suspend fun login(
        email: String,
        password: String,
        name: String? = null,
        surname: String? = null
    ) {
        val token = api.login(email, password)
        val userId = decodeJwtUserId(token.accessToken)
        sessionStore.saveLogin(
            token = token.accessToken,
            userId = userId,
            name = name,
            surname = surname,
            email = email
        )
    }

    suspend fun logout() {
        runCatching { api.logout() }
        sessionStore.clear()
    }

    suspend fun updateProfile(userId: Int, data: UserUpdateDto) {
        val updated = api.updateUser(userId, data)
        val token = sessionStore.session.first().accessToken.orEmpty()
        sessionStore.saveLogin(
            token = token,
            userId = updated.id,
            name = updated.name,
            surname = updated.surname,
            email = updated.email
        )
    }
}
