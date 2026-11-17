package com.example.upaos.data.upao

import com.example.upaos.data.model.ComponenteDetalle
import com.example.upaos.data.model.PromedioCursoItem
import java.util.Locale
import kotlin.math.roundToInt

object Calculators {

    private val percentRegex = Regex("""(\d+(?:[.,]\d+)?)\s*%""")
    private val pendingMarkers = setOf("SN", "SP", "PE", "NR", "PEND", "PENDIENTE", "-", "--", "NULL", "NONE", "N/A")
    private val exactExcludedLabels = setOf(
        "PROMEDIO", "PROM", "PROM.", "TOTAL", "TOT", "SUBTOTAL",
        "NOTA FINAL", "CALIFICACION FINAL", "CALIFICACIÓN FINAL",
        "ACUMULADO", "PF", "NF", "ASIST", "ASISTENCIA"
    )

    private fun hasPendingMarker(raw: Any?): Boolean {
        val s = raw?.toString()?.trim()?.uppercase(Locale.ROOT) ?: return false
        return s.isNotEmpty() && s in pendingMarkers
    }

    private fun isExcludedLabels(vararg fields: String?): Boolean {
        val normalizedFields = fields
            .mapNotNull { it?.trim()?.takeIf(String::isNotEmpty)?.uppercase(Locale.ROOT) }
        if (normalizedFields.isEmpty()) return false
        if (normalizedFields.any { it in exactExcludedLabels }) return true

        val combined = normalizedFields.joinToString(" ")
        val isAttendance = listOf(
            "ASISTENCIA", "INASISTENCIA", "ATTENDANCE", "FALTAS", "INHABILIT"
        ).any { combined.contains(it) }
        if (isAttendance) return true

        return listOf(
            "PROMEDIO FINAL", "PROMEDIO PARCIAL", "PROMEDIO GENERAL",
            "PROMEDIO DEL CURSO", "PROMEDIO DE CURSO", "PROMEDIO PONDERADO",
            "NOTA FINAL", "CALIFICACION FINAL", "CALIFICACIÓN FINAL",
            "ACUMULADO", "SUBTOTAL", "TOTAL CURSO", "TOTAL DEL CURSO", "TOTAL GENERAL"
        ).any { phrase -> normalizedFields.any { it.contains(phrase) } || combined.contains(phrase) }
    }

    private fun esComponenteExcluido(componente: ComponenteDetalle): Boolean =
        isExcludedLabels(
            componente.nombre,
            componente.description,
            componente.codigo,
            componente.componente
        )

    private fun esSubcomponenteExcluido(sub: com.example.upaos.data.model.SubComponente): Boolean =
        isExcludedLabels(
            sub.nombre,
            sub.description,
            sub.codigo,
            sub.componente
        )

    private fun parseWeightOrZero(raw: Any?): Double? {
        val text = raw?.toString()?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val hasPercentSign = "%" in text
        val cleaned = text.replace("%", "").replace(",", ".").trim()
        val parsed = toDouble(cleaned)?.takeIf { it.isFinite() && it >= 0.0 && it <= 100.0 } ?: return null
        return if (!hasPercentSign && parsed > 0.0 && parsed < 1.0) parsed * 100.0 else parsed
    }

    private fun parseWeightValue(raw: Any?): Double? =
        parseWeightOrZero(raw)?.takeIf { it > 0.0 }

    private fun recognizedDefaultWeight(componente: ComponenteDetalle, periodo: String? = null): Double? {
        val texto = listOfNotNull(
            componente.nombre,
            componente.description,
            componente.codigo,
            componente.componente
        ).joinToString(" ").uppercase(Locale.ROOT)

        val historico = periodo?.trim() == "202520"
        return when {
            texto.contains("FINAL") || texto.contains("EVF") -> if (historico) 25.0 else 30.0
            texto.contains("PARCIAL") || texto.contains("EVP") -> if (historico) 20.0 else 30.0
            texto.contains("EP2") || texto.contains("PROCESO 2") || texto.contains("PERMANENTE 2") || texto.contains("EP 2") -> if (historico) 30.0 else 20.0
            texto.contains("EP1") || texto.contains("PROCESO 1") || texto.contains("PERMANENTE 1") || texto.contains("EP 1") -> if (historico) 25.0 else 20.0
            else -> null
        }
    }

    private fun normalizedGrade(rawNota: Any?, sobreRaw: Any?, gradeOficial: Any? = null): Double? {
        if (hasPendingMarker(rawNota) || hasPendingMarker(gradeOficial)) return null
        val raw = toDouble(rawNota)?.takeIf { it.isFinite() && it >= 0.0 } ?: return null
        val sobre = toDouble(sobreRaw)?.takeIf { it.isFinite() && it > 0.0 }
        if (sobre != null && kotlin.math.abs(sobre - 20.0) > 1e-6 && raw <= sobre + 1e-6) {
            return (raw / sobre * 20.0).coerceIn(0.0, 20.0)
        }
        return raw.takeIf { it in 0.0..20.0 }
    }

