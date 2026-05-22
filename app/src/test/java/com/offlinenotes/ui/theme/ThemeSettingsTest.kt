package com.offlinenotes.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ThemeSettingsTest {

    @Test
    fun `palette defaults to charcoal`() {
        assertEquals(ThemePalette.Charcoal, ThemePalette.fromStorageValue(null))
    }

    @Test
    fun `existing palette storage values still resolve`() {
        assertEquals(ThemePalette.TokyoNight, ThemePalette.fromStorageValue("tokyo_night"))
        assertEquals(ThemePalette.Catppuccin, ThemePalette.fromStorageValue("catppuccin"))
        assertEquals(ThemePalette.RosePine, ThemePalette.fromStorageValue("rose_pine"))
        assertEquals(ThemePalette.Obsidianite, ThemePalette.fromStorageValue("obsidianite"))
        assertEquals(ThemePalette.Charcoal, ThemePalette.fromStorageValue("charcoal"))
    }

    @Test
    fun `charcoal dark scheme has warm charcoal background`() {
        val scheme = offlineColorScheme(ThemePalette.Charcoal, darkTheme = true)
        assertEquals(CharcoalBackground, scheme.background)
        assertEquals(CharcoalPrimary, scheme.primary)
    }

    @Test
    fun `mode parses stored values`() {
        assertEquals(ThemeMode.System, ThemeMode.fromStorageValue("system"))
        assertEquals(ThemeMode.Dark, ThemeMode.fromStorageValue("invalid"))
    }

    @Test
    fun `tokyo night dark scheme keeps required background anchor`() {
        val scheme = offlineColorScheme(ThemePalette.TokyoNight, darkTheme = true)

        assertEquals(TokyoBackground, scheme.background)
        assertEquals(TokyoPrimary, scheme.primary)
    }

    @Test
    fun `light and dark theme schemes differ`() {
        val darkScheme = offlineColorScheme(ThemePalette.Catppuccin, darkTheme = true)
        val lightScheme = offlineColorScheme(ThemePalette.Catppuccin, darkTheme = false)

        assertNotEquals(darkScheme.background, lightScheme.background)
        assertNotEquals(darkScheme.onBackground, lightScheme.onBackground)
    }
}
