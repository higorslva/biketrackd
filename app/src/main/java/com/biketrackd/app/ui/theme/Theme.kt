package com.biketrackd.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.biketrackd.app.data.ThemePreferences

private val DarkColorScheme = darkColorScheme(
    primary = Green500,
    onPrimary = DarkBackground,
    primaryContainer = Green700,
    onPrimaryContainer = Green200,
    secondary = Green400,
    onSecondary = DarkBackground,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = TextTertiary,
)

private val LightColorScheme = lightColorScheme(
    primary = Green500,
    onPrimary = LightSurface,
    primaryContainer = Green200,
    onPrimaryContainer = Green700,
    secondary = Green400,
    onSecondary = LightSurface,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnBackground,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOnSurfaceVariant,
)

@Composable
fun GpsOssTheme(
    themeMode: ThemePreferences.ThemeMode = ThemePreferences.ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val useDark = when (themeMode) {
        ThemePreferences.ThemeMode.DARK -> true
        ThemePreferences.ThemeMode.LIGHT -> false
        ThemePreferences.ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = if (useDark) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
