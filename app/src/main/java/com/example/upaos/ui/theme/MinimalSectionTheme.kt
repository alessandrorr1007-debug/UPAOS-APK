package com.example.upaos.ui.theme

import androidx.compose.ui.graphics.Color

internal data class MinimalSectionPalette(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceSoft: Color,
    val surfaceSoftBlue: Color,
    val border: Color,
    val borderBlue: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accentBlue: Color,
    val softBlueBg: Color,
    val softBlueText: Color,
    val pillBg: Color,
    val dangerBg: Color,
    val dangerBorder: Color,
    val dangerText: Color,
    val greenBg: Color,
    val greenText: Color
)

internal fun minimalSectionPalette(isDark: Boolean): MinimalSectionPalette = if (isDark) {
    // Modo oscuro: 75% negro, 15% blanco, 10% azul
    MinimalSectionPalette(
        isDark = true,
        background = Color(0xFF060B14),
        surface = Color(0xFF0D1724),
        surfaceSoft = Color(0xFF121E2E),
        surfaceSoftBlue = Color(0xFF0F203D),
        border = Color(0xFF1A2940),
        borderBlue = Color(0xFF1E3A68),
        textPrimary = Color(0xFFF8FAFC),
        textSecondary = Color(0xFF94A3B8),
        textMuted = Color(0xFF7B8CA6),
        accentBlue = Color(0xFF1A6FF6),
        softBlueBg = Color(0xFF132952),
        softBlueText = Color(0xFF60A5FA),
        pillBg = Color(0xFF1B293E),
        dangerBg = Color(0xFF2B181C),
        dangerBorder = Color(0xFF4B2329),
        dangerText = Color(0xFFF87171),
        greenBg = Color(0xFF0E3522),
        greenText = Color(0xFF4ADE80)
    )
} else {
    // Modo claro: 75% blanco, 15% negro, 10% azul
    MinimalSectionPalette(
        isDark = false,
        background = Color(0xFFF6F8FC),
        surface = Color(0xFFFFFFFF),
        surfaceSoft = Color(0xFFF1F5FB),
        surfaceSoftBlue = Color(0xFFEEF4FE),
        border = Color(0xFFE5ECF6),
        borderBlue = Color(0xFFD2E2FC),
        textPrimary = Color(0xFF0F172A),
        textSecondary = Color(0xFF475569),
        textMuted = Color(0xFF64748B),
        accentBlue = Color(0xFF1466F5),
        softBlueBg = Color(0xFFE5EEFD),
        softBlueText = Color(0xFF1466F5),
        pillBg = Color(0xFFE8EEF8),
        dangerBg = Color(0xFFFCE8E8),
        dangerBorder = Color(0xFFF5C6C6),
        dangerText = Color(0xFFC81E1E),
        greenBg = Color(0xFFDCFCE7),
        greenText = Color(0xFF16A34A)
    )
}
