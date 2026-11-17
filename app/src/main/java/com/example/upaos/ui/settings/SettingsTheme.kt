package com.example.upaos.ui.settings

import androidx.compose.ui.graphics.Color

internal data class SettingsPalette(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceSoftBlue: Color,
    val border: Color,
    val borderBlue: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accentBlue: Color,
    val softBlueBg: Color,
    val softBlueText: Color,
    val selectedAccountBg: Color,
    val selectedAccountText: Color,
    val dangerBg: Color,
    val dangerText: Color,
    val amberBg: Color,
    val amberText: Color,
    val greenBg: Color,
    val greenText: Color
)

internal fun settingsPalette(isDark: Boolean): SettingsPalette = if (isDark) {
    // Modo oscuro: 75% negro/oscuro, 15% blanco, 10% azul
    SettingsPalette(
        isDark = true,
        background = Color(0xFF060B14),
        surface = Color(0xFF0B1526),
        surfaceSoftBlue = Color(0xFF0F203D),
        border = Color(0xFF172740),
        borderBlue = Color(0xFF1D3866),
        textPrimary = Color(0xFFF8FAFC),
        textSecondary = Color(0xFF94A3B8),
        textMuted = Color(0xFF7B8CA6),
        accentBlue = Color(0xFF2573F7),
        softBlueBg = Color(0xFF132952),
        softBlueText = Color(0xFF60A5FA),
        selectedAccountBg = Color(0xFF17366B),
        selectedAccountText = Color(0xFFF8FAFC),
        dangerBg = Color(0xFF3A1419),
        dangerText = Color(0xFFF87171),
        amberBg = Color(0xFF3A240C),
        amberText = Color(0xFFFBBF24),
        greenBg = Color(0xFF0E3522),
        greenText = Color(0xFF4ADE80)
    )
} else {
    // Modo claro: 75% blanco, 15% negro/slate, 10% azul
    SettingsPalette(
        isDark = false,
        background = Color(0xFFF6F8FC),
        surface = Color(0xFFFFFFFF),
        surfaceSoftBlue = Color(0xFFEEF4FE),
        border = Color(0xFFE5ECF6),
        borderBlue = Color(0xFFD2E2FC),
        textPrimary = Color(0xFF0F172A),
        textSecondary = Color(0xFF475569),
        textMuted = Color(0xFF64748B),
        accentBlue = Color(0xFF1466F5),
        softBlueBg = Color(0xFFE5EEFD),
        softBlueText = Color(0xFF1466F5),
        selectedAccountBg = Color(0xFFBED6FE),
        selectedAccountText = Color(0xFF0F172A),
        dangerBg = Color(0xFFFCE6E6),
        dangerText = Color(0xFFC81E1E),
        amberBg = Color(0xFFFEF3D6),
        amberText = Color(0xFFD97706),
        greenBg = Color(0xFFDCFCE7),
        greenText = Color(0xFF16A34A)
    )
}