    private fun hasExplicitDirectWeight(componente: ComponenteDetalle): Boolean =
        listOf(
            componente.peso,
            componente.weight,
            componente.percentage,
            componente.porcentaje,
            componente.percent,
            componente.porcentajeComponente,
            componente.pesoComponente
        ).any { parseWeightOrZero(it) != null } ||
            listOfNotNull(componente.nombre, componente.description, componente.componente)
                .any { percentRegex.containsMatchIn(it) }

    /**
     * Resuelve el peso real de un componente.
     * Excluye filas de asistencia y resúmenes, respeta pesos explícitos 0% (sin fallback),
     * y prioriza los pesos reales reportados por Banner sobre los valores estándar del periodo
     * (25/20/30/25 en 202520; 20/30/20/30 en los demás).
     */
    fun pesoComponente(componente: ComponenteDetalle, periodo: String? = null): Double? {
        if (esComponenteExcluido(componente)) return null

        val directExplicit = listOf(
            componente.peso,
            componente.weight,
            componente.percentage,
            componente.porcentaje,
            componente.percent,
            componente.porcentajeComponente,
            componente.pesoComponente
        ).firstNotNullOfOrNull(::parseWeightOrZero)
        if (directExplicit != null) return directExplicit.takeIf { it > 0.0 }

        val textWeight = listOfNotNull(componente.nombre, componente.description, componente.componente)
            .firstNotNullOfOrNull { label ->
                percentRegex.find(label)?.groupValues?.getOrNull(1)?.let(::parseWeightOrZero)
            }
        if (textWeight != null) return textWeight.takeIf { it > 0.0 }

        val reconocido = recognizedDefaultWeight(componente, periodo)
        if (reconocido != null) return reconocido

        val achievedCandidate = parseWeightValue(componente.porcentajeLogrado)
        if (achievedCandidate != null) {
            val grade20 = normalizedGrade(componente.displayNota, componente.puntajeSobre, componente.gradeOficial)
            val looksLikeAchievedPct = grade20 != null &&
                kotlin.math.abs((grade20 / 20.0 * 100.0) - achievedCandidate) < 0.5
            if (!looksLikeAchievedPct) return achievedCandidate
        }

        return null
    }

    private fun resolvedComponentWeights(
        componentes: List<ComponenteDetalle>,
        periodo: String? = null
    ): List<Double?> {
        val raw = componentes.mapIndexed { idx, comp ->
            val resolved = pesoComponente(comp, periodo)
            if (resolved != null) {
                resolved
            } else if (!esComponenteExcluido(comp) &&
                !hasExplicitDirectWeight(comp) &&
                recognizedDefaultWeight(comp, periodo) == null
            ) {
                // Si porcentajeLogrado coincidió numéricamente con grade/20*100 pero completa exactamente el 100%
                // de los pesos del curso, aceptarlo como peso real del componente.
                val candidate = parseWeightValue(comp.porcentajeLogrado)
                if (candidate != null) {
                    val otherKnownSum = componentes.indices
                        .filter { it != idx }
                        .mapNotNull { pesoComponente(componentes[it], periodo) }
                        .sum()
                    if (kotlin.math.abs((otherKnownSum + candidate) - 100.0) <= 0.5) candidate else null
                } else null
            } else null
        }

        val nonNull = raw.filterNotNull()
        if (nonNull.isEmpty()) return raw

        val allUnitFraction = nonNull.all { it <= 1.0 } && nonNull.sum() <= 1.0 + 1e-6
        val scaled = if (allUnitFraction) {
            raw.map { it?.let { w -> w * 100.0 } }
        } else {
            raw
        }

        val total = scaled.filterNotNull().sum()
        return if (total > 100.0 + 1e-6) {
            val factor = 100.0 / total
            scaled.map { it?.let { w -> w * factor } }
        } else {
            scaled
        }
    }

    private data class WeightedGradeSample(val nota20: Double, val pesoPct: Double)

