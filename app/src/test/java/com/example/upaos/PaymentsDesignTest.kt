package com.example.upaos

import androidx.compose.ui.graphics.Color
import com.example.upaos.ui.payments.hasPositiveBalance
import com.example.upaos.ui.payments.paymentsPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentsDesignTest {

    @Test
    fun lightAndDarkPalettesFollow751510ColorSplit() {
        val light = paymentsPalette(isDark = false)
        val dark = paymentsPalette(isDark = true)

        assertFalse(light.isDark)
        assertTrue(dark.isDark)

        // Modo claro: 75% blanco, 15% negro/slate, 10% azul
        assertEquals(Color(0xFFF6F8FC), light.background)
        assertEquals(Color(0xFFFFFFFF), light.surface)
        assertEquals(Color(0xFF0F172A), light.textPrimary)
        assertEquals(Color(0xFF1466F5), light.accentBlue)
        assertEquals(Color(0xFF0B49D4), light.heroValue)

        // Modo oscuro: 75% negro/oscuro, 15% blanco, 10% azul
        assertEquals(Color(0xFF060B14), dark.background)
        assertEquals(Color(0xFF0B1526), dark.surface)
        assertEquals(Color(0xFFF8FAFC), dark.textPrimary)
        assertEquals(Color(0xFF2573F7), dark.accentBlue)
        assertEquals(Color(0xFFFFFFFF), dark.heroValue)

        assertNotEquals(light.heroStart, dark.heroStart)
        assertNotEquals(light.greenBg, dark.greenBg)
    }

    @Test
    fun hasPositiveBalanceDetectsActiveAmountsSafely() {
        assertTrue(hasPositiveBalance("PEN 1,240.00"))
        assertTrue(hasPositiveBalance("PEN 0.50"))
        assertFalse(hasPositiveBalance("PEN 0.00"))
        assertFalse(hasPositiveBalance("0.00"))
        assertFalse(hasPositiveBalance("-10.00"))
        assertFalse(hasPositiveBalance(null))
        assertFalse(hasPositiveBalance(""))
        assertFalse(hasPositiveBalance("No disponible"))
    }
}
