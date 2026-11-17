package com.example.upaos.ui.grades

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Architecture
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Biotech
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import java.text.Normalizer
import java.util.Locale
import kotlin.math.abs

data class CourseCardStyle(
    val icon: ImageVector,
    val iconBgColor: Color,
    val lightPillBg: Color,
    val lightPillLabel: Color,
    val lightPillValue: Color,
    val darkPillBg: Color,
    val darkPillLabel: Color,
    val darkPillValue: Color
)

private val StyleBlue = CourseCardStyle(
    icon = Icons.Rounded.Code,
    iconBgColor = Color(0xFF1D72F3),
    lightPillBg = Color(0xFFEAF2FF),
    lightPillLabel = Color(0xFF3E5C8A),
    lightPillValue = Color(0xFF0A3A8A),
    darkPillBg = Color(0xFF132948),
    darkPillLabel = Color(0xFF8CAAD6),
    darkPillValue = Color(0xFFFFFFFF)
)

private val StyleViolet = CourseCardStyle(
    icon = Icons.Rounded.Memory,
    iconBgColor = Color(0xFF7C5CFF),
    lightPillBg = Color(0xFFEAF2FF),
    lightPillLabel = Color(0xFF3E5C8A),
    lightPillValue = Color(0xFF0A3A8A),
    darkPillBg = Color(0xFF132948),
    darkPillLabel = Color(0xFF8CAAD6),
    darkPillValue = Color(0xFFFFFFFF)
)

private val StyleEmerald = CourseCardStyle(
    icon = Icons.Rounded.Assessment,
    iconBgColor = Color(0xFF0D9F72),
    lightPillBg = Color(0xFFE5F6F0),
    lightPillLabel = Color(0xFF286655),
    lightPillValue = Color(0xFF054F3B),
    darkPillBg = Color(0xFF113344),
    darkPillLabel = Color(0xFF8CC4CF),
    darkPillValue = Color(0xFFFFFFFF)
)

private val StyleAmber = CourseCardStyle(
    icon = Icons.Rounded.Storage,
    iconBgColor = Color(0xFFE89B2D),
    lightPillBg = Color(0xFFFDF3E2),
    lightPillLabel = Color(0xFF7B5926),
    lightPillValue = Color(0xFF613B00),
    darkPillBg = Color(0xFF142D45),
    darkPillLabel = Color(0xFF9AB4CC),
    darkPillValue = Color(0xFFFFFFFF)
)

private val StyleIndigo = CourseCardStyle(
    icon = Icons.Rounded.Hub,
    iconBgColor = Color(0xFF814EF2),
    lightPillBg = Color(0xFFF0ECFF),
    lightPillLabel = Color(0xFF56428F),
    lightPillValue = Color(0xFF351B7C),
    darkPillBg = Color(0xFF132948),
    darkPillLabel = Color(0xFF8CAAD6),
    darkPillValue = Color(0xFFFFFFFF)
)

fun resolveCourseCardStyle(courseName: String, crn: String? = null, index: Int = 0): CourseCardStyle {
    val normalized = Normalizer.normalize(courseName, Normalizer.Form.NFD)
        .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        .lowercase(Locale.ROOT)

    return when {
        listOf("percepcion", "vision", "inteligencia", "analit", "estadist", "calcul", "matem", "fisica").any {
            normalized.contains(it)
        } -> StyleEmerald

        listOf("arquitectura", "sistema", "program", "algorit", "softw", "web", "movil", "compil").any {
            normalized.contains(it)
        } -> StyleBlue

        listOf("internet", "iot", "red", "comput", "hardware", "robot", "electron", "micro", "telecom").any {
            normalized.contains(it)
        } -> StyleViolet

        listOf("dato", "base de datos", "gobierno", "sql", "cloud", "mineria", "econom", "contab", "finanz").any {
            normalized.contains(it)
        } -> StyleAmber

        listOf("proyecto", "gestion", "gerenc", "tesis", "seminar", "administr", "emprend").any {
            normalized.contains(it)
        } -> StyleIndigo

        listOf("biolog", "salud", "medic", "anatom", "quimic", "enferm", "farmac", "nutric").any {
            normalized.contains(it)
        } -> StyleEmerald.copy(icon = Icons.Rounded.Biotech)

        listOf("derecho", "civil", "penal", "constituc", "empresa", "market", "negoc", "comercio").any {
            normalized.contains(it)
        } -> StyleAmber.copy(icon = Icons.Rounded.AccountBalance)

        listOf("comunic", "lengu", "ingles", "etica", "filosof", "histor", "psicolog", "redacc").any {
            normalized.contains(it)
        } -> StyleIndigo.copy(icon = Icons.Rounded.AutoStories)

        listOf("diseno", "diseño", "taller", "urban", "plano", "dibujo", "construc").any {
            normalized.contains(it)
        } -> StyleViolet.copy(icon = Icons.Rounded.Architecture)

        else -> {
            val pool = listOf(StyleBlue, StyleViolet, StyleEmerald, StyleAmber, StyleIndigo)
            val seed = abs((normalized + (crn ?: "")).hashCode() + index)
            pool[seed % pool.size]
        }
    }
}
