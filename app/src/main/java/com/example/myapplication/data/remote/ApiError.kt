package ru.chsu.studentmap.data.remote

import java.io.IOException
import org.json.JSONObject
import retrofit2.HttpException

fun Throwable.toUserMessage(): String {
    return when (this) {
        is HttpException -> {
            val raw = response()?.errorBody()?.string().orEmpty()
            val detail = runCatching { JSONObject(raw).optString("detail") }.getOrNull()
            when {
                !detail.isNullOrBlank() -> detail
                code() == 401 -> "Нужно войти в аккаунт"
                code() == 404 -> "Метод на бэке ещё не реализован"
                else -> "Ошибка сервера (${code()})"
            }
        }
        is IOException -> "Нет связи с сервером. Проверьте, что бэкенд запущен (uvicorn :8000)."
        else -> localizedMessage ?: "Неизвестная ошибка"
    }
}
