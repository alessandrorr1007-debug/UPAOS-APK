package com.example.upaos.ui.payments

import androidx.compose.ui.graphics.Color

internal data class PaymentsPalette(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accentBlue: Color,
    val softBlueBg: Color,
    val softBlueText: Color,
    val heroStart: Color,
    val heroEnd: Color,
    val heroBorder: Color,
    val heroLabel: Color,
    val heroValue: Color,
    val heroCaption: Color,
    val greenAccent: Color,
    val greenBg: Color,
    val greenBorder: Color,
    val warningBg: Color,
    val warningText: Color
)

internal fun paymentsPalette(isDark: Boolean): PaymentsPalette = if (isDark) {
    // Modo oscuro: 75% negro/oscuro, 15% blanco, 10% azul
    PaymentsPalette(
        isDark = true,
        background = Color(0xFF060B14),
        surface = Color(0xFF0B1526),
        surfaceElevated = Color(0xFF101D34),
        border = Color(0xFF172740),
        textPrimary = Color(0xFFF8FAFC),
        textSecondary = Color(0xFF94A3B8),
        textMuted = Color(0xFF7B8CA6),
        accentBlue = Color(0xFF2573F7),
        softBlueBg = Color(0xFF122952),
        softBlueText = Color(0xFF7CB4FF),
        heroStart = Color(0xFF0D2D6C),
        heroEnd = Color(0xFF081E4C),
        heroBorder = Color(0xFF1B438E),
        heroLabel = Color(0xFFBFDBFE),
        heroValue = Color(0xFFFFFFFF),
        heroCaption = Color(0xFF93C5FD),
        greenAccent = Color(0xFF4ADE80),
        greenBg = Color(0xFF0C2A1E),
        greenBorder = Color(0xFF184C35),
        warningBg = Color(0xFF33230F),
        warningText = Color(0xFFFBBF24)
    )
} else {
    // Modo claro: 75% blanco, 15% negro/slate, 10% azul
    PaymentsPalette(
        isDark = false,
        background = Color(0xFFF6F8FC),
        surface = Color(0xFFFFFFFF),
        surfaceElevated = Color(0xFFF1F5FB),
        border = Color(0xFFE5ECF6),
        textPrimary = Color(0xFF0F172A),
        textSecondary = Color(0xFF475569),
        textMuted = Color(0xFF64748B),
        accentBlue = Color(0xFF1466F5),
        softBlueBg = Color(0xFFE8F0FE),
        softBlueText = Color(0xFF1466F5),
        heroStart = Color(0xFFEAF1FE),
        heroEnd = Color(0xFFE1ECFE),
        heroBorder = Color(0xFFD0E1FD),
        heroLabel = Color(0xFF1E293B),
        heroValue = Color(0xFF0B49D4),
        heroCaption = Color(0xFF475569),
        greenAccent = Color(0xFF16A34A),
        greenBg = Color(0xFFEAF7EE),
        greenBorder = Color(0xFFCBEBD6),
        warningBg = Color(0xFFFEF3C7),
        warningText = Color(0xFFB45309)
    )
}

internal fun hasPositiveBalance(amount: String?): Boolean {
    if (amount.isNullOrBlank()) return false
    val numeric = amount.replace(Regex("[^0-9.,-]"), "")
        .replace(",", "")
        .toDoubleOrNull() ?: return false
    return numeric > 0.001
}
