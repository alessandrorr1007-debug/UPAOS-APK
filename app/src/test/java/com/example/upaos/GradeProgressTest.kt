package com.example.upaos

import com.example.upaos.data.model.ComponenteDetalle
import com.example.upaos.data.model.CourseGrade
import com.example.upaos.data.upao.Calculators
import com.example.upaos.ui.components.gradeColor
import com.example.upaos.ui.grades.calcularAcumuladoCurso
import com.example.upaos.ui.theme.UpaoGreen
import com.example.upaos.ui.theme.UpaoRed
import org.junit.Assert.*
import org.junit.Test

class GradeProgressTest {
    @Test fun partialAverageColorUses105InsteadOfAccumulatedPoints() {
        for (grade in listOf(10.5, 10.66, 13.0)) {
            val progress = Calculators.progreso(components(grade), "202620")!!
            assertTrue(progress.acumulado < 10.5)
            assertEquals(UpaoGreen, gradeColor(progress.promedio))
        }
        assertEquals(UpaoRed, gradeColor(Calculators.progreso(components(13.0, 2.0))!!.promedio))
        assertEquals(UpaoRed, gradeColor(10.49))
    }
    private fun components(vararg notes: Double?) =
        listOf("EP1", "PARCIAL", "EP2", "FINAL").mapIndexed { i, name ->
            ComponenteDetalle(nombre = name, nota = notes.getOrNull(i))
        }

    @Test fun agileUsesBothEvaluationsInsteadOfOnlyItsPassingEp1() {
        val progress = Calculators.progreso(components(13.0, 2.0), "202620")!!
        assertEquals(3.2, progress.acumulado, 1e-9)
        assertEquals(6.4, progress.promedio, 1e-9)
        assertEquals(50.0, progress.pesoEvaluado, 1e-9)
        assertEquals(5.25, progress.minimoAcumulado, 1e-9)
        assertFalse(progress.enRitmoAprobatorio)
    }

    @Test fun minimumTracksOnlyEvaluatedWeightIncludingZeroGrades() {
        for ((count, target) in listOf(1 to 2.1, 2 to 5.25, 3 to 7.35, 4 to 10.5)) {
            val notes = List<Double?>(4) { if (it < count) 10.5 else null }
            val progress = Calculators.progreso(components(*notes.toTypedArray()), "202610")!!
            assertEquals(target, progress.acumulado, 1e-9)
            assertEquals(target, progress.minimoAcumulado, 1e-9)
            assertTrue(progress.enRitmoAprobatorio)
        }
        val zero = Calculators.progreso(components(0.0), "202620")!!
        assertEquals(20.0, zero.pesoEvaluado, 1e-9)
        assertFalse(zero.enRitmoAprobatorio)
        assertNull(Calculators.progreso(components(), "202620"))
    }

    @Test fun historicalTermUsesItsOwnWeightsInAverageAndAccumulation() {
        val comps = components(13.0, 2.0, 15.0, 12.0)
        assertEquals(listOf(25.0, 20.0, 30.0, 25.0), comps.map { Calculators.pesoComponente(it, "202520") })
        assertEquals(listOf(20.0, 30.0, 20.0, 30.0), comps.map { Calculators.pesoComponente(it, "202610") })
        assertEquals(11.15, Calculators.progreso(comps, "202520")!!.acumulado, 1e-9)
        assertEquals(9.8, Calculators.progreso(comps, "202610")!!.acumulado, 1e-9)
        assertEquals(3.65, Calculators.calcularNotaProyectada(components(13.0, 2.0), "202520").first!!, 1e-9)
        assertEquals(8.11, Calculators.calcularNotaActual(components(13.0, 2.0), "202520")!!, 1e-9)
    }

