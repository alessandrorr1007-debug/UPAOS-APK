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
}
