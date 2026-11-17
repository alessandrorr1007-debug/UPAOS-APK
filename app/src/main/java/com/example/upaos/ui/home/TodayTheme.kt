package com.example.upaos.ui.home

import androidx.compose.ui.graphics.Color

internal data class TodayMetricStyle(
    val containerBg: Color,
    val iconTint: Color,
    val countColor: Color,
    val labelColor: Color
)

internal data class TodayStatusStyle(
    val dotColor: Color,
    val badgeBg: Color,
    val badgeText: Color
)

internal data class TodayPalette(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val innerSurface: Color,
    val border: Color,
    val innerBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accentBlue: Color,
    val softBlueBg: Color,
    val softBlueText: Color,
    val hoyBadgeBg: Color,
    val hoyBadgeText: Color,
    val weekGradientStart: Color,
    val weekGradientMid: Color,
    val weekGradientEnd: Color,
    val classesMetric: TodayMetricStyle,
    val pendingMetric: TodayMetricStyle,
    val newsMetric: TodayMetricStyle,
    val finishedStatus: TodayStatusStyle,
    val activeStatus: TodayStatusStyle,
    val upcomingStatus: TodayStatusStyle,
    val successGreen: Color,
    val timelineNeutralTrack: Color,
    val urgentText: Color,
    val urgentBg: Color
)

internal fun todayPalette(isDark: Boolean): TodayPalette = if (isDark) {
    // Modo oscuro: 75% negro/oscuro, 15% blanco, 10% azul
    TodayPalette(
        isDark = true,
        background = Color(0xFF060B14),
        surface = Color(0xFF0B1526),
        innerSurface = Color(0xFF0F1C31),
        border = Color(0xFF172740),
        innerBorder = Color(0xFF1E3150),
        textPrimary = Color(0xFFF8FAFC),
        textSecondary = Color(0xFF94A3B8),
        textMuted = Color(0xFF7B8CA6),
        accentBlue = Color(0xFF2573F7),
        softBlueBg = Color(0xFF132952),
        softBlueText = Color(0xFF60A5FA),
        hoyBadgeBg = Color(0xFF1466F5),
        hoyBadgeText = Color(0xFFFFFFFF),
        weekGradientStart = Color(0xFF1254DD),
        weekGradientMid = Color(0xFF0E7AE0),
        weekGradientEnd = Color(0xFF0AA1CB),
        classesMetric = TodayMetricStyle(
            containerBg = Color(0xFF0F254A),
            iconTint = Color(0xFF60A5FA),
            countColor = Color(0xFFFFFFFF),
            labelColor = Color(0xFF93C5FD)
        ),
        pendingMetric = TodayMetricStyle(
            containerBg = Color(0xFF3A240C),
            iconTint = Color(0xFFFBBF24),
            countColor = Color(0xFFFFFFFF),
            labelColor = Color(0xFFFCD34D)
        ),
        newsMetric = TodayMetricStyle(
            containerBg = Color(0xFF231B4B),
            iconTint = Color(0xFFA78BFA),
            countColor = Color(0xFFFFFFFF),
            labelColor = Color(0xFFC4B5FD)
        ),
        finishedStatus = TodayStatusStyle(
            dotColor = Color(0xFF22C55E),
            badgeBg = Color(0xFF0E3B25),
            badgeText = Color(0xFF4ADE80)
        ),
        activeStatus = TodayStatusStyle(
            dotColor = Color(0xFF3B82F6),
            badgeBg = Color(0xFF132D5E),
            badgeText = Color(0xFF60A5FA)
        ),
        upcomingStatus = TodayStatusStyle(
            dotColor = Color(0xFF60A5FA),
            badgeBg = Color(0xFF122952),
            badgeText = Color(0xFF7CB4FF)
        ),
        successGreen = Color(0xFF22C55E),
        timelineNeutralTrack = Color(0xFF243656),
        urgentText = Color(0xFFF87171),
        urgentBg = Color(0xFF3B151B)
    )
} else {
    // Modo claro: 75% blanco, 15% negro/slate, 10% azul
    TodayPalette(
        isDark = false,
        background = Color(0xFFF6F8FC),
        surface = Color(0xFFFFFFFF),
        innerSurface = Color(0xFFFAFBFD),
        border = Color(0xFFE5ECF6),
        innerBorder = Color(0xFFE2E8F0),
        textPrimary = Color(0xFF0F172A),
        textSecondary = Color(0xFF475569),
        textMuted = Color(0xFF64748B),
        accentBlue = Color(0xFF1466F5),
        softBlueBg = Color(0xFFE8F0FE),
        softBlueText = Color(0xFF1466F5),
        hoyBadgeBg = Color(0xFFDCEAFE),
        hoyBadgeText = Color(0xFF1466F5),
        weekGradientStart = Color(0xFF1254DD),
        weekGradientMid = Color(0xFF0E7AE0),
        weekGradientEnd = Color(0xFF0AA1CB),
        classesMetric = TodayMetricStyle(
            containerBg = Color(0xFFE7F0FE),
            iconTint = Color(0xFF1466F5),
            countColor = Color(0xFF0F172A),
            labelColor = Color(0xFF1D4ED8)
        ),
        pendingMetric = TodayMetricStyle(
            containerBg = Color(0xFFFEF1DF),
            iconTint = Color(0xFFD97706),
            countColor = Color(0xFF0F172A),
            labelColor = Color(0xFFB45309)
        ),
        newsMetric = TodayMetricStyle(
            containerBg = Color(0xFFECEAFE),
            iconTint = Color(0xFF6D4AFF),
            countColor = Color(0xFF0F172A),
            labelColor = Color(0xFF5B42D6)
        ),
        finishedStatus = TodayStatusStyle(
            dotColor = Color(0xFF22C55E),
            badgeBg = Color(0xFFDCFCE7),
            badgeText = Color(0xFF15803D)
        ),
        activeStatus = TodayStatusStyle(
            dotColor = Color(0xFF1466F5),
            badgeBg = Color(0xFFDBEAFE),
            badgeText = Color(0xFF1D4ED8)
        ),
        upcomingStatus = TodayStatusStyle(
            dotColor = Color(0xFF3B82F6),
            badgeBg = Color(0xFFE8F0FE),
            badgeText = Color(0xFF1466F5)
        ),
        successGreen = Color(0xFF22C55E),
        timelineNeutralTrack = Color(0xFFCBD5E1),
        urgentText = Color(0xFFDC2626),
        urgentBg = Color(0xFFFEE2E2)
    )
}
