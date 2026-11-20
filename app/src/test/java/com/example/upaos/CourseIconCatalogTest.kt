package com.example.upaos

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.Agriculture
import androidx.compose.material.icons.rounded.Architecture
import androidx.compose.material.icons.rounded.Balance
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Construction
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Forest
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.IntegrationInstructions
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.SettingsSuggest
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.ui.graphics.Color
import com.example.upaos.ui.asistencia.resolveAttendanceCourseIcon
import com.example.upaos.ui.components.ACADEMIC_COURSE_ICON_CATALOG
import com.example.upaos.ui.components.CourseIconBadgeDark
import com.example.upaos.ui.components.CourseIconBadgeLight
import com.example.upaos.ui.components.resolveCourseSubjectIcon
import com.example.upaos.ui.grades.resolveCourseCardStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseIconCatalogTest {

    @Test
    fun catalogProvidesBetween200And300DistinctAcademicVectorIcons() {
        val count = ACADEMIC_COURSE_ICON_CATALOG.size
        assertTrue(
            "Expected catalog size in 200..300 but was $count",
            count in 200..300
        )
        assertEquals(count, ACADEMIC_COURSE_ICON_CATALOG.map { it.id }.distinct().size)
        assertEquals(count, ACADEMIC_COURSE_ICON_CATALOG.map { it.icon }.distinct().size)
    }

    @Test
    fun sameCourseAlwaysGetsIdenticalIconInCursosAndAsistenciaRegardlessOfCaseAccentsOrIndex() {
        val pairs = listOf(
            "ARQUITECTURA DE SISTEMAS" to "Arquitectura de Sistemas",
            "PERCEPCIÓN COMPUTACIONAL" to "Percepcion Computacional",
            "AUTOMATIZACIÓN INTELIGENTE DE PROCESOS" to "Automatizacion Inteligente de Procesos",
            "INGENIERÍA DE SOFTWARE I" to "Ingenieria de Software I",
            "BASE DE DATOS II" to "Base de Datos II",
            "DERECHO CONSTITUCIONAL" to "Derecho Constitucional",
            "ANATOMÍA HUMANA" to "Anatomia Humana",
            "TALLER DE DISEÑO ARQUITECTÓNICO" to "Taller de Diseno Arquitectonico",
            "CURSO INTERDISCIPLINARIO AVANZADO X9" to "Curso Interdisciplinario Avanzado X9"
        )

        pairs.forEachIndexed { idx, (rawCursosName, titleCaseAsistenciaName) ->
            val cursosStyle = resolveCourseCardStyle(rawCursosName, crn = "100$idx", index = idx)
            val asistenciaStyle = resolveAttendanceCourseIcon(titleCaseAsistenciaName, crn = "999$idx", index = idx + 7)
            assertEquals(
                "Icon mismatch between Cursos and Asistencia for $titleCaseAsistenciaName",
                cursosStyle.icon,
                asistenciaStyle.icon
            )
        }
    }

    @Test
    fun assignsSemanticallyRelevantAndDistinctIconsAcrossAllCareers() {
        val mapping = mapOf(
            "Arquitectura de Sistemas" to Icons.Rounded.AccountTree,
            "Internet de las Cosas" to Icons.Rounded.Hub,
            "Percepción Computacional" to Icons.Rounded.Radar,
            "Machine Learning" to Icons.Rounded.Memory,
            "Agile Development" to Icons.Rounded.IntegrationInstructions,
            "Customer Development" to Icons.Rounded.SupportAgent,
            "Automatización Inteligente de Procesos" to Icons.Rounded.SettingsSuggest,
            "Ingeniería de Software" to Icons.Rounded.Code,
            "Base de Datos" to Icons.Rounded.Storage,
            "Cálculo Diferencial" to Icons.Rounded.Calculate,
            "Anatomía Humana" to Icons.Rounded.Favorite,
            "Fisiología Humana" to Icons.Rounded.MonitorHeart,
            "Derecho Civil" to Icons.Rounded.Balance,
            "Derecho Penal" to Icons.Rounded.Gavel,
            "Derecho Constitucional" to Icons.Rounded.AccountBalance,
            "Taller de Diseño Arquitectónico" to Icons.Rounded.Architecture,
            "Ingeniería Civil" to Icons.Rounded.Construction,
            "Fundamentos de Marketing" to Icons.Rounded.Campaign,
            "Ecología y Medio Ambiente" to Icons.Rounded.Forest,
            "Ingeniería Agronómica" to Icons.Rounded.Agriculture,
            "Inglés Técnico" to Icons.Rounded.Translate
        )

        mapping.forEach { (courseName, expectedIcon) ->
            assertEquals("Unexpected icon for $courseName", expectedIcon, resolveCourseSubjectIcon(courseName))
        }
        assertEquals(mapping.size, mapping.values.toSet().size)
    }

    @Test
    fun courseIconsUse751510BlueBadgeColorsInLightAndDarkThemes() {
        val cursosStyle = resolveCourseCardStyle("Arquitectura de Sistemas", "101", 0)
        val asistenciaStyle = resolveAttendanceCourseIcon("Arquitectura de Sistemas", "101", 0)

        assertEquals(CourseIconBadgeLight, cursosStyle.iconBgColor)
        assertEquals(CourseIconBadgeLight, asistenciaStyle.bgColor)
        assertNotEquals(Color(0xFF0D9F72), asistenciaStyle.bgColor)
        assertNotEquals(Color(0xFF7C5CFF), cursosStyle.iconBgColor)
        assertNotEquals(CourseIconBadgeLight, CourseIconBadgeDark)
    }
}
