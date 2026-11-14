package ru.chsu.studentmap.data.remote

import java.io.IOException
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.HttpException

/**
 * Преобразует исключения Retrofit и сетевые ошибки в понятные пользователю сообщения на русском языке.
 * 
 * Особенности обработки бэкенда FastAPI:
 * 1. При бизнес-ошибках (400, 401, 404) FastAPI возвращает JSON вида: {"detail": "Сообщение"}
 * 2. При ошибках валидации (422 Unprocessable Entity) FastAPI возвращает массив:
 *    {"detail": [{"loc": ["query", "email"], "msg": "value is not a valid email address", "type": "..."}]}
 * 3. При недоступности сервера (нет связи, тайм-аут) OkHttp выбрасывает IOException.
 */
fun Throwable.toUserMessage(): String {
    return when (this) {
        is HttpException -> {
            // Читаем сырое тело ошибки от сервера
            val rawBody = response()?.errorBody()?.string().orEmpty()
            
            // Пытаемся извлечь поле 'detail' из JSON-ответа FastAPI
            val detail = runCatching {
                val json = JSONObject(rawBody)
                val detailField = json.opt("detail")
                when (detailField) {
                    // Если detail — обычная строка (например, {"detail": "Email already exists"})
                    is String -> detailField
                    // Если detail — массив ошибок валидации FastAPI (код 422)
                    is JSONArray -> {
                        val messages = mutableListOf<String>()
                        for (i in 0 until detailField.length()) {
                            val errObj = detailField.optJSONObject(i)
                            val msg = errObj?.optString("msg")
                            if (!msg.isNullOrBlank()) {
                                messages.add(msg)
                            }
                        }
                        if (messages.isNotEmpty()) messages.joinToString("; ") else null
                    }
                    else -> null
                }
            }.getOrNull()

            // Переводим стандартные ответы бэкенда на русский язык
            when {
                !detail.isNullOrBlank() -> {
                    when {
                        detail.contains("Email already exists", ignoreCase = true) ->
                            "Пользователь с такой почтой уже зарегистрирован"
                        detail.contains("Invalid email or password", ignoreCase = true) ->
                            "Неверная почта или пароль"
                        detail.contains("Signature expired", ignoreCase = true) ->
                            "Срок действия сессии истёк. Пожалуйста, выполните вход заново"
                        detail.contains("Could not validate credentials", ignoreCase = true) ||
                        detail.contains("Invalid token", ignoreCase = true) ->
                            "Ошибка авторизации. Пожалуйста, войдите в аккаунт заново"
                        detail.contains("User not found", ignoreCase = true) ->
                            "Пользователь не найден"
                        else -> detail
                    }
                }
                code() == 400 -> "Некорректный запрос"
                code() == 401 -> "Требуется авторизация (неверный токен или пароль)"
                code() == 403 -> "Доступ запрещён"
                code() == 404 -> "Запрашиваемый ресурс не найден на сервере"
                code() == 422 -> "Некорректный формат переданных данных"
                code() in 500..599 -> "Внутренняя ошибка сервера (${code()})"
                else -> "Ошибка сервера (${code()})"
            }
        }
        // Ошибка подключения (нет интернета, сервер недоступен)
        is IOException -> "Не удалось подключиться к серверу. Проверьте подключение к сети и повторите попытку"
        else -> localizedMessage ?: "Неизвестная ошибка"
    }
}
