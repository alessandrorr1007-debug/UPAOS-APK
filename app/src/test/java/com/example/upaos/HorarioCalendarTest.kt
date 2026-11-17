package com.example.upaos

import com.example.upaos.data.model.HorarioBloque
import com.example.upaos.data.model.HorarioCurso
import com.example.upaos.ui.horario.*
import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.TimeZone

class HorarioCalendarTest {
    private fun date(value: String): Long = SimpleDateFormat("yyyy-MM-dd").apply {
        timeZone = TimeZone.getTimeZone("America/Lima")
    }.parse(value)!!.time

    @Test fun sundayShowsOnlySundayClassesInChronologicalOrder() {
        val course = HorarioCurso(nombre = "Curso", bloques = listOf(
            HorarioBloque(dia = 6, horaInicio = "14:00", horaFin = "16:00"),
            HorarioBloque(dia = 0, horaInicio = "08:00", horaFin = "10:00"),
            HorarioBloque(dia = 6, horaInicio = "9:00", horaFin = "11:00"),
            HorarioBloque(dia = null, horaInicio = "07:00")
        ))
        assertEquals(listOf("9:00", "14:00"),
            classesForScheduleDate(listOf(course), date("2026-10-18")).map { it.bloque.horaInicio })
        assertEquals(listOf("08:00"),
            classesForScheduleDate(listOf(course), date("2026-10-19")).map { it.bloque.horaInicio })
        assertTrue(classesForScheduleDate(listOf(course), date("2026-10-20")).isEmpty())

        // En semana 18 (2026-12-28) ya está fuera del ciclo de 17 semanas y no debe retornar clases
        assertTrue(classesForScheduleDate(listOf(course), date("2026-12-28"), "202620").isEmpty())
    }

    @Test fun monthNavigationClampsDayAndCrossesYearBoundaries() {
        assertEquals(date("2026-02-28"), moveScheduleMonth(date("2026-01-31"), 1))
        assertEquals(date("2024-02-29"), moveScheduleMonth(date("2024-01-31"), 1))
        assertEquals(date("2027-01-18"), moveScheduleMonth(date("2026-12-18"), 1))
        assertEquals(date("2026-12-18"), moveScheduleMonth(date("2027-01-18"), -1))
    }

    @Test fun stripIncludesEveryDayOfSelectedMonth() {
        val days = scheduleMonthDays(date("2024-02-18"))
        assertEquals(29, days.size)
        assertEquals(date("2024-02-01"), days.first())
        assertEquals(date("2024-02-29"), days.last())
        assertEquals(31, scheduleMonthDays(date("2026-10-18")).size)
    }

