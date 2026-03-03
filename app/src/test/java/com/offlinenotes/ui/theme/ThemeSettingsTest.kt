package com.offlinenotes.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ThemeSettingsTest {

    @Test
    fun `palette defaults to tokyo night`() {
        assertEquals(ThemePalette.TokyoNight, ThemePalette.fromStorageValue(null))
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