    @Test fun completed1066And105ArePassingWithoutIntegerRounding() {
        for (grade in listOf(10.5, 10.66, 11.0)) {
            val course = CourseGrade(componentes = components(grade, grade, grade, grade))
            val progress = Calculators.progresoCurso(course, "202610")!!
            assertTrue(progress.enRitmoAprobatorio)
            assertEquals(grade, calcularAcumuladoCurso(course, 2.0, "202610")!!, 1e-9)
            assertEquals(UpaoGreen, gradeColor(grade))
        }
        assertFalse(Calculators.progreso(components(10.499, 10.499, 10.499, 10.499))!!.enRitmoAprobatorio)
        val recovered = Calculators.progreso(components(10.0, 10.0, 10.0, 12.2), "202610")!!
        assertEquals(10.66, recovered.acumulado, 1e-9)
        assertTrue(recovered.enRitmoAprobatorio)
        assertEquals(10.66, calcularAcumuladoCurso(CourseGrade(notaActual = 10.66), periodo = "202610")!!, 1e-9)
    }

    @Test fun componentBreakdownOverridesAnOldCachedProjection() {
        val course = CourseGrade(componentes = components(13.0, 2.0))
        assertEquals(3.2, calcularAcumuladoCurso(course, 2.6, "202620")!!, 1e-9)
        assertEquals(3.65, calcularAcumuladoCurso(course, 2.6, "202520")!!, 1e-9)
    }

    @Test fun ep1AndParcialEqualToElevenAccumulateTo550() {
        val course = CourseGrade(
            componentes = listOf(
                ComponenteDetalle(nombre = "EP1", peso = 20.0, nota = 11.0),
                ComponenteDetalle(nombre = "PARCIAL", peso = 30.0, nota = 11.0),
                ComponenteDetalle(nombre = "EP2", peso = 20.0, nota = null),
                ComponenteDetalle(nombre = "FINAL", peso = 30.0, nota = null)
            )
        )
        val progress = Calculators.progresoCurso(course, "202620")!!
        assertEquals(5.50, progress.acumulado, 1e-9)
        assertEquals(50.0, progress.pesoEvaluado, 1e-9)
        assertEquals(11.0, progress.promedio, 1e-9)
        val acumuladoCursos = calcularAcumuladoCurso(course, periodo = "202620")!!
        assertEquals(5.50, acumuladoCursos, 1e-9)
        assertEquals("5.50", com.example.upaos.ui.grades.formatCourseAverage(acumuladoCursos))
    }

    @Test fun explicitBannerWeightsOverrideStandardPeriodDefaults() {
        val comps = listOf(
            ComponenteDetalle(nombre = "EP1", peso = 15.0, nota = 12.0),
            ComponenteDetalle(nombre = "EXAMEN PARCIAL", peso = 35.0, nota = 16.0),
            ComponenteDetalle(nombre = "EP2", peso = 15.0, nota = null),
            ComponenteDetalle(nombre = "EXAMEN FINAL", peso = 35.0, nota = null)
        )
        assertEquals(listOf(15.0, 35.0, 15.0, 35.0), comps.map { Calculators.pesoComponente(it, "202620") })
        val progress = Calculators.progreso(comps, "202620")!!
        // 12 * 0.15 + 16 * 0.35 = 1.80 + 5.60 = 7.40
        assertEquals(7.40, progress.acumulado, 1e-9)
        assertEquals(50.0, progress.pesoEvaluado, 1e-9)
        assertEquals(14.80, progress.promedio, 1e-9)
        assertEquals(14.80, Calculators.calcularNotaActual(comps, "202620")!!, 1e-9)
        assertEquals(7.40, Calculators.calcularNotaProyectada(comps, "202620").first!!, 1e-9)
    }

