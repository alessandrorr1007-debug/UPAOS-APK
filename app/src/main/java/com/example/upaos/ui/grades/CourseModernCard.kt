package com.example.upaos.ui.grades

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.upaos.data.model.AsistenciaCurso
import com.example.upaos.data.model.CourseGrade
import com.example.upaos.data.model.HorarioCurso
import com.example.upaos.data.model.extraerNombreDocente
import com.example.upaos.ui.components.CourseIconBadgeDark
import com.example.upaos.ui.components.toTitleCase
import java.util.Locale

data class CoursesThemePalette(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accentBlue: Color
)

fun coursesThemePalette(isDark: Boolean): CoursesThemePalette = if (isDark) {
    // Modo oscuro: 75% negro/oscuro, 15% blanco, 10% azul
    CoursesThemePalette(
        isDark = true,
        background = Color(0xFF060B14),
        surface = Color(0xFF0B1526),
        border = Color(0xFF172740),
        textPrimary = Color(0xFFF8FAFC),
        textSecondary = Color(0xFF94A3B8),
        textMuted = Color(0xFF7B8CA6),
        accentBlue = Color(0xFF3B82F6)
    )
} else {
    // Modo claro: 75% blanco, 15% negro/slate, 10% azul
    CoursesThemePalette(
        isDark = false,
        background = Color(0xFFF6F8FC),
        surface = Color(0xFFFFFFFF),
        border = Color(0xFFE5ECF6),
        textPrimary = Color(0xFF0F172A),
        textSecondary = Color(0xFF475569),
        textMuted = Color(0xFF64748B),
        accentBlue = Color(0xFF1A6DFF)
    )
}

fun resolveCourseDocente(
    course: CourseGrade,
    horarioCurso: HorarioCurso?,
    asistenciaCurso: AsistenciaCurso?
): String? {
    val fromSchedule = horarioCurso?.displayDocente
        ?: horarioCurso?.bloques?.firstNotNullOfOrNull { it.displayDocente }
    if (!fromSchedule.isNullOrBlank()) return fromSchedule

    val fromAttendance = asistenciaCurso?.displayDocente
        ?: asistenciaCurso?.componentes?.firstNotNullOfOrNull { it.displayDocente }
    if (!fromAttendance.isNullOrBlank()) return fromAttendance

    val raw = course.rawBanner
    if (raw != null) {
        val candidate = raw["faculty"]
            ?: raw["instructor"]
            ?: raw["instructorName"]
            ?: raw["docente"]
            ?: raw["profesor"]
        extraerNombreDocente(candidate)?.takeIf { it.isNotBlank() }?.let { return it }
    }
    return null
}

fun formatCourseAverage(value: Double?): String {
    if (value == null || value.isNaN() || value.isInfinite()) return "--"
    return String.format(Locale.US, "%.2f", value)
}

@Composable
fun CourseModernCard(
    course: CourseGrade,
    periodo: String,
    creditos: Int,
    notaProyectada: Any?,
    horarioCurso: HorarioCurso?,
    asistenciaCurso: AsistenciaCurso?,
    palette: CoursesThemePalette,
    index: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val crn = course.crn ?: course.courseReferenceNumber ?: ""
    val style = remember(course.displayNombre, crn, index) {
        resolveCourseCardStyle(course.displayNombre, crn, index)
    }
    val promedioValue = remember(course, notaProyectada, periodo) {
        calcularAcumuladoCurso(course, notaProyectada, periodo)
    }
    val promedioText = remember(promedioValue) { formatCourseAverage(promedioValue) }
    val docente = remember(course, horarioCurso, asistenciaCurso) {
        resolveCourseDocente(course, horarioCurso, asistenciaCurso)
            ?.let { toTitleCase(it) }
            ?: "Docente por asignar"
    }
    val creditosText = when {
        creditos == 1 -> "1 crédito"
        creditos > 1 -> "$creditos créditos"
        else -> "-- créditos"
    }

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium),
        label = "courseCardScale"
    )

    val pillBg = if (palette.isDark) style.darkPillBg else style.lightPillBg
    val pillLabel = if (palette.isDark) style.darkPillLabel else style.lightPillLabel
    val pillValue = if (palette.isDark) style.darkPillValue else style.lightPillValue

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = palette.surface,
        border = BorderStroke(1.dp, palette.border),
        shadowElevation = if (palette.isDark) 0.dp else 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(20.dp))
            .testTag("course_card_$crn")
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                onClick = onClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono de curso con fondo azul 75/15/10
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (palette.isDark) CourseIconBadgeDark else style.iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = style.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Nombre del curso, docente y créditos (únicamente)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = toTitleCase(course.displayNombre),
                    style = MaterialTheme.typography.titleSmall,
                    fontSize = 14.5.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        tint = palette.textMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = docente,
                        fontSize = 12.sp,
                        color = palette.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Layers,
                        contentDescription = null,
                        tint = palette.textMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = creditosText,
                        fontSize = 12.sp,
                        color = palette.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Promedio
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = pillBg,
                modifier = Modifier.widthIn(min = 74.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Promedio",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = pillLabel,
                        maxLines = 1
                    )
                    Text(
                        text = promedioText,
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = pillValue,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Abrir curso",
                tint = palette.textMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
