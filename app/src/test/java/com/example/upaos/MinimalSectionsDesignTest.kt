package com.example.upaos

import androidx.compose.ui.graphics.Color
import com.example.upaos.ui.theme.minimalSectionPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MinimalSectionsDesignTest {

    @Test
    fun minimalSectionPalettesFollow751510RuleInLightAndDarkModes() {
        val light = minimalSectionPalette(isDark = false)
        val dark = minimalSectionPalette(isDark = true)

        assertFalse(light.isDark)
        assertTrue(dark.isDark)

        // Modo claro: 75% blanco, 15% negro/slate, 10% azul
        assertEquals(Color(0xFFF6F8FC), light.background)
        assertEquals(Color(0xFFFFFFFF), light.surface)
        assertEquals(Color(0xFF0F172A), light.textPrimary)
        assertEquals(Color(0xFF1466F5), light.accentBlue)

        // Modo oscuro: 75% negro, 15% blanco, 10% azul
        assertEquals(Color(0xFF060B14), dark.background)
        assertEquals(Color(0xFF0D1724), dark.surface)
        assertEquals(Color(0xFFF8FAFC), dark.textPrimary)
        assertEquals(Color(0xFF1A6FF6), dark.accentBlue)

        assertNotEquals(light.dangerBg, dark.dangerBg)
        assertNotEquals(light.softBlueBg, dark.softBlueBg)
        assertNotEquals(light.pillBg, dark.pillBg)
    }
}
