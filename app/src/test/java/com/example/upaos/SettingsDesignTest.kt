package com.example.upaos

import androidx.compose.ui.graphics.Color
import com.example.upaos.ui.settings.settingsPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsDesignTest {

    @Test
    fun lightAndDarkPalettesFollow751510ColorSplit() {
        val light = settingsPalette(isDark = false)
        val dark = settingsPalette(isDark = true)

        assertFalse(light.isDark)
        assertTrue(dark.isDark)

        // Modo claro: 75% blanco, 15% negro/slate, 10% azul
        assertEquals(Color(0xFFF6F8FC), light.background)
        assertEquals(Color(0xFFFFFFFF), light.surface)
        assertEquals(Color(0xFF0F172A), light.textPrimary)
        assertEquals(Color(0xFF1466F5), light.accentBlue)
        assertEquals(Color(0xFFBED6FE), light.selectedAccountBg)

        // Modo oscuro: 75% negro/oscuro, 15% blanco, 10% azul
        assertEquals(Color(0xFF060B14), dark.background)
        assertEquals(Color(0xFF0B1526), dark.surface)
        assertEquals(Color(0xFFF8FAFC), dark.textPrimary)
        assertEquals(Color(0xFF2573F7), dark.accentBlue)
        assertEquals(Color(0xFF17366B), dark.selectedAccountBg)

        assertNotEquals(light.surfaceSoftBlue, dark.surfaceSoftBlue)
        assertNotEquals(light.dangerBg, dark.dangerBg)
    }
}
