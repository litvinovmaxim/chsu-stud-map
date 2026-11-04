package ru.chsu.studentmap.data.remote

import android.util.Base64
import org.json.JSONObject

/** Достаём `sub` (id пользователя) из payload JWT. Подпись на клиенте не проверяем. */
fun decodeJwtUserId(token: String): Int? {
    return try {
        val parts = token.split(".")
        if (parts.size < 2) return null
        val payload = String(
            Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        )
        JSONObject(payload).optString("sub").toIntOrNull()
    } catch (_: Exception) {
        null
    }
}
