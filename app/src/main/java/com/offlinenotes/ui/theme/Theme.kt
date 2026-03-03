package com.offlinenotes.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun OfflineNotesTheme(
    palette: ThemePalette = ThemePalette.TokyoNight,
    mode: ThemeMode = ThemeMode.System,
    content: @Composable () -> Unit
) {
    val darkTheme = when (mode) {
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
        ThemeMode.System -> isSystemInDarkTheme()
    }
    val colorScheme = offlineColorScheme(palette = palette, darkTheme = darkTheme)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = OfflineTypography,
        shapes = OfflineShapes,
        content = content
    )
}