    @Test fun fractionalWeightsNon20ScalesSubcomponentsAndAchievedPercentGuardWork() {
        // Fractional weights 0.25 each
        val fractional = listOf(
            ComponenteDetalle(nombre = "EP1", peso = 0.25, nota = 12.0),
            ComponenteDetalle(nombre = "PARCIAL", peso = 0.25, nota = 16.0),
            ComponenteDetalle(nombre = "EP2", peso = 0.25, nota = null),
            ComponenteDetalle(nombre = "FINAL", peso = 0.25, nota = null)
        )
        val fracProg = Calculators.progreso(fractional, "202620")!!
        assertEquals(7.0, fracProg.acumulado, 1e-9)
        assertEquals(14.0, fracProg.promedio, 1e-9)

        // Score on /100 scale (75/100 = 15/20 at 20% weight -> 3.0 pts)
        val scaled = listOf(
            ComponenteDetalle(nombre = "Evaluación (20%)", puntajeObtenido = 75.0, puntajeSobre = 100.0)
        )
        val scaledProg = Calculators.progreso(scaled, "202620")!!
        assertEquals(3.0, scaledProg.acumulado, 1e-9)
        assertEquals(15.0, scaledProg.promedio, 1e-9)

        // Subcomponents fallback when parent nota is null: 50% of EP1(20%) = 10% weight, 16 * 0.10 = 1.60
        val withSub = listOf(
            ComponenteDetalle(
                nombre = "EP1",
                peso = 20.0,
                nota = null,
                hasSubComponents = true,
                subcomponentes = listOf(
                    com.example.upaos.data.model.SubComponente(nombre = "PC1", peso = 50.0, nota = 16.0),
                    com.example.upaos.data.model.SubComponente(nombre = "PC2", peso = 50.0, nota = null)
                )
            )
        )
        val subProg = Calculators.progreso(withSub, "202620")!!
        assertEquals(1.60, subProg.acumulado, 1e-9)
        assertEquals(10.0, subProg.pesoEvaluado, 1e-9)
        assertEquals(16.0, subProg.promedio, 1e-9)

        // percentAchieved=55.0 (for 11/20) must NOT override EP1's standard 20% weight when peso is null
        val ep1WithAchievedOnly = ComponenteDetalle(
            nombre = "EP1",
            peso = null,
            porcentajeLogrado = 55.0,
            puntajeObtenido = 11.0,
            puntajeSobre = 20.0
        )
        assertEquals(20.0, Calculators.pesoComponente(ep1WithAchievedOnly, "202620")!!, 1e-9)
    }

    @Test fun attendanceAndSummaryRowsAreExcludedWhilePromPracticasIsKept() {
        val comps = listOf(
            ComponenteDetalle(nombre = "EP1", peso = 20.0, nota = 11.0),
            ComponenteDetalle(nombre = "PARCIAL", peso = 30.0, nota = 11.0),
            ComponenteDetalle(nombre = "Asistencia", description = "Asistencia", codigo = "ASIST", peso = 10.0, nota = 20.0),
            ComponenteDetalle(nombre = "Promedio", description = "Promedio", componente = "Promedio", codigo = "PF", peso = null, nota = 11.0),
            ComponenteDetalle(nombre = "Nota Final", description = "Nota Final", codigo = "NF", peso = 0.0, nota = 11.0),
            ComponenteDetalle(nombre = "Total del Curso", peso = null, porcentajeLogrado = 55.0, nota = 11.0)
        )
        assertNull(Calculators.pesoComponente(comps[2], "202620"))
        assertNull(Calculators.pesoComponente(comps[3], "202620"))
        assertNull(Calculators.pesoComponente(comps[4], "202620"))
        assertNull(Calculators.pesoComponente(comps[5], "202620"))

        val prog = Calculators.progreso(comps, "202620")!!
        assertEquals(5.50, prog.acumulado, 1e-9)
        assertEquals(50.0, prog.pesoEvaluado, 1e-9)
        assertEquals(11.0, prog.promedio, 1e-9)

        // Legitimate graded component named "Promedio de Prácticas" MUST NOT be excluded
        val conPromPracticas = listOf(
            ComponenteDetalle(nombre = "Promedio de Prácticas", peso = 20.0, nota = 15.0),
            ComponenteDetalle(nombre = "EXAMEN PARCIAL", peso = 30.0, nota = 14.0)
        )
        assertEquals(20.0, Calculators.pesoComponente(conPromPracticas[0], "202620")!!, 1e-9)
        assertEquals(7.20, Calculators.progreso(conPromPracticas, "202620")!!.acumulado, 1e-9)
    }

