package com.example.upaos

import androidx.compose.ui.graphics.Color
import com.example.upaos.data.model.AsistenciaCurso
import com.example.upaos.data.model.CourseGrade
import com.example.upaos.data.model.HorarioCurso
import com.example.upaos.ui.grades.coursesThemePalette
import com.example.upaos.ui.grades.formatCourseAverage
import com.example.upaos.ui.grades.resolveCourseCardStyle
import com.example.upaos.ui.grades.resolveCourseDocente
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoursesDesignTest {

    @Test
    fun lightAndDarkPalettesFollowRequestedColorRatios() {
        val light = coursesThemePalette(isDark = false)
        val dark = coursesThemePalette(isDark = true)

        assertFalse(light.isDark)
        assertTrue(dark.isDark)
        assertEquals(Color(0xFFF6F8FC), light.background)
        assertEquals(Color(0xFFFFFFFF), light.surface)
        assertEquals(Color(0xFF0F172A), light.textPrimary)
        assertEquals(Color(0xFF1A6DFF), light.accentBlue)

        assertEquals(Color(0xFF060B14), dark.background)
        assertEquals(Color(0xFF0B1526), dark.surface)
        assertEquals(Color(0xFFF8FAFC), dark.textPrimary)
        assertEquals(Color(0xFF3B82F6), dark.accentBlue)
        assertNotEquals(light.background, dark.background)
    }

    @Test
    fun averageFormattingUsesTwoDecimalsAndDashForNull() {
        assertEquals("6.40", formatCourseAverage(6.4))
        assertEquals("7.74", formatCourseAverage(7.74))
        assertEquals("16.00", formatCourseAverage(16.0))
        assertEquals("--", formatCourseAverage(null))
        assertEquals("--", formatCourseAverage(Double.NaN))
    }

    @Test
    fun docenteResolutionPrefersScheduleThenAttendanceThenRawBanner() {
        val course = CourseGrade(
            nombre = "Arquitectura de Sistemas",
            rawBanner = mapOf("faculty" to "Banner Docente")
        )
        val horario = HorarioCurso(nombre = "Arquitectura de Sistemas", docente = "Caballero Alvarado, Armando")
        val asistencia = AsistenciaCurso(materia = "Arquitectura de Sistemas", docente = "Asistencia Docente")

        assertEquals("Caballero Alvarado, Armando", resolveCourseDocente(course, horario, asistencia))
        assertEquals("Asistencia Docente", resolveCourseDocente(course, null, asistencia))
        assertEquals("Banner Docente", resolveCourseDocente(course, null, null))
        assertNull(resolveCourseDocente(CourseGrade(nombre = "Curso"), null, null))
    }

    @Test
    fun courseStylesAssignDistinctIconsAndReadablePillColorsForBothThemes() {
        val arch = resolveCourseCardStyle("Arquitectura de Sistemas", "101", 0)
        val iot = resolveCourseCardStyle("Internet de las Cosas", "102", 1)
        val perc = resolveCourseCardStyle("Percepción Computacional", "103", 2)
        val data = resolveCourseCardStyle("Gobierno de Datos", "104", 3)
        val proj = resolveCourseCardStyle("Gestión de Proyectos", "105", 4)

        val icons = setOf(arch.icon, iot.icon, perc.icon, data.icon, proj.icon)
        assertEquals(5, icons.size)
        assertEquals(Color(0xFFFFFFFF), arch.darkPillValue)
        assertNotEquals(arch.lightPillBg, arch.darkPillBg)
    }
}
