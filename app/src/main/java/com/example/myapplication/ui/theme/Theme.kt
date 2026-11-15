package com.example.myapplication.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Темная тема в фирменных оттенках
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFF6B6B),
    onPrimary = Color(0xFF4D0005),
    primaryContainer = Color(0xFF93000D),
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = Color(0xFFFF897D),
    onSecondary = Color(0xFF5F1511),
    secondaryContainer = Color(0xFF531113),
    onSecondaryContainer = Color(0xFFFF897D),
    background = Color(0xFF141212),
    onBackground = Color(0xFFEDE0DE),
    surface = Color(0xFF1E1A1A),
    onSurface = Color(0xFFEDE0DE),
    surfaceVariant = Color(0xFF2E2727),
    onSurfaceVariant = Color(0xFFD8C2BF),
    surfaceContainer = Color(0xFF1E1A1A),
    surfaceContainerLow = Color(0xFF141212),
    surfaceContainerHigh = Color(0xFF2E2727),
    outline = Color(0xFFA08C8A)
)

// Светлая тема: наш фирменный красный ЧГУ и белый цвет
private val LightColorScheme = lightColorScheme(
    primary = ChsuRed,
    onPrimary = ChsuWhite,
    primaryContainer = ChsuRedLight,
    onPrimaryContainer = ChsuRedText,
    secondary = ChsuRedDark,
    onSecondary = ChsuWhite,
    secondaryContainer = ChsuRedLight,
    onSecondaryContainer = ChsuRed,
    background = ChsuWhite,
    onBackground = Color(0xFF1C1B1F),
    surface = ChsuWhite,
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = ChsuSurfaceVariant,
    onSurfaceVariant = ChsuGrayText,
    surfaceContainer = ChsuWhite,
    surfaceContainerLow = ChsuBackground,
    surfaceContainerHigh = ChsuSurfaceVariant,
    outline = ChsuOutline
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // dynamicColor по умолчанию выключаем, чтобы не перебивать фирменные цвета ЧГУ системной темой Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