    private fun collectGradedSamples(
        componentes: List<ComponenteDetalle>,
        periodo: String? = null
    ): List<WeightedGradeSample> {
        val weights = resolvedComponentWeights(componentes, periodo)
        val samples = mutableListOf<WeightedGradeSample>()

        componentes.forEachIndexed { index, componente ->
            if (esComponenteExcluido(componente)) return@forEachIndexed
            val compPeso = weights[index]?.takeIf { it.isFinite() && it > 0.0 && it <= 100.0 } ?: return@forEachIndexed
            val rawDirectNota = normalizedGrade(componente.displayNota, componente.puntajeSobre, componente.gradeOficial)
            val activeSubs = componente.subcomponentes.filterNot(::esSubcomponenteExcluido)
            val hasPositiveSub = activeSubs.any {
                (normalizedGrade(it.displayNota, it.puntajeSobre) ?: 0.0) > 0.0
            }
            val gradeOficialClean = componente.gradeOficial?.toString()?.trim()?.uppercase(Locale.ROOT)
            val directNota = if (
                componente.hasSubComponents &&
                activeSubs.isNotEmpty() &&
                rawDirectNota == 0.0 &&
                (gradeOficialClean.isNullOrEmpty() || gradeOficialClean in pendingMarkers) &&
                hasPositiveSub
            ) {
                null
            } else {
                rawDirectNota
            }

            if (directNota != null) {
                samples.add(WeightedGradeSample(nota20 = directNota, pesoPct = compPeso))
            } else if (activeSubs.isNotEmpty()) {
                val subRawWeights = activeSubs.map { sub ->
                    val explicitSub = listOf(
                        sub.peso, sub.weight, sub.percentage, sub.porcentaje,
                        sub.percent, sub.porcentajeComponente, sub.pesoComponente
                    ).firstNotNullOfOrNull(::parseWeightOrZero)
                    if (explicitSub != null) {
                        explicitSub
                    } else {
                        val fromText = sub.displayNombre.let { label ->
                            percentRegex.find(label)?.groupValues?.getOrNull(1)?.let(::parseWeightOrZero)
                        }
                        if (fromText != null) {
                            fromText
                        } else {
                            val achievedCandidate = parseWeightValue(sub.porcentajeLogrado)
                            val subGrade20 = normalizedGrade(sub.displayNota, sub.puntajeSobre)
                            val looksLikeAchieved = achievedCandidate != null && subGrade20 != null &&
                                kotlin.math.abs((subGrade20 / 20.0 * 100.0) - achievedCandidate) < 0.5
                            if (looksLikeAchieved) null else achievedCandidate
                        }
                    }
                }
                val weightedIndices = activeSubs.indices.filter { (subRawWeights[it] ?: 0.0) > 0.0 }
                val zeroWeightedIndices = activeSubs.indices.filter { subRawWeights[it] == 0.0 }.toSet()
                val activeSubCount = activeSubs.size - zeroWeightedIndices.size
                if (activeSubCount <= 0) return@forEachIndexed

                val allHaveWeights = subRawWeights.indices.all { it in zeroWeightedIndices || subRawWeights[it] != null }
                val subTotalWeight = weightedIndices.sumOf { subRawWeights[it] ?: 0.0 }
                val nullCount = activeSubs.indices.count { it !in zeroWeightedIndices && subRawWeights[it] == null }
                val remainingShare = if (subTotalWeight in 0.0..100.0) (100.0 - subTotalWeight) / nullCount.coerceAtLeast(1) else 0.0
                val equalShare = 100.0 / activeSubCount

                activeSubs.forEachIndexed { subIdx, sub ->
                    if (subIdx in zeroWeightedIndices) return@forEachIndexed
                    val subNota = normalizedGrade(sub.displayNota, sub.puntajeSobre) ?: return@forEachIndexed
                    val subSharePct = when {
                        weightedIndices.isNotEmpty() -> {
                            val w = subRawWeights[subIdx]
                            when {
                                w == null -> remainingShare
                                allHaveWeights && subTotalWeight > 0.0 -> w / subTotalWeight * 100.0
                                subTotalWeight > 0.0 && (subTotalWeight > 100.0 + 1e-6 || subTotalWeight <= 1.0 + 1e-6) ->
                                    w / subTotalWeight * 100.0
                                else -> w
                            }
                        }
                        else -> equalShare
                    }
                    val effectivePeso = compPeso * subSharePct / 100.0
                    if (effectivePeso > 0.0) {
                        samples.add(WeightedGradeSample(nota20 = subNota, pesoPct = effectivePeso))
                    }
                }
            }
        }
        return samples
    }

    data class Progreso(val acumulado: Double, val pesoEvaluado: Double) {
        val promedio: Double get() = acumulado * 100.0 / pesoEvaluado
        val minimoAcumulado: Double get() = com.example.upaos.domain.academic.AcademicRules.PASS_GRADE * pesoEvaluado / 100.0
        val enRitmoAprobatorio: Boolean get() = acumulado + 1e-9 >= minimoAcumulado
    }

    fun progreso(componentes: List<ComponenteDetalle>, periodo: String? = null): Progreso? {
        val samples = collectGradedSamples(componentes, periodo)
        if (samples.isEmpty()) return null
        val puntos = samples.sumOf { it.nota20 * it.pesoPct / 100.0 }
        val evaluado = samples.sumOf { it.pesoPct }
        return if (evaluado > 0.0 && evaluado <= 100.0 + 1e-9) Progreso(puntos, evaluado) else null
    }

