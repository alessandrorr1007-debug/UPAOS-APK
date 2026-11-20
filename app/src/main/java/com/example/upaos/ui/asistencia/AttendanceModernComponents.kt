package com.example.upaos.ui.asistencia

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.upaos.data.upao.AcademicCalendar
import com.example.upaos.ui.components.CourseIconBadgeDark
import com.example.upaos.ui.components.CourseIconBadgeLight
import com.example.upaos.ui.components.resolveCourseSubjectIcon

data class AttendanceThemePalette(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accentBlue: Color,
    val weekPillBg: Color,
    val weekPillBorder: Color,
    val weekPillText: Color,
    val trackBg: Color
)

fun attendanceThemePalette(isDark: Boolean): AttendanceThemePalette = if (isDark) {
    // Modo oscuro: 75% negro/oscuro, 15% blanco, 10% azul
    AttendanceThemePalette(
        isDark = true,
        background = Color(0xFF060B14),
        surface = Color(0xFF0B1526),
        border = Color(0xFF172740),
        textPrimary = Color(0xFFF8FAFC),
        textSecondary = Color(0xFF94A3B8),
        textMuted = Color(0xFF7B8CA6),
        accentBlue = Color(0xFF3B82F6),
        weekPillBg = Color(0xFF102851),
        weekPillBorder = Color(0xFF1D3B6E),
        weekPillText = Color(0xFF60A5FA),
        trackBg = Color(0xFF1E293B)
    )
} else {
    // Modo claro: 75% blanco, 15% negro/slate, 10% azul
    AttendanceThemePalette(
        isDark = false,
        background = Color(0xFFF6F8FC),
        surface = Color(0xFFFFFFFF),
        border = Color(0xFFE5ECF6),
        textPrimary = Color(0xFF0F172A),
        textSecondary = Color(0xFF475569),
        textMuted = Color(0xFF64748B),
        accentBlue = Color(0xFF1A6DFF),
        weekPillBg = Color(0xFFEAF2FF),
        weekPillBorder = Color(0xFFD6E4FF),
        weekPillText = Color(0xFF1A6DFF),
        trackBg = Color(0xFFE2E8F0)
    )
}

enum class AttendanceLevel {
    GREEN,
    ORANGE,
    RED,
    PENDING
}

data class AttendanceLevelStyle(
    val level: AttendanceLevel,
    val accent: Color,
    val pillBg: Color,
    val pillLabel: Color
)

fun resolveAttendanceLevelStyle(
    pct: Double?,
    tieneRegistro: Boolean,
    isDark: Boolean
): AttendanceLevelStyle {
    if (!tieneRegistro || pct == null || !pct.isFinite()) {
        return if (isDark) {
            AttendanceLevelStyle(
                level = AttendanceLevel.PENDING,
                accent = Color(0xFF94A3B8),
                pillBg = Color(0xFF152238),
                pillLabel = Color(0xFF94A3B8)
            )
        } else {
            AttendanceLevelStyle(
                level = AttendanceLevel.PENDING,
                accent = Color(0xFF64748B),
                pillBg = Color(0xFFF1F5F9),
                pillLabel = Color(0xFF64748B)
            )
        }
    }

    return when {
        // Umbrales existentes: >= 90 verde, >= 70 naranja, < 70 rojo
        pct >= 90.0 -> if (isDark) {
            AttendanceLevelStyle(
                level = AttendanceLevel.GREEN,
                accent = Color(0xFF22C55E),
                pillBg = Color(0xFF0D3523),
                pillLabel = Color(0xFFD1FAE5)
            )
        } else {
            AttendanceLevelStyle(
                level = AttendanceLevel.GREEN,
                accent = Color(0xFF15803D),
                pillBg = Color(0xFFE6F7ED),
                pillLabel = Color(0xFF286646)
            )
        }
        pct >= 70.0 -> if (isDark) {
            AttendanceLevelStyle(
                level = AttendanceLevel.ORANGE,
                accent = Color(0xFFF59E0B),
                pillBg = Color(0xFF3D2812),
                pillLabel = Color(0xFFFDE68A)
            )
        } else {
            AttendanceLevelStyle(
                level = AttendanceLevel.ORANGE,
                accent = Color(0xFFEA580C),
                pillBg = Color(0xFFFEF0E1),
                pillLabel = Color(0xFF7C4A21)
            )
        }
        else -> if (isDark) {
            AttendanceLevelStyle(
                level = AttendanceLevel.RED,
                accent = Color(0xFFEF4444),
                pillBg = Color(0xFF3D151B),
                pillLabel = Color(0xFFFECACA)
            )
        } else {
            AttendanceLevelStyle(
                level = AttendanceLevel.RED,
                accent = Color(0xFFDC2626),
                pillBg = Color(0xFFFDECEC),
                pillLabel = Color(0xFF7F1D1D)
            )
        }
    }
}

data class AttendanceCourseIconStyle(
    val icon: ImageVector,
    val bgColor: Color
)

fun resolveAttendanceCourseIcon(courseName: String, crn: String? = null, index: Int = 0): AttendanceCourseIconStyle {
    return AttendanceCourseIconStyle(
        icon = resolveCourseSubjectIcon(courseName),
        bgColor = CourseIconBadgeLight
    )
}