    @Test fun explicitZeroWeightIsNeverReplacedByStandardOrAchievedWeight() {
        val comps = listOf(
            ComponenteDetalle(nombre = "EP1", peso = 20.0, nota = 11.0),
            ComponenteDetalle(nombre = "EXAMEN PARCIAL", peso = 0.0, nota = 20.0),
            ComponenteDetalle(nombre = "EP2 (0%)", peso = null, nota = 18.0),
            ComponenteDetalle(nombre = "Taller", peso = "0.5%", nota = 20.0)
        )
        assertNull(Calculators.pesoComponente(comps[1], "202620"))
        assertNull(Calculators.pesoComponente(comps[2], "202620"))
        assertEquals(0.5, Calculators.pesoComponente(comps[3], "202620")!!, 1e-9)

        val prog = Calculators.progreso(comps, "202620")!!
        // 11 * 0.20 + 20 * 0.005 = 2.20 + 0.10 = 2.30
        assertEquals(2.30, prog.acumulado, 1e-9)
        assertEquals(20.5, prog.pesoEvaluado, 1e-9)
    }

    @Test fun pendingMarkersNeverCountAsZeroWhileRealZeroStillCounts() {
        val comps = listOf(
            ComponenteDetalle(nombre = "EP1", peso = 20.0, nota = 15.0),
            ComponenteDetalle(nombre = "PARCIAL", peso = 30.0, puntajeObtenido = 0.0, score = 0.0, gradeOficial = "SN"),
            ComponenteDetalle(nombre = "EP2", peso = 20.0, nota = "PE"),
            ComponenteDetalle(nombre = "FINAL", peso = 30.0, nota = "-")
        )
        val prog = Calculators.progreso(comps, "202620")!!
        assertEquals(3.0, prog.acumulado, 1e-9)
        assertEquals(20.0, prog.pesoEvaluado, 1e-9)
        assertEquals(15.0, prog.promedio, 1e-9)

        val (proy, pendientes) = Calculators.calcularNotaProyectada(comps, "202620")
        assertEquals(3.0, proy!!, 1e-9)
        assertEquals(3, pendientes.size)

        // Real numeric zero grade on PARCIAL must count toward evaluated weight
        val realZeroComps = listOf(
            ComponenteDetalle(nombre = "EP1", peso = 20.0, nota = 15.0),
            ComponenteDetalle(nombre = "PARCIAL", peso = 30.0, puntajeObtenido = 0.0, score = 0.0, gradeOficial = "0")
        )
        val realZeroProg = Calculators.progreso(realZeroComps, "202620")!!
        assertEquals(3.0, realZeroProg.acumulado, 1e-9)
        assertEquals(50.0, realZeroProg.pesoEvaluado, 1e-9)
        assertEquals(6.0, realZeroProg.promedio, 1e-9)
    }

    @Test fun parentPlaceholderWithSubcomponentsAndCourseScaledSubWeightsWork() {
        val comps = listOf(
            ComponenteDetalle(
                nombre = "EP1",
                peso = 20.0,
                puntajeObtenido = 0.0,
                score = 0.0,
                gradeOficial = "",
                hasSubComponents = true,
                subcomponentes = listOf(
                    com.example.upaos.data.model.SubComponente(nombre = "Práctica 1", peso = 10.0, nota = 16.0),
                    com.example.upaos.data.model.SubComponente(nombre = "Práctica 2", peso = 10.0, nota = "SN")
                )
            ),
            ComponenteDetalle(
                nombre = "PARCIAL",
                peso = 30.0,
                nota = null,
                hasSubComponents = true,
                subcomponentes = listOf(
                    com.example.upaos.data.model.SubComponente(nombre = "Oral", peso = null, porcentajeLogrado = 70.0, nota = 14.0),
                    com.example.upaos.data.model.SubComponente(nombre = "Escrito", peso = null, nota = null)
                )
            )
        )
        // EP1: Práctica 1 = 10/20 of EP1 = 10% of course -> 16 * 0.10 = 1.60
        // PARCIAL: Oral has no explicit weight (70% was achieved pct for 14/20, ignored) -> equal 50% of 30% = 15% -> 14 * 0.15 = 2.10
        val prog = Calculators.progreso(comps, "202620")!!
        assertEquals(3.70, prog.acumulado, 1e-9)
        assertEquals(25.0, prog.pesoEvaluado, 1e-9)
        assertEquals(14.8, prog.promedio, 1e-9)
    }

