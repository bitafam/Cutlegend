package com.example.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = DarkStonePrimary,
    primaryContainer = DarkStonePrimaryContainer,
    secondary = DarkStoneSecondary,
    background = DarkStoneBg,
    surface = DarkStoneSurface,
    surfaceVariant = DarkStoneSurfaceVariant,
    onPrimary = TextDark,
    onSecondary = TextDark,
    onBackground = DarkStoneTextPrimary,
    onSurface = DarkStoneTextPrimary,
    outline = DarkStoneOutline
)

private val LightColorScheme = lightColorScheme(
    primary = WarmPrimary,
    primaryContainer = WarmPrimaryContainer,
    secondary = WarmSecondary,
    background = WarmBg,
    surface = WarmSurface,
    surfaceVariant = WarmSurfaceVariant,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = WarmTextPrimary,
    onSurface = WarmTextPrimary,
    outline = WarmOutline
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set false to ensure our premium stone theme is strictly used
    content: @Composable () -> Unit,
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