    @Test fun phoneTimezoneCannotShiftSelectedCampusDate() {
        val original = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Auckland"))
            val sunday = date("2026-10-18")
            assertEquals(sunday, scheduleDateStart(sunday + 23 * 60 * 60 * 1000))
            assertEquals(Calendar.SUNDAY, scheduleCalendar(sunday).get(Calendar.DAY_OF_WEEK))
        } finally {
            TimeZone.setDefault(original)
        }
    }

    @Test fun resolverTipoClaseDistinguishesTheoryAndLabOnSameDay() {
        val bloqueTeoria = HorarioBloque(dia = 1, horaInicio = "07:15", horaFin = "08:50", aula = "G-301", tipo = "Clase")
        val bloqueLab = HorarioBloque(dia = 1, horaInicio = "08:50", horaFin = "10:30", aula = "Pab. H - LAB 202", tipo = "Clase")
        val curso = HorarioCurso(
            nombre = "MACHINE LEARNING",
            crn = "5045",
            bloques = listOf(bloqueTeoria, bloqueLab)
        )

        val infoTeoria = deducirInfoComponente(curso, bloqueTeoria, emptyList())
        val resTeoria = resolverTipoClase(curso, bloqueTeoria, infoTeoria)
        assertEquals("T", resTeoria.letra)
        assertEquals("Teoría", resTeoria.nombreCompleto)
        assertFalse(resTeoria.esLab)

        val infoLab = deducirInfoComponente(curso, bloqueLab, emptyList())
        val resLab = resolverTipoClase(curso, bloqueLab, infoLab)
        assertEquals("L", resLab.letra)
        assertEquals("Laboratorio", resLab.nombreCompleto)
        assertTrue(resLab.esLab)
    }

    @Test fun resolverTipoClaseDetectsDirectLetterTypes() {
        val curso = HorarioCurso(nombre = "CURSO EJEMPLO")
        val bloqueT = HorarioBloque(dia = 0, tipo = "T")
        val bloqueL = HorarioBloque(dia = 0, tipo = "L")
        val bloqueP = HorarioBloque(dia = 0, tipo = "P")

        val resT = resolverTipoClase(curso, bloqueT, InfoBloqueComponente("T", null, null, null))
        assertEquals("T", resT.letra)
        assertEquals("Teoría", resT.nombreCompleto)

        val resL = resolverTipoClase(curso, bloqueL, InfoBloqueComponente("L", null, null, null))
        assertEquals("L", resL.letra)
        assertEquals("Laboratorio", resL.nombreCompleto)
        assertTrue(resL.esLab)

        val resP = resolverTipoClase(curso, bloqueP, InfoBloqueComponente("P", null, null, null))
        assertEquals("P", resP.letra)
        assertEquals("Práctica", resP.nombreCompleto)
        assertTrue(resP.esPrac)
    }

    @Test fun scheduleWeekDaysSpansMondayThroughSunday() {
        val wednesday = date("2026-11-18")
        val week = scheduleWeekDays(wednesday)
        assertEquals(7, week.size)
        assertEquals(date("2026-11-16"), week.first()) // Monday
        assertEquals(date("2026-11-22"), week.last())  // Sunday
        assertEquals(Calendar.MONDAY, scheduleCalendar(week.first()).get(Calendar.DAY_OF_WEEK))
        assertEquals(Calendar.SUNDAY, scheduleCalendar(week.last()).get(Calendar.DAY_OF_WEEK))
    }

    @Test fun moveScheduleWeekNavigatesAcrossMonthsAndYears() {
        val dec30 = date("2026-12-30")
        assertEquals(date("2027-01-06"), moveScheduleWeek(dec30, 1))
        assertEquals(date("2026-12-23"), moveScheduleWeek(dec30, -1))
    }

    @Test fun parseTimeMinutesHandles24hAnd12hFormats() {
        assertEquals(8 * 60, parseTimeMinutes("08:00"))
        assertEquals(18 * 60 + 45, parseTimeMinutes("18:45"))
        assertEquals(14 * 60 + 15, parseTimeMinutes("2:15 PM"))
        assertEquals(0, parseTimeMinutes("12:00 AM"))
        assertEquals(12 * 60 + 30, parseTimeMinutes("12:30 p.m."))
        assertNull(parseTimeMinutes(null))
        assertNull(parseTimeMinutes("sin hora"))
    }

    @Test fun classesForScheduleWeekAggregatesAllDaysAndAssignsLanes() {
        val mon = HorarioCurso(
            nombre = "ARQUITECTURA DE SISTEMAS",
            bloques = listOf(
                HorarioBloque(dia = 0, horaInicio = "08:00", horaFin = "10:00", aula = "G703", tipo = "T"),
                HorarioBloque(dia = 6, horaInicio = "10:00", horaFin = "12:00", aula = "H202", tipo = "L")
            )
        )
        val overlap = HorarioCurso(
            nombre = "INTERNET DE LAS COSAS",
            bloques = listOf(
                HorarioBloque(dia = 0, horaInicio = "09:00", horaFin = "11:00", aula = "G702", tipo = "T")
            )
        )

        val blocks = classesForScheduleWeek(listOf(mon, overlap), date("2026-11-18"))
        assertEquals(3, blocks.size)

        val mondayBlocks = blocks.filter { it.dayIndex == 0 }
        assertEquals(2, mondayBlocks.size)
        assertTrue(mondayBlocks.all { it.laneCount == 2 })
        assertEquals(setOf(0, 1), mondayBlocks.map { it.laneIndex }.toSet())

        val sundayBlocks = blocks.filter { it.dayIndex == 6 }
        assertEquals(1, sundayBlocks.size)
        assertEquals(1, sundayBlocks.first().laneCount)
        assertEquals(0, sundayBlocks.first().laneIndex)

        // Fuera de ciclo devuelve vacío para toda la semana
        assertTrue(classesForScheduleWeek(listOf(mon, overlap), date("2026-12-28"), "202620").isEmpty())
    }

    @Test fun horarioThemePaletteMaintainsLightAndDarkContrastAndFormatting() {
        val light = horarioThemePalette(isDark = false)
        val dark = horarioThemePalette(isDark = true)
        assertFalse(light.isDark)
        assertTrue(dark.isDark)
        assertNotEquals(light.background, dark.background)
        assertNotEquals(light.surface, dark.surface)
        assertNotEquals(light.textPrimary, dark.textPrimary)

        val range = formatWeekRangeLabel(date("2026-11-16"), date("2026-11-22"))
        assertTrue(range.contains("16"))
        assertTrue(range.contains("22"))
        assertTrue(range.contains("2026"))

        assertEquals("Pab. G G704", compactAulaLabel("PABELLÓN G G704"))
        assertEquals("Sin aula", compactAulaLabel(""))
    }

    @Test fun weeklyHeaderFormatsMonthAndDynamicAcademicWeek() {
        val nov17 = date("2026-11-17")
        assertEquals("Noviembre 2026", formatWeekMonthLabel(nov17))

        val semanaInfo = com.example.upaos.data.upao.AcademicCalendar.calcularSemana("202620", java.util.Date(nov17))
        assertTrue(semanaInfo.configurada)
        assertEquals(12, semanaInfo.semana)
        assertEquals("Semana 12 de 17", semanaInfo.etiqueta)

        val course = HorarioCurso(
            nombre = "GOBIERNO DE DATOS",
            bloques = listOf(
                HorarioBloque(dia = 0, horaInicio = "19:50", horaFin = "21:35", aula = "G704", tipo = "L"),
                HorarioBloque(dia = 0, horaInicio = "18:00", horaFin = "19:45", aula = "G704", tipo = "T")
            )
        )
        val mondayBlocks = classesForScheduleWeek(listOf(course), nov17, "202620")
            .filter { it.dayIndex == 0 }
            .sortedWith(compareBy<WeeklyScheduleBlock> { it.startMinutes }.thenBy { it.endMinutes })
        assertEquals(listOf("18:00", "19:50"), mondayBlocks.map { it.bloque.horaInicio })
    }
}
