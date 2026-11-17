package com.example.upaos.ui.horario

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CalendarViewWeek
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.upaos.data.model.AsistenciaCurso
import com.example.upaos.data.model.HorarioBloque
import com.example.upaos.data.model.HorarioCurso
import com.example.upaos.data.model.SemanaInfo
import com.example.upaos.data.upao.AcademicCalendar
import com.example.upaos.ui.components.EmptyState
import com.example.upaos.ui.components.toTitleCase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

internal enum class HorarioViewMode {
    DAILY,
    WEEKLY
}

internal data class HorarioThemePalette(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val border: Color,
    val gridLine: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accentBlue: Color,
    val accentBlueSoft: Color,
    val toggleContainer: Color,
    val teoriaBg: Color,
    val teoriaText: Color,
    val teoriaDot: Color,
    val labBg: Color,
    val labText: Color,
    val labDot: Color,
    val pracBg: Color,
    val pracText: Color,
    val pracDot: Color
)

internal fun horarioThemePalette(isDark: Boolean): HorarioThemePalette = if (isDark) {
    // Modo oscuro: 75% negro/oscuro, 15% blanco, 10% azul
    HorarioThemePalette(
        isDark = true,
        background = Color(0xFF060B14),
        surface = Color(0xFF0B1526),
        surfaceElevated = Color(0xFF101D34),
        border = Color(0xFF172740),
        gridLine = Color(0xFF182842),
        textPrimary = Color(0xFFF8FAFC),
        textSecondary = Color(0xFF94A3B8),
        textMuted = Color(0xFF7B8CA6),
        accentBlue = Color(0xFF1D6FF2),
        accentBlueSoft = Color(0xFF132C58),
        toggleContainer = Color(0xFF0D182C),
        teoriaBg = Color(0xFF102B5A),
        teoriaText = Color(0xFF60A5FA),
        teoriaDot = Color(0xFF3B82F6),
        labBg = Color(0xFF2B1C50),
        labText = Color(0xFFC084FC),
        labDot = Color(0xFFA855F7),
        pracBg = Color(0xFF0D3A34),
        pracText = Color(0xFF5EEAD4),
        pracDot = Color(0xFF14B8A6)
    )
} else {
    // Modo claro: 75% blanco, 15% negro/slate, 10% azul
    HorarioThemePalette(
        isDark = false,
        background = Color(0xFFF6F8FC),
        surface = Color(0xFFFFFFFF),
        surfaceElevated = Color(0xFFEEF3FB),
        border = Color(0xFFE5ECF6),
        gridLine = Color(0xFFE7EDF6),
        textPrimary = Color(0xFF0F172A),
        textSecondary = Color(0xFF475569),
        textMuted = Color(0xFF64748B),
        accentBlue = Color(0xFF1A6DFF),
        accentBlueSoft = Color(0xFFE6F0FF),
        toggleContainer = Color(0xFFEDF2FA),
        teoriaBg = Color(0xFFE6F0FF),
        teoriaText = Color(0xFF1A6DFF),
        teoriaDot = Color(0xFF1A6DFF),
        labBg = Color(0xFFF1EAFF),
        labText = Color(0xFF7C3AED),
        labDot = Color(0xFF8B5CF6),
        pracBg = Color(0xFFE2F7F2),
        pracText = Color(0xFF0D9488),
        pracDot = Color(0xFF14B8A6)
    )
}

internal data class WeeklyBlockColors(
    val background: Color,
    val border: Color,
    val titleColor: Color,
    val metaColor: Color
)

internal data class WeeklyTypePillColors(
    val background: Color,
    val text: Color
)

private fun weeklyTypePillColors(
    tipoInfo: TipoClaseInfo,
    isDark: Boolean
): WeeklyTypePillColors = if (isDark) {
    when {
        tipoInfo.esLab -> WeeklyTypePillColors(
            background = Color(0xFF6D28D9),
            text = Color(0xFFF5F3FF)
        )
        tipoInfo.esPrac -> WeeklyTypePillColors(
            background = Color(0xFF0F766E),
            text = Color(0xFFECFDF5)
        )
        else -> WeeklyTypePillColors(
            background = Color(0xFF1D4ED8),
            text = Color(0xFFEFF6FF)
        )
    }
} else {
    when {
        tipoInfo.esLab -> WeeklyTypePillColors(
            background = Color(0xFFA855F7),
            text = Color(0xFFFFFFFF)
        )
        tipoInfo.esPrac -> WeeklyTypePillColors(
            background = Color(0xFF5EEAD4),
            text = Color(0xFF064E3B)
        )
        else -> WeeklyTypePillColors(
            background = Color(0xFF8EC5FF),
            text = Color(0xFF0B3578)
        )
    }
}