    @Test fun arquitecturaDeSistemas774Vs1335UnifiedBetweenCursosAndDetalle() {
        // Reproducción del caso 7.74 vs 13.35:
        // 1) Con 58% evaluado: EP1(20%)=13.35 (2.67) + Parcial(30%)=13.50 (4.05) + Avance(8%)=12.75 (1.02) => Acumulado = 7.74
        //    (El promedio parcial normalizado sería 7.74 / 0.58 = 13.3448... ≈ 13.35)
        // 2) Además con filas extra de Asistencia y Nota Final (antes no excluidas) que podían inflar a ~13.35
        val archComps = listOf(
            ComponenteDetalle(nombre = "EP1", peso = 20.0, nota = 13.35),
            ComponenteDetalle(nombre = "PARCIAL", peso = 30.0, nota = 13.50),
            ComponenteDetalle(nombre = "Avance Proyecto", peso = 8.0, nota = 12.75),
            ComponenteDetalle(nombre = "EP2", peso = 12.0, nota = "SN"),
            ComponenteDetalle(nombre = "FINAL", peso = 30.0, nota = null),
            ComponenteDetalle(nombre = "Asistencia", peso = 0.0, nota = 20.0),
            ComponenteDetalle(nombre = "NOTA FINAL", peso = null, nota = 18.70)
        )
        val course = CourseGrade(nombre = "Arquitectura de Sistemas", componentes = archComps)

        val detalleProgreso = Calculators.progreso(archComps, "202620")!!
        val acumuladoCursos = calcularAcumuladoCurso(course, notaProyectada = null, periodo = "202620")!!

        // Cursos ("Promedio") y Detalle ("Acumulado") deben dar exactamente 7.74
        assertEquals(7.74, detalleProgreso.acumulado, 1e-9)
        assertEquals(7.74, acumuladoCursos, 1e-9)
        assertEquals(58.0, detalleProgreso.pesoEvaluado, 1e-9)
        assertEquals("7.74", com.example.upaos.ui.grades.formatCourseAverage(acumuladoCursos))
        assertEquals(13.344827586, detalleProgreso.promedio, 1e-6)
    }

    @Test fun multipleCoursesWithDistinctWeightsStayConsistentBetweenCursosAndDetalle() {
        val c1 = CourseGrade(
            nombre = "Curso A (20/30/20/30)",
            componentes = listOf(
                ComponenteDetalle(nombre = "EP1", peso = 20.0, nota = 11.0),
                ComponenteDetalle(nombre = "PARCIAL", peso = 30.0, nota = 11.0),
                ComponenteDetalle(nombre = "EP2", peso = 20.0, nota = "SN"),
                ComponenteDetalle(nombre = "FINAL", peso = 30.0, nota = null)
            )
        )
        val c2 = CourseGrade(
            nombre = "Curso B (10/40/10/40)",
            componentes = listOf(
                ComponenteDetalle(nombre = "EP1", peso = 10.0, nota = 18.0),
                ComponenteDetalle(nombre = "PARCIAL", peso = 40.0, nota = 15.0),
                ComponenteDetalle(nombre = "EP2", peso = 10.0, nota = null),
                ComponenteDetalle(nombre = "FINAL", peso = 40.0, nota = null)
            )
        )
        val c3 = CourseGrade(
            nombre = "Curso C (25/25/25/25)",
            componentes = listOf(
                ComponenteDetalle(nombre = "Evaluación 1", peso = 25.0, nota = 16.0),
                ComponenteDetalle(nombre = "Evaluación 2", peso = 25.0, nota = 14.0),
                ComponenteDetalle(nombre = "Evaluación 3", peso = 25.0, nota = 12.0),
                ComponenteDetalle(nombre = "Evaluación 4", peso = 25.0, nota = null)
            )
        )
        val expected = listOf(
            c1 to 5.50,
            c2 to 7.80,   // 18*0.10 + 15*0.40 = 1.8 + 6.0 = 7.80
            c3 to 10.50   // (16+14+12)*0.25 = 10.50
        )
        for ((course, expectedAccum) in expected) {
            val fromDetalle = Calculators.progreso(course.componentes, "202620")!!.acumulado
            val fromCursos = calcularAcumuladoCurso(course, null, "202620")!!
            assertEquals(expectedAccum, fromDetalle, 1e-9)
            assertEquals(expectedAccum, fromCursos, 1e-9)
        }
    }
}