@Composable
internal fun AttendanceHeaderBar(
    periodo: String,
    periodos: List<String>,
    semanaCiclo: Int,
    palette: AttendanceThemePalette,
    onSelectPeriodo: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var periodMenuExpanded by remember { mutableStateOf(false) }
    val opcionesPeriodo = remember(periodos, periodo) {
        if (periodos.isNotEmpty()) periodos else listOfNotNull(periodo.takeIf { it.isNotBlank() })
    }
    val canSelectPeriodo = opcionesPeriodo.size > 1
    val weekText = if (semanaCiclo > 0) {
        "Semana $semanaCiclo/${AcademicCalendar.TOTAL_SEMANAS_REGULARES}"
    } else {
        "Sin semana activa"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1.12f)
                .fillMaxHeight()
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = palette.surface,
                border = BorderStroke(1.dp, palette.border),
                shadowElevation = if (palette.isDark) 0.dp else 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .defaultMinSize(minHeight = 48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .testTag("attendance_period_selector")
                    .clickable(enabled = canSelectPeriodo) { periodMenuExpanded = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.EventAvailable,
                        contentDescription = null,
                        tint = palette.accentBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (periodo.isNotBlank()) "Período $periodo" else "Período --",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (canSelectPeriodo) "Seleccionar período" else null,
                        tint = palette.textMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = periodMenuExpanded,
                onDismissRequest = { periodMenuExpanded = false }
            ) {
                opcionesPeriodo.forEach { item ->
                    DropdownMenuItem(
                        text = { Text("Período $item", fontSize = 13.sp) },
                        onClick = {
                            periodMenuExpanded = false
                            onSelectPeriodo(item)
                        }
                    )
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = palette.weekPillBg,
            border = BorderStroke(1.dp, palette.weekPillBorder),
            modifier = Modifier
                .weight(0.88f)
                .fillMaxHeight()
                .defaultMinSize(minHeight = 48.dp)
                .testTag("attendance_week_badge")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    tint = palette.weekPillText,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    text = weekText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.weekPillText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
internal fun AttendanceModernCourseCard(
    courseName: String,
    crn: String?,
    daysText: String,
    pct: Double?,
    formattedPct: String,
    tieneRegistro: Boolean,
    faltas: Int,
    palette: AttendanceThemePalette,
    index: Int,
    onClick: (() -> Unit)?
) {
    val iconStyle = remember(courseName, crn, index) {
        resolveAttendanceCourseIcon(courseName, crn, index)
    }
    val levelStyle = remember(pct, tieneRegistro, palette.isDark) {
        resolveAttendanceLevelStyle(pct, tieneRegistro, palette.isDark)
    }

    val absenceBg = when {
        !tieneRegistro -> if (palette.isDark) Color(0xFF152238) else Color(0xFFF1F5F9)
        faltas > 0 -> if (palette.isDark) Color(0xFF3C171D) else Color(0xFFFCE8E8)
        else -> if (palette.isDark) Color(0xFF103824) else Color(0xFFE6F7ED)
    }
    val absenceFg = when {
        !tieneRegistro -> palette.textMuted
        faltas > 0 -> if (palette.isDark) Color(0xFFF87171) else Color(0xFFDC2626)
        else -> if (palette.isDark) Color(0xFF4ADE80) else Color(0xFF15803D)
    }
    val absenceText = when {
        !tieneRegistro -> "Sin registro"
        faltas == 1 -> "1 falta"
        else -> "$faltas faltas"
    }

    val progress by animateFloatAsState(
        targetValue = if (tieneRegistro && pct != null) (pct / 100.0).toFloat().coerceIn(0f, 1f) else 0f,
        animationSpec = tween(350),
        label = "attendanceProgress"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium),
        label = "attendanceCardScale"
    )

    val cardTagId = crn?.takeIf { it.isNotBlank() } ?: index.toString()

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = palette.surface,
        border = BorderStroke(1.dp, palette.border),
        shadowElevation = if (palette.isDark) 0.dp else 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(20.dp))
            .testTag("attendance_card_$cardTagId")
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = ripple(),
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono representativo del curso
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (palette.isDark) CourseIconBadgeDark else iconStyle.bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconStyle.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(23.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Contenido principal de la tarjeta
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = courseName,
                            fontSize = 14.sp,
                            lineHeight = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (daysText.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Schedule,
                                    contentDescription = null,
                                    tint = palette.textMuted,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = daysText,
                                    fontSize = 12.sp,
                                    color = palette.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Pill de porcentaje de asistencia
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = levelStyle.pillBg,
                        modifier = Modifier.widthIn(min = 72.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (tieneRegistro && pct != null) "$formattedPct%" else "—",
                                fontSize = 17.sp,
                                lineHeight = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = levelStyle.accent,
                                maxLines = 1
                            )
                            Text(
                                text = "Asistencia",
                                fontSize = 10.sp,
                                lineHeight = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = levelStyle.pillLabel,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Barra de progreso
                LinearProgressIndicator(
                    progress = { progress },
                    color = levelStyle.accent,
                    trackColor = palette.trackBg,
                    strokeCap = StrokeCap.Round,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                )

                // Fila inferior: cantidad de faltas + Ver desglose
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = absenceBg
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = when {
                                    !tieneRegistro -> Icons.Outlined.Schedule
                                    faltas > 0 -> Icons.Filled.Cancel
                                    else -> Icons.Filled.CheckCircle
                                },
                                contentDescription = null,
                                tint = absenceFg,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = absenceText,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = absenceFg,
                                maxLines = 1
                            )
                        }
                    }

                    if (onClick != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier
                                .testTag("attendance_detail_button_$cardTagId")
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Ver desglose",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.accentBlue,
                                maxLines = 1
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Ver desglose de $courseName",
                                tint = palette.accentBlue,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