private fun weeklyBlockColors(
    curso: HorarioCurso,
    isDark: Boolean
): WeeklyBlockColors {
    val paletteIndex = abs((curso.displayNombre.trim().uppercase(Locale.ROOT)).hashCode()) % 4
    return if (isDark) {
        when (paletteIndex) {
            0 -> WeeklyBlockColors(
                background = Color(0xFF10203B),
                border = Color(0xFF1D3761),
                titleColor = Color(0xFFF8FAFC),
                metaColor = Color(0xFFCBD5E1)
            )
            1 -> WeeklyBlockColors(
                background = Color(0xFF0D3429),
                border = Color(0xFF1A5745),
                titleColor = Color(0xFFF8FAFC),
                metaColor = Color(0xFFD1FAE5)
            )
            2 -> WeeklyBlockColors(
                background = Color(0xFF251A42),
                border = Color(0xFF3E2D6B),
                titleColor = Color(0xFFF8FAFC),
                metaColor = Color(0xFFE9DDFF)
            )
            else -> WeeklyBlockColors(
                background = Color(0xFF3B2D14),
                border = Color(0xFF5D4720),
                titleColor = Color(0xFFF8FAFC),
                metaColor = Color(0xFFFEF0C7)
            )
        }
    } else {
        when (paletteIndex) {
            0 -> WeeklyBlockColors(
                background = Color(0xFFDCEBFF),
                border = Color(0xFFC3DBFF),
                titleColor = Color(0xFF0F172A),
                metaColor = Color(0xFF334155)
            )
            1 -> WeeklyBlockColors(
                background = Color(0xFFD2F4DF),
                border = Color(0xFFB0E8C6),
                titleColor = Color(0xFF0F172A),
                metaColor = Color(0xFF334155)
            )
            2 -> WeeklyBlockColors(
                background = Color(0xFFECE1FF),
                border = Color(0xFFD5C1FF),
                titleColor = Color(0xFF0F172A),
                metaColor = Color(0xFF334155)
            )
            else -> WeeklyBlockColors(
                background = Color(0xFFFDECC8),
                border = Color(0xFFF4D89B),
                titleColor = Color(0xFF0F172A),
                metaColor = Color(0xFF334155)
            )
        }
    }
}

