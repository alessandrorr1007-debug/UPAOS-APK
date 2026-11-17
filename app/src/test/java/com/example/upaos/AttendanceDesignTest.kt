package com.example.upaos

import androidx.compose.ui.graphics.Color
import com.example.upaos.ui.asistencia.AttendanceLevel
import com.example.upaos.ui.asistencia.attendanceThemePalette
import com.example.upaos.ui.asistencia.resolveAttendanceCourseIcon
import com.example.upaos.ui.asistencia.resolveAttendanceLevelStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AttendanceDesignTest {

    @Test
    fun lightAndDarkPalettesFollow751510ColorSplit() {
        val light = attendanceThemePalette(isDark = false)
        val dark = attendanceThemePalette(isDark = true)

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
        assertNotEquals(light.weekPillBg, dark.weekPillBg)
    }

    @Test
    fun attendanceLevelStylesRespectExistingThresholdsInBothThemes() {
        for (isDark in listOf(false, true)) {
            assertEquals(AttendanceLevel.GREEN, resolveAttendanceLevelStyle(100.0, true, isDark).level)
            assertEquals(AttendanceLevel.GREEN, resolveAttendanceLevelStyle(90.0, true, isDark).level)
            assertEquals(AttendanceLevel.ORANGE, resolveAttendanceLevelStyle(89.9, true, isDark).level)
            assertEquals(AttendanceLevel.ORANGE, resolveAttendanceLevelStyle(70.0, true, isDark).level)
            assertEquals(AttendanceLevel.RED, resolveAttendanceLevelStyle(69.9, true, isDark).level)
            assertEquals(AttendanceLevel.RED, resolveAttendanceLevelStyle(50.0, true, isDark).level)
            assertEquals(AttendanceLevel.PENDING, resolveAttendanceLevelStyle(null, false, isDark).level)
            assertEquals(AttendanceLevel.PENDING, resolveAttendanceLevelStyle(100.0, false, isDark).level)
        }
    }

    @Test
    fun courseIconsAreDistinctAndRepresentative() {
        val agile = resolveAttendanceCourseIcon("Agile Development", "101", 0)
        val customer = resolveAttendanceCourseIcon("Customer Development", "102", 1)
        val ml = resolveAttendanceCourseIcon("Machine Learning", "103", 2)
        val perc = resolveAttendanceCourseIcon("Percepcion Computacional", "104", 3)
        val auto = resolveAttendanceCourseIcon("Automatizacion Inteligente de Procesos", "105", 4)

        val icons = setOf(agile.icon, customer.icon, ml.icon, perc.icon, auto.icon)
        assertEquals(5, icons.size)
    }
}