    fun progresoCurso(curso: com.example.upaos.data.model.CourseGrade, periodo: String? = null): Progreso? =
        progreso(curso.componentes.ifEmpty {
            listOf(ComponenteDetalle(nombre = "EP1", nota = curso.displayEp1.nota),
                ComponenteDetalle(nombre = "EP2", nota = curso.displayEp2.nota))
        }, periodo)

    fun toDouble(v: Any?): Double? {
        if (v == null) return null
        return when (v) {
            is Number -> v.toDouble()
            is String -> {
                val s = v.trim()
                if (s.isEmpty() || s.uppercase(Locale.ROOT) in pendingMarkers) null
                else (if (',' in s && '.' !in s) s.replace(',', '.') else s).toDoubleOrNull()
            }
            else -> null
        }
    }

    /**
     * Calcula la nota actual progresiva de un curso:
     * Promedio ponderado de los componentes que YA tienen nota, normalizado por la suma de sus pesos.
     * Ejemplo: EP1=15 (20%) y Parcial=14 (30%) -> (15*20 + 14*30) / (20+30) = 14.4
     */
    fun calcularNotaActual(componentes: List<ComponenteDetalle>, periodo: String? = null): Double? {
        val prog = progreso(componentes, periodo) ?: return null
        return (prog.promedio * 100.0).roundToInt() / 100.0
    }

    /**
     * Calcula la nota proyectada (acumulada sobre el total del curso) y lista los componentes pendientes.
     * Si el examen final ya tiene nota, la proyección no aplica.
     */
    fun calcularNotaProyectada(componentes: List<ComponenteDetalle>, periodo: String? = null): Pair<Double?, List<String>> {
        val compFinal = componentes.firstOrNull { c ->
            if (esComponenteExcluido(c)) return@firstOrNull false
            val txt = listOfNotNull(c.nombre, c.description, c.codigo).joinToString(" ").uppercase(Locale.ROOT)
            txt.contains("FINAL") || txt.contains("EVF")
        }

        if (compFinal != null && normalizedGrade(compFinal.displayNota, compFinal.puntajeSobre, compFinal.gradeOficial) != null) {
            return Pair(null, emptyList())
        }

        val weights = resolvedComponentWeights(componentes, periodo)
        val pendientes = mutableListOf<String>()
        componentes.forEachIndexed { index, c ->
            if (esComponenteExcluido(c)) return@forEachIndexed
            val peso = weights[index] ?: return@forEachIndexed
            if (peso <= 0.0) return@forEachIndexed
            val hasDirect = normalizedGrade(c.displayNota, c.puntajeSobre, c.gradeOficial) != null
            val hasSub = c.subcomponentes
                .filterNot(::esSubcomponenteExcluido)
                .any { normalizedGrade(it.displayNota, it.puntajeSobre) != null }
            if (!hasDirect && !hasSub) {
                pendientes.add(c.displayNombre)
            }
        }

        val prog = progreso(componentes, periodo) ?: return Pair(null, emptyList())
        val proyectada = (prog.acumulado * 100.0).roundToInt() / 100.0
        return Pair(proyectada, pendientes)
    }

    /**
     * Cálculo oficial del Promedio Ponderado Semestral (PPS) de la UPAO:
     * PPS = Σ(redondear(nota) * creditos) / Σ(creditos)
     * Cada nota se redondea a entero (>= .5 sube) antes de multiplicar por los créditos.
     */
    fun calcularPps(cursos: List<PromedioCursoItem>): Double? {
        val validos = mutableListOf<Pair<Double, Int>>()

        for (c in cursos) {
            val nota = c.nota?.toDouble()
            val cred = c.creditos
            if (nota != null && cred != null && cred > 0) {
                validos.add(nota to cred)
            }
        }

        if (validos.isEmpty()) return null

        var sumaPonderada = 0
        var totalCreditos = 0

        for ((nota, cred) in validos) {
            val notaEntera = (nota + 0.5).toInt()
            sumaPonderada += notaEntera * cred
            totalCreditos += cred
        }

        if (totalCreditos <= 0) return null

        val pps = sumaPonderada.toDouble() / totalCreditos
        return (pps * 10000.0).roundToInt() / 10000.0
    }

    /**
     * Valida si un código de periodo corresponde a un ciclo regular (termina en 10 o 20).
     * Los códigos terminados en 90 son idiomas / extraordinarios.
     */
    fun esPeriodoRegular(code: String?): Boolean =
        com.example.upaos.domain.academic.AcademicRules.isRegular(code)

    fun detectarPeriodoActual(periodos: List<String>, fallback: String? = null): String =
        com.example.upaos.domain.academic.AcademicRules.latestAvailable(periodos, fallback)
}