@Composable
internal fun HorarioHeaderAndToggle(
    viewMode: HorarioViewMode,
    onViewModeChange: (HorarioViewMode) -> Unit,
    isToday: Boolean,
    onSelectToday: () -> Unit,
    onExportCalendar: () -> Unit,
    onShare: () -> Unit,
    palette: HorarioThemePalette,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Horario",
                    fontSize = 23.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.3).sp,
                    color = palette.textPrimary
                )
                Text(
                    text = "Gestión de clases",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = palette.textSecondary
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    onClick = onSelectToday,
                    shape = RoundedCornerShape(10.dp),
                    color = if (isToday) palette.accentBlueSoft else palette.surface,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isToday) palette.accentBlue.copy(alpha = 0.45f) else palette.border
                    ),
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 40.dp)
                        .testTag("horario_today_button")
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Hoy",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isToday) palette.accentBlue else palette.textSecondary
                        )
                    }
                }

                IconButton(
                    onClick = onExportCalendar,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("horario_export_ics_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = "Exportar horario al calendario",
                        tint = palette.accentBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onShare,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("horario_share_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "Compartir horario",
                        tint = palette.accentBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Selector segmentado: Vista diaria | Vista semanal
        Surface(
            shape = RoundedCornerShape(50),
            color = palette.toggleContainer,
            border = BorderStroke(1.dp, palette.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ViewTogglePill(
                    label = "Vista diaria",
                    icon = Icons.Outlined.CalendarToday,
                    selected = viewMode == HorarioViewMode.DAILY,
                    onClick = { onViewModeChange(HorarioViewMode.DAILY) },
                    palette = palette,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("horario_view_daily")
                )
                ViewTogglePill(
                    label = "Vista semanal",
                    icon = Icons.Outlined.CalendarViewWeek,
                    selected = viewMode == HorarioViewMode.WEEKLY,
                    onClick = { onViewModeChange(HorarioViewMode.WEEKLY) },
                    palette = palette,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("horario_view_weekly")
                )
            }
        }
    }
}

@Composable
private fun ViewTogglePill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    palette: HorarioThemePalette,
    modifier: Modifier = Modifier
) {
    val bgColor = if (selected) palette.accentBlue else Color.Transparent
    val contentColor = if (selected) Color.White else palette.textSecondary

    Row(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .selectable(
                selected = selected,
                role = Role.Tab,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun HorarioDailyCalendarBar(
    selectedDate: Long,
    onDateSelected: (Long) -> Unit,
    palette: HorarioThemePalette,
    modifier: Modifier = Modifier
) {
    val locale = remember { Locale.forLanguageTag("es-PE") }
    val days = remember(selectedDate) { scheduleMonthDays(selectedDate) }
    val listState = rememberLazyListState()
    val selectedIndex = days.indexOf(selectedDate).coerceAtLeast(0)
    val today = scheduleDateStart(System.currentTimeMillis())
    val monthLabel = remember(selectedDate) {
        SimpleDateFormat("MMMM yyyy", locale).apply {
            timeZone = scheduleCalendar(selectedDate).timeZone
        }.format(Date(selectedDate)).replaceFirstChar { it.titlecase(locale) }
    }
    // Keep horizontal day-strip swipes inside the calendar instead of paging the outer tabs.
    val calendarScroll = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource) =
                Offset(available.x, 0f)
        }
    }

    LaunchedEffect(selectedDate) {
        listState.animateScrollToItem((selectedIndex - 2).coerceAtLeast(0))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = monthLabel,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
                IconButton(
                    onClick = { onDateSelected(moveScheduleMonth(selectedDate, -1)) },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("horario_prev_month")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Mes anterior",
                        tint = palette.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = { onDateSelected(moveScheduleMonth(selectedDate, 1)) },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("horario_next_month")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Mes siguiente",
                        tint = palette.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .nestedScroll(calendarScroll)
        ) {
            items(days, key = { it }) { date ->
                val selected = date == selectedDate
                val calendar = scheduleCalendar(date)
                val weekday = listOf("lun", "mar", "mié", "jue", "vie", "sáb", "dom")[
                    (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7
                ]
                val fullDate = remember(date) {
                    SimpleDateFormat("EEEE d 'de' MMMM 'de' yyyy", locale)
                        .apply { timeZone = calendar.timeZone }
                        .format(Date(date))
                }
                val bgColor = if (selected) palette.accentBlue else palette.surface
                val borderColor = if (selected) palette.accentBlue else palette.border
                val labelColor = if (selected) Color.White.copy(alpha = 0.9f) else palette.textSecondary
                val numColor = if (selected) Color.White else palette.textPrimary
                val dotColor = when {
                    date == today && selected -> Color.White
                    date == today -> palette.accentBlue
                    else -> Color.Transparent
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = bgColor,
                    border = BorderStroke(1.dp, borderColor),
                    shadowElevation = if (!palette.isDark && !selected) 1.dp else 0.dp,
                    modifier = Modifier
                        .width(50.dp)
                        .defaultMinSize(minHeight = 64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .selectable(
                            selected = selected,
                            role = Role.Tab,
                            onClick = { onDateSelected(date) }
                        )
                        .semantics(mergeDescendants = true) { contentDescription = fullDate }
                        .testTag("horario_day_pill_${calendar.get(Calendar.DAY_OF_MONTH)}")
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = weekday,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = labelColor,
                            maxLines = 1
                        )
                        Text(
                            text = calendar.get(Calendar.DAY_OF_MONTH).toString(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = numColor,
                            maxLines = 1
                        )
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun HorarioDailyTimeline(
    cursos: List<HorarioCurso>,
    asistenciaCursos: List<AsistenciaCurso>,
    selectedDate: Long,
    selectedPeriodo: String,
    palette: HorarioThemePalette,
    onBloqueClick: (HorarioCurso, HorarioBloque) -> Unit,
    modifier: Modifier = Modifier
) {
    val classes = remember(cursos, selectedDate, selectedPeriodo) {
        classesForScheduleDate(cursos, selectedDate, selectedPeriodo)
    }
    val locale = remember { Locale.forLanguageTag("es-PE") }
    val dateLabel = remember(selectedDate) {
        SimpleDateFormat("EEEE d 'de' MMMM", locale).apply {
            timeZone = scheduleCalendar(selectedDate).timeZone
        }.format(Date(selectedDate)).replaceFirstChar { it.titlecase(locale) }
    }
    val semanaInfo: SemanaInfo = remember(selectedPeriodo, selectedDate) {
        AcademicCalendar.calcularSemana(selectedPeriodo, Date(selectedDate))
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Clases del día",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
                Text(
                    text = if (semanaInfo.configurada && semanaInfo.semana != null) {
                        "$dateLabel · Semana ${semanaInfo.semana} de ${AcademicCalendar.TOTAL_SEMANAS_REGULARES}"
                    } else {
                        dateLabel
                    },
                    fontSize = 12.sp,
                    color = palette.textSecondary
                )
            }
            Text(
                text = "${classes.size} ${if (classes.size == 1) "clase" else "clases"}",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.textSecondary
            )
        }

        if (classes.isEmpty()) {
            val title = when {
                semanaInfo.fueraDeCiclo == true && (semanaInfo.diasTranscurridos ?: 0) < 0 -> "Ciclo aún no iniciado"
                semanaInfo.fueraDeCiclo == true -> "Fuera de periodo de clases"
                else -> "Sin clases este día"
            }
            val subtitle = when {
                semanaInfo.fueraDeCiclo == true && (semanaInfo.diasTranscurridos ?: 0) < 0 ->
                    "El periodo $selectedPeriodo aún no iniciaba en esta fecha."
                semanaInfo.fueraDeCiclo == true ->
                    "Las ${AcademicCalendar.TOTAL_SEMANAS_REGULARES} semanas académicas han finalizado. Ya no hay clases regulares."
                else ->
                    "Selecciona otra fecha para ver tus clases."
            }
            EmptyState(
                icon = Icons.Filled.Schedule,
                title = title,
                subtitle = subtitle
            )
        } else {
            Column {
                classes.forEachIndexed { index, scheduled ->
                    val infoComp = remember(scheduled, asistenciaCursos) {
                        deducirInfoComponente(scheduled.curso, scheduled.bloque, asistenciaCursos)
                    }
                    val tipoInfo = remember(scheduled, infoComp) {
                        resolverTipoClase(scheduled.curso, scheduled.bloque, infoComp)
                    }
                    HorarioDailyTimelineRow(
                        curso = scheduled.curso,
                        bloque = scheduled.bloque,
                        tipoInfo = tipoInfo,
                        connectAbove = index > 0,
                        connectBelow = index < classes.lastIndex,
                        palette = palette,
                        onClick = { onBloqueClick(scheduled.curso, scheduled.bloque) }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
private fun HorarioDailyTimelineRow(
    curso: HorarioCurso,
    bloque: HorarioBloque,
    tipoInfo: TipoClaseInfo,
    connectAbove: Boolean,
    connectBelow: Boolean,
    palette: HorarioThemePalette,
    onClick: () -> Unit
) {
    val startTime = bloque.horaInicio ?: bloque.horaInicio12h ?: "—"
    val endTime = bloque.horaFin ?: bloque.horaFin12h ?: "—"
    val room = bloque.aula?.takeIf { it.isNotBlank() } ?: "Aula por confirmar"

    val (badgeBg, badgeText, dotColor) = when {
        tipoInfo.esLab -> Triple(palette.labBg, palette.labText, palette.labDot)
        tipoInfo.esPrac -> Triple(palette.pracBg, palette.pracText, palette.pracDot)
        else -> Triple(palette.teoriaBg, palette.teoriaText, palette.teoriaDot)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // Columna izquierda de horarios
        Column(
            modifier = Modifier
                .width(50.dp)
                .padding(top = 14.dp, bottom = 10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = startTime,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary,
                maxLines = 1
            )
            Text(
                text = endTime,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                color = palette.textSecondary,
                maxLines = 1
            )
        }

        // Línea de tiempo conectada
        Canvas(
            modifier = Modifier
                .width(20.dp)
                .fillMaxHeight()
        ) {
            val centerX = size.width / 2f
            val dotCenterY = 24.dp.toPx()
            val dotRadius = 5.dp.toPx()
            val lineColor = palette.accentBlue.copy(alpha = if (palette.isDark) 0.55f else 0.45f)

            if (connectAbove) {
                drawLine(
                    color = lineColor,
                    start = Offset(centerX, 0f),
                    end = Offset(centerX, (dotCenterY - dotRadius - 3.dp.toPx()).coerceAtLeast(0f)),
                    strokeWidth = 2.dp.toPx()
                )
            }
            if (connectBelow) {
                drawLine(
                    color = lineColor,
                    start = Offset(centerX, dotCenterY + dotRadius + 3.dp.toPx()),
                    end = Offset(centerX, size.height),
                    strokeWidth = 2.dp.toPx()
                )
            }
            drawCircle(
                color = dotColor.copy(alpha = 0.22f),
                radius = dotRadius + 3.dp.toPx(),
                center = Offset(centerX, dotCenterY)
            )
            drawCircle(
                color = dotColor,
                radius = dotRadius,
                center = Offset(centerX, dotCenterY)
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Tarjeta moderna del curso
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = palette.surface,
            border = BorderStroke(1.dp, palette.border),
            shadowElevation = if (palette.isDark) 0.dp else 2.dp,
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 12.dp)
                .clip(RoundedCornerShape(18.dp))
                .testTag("horario_daily_card_${curso.crn ?: curso.displayNombre}_${bloque.horaInicio ?: ""}")
                .clickable(onClick = onClick)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = toTitleCase(curso.displayNombre),
                        fontSize = 15.sp,
                        lineHeight = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = "Aula",
                                tint = palette.textSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = room,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = palette.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = badgeBg
                        ) {
                            Text(
                                text = tipoInfo.nombreCompleto,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeText,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Ver detalle de clase",
                    tint = palette.textMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
internal fun HorarioWeeklyGrid(
    cursos: List<HorarioCurso>,
    asistenciaCursos: List<AsistenciaCurso>,
    selectedDate: Long,
    selectedPeriodo: String,
    palette: HorarioThemePalette,
    onDateSelected: (Long) -> Unit,
    onBloqueClick: (HorarioCurso, HorarioBloque) -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = remember { Locale.forLanguageTag("es-PE") }
    val weekDays = remember(selectedDate) { scheduleWeekDays(selectedDate) }
    val weeklyBlocks = remember(cursos, selectedDate, selectedPeriodo) {
        classesForScheduleWeek(cursos, selectedDate, selectedPeriodo)
    }
    val blocksByDay = remember(weeklyBlocks) {
        weeklyBlocks
            .groupBy { it.dayIndex }
            .mapValues { (_, list) ->
                list.sortedWith(compareBy<WeeklyScheduleBlock> { it.startMinutes }.thenBy { it.endMinutes })
            }
    }
    val semanaInfo: SemanaInfo = remember(selectedPeriodo, selectedDate) {
        AcademicCalendar.calcularSemana(selectedPeriodo, Date(selectedDate))
    }
    val monthLabel = remember(selectedDate) {
        formatWeekMonthLabel(selectedDate, locale)
    }
    val weekSubtitle = remember(semanaInfo) {
        when {
            semanaInfo.configurada && semanaInfo.semana != null ->
                "Semana ${semanaInfo.semana} de ${AcademicCalendar.TOTAL_SEMANAS_REGULARES}"
            semanaInfo.configurada && !semanaInfo.etiqueta.isNullOrBlank() ->
                semanaInfo.etiqueta
            else -> null
        }
    }

    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberScrollState()

    // Keep horizontal grid scroll gestures from swiping the outer HorizontalPager tabs.
    val gridNestedScroll = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource) =
                Offset(available.x, 0f)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Barra de navegación semanal: < Mes Año / Semana X de 17 >
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = { onDateSelected(moveScheduleWeek(selectedDate, -1)) },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("horario_prev_week")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Semana anterior",
                    tint = palette.textPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = monthLabel,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                if (!weekSubtitle.isNullOrBlank()) {
                    Text(
                        text = weekSubtitle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.accentBlue,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }

            IconButton(
                onClick = { onDateSelected(moveScheduleWeek(selectedDate, 1)) },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("horario_next_week")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Semana siguiente",
                    tint = palette.textPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        if (semanaInfo.fueraDeCiclo == true) {
            val msg = if ((semanaInfo.diasTranscurridos ?: 0) < 0) {
                "El periodo $selectedPeriodo aún no inicia en esta semana."
            } else {
                "Las ${AcademicCalendar.TOTAL_SEMANAS_REGULARES} semanas académicas han finalizado en esta semana."
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = palette.surface,
                border = BorderStroke(1.dp, palette.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = msg,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = palette.textSecondary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                )
            }
        }

        // Vista semanal en columnas por día (sin columna de horas ni espacios vacíos)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("horario_weekly_grid")
        ) {
            val dayColWidth = maxOf(102.dp, maxWidth / 7f)
            val gridContentWidth = dayColWidth * 7
            val pillRowHeight = 48.dp
            val columnsMinHeight = (maxHeight - pillRowHeight - 8.dp).coerceAtLeast(180.dp)
            val dayNames = remember { listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom") }

            val density = LocalDensity.current
            val colPx = with(density) { dayColWidth.toPx() }
            val viewportPx = with(density) { maxWidth.toPx() }
            val selectedDayIdx = remember(selectedDate) {
                (scheduleCalendar(selectedDate).get(Calendar.DAY_OF_WEEK) + 5) % 7
            }

            LaunchedEffect(selectedDate, viewportPx, colPx) {
                val colStart = selectedDayIdx * colPx
                val colEnd = colStart + colPx
                val currentScroll = horizontalScrollState.value.toFloat()
                if (colStart < currentScroll) {
                    horizontalScrollState.animateScrollTo(colStart.toInt())
                } else if (colEnd > currentScroll + viewportPx) {
                    horizontalScrollState.animateScrollTo((colEnd - viewportPx).toInt().coerceAtLeast(0))
                }
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Cabecera de días con píldoras redondeadas sincronizadas horizontalmente
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(horizontalScrollState)
                        .nestedScroll(gridNestedScroll)
                ) {
                    Row(modifier = Modifier.width(gridContentWidth)) {
                        weekDays.forEachIndexed { idx, dayDate ->
                            val cal = scheduleCalendar(dayDate)
                            val dayNum = cal.get(Calendar.DAY_OF_MONTH)
                            val isSelectedDay = dayDate == selectedDate
                            val pillBg = if (isSelectedDay) {
                                palette.accentBlue
                            } else {
                                if (palette.isDark) palette.surfaceElevated else Color(0xFFEEF2F8)
                            }
                            val pillText = if (isSelectedDay) Color.White else palette.textPrimary

                            Box(
                                modifier = Modifier
                                    .width(dayColWidth)
                                    .height(pillRowHeight)
                                    .padding(horizontal = 3.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .selectable(
                                        selected = isSelectedDay,
                                        role = Role.Tab,
                                        onClick = { onDateSelected(dayDate) }
                                    )
                                    .testTag("horario_week_header_$idx"),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = pillBg,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(36.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    ) {
                                        Text(
                                            text = "${dayNames[idx]} $dayNum",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = pillText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Columnas por día con scroll vertical + horizontal sincronizado
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (palette.isDark) palette.background else palette.surface,
                    border = BorderStroke(1.dp, palette.border),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(verticalScrollState)
                            .horizontalScroll(horizontalScrollState)
                            .nestedScroll(gridNestedScroll)
                    ) {
                        Row(
                            modifier = Modifier
                                .width(gridContentWidth)
                                .heightIn(min = columnsMinHeight)
                                .drawBehind {
                                    val divColPx = dayColWidth.toPx()
                                    val stroke = 0.8.dp.toPx()
                                    for (d in 1 until 7) {
                                        val x = d * divColPx
                                        drawLine(
                                            color = palette.gridLine,
                                            start = Offset(x, 0f),
                                            end = Offset(x, size.height),
                                            strokeWidth = stroke
                                        )
                                    }
                                }
                        ) {
                            weekDays.forEachIndexed { dayIndex, _ ->
                                val dayBlocks = blocksByDay[dayIndex].orEmpty()
                                Column(
                                    modifier = Modifier
                                        .width(dayColWidth)
                                        .padding(horizontal = 4.dp, vertical = 6.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    dayBlocks.forEach { block ->
                                        val infoComp = remember(block, asistenciaCursos) {
                                            deducirInfoComponente(block.curso, block.bloque, asistenciaCursos)
                                        }
                                        val tipoInfo = remember(block, infoComp) {
                                            resolverTipoClase(block.curso, block.bloque, infoComp)
                                        }
                                        HorarioWeeklyClassCard(
                                            block = block,
                                            tipoInfo = tipoInfo,
                                            isDark = palette.isDark,
                                            onClick = { onBloqueClick(block.curso, block.bloque) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HorarioWeeklyClassCard(
    block: WeeklyScheduleBlock,
    tipoInfo: TipoClaseInfo,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val blockColors = remember(block.curso.displayNombre, isDark) {
        weeklyBlockColors(block.curso, isDark)
    }
    val pillColors = remember(tipoInfo, isDark) {
        weeklyTypePillColors(tipoInfo, isDark)
    }

    val startTxt = block.bloque.horaInicio ?: block.bloque.horaInicio12h ?: ""
    val endTxt = block.bloque.horaFin ?: block.bloque.horaFin12h ?: ""
    val timeTxt = when {
        startTxt.isNotBlank() && endTxt.isNotBlank() -> "$startTxt – $endTxt"
        else -> startTxt.ifBlank { endTxt }
    }
    val aulaTxt = compactAulaLabel(block.bloque.aula)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = blockColors.background,
        border = BorderStroke(1.dp, blockColors.border),
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(14.dp))
            .testTag("horario_weekly_block_${block.dayIndex}_${block.startMinutes}")
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = toTitleCase(block.curso.displayNombre),
                fontSize = 12.sp,
                lineHeight = 14.5.sp,
                fontWeight = FontWeight.Bold,
                color = blockColors.titleColor,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            if (timeTxt.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = blockColors.metaColor,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = timeTxt,
                        fontSize = 9.5.sp,
                        lineHeight = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = blockColors.metaColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = blockColors.metaColor,
                    modifier = Modifier.size(11.dp)
                )
                Text(
                    text = aulaTxt,
                    fontSize = 9.5.sp,
                    lineHeight = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = blockColors.metaColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                shape = RoundedCornerShape(50),
                color = pillColors.background,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Text(
                    text = tipoInfo.nombreCompleto,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = pillColors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                )
            }
        }
    }
}

internal fun formatWeekMonthLabel(anchorDate: Long, locale: Locale = Locale.forLanguageTag("es-PE")): String {
    val cal = scheduleCalendar(anchorDate)
    return SimpleDateFormat("MMMM yyyy", locale).apply {
        timeZone = cal.timeZone
    }.format(Date(anchorDate)).replaceFirstChar { it.titlecase(locale) }
}

internal fun formatWeekRangeLabel(startDate: Long, endDate: Long, locale: Locale = Locale.forLanguageTag("es-PE")): String {
    val startCal = scheduleCalendar(startDate)
    val endCal = scheduleCalendar(endDate)
    val startDay = startCal.get(Calendar.DAY_OF_MONTH)
    val endDay = endCal.get(Calendar.DAY_OF_MONTH)
    val endYear = endCal.get(Calendar.YEAR)

    return if (startCal.get(Calendar.MONTH) == endCal.get(Calendar.MONTH)) {
        val month = SimpleDateFormat("MMMM", locale).apply { timeZone = endCal.timeZone }
            .format(Date(endDate)).lowercase(locale)
        "Semana: $startDay – $endDay de $month $endYear"
    } else {
        val startMonth = SimpleDateFormat("MMM", locale).apply { timeZone = startCal.timeZone }
            .format(Date(startDate)).replace(".", "").lowercase(locale)
        val endMonth = SimpleDateFormat("MMM", locale).apply { timeZone = endCal.timeZone }
            .format(Date(endDate)).replace(".", "").lowercase(locale)
        "Semana: $startDay $startMonth – $endDay $endMonth $endYear"
    }
}

internal fun compactAulaLabel(aula: String?): String {
    val raw = aula?.trim().orEmpty()
    if (raw.isBlank()) return "Sin aula"
    val cleaned = raw
        .replace(Regex("""(?i)pabell[oóOÓ]n\s+"""), "Pab. ")
        .trim()
    return if (cleaned.length <= 12) cleaned else cleaned.take(11) + "…"
}
