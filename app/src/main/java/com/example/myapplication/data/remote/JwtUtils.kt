package ru.chsu.studentmap.data.remote

import android.util.Base64
import org.json.JSONObject

// Достаём `sub` (id пользователя) из payload JWT. Подпись на клиенте не проверяем
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

// Проверяем, истёк ли срок действия JWT токена (по полю exp).
fun isJwtExpired(token: String?): Boolean {
    if (token.isNullOrBlank()) return true
    return try {
        val parts = token.split(".")
        if (parts.size < 2) return true
        val payload = String(
            Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        )
        val exp = JSONObject(payload).optLong("exp", 0L)
        if (exp == 0L) return false
        val nowSeconds = System.currentTimeMillis() / 1000
        nowSeconds >= (exp - 5)
    } catch (_: Exception) {
        true
    }
}
