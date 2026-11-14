package ru.chsu.studentmap.data.repository

import kotlinx.coroutines.flow.first
import ru.chsu.studentmap.data.local.SessionStore
import ru.chsu.studentmap.data.remote.StudMapApi
import ru.chsu.studentmap.data.remote.decodeJwtUserId
import ru.chsu.studentmap.data.remote.dto.UserResponseDto
import ru.chsu.studentmap.data.remote.dto.UserUpdateDto

/**
 * Репозиторий аутентификации и профиля пользователя.
 * Связывает сетевой слой Retrofit (StudMapApi) с локальным хранилищем сессий (SessionStore).
 * 
 * Основные задачи:
 * 1. Регистрация нового аккаунта на бэкенде (/auth/register) и последующий автоматический вход.
 * 2. Авторизация по email и паролю (/auth/login), декодирование ID из JWT и сохранение токена.
 * 3. Загрузка актуальных данных пользователя (имя, фамилия, email) с сервера.
 * 4. Завершение сессии (/auth/logout) с отзывом токена на сервере и локальной очисткой.
 * 5. Обновление личных данных профиля через PUT /users/update/{userId}.
 */
class AuthRepository(
    private val api: StudMapApi,
    private val sessionStore: SessionStore,
    private val onTokenUpdated: ((String?) -> Unit)? = null
) {

    /**
     * Регистрация нового пользователя.
     * 
     * Алгоритм:
     * 1. Отправляем POST /auth/register?name=...&surname=...&email=...&password=...
     *    Бэкенд проверяет уникальность email, хеширует пароль через pbkdf2_sha256 и сохраняет пользователя в БД.
     * 2. Так как бэкенд на /register возвращает только сообщение подтверждения, но не выдаёт токен,
     *    сразу после успешной регистрации автоматически вызываем метод [login], чтобы пользователь мгновенно
     *    стал авторизованным без необходимости повторного ввода пароля.
     */
    suspend fun register(
        name: String,
        surname: String,
        email: String,
        password: String
    ) {
        // Шаг 1: Регистрация на сервере
        api.register(
            name = name.trim(),
            surname = surname.trim(),
            email = email.trim(),
            password = password
        )

        // Шаг 2: Автоматический вход с сохранением переданных имени и фамилии
        login(
            email = email.trim(),
            password = password,
            name = name.trim(),
            surname = surname.trim()
        )
    }

    /**
     * Авторизация существующего пользователя по email и паролю.
     * 
     * Алгоритм:
     * 1. Отправляем запрос POST /auth/login?email=...&password=...
     *    Сервер проверяет совпадение хеша пароля и генерирует JWT токен доступа.
     * 2. Извлекаем userId из поля "sub" полученного JWT токена (см. decodeJwtUserId).
     * 3. Мгновенно обновляем токен в памяти (onTokenUpdated) для корректной работы перехватчика OkHttp.
     * 4. Сохраняем токен, userId и email в SessionStore на постоянное хранение.
     * 5. Если имя или фамилия не были переданы (прямой вход через форму логина),
     *    запрашиваем с бэкенда актуальный профиль через PUT /users/update/{userId} с пустым объектом,
     *    чтобы отобразить в профиле реальное имя и фамилию пользователя из базы данных.
     */
    suspend fun login(
        email: String,
        password: String,
        name: String? = null,
        surname: String? = null
    ) {
        val cleanEmail = email.trim()
        val tokenResponse = api.login(cleanEmail, password)
        val accessToken = tokenResponse.accessToken

        // Извлекаем идентификатор пользователя из JWT-токена
        val userId = decodeJwtUserId(accessToken)

        // Мгновенно кэшируем токен в памяти для перехватчика
        onTokenUpdated?.invoke(accessToken)

        // Сохраняем базовые данные сессии в DataStore
        sessionStore.saveLogin(
            token = accessToken,
            userId = userId,
            name = name,
            surname = surname,
            email = cleanEmail
        )

        // Если имя или фамилия неизвестны (пользователь вошел через форму входа),
        // загружаем их с бэкенда через запрос данных пользователя
        if ((name.isNullOrBlank() || surname.isNullOrBlank()) && userId != null) {
            runCatching {
                // Бэкенд возвращает UserResponseDto(id, name, surname, email)
                val profile = api.updateUser(userId, UserUpdateDto())
                sessionStore.updateProfile(
                    name = profile.name,
                    surname = profile.surname,
                    email = profile.email
                )
            }
        }
    }

    /**
     * Завершение сеанса работы (Logout).
     * 
     * Алгоритм:
     * 1. Считываем текущий токен доступа перед очисткой.
     * 2. Мгновенно сбрасываем токен в оперативной памяти и очищаем локальное хранилище DataStore.
     *    Благодаря этому интерфейс сразу же переключается на состояние неавторизованного пользователя
     *    без зависаний и задержек.
     * 3. В фоновом режиме отправляем запрос POST /auth/logout на бэкенд с явным заголовком Bearer токена,
     *    чтобы бэкенд успешно завершил сессию (HTTP 200). Возможные ошибки сети игнорируются (runCatching).
     */
    suspend fun logout() {
        val token = sessionStore.session.first().accessToken
        onTokenUpdated?.invoke(null)
        sessionStore.clear()
        if (!token.isNullOrBlank()) {
            runCatching { api.logout(authHeader = "Bearer $token") }
        }
    }

    /**
     * Обновление персональных данных профиля на бэкенде.
     * 
     * Отправляет PUT /users/update/{userId} с новыми данными.
     * Сервер обновляет запись в таблице Users и возвращает обновленный объект UserResponseDto.
     * После этого обновленные данные синхронизируются в локальном SessionStore.
     */
    suspend fun updateProfile(
        name: String? = null,
        surname: String? = null,
        email: String? = null,
        password: String? = null
    ): UserResponseDto {
        val currentSession = sessionStore.session.first()
        val userId = checkNotNull(currentSession.userId) { "Пользователь не авторизован" }

        val requestData = UserUpdateDto(
            name = name?.trim()?.ifBlank { null },
            surname = surname?.trim()?.ifBlank { null },
            email = email?.trim()?.ifBlank { null },
            password = password?.ifBlank { null }
        )

        // Выполняем сетевой запрос к бэкенду
        val updated = api.updateUser(userId, requestData)

        // Сохраняем обновленные данные в локальное хранилище
        sessionStore.updateProfile(
            name = updated.name,
            surname = updated.surname,
            email = updated.email
        )

        return updated
    }

    /**
     * Загружает актуальную информацию о пользователе из базы данных бэкенда
     * и обновляет локальную сессию.
     */
    suspend fun refreshProfile(): UserResponseDto? {
        val currentSession = sessionStore.session.first()
        val userId = currentSession.userId ?: return null
        return runCatching {
            val profile = api.updateUser(userId, UserUpdateDto())
            sessionStore.updateProfile(
                name = profile.name,
                surname = profile.surname,
                email = profile.email
            )
            profile
        }.getOrNull()
    }
}
