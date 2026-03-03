package com.offlinenotes.ui.theme

enum class ThemePalette(
    val storageValue: String,
    val displayName: String
) {
    TokyoNight("tokyo_night", "Tokyo Night"),
    Catppuccin("catppuccin", "Catppuccin"),
    RosePine("rose_pine", "Rose Pine");

    companion object {
        fun fromStorageValue(value: String?): ThemePalette {
            return entries.firstOrNull { it.storageValue == value } ?: TokyoNight
        }
    }
}

enum class ThemeMode(
    val storageValue: String,
    val displayName: String
) {
    Light("light", "Light"),
    Dark("dark", "Dark"),
    System("system", "System");

    companion object {
        fun fromStorageValue(value: String?): ThemeMode {
            return entries.firstOrNull { it.storageValue == value } ?: Dark
        }
    }
}

data class ThemeSettings(
    val palette: ThemePalette = ThemePalette.TokyoNight,
    val mode: ThemeMode = ThemeMode.Dark
)
