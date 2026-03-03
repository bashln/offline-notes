package com.offlinenotes.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val TokyoBackground = Color(0xFF24283B)
val TokyoSurface = Color(0xFF292E42)
val TokyoSurfaceVariant = Color(0xFF1F2335)
val TokyoPrimary = Color(0xFF9ECE6A)
val TokyoOnPrimary = Color(0xFF1F2335)
val TokyoOnBackground = Color(0xFFC0CAF5)
val TokyoOnSurface = Color(0xFFC0CAF5)
val TokyoSecondaryText = Color(0xFFA9B1D6)
val TokyoMuted = Color(0xFF565F89)
val TokyoError = Color(0xFFF7768E)

private data class PaletteTokens(
    val primary: Color,
    val onPrimary: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val error: Color
)

private val tokyoNightDark = PaletteTokens(
    primary = TokyoPrimary,
    onPrimary = TokyoOnPrimary,
    background = TokyoBackground,
    onBackground = TokyoOnBackground,
    surface = TokyoSurface,
    onSurface = TokyoOnSurface,
    surfaceVariant = TokyoSurfaceVariant,
    onSurfaceVariant = TokyoSecondaryText,
    outline = TokyoMuted,
    error = TokyoError
)

private val tokyoNightLight = PaletteTokens(
    primary = Color(0xFF5C7C2F),
    onPrimary = Color(0xFFF7F9FF),
    background = Color(0xFFF4F6FB),
    onBackground = Color(0xFF23263A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF23263A),
    surfaceVariant = Color(0xFFE6EAF5),
    onSurfaceVariant = Color(0xFF4D5679),
    outline = Color(0xFF858EAE),
    error = Color(0xFFB42348)
)

private val catppuccinDark = PaletteTokens(
    primary = Color(0xFFA6E3A1),
    onPrimary = Color(0xFF1E1E2E),
    background = Color(0xFF1E1E2E),
    onBackground = Color(0xFFCDD6F4),
    surface = Color(0xFF313244),
    onSurface = Color(0xFFCDD6F4),
    surfaceVariant = Color(0xFF45475A),
    onSurfaceVariant = Color(0xFFBAC2DE),
    outline = Color(0xFF6C7086),
    error = Color(0xFFF38BA8)
)

private val catppuccinLight = PaletteTokens(
    primary = Color(0xFF40A02B),
    onPrimary = Color(0xFFFDFCF8),
    background = Color(0xFFEFF1F5),
    onBackground = Color(0xFF4C4F69),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF4C4F69),
    surfaceVariant = Color(0xFFDCE0E8),
    onSurfaceVariant = Color(0xFF5C5F77),
    outline = Color(0xFF8C8FA1),
    error = Color(0xFFD20F39)
)

private val rosePineDark = PaletteTokens(
    primary = Color(0xFF9CCFD8),
    onPrimary = Color(0xFF191724),
    background = Color(0xFF191724),
    onBackground = Color(0xFFE0DEF4),
    surface = Color(0xFF1F1D2E),
    onSurface = Color(0xFFE0DEF4),
    surfaceVariant = Color(0xFF26233A),
    onSurfaceVariant = Color(0xFF908CAA),
    outline = Color(0xFF6E6A86),
    error = Color(0xFFEB6F92)
)

private val rosePineLight = PaletteTokens(
    primary = Color(0xFF286983),
    onPrimary = Color(0xFFFAF4ED),
    background = Color(0xFFFAF4ED),
    onBackground = Color(0xFF575279),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF575279),
    surfaceVariant = Color(0xFFF2E9E1),
    onSurfaceVariant = Color(0xFF797593),
    outline = Color(0xFF9893A5),
    error = Color(0xFFB4637A)
)

fun offlineColorScheme(
    palette: ThemePalette,
    darkTheme: Boolean
): ColorScheme {
    val tokens = when (palette) {
        ThemePalette.TokyoNight -> if (darkTheme) tokyoNightDark else tokyoNightLight
        ThemePalette.Catppuccin -> if (darkTheme) catppuccinDark else catppuccinLight
        ThemePalette.RosePine -> if (darkTheme) rosePineDark else rosePineLight
    }

    return if (darkTheme) {
        darkColorScheme(
            primary = tokens.primary,
            onPrimary = tokens.onPrimary,
            background = tokens.background,
            onBackground = tokens.onBackground,
            surface = tokens.surface,
            onSurface = tokens.onSurface,
            surfaceVariant = tokens.surfaceVariant,
            onSurfaceVariant = tokens.onSurfaceVariant,
            outline = tokens.outline,
            error = tokens.error
        )
    } else {
        lightColorScheme(
            primary = tokens.primary,
            onPrimary = tokens.onPrimary,
            background = tokens.background,
            onBackground = tokens.onBackground,
            surface = tokens.surface,
            onSurface = tokens.onSurface,
            surfaceVariant = tokens.surfaceVariant,
            onSurfaceVariant = tokens.onSurfaceVariant,
            outline = tokens.outline,
            error = tokens.error
        )
    }
}
