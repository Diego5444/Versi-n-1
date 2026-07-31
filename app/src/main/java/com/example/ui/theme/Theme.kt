package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CinemaColorScheme = darkColorScheme(
    primary = CinemaRed,
    onPrimary = OnDarkPrimary,
    secondary = CinemaGold,
    onSecondary = DarkObsidian,
    tertiary = AccentCyan,
    background = DarkObsidian,
    onBackground = OnDarkPrimary,
    surface = DarkSurface,
    onSurface = OnDarkPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = OnDarkSecondary
)

@Composable
fun CineSyncTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CinemaColorScheme,
        typography = Typography,
        content = content
    )
}
