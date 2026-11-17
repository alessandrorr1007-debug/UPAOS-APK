package com.example.upaos.ui.horario

import com.example.upaos.data.model.HorarioBloque
import com.example.upaos.data.model.HorarioCurso
import com.example.upaos.data.upao.AcademicCalendar
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal fun scheduleCalendar(date: Long): Calendar =
    Calendar.getInstance(TimeZone.getTimeZone("America/Lima")).apply { timeInMillis = date }

internal fun scheduleDateStart(date: Long): Long = scheduleCalendar(date).apply {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

internal fun scheduleMonthDays(date: Long): List<Long> {
    val calendar = scheduleCalendar(scheduleDateStart(date)).apply { set(Calendar.DAY_OF_MONTH, 1) }
    return List(calendar.getActualMaximum(Calendar.DAY_OF_MONTH)) {
        calendar.timeInMillis.also { calendar.add(Calendar.DAY_OF_MONTH, 1) }
    }
}

internal fun moveScheduleMonth(date: Long, months: Int): Long =
    scheduleCalendar(date).apply { add(Calendar.MONTH, months) }.timeInMillis

/** Returns the 7 day-start timestamps [Monday .. Sunday] for the week containing [date]. */
internal fun scheduleWeekDays(date: Long): List<Long> {
    val calendar = scheduleCalendar(scheduleDateStart(date))
    val offsetFromMonday = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Mon=0 .. Sun=6
    calendar.add(Calendar.DAY_OF_MONTH, -offsetFromMonday)
    return List(7) {
        calendar.timeInMillis.also { calendar.add(Calendar.DAY_OF_MONTH, 1) }
    }
}

internal fun moveScheduleWeek(date: Long, weeks: Int): Long =
    scheduleCalendar(date).apply { add(Calendar.DAY_OF_MONTH, weeks * 7) }.timeInMillis

internal fun parseTimeMinutes(value: String?): Int? {
    val raw = value?.trim()?.takeIf { it.isNotBlank() } ?: return null
    val upper = raw.uppercase(Locale.ROOT)
    val isPm = upper.contains("PM") || upper.contains("P.M") || upper.contains("P. M")
    val isAm = upper.contains("AM") || upper.contains("A.M") || upper.contains("A. M")
    val match = Regex("""(\d{1,2}):(\d{2})""").find(upper) ?: return null
    var hour = match.groupValues[1].toIntOrNull() ?: return null
    val minute = match.groupValues[2].toIntOrNull() ?: return null
    if (minute !in 0..59) return null
    if (isPm && hour in 1..11) hour += 12
    else if (isAm && hour == 12) hour = 0
    return if (hour in 0..23) hour * 60 + minute else null
}

internal data class ScheduleDayClass(val curso: HorarioCurso, val bloque: HorarioBloque)

internal fun classesForScheduleDate(
    courses: List<HorarioCurso>,
    date: Long,
    term: String? = null
): List<ScheduleDayClass> {
    if (term != null) {
        val semanaInfo = AcademicCalendar.calcularSemana(term, Date(date))
        if (semanaInfo.fueraDeCiclo == true) {
            return emptyList()
        }
    }
    val weekday = (scheduleCalendar(date).get(Calendar.DAY_OF_WEEK) + 5) % 7
    return courses.flatMap { course ->
        course.bloques.filter { it.dia == weekday }.map { ScheduleDayClass(course, it) }
    }.sortedBy { parseTimeMinutes(it.bloque.horaInicio) ?: parseTimeMinutes(it.bloque.horaInicio12h) ?: Int.MAX_VALUE }
}

internal data class WeeklyScheduleBlock(
    val dayIndex: Int, // 0=Mon .. 6=Sun
    val dayDate: Long,
    val curso: HorarioCurso,
    val bloque: HorarioBloque,
    val startMinutes: Int,
    val endMinutes: Int,
    val laneIndex: Int = 0,
    val laneCount: Int = 1
)

internal fun classesForScheduleWeek(
    courses: List<HorarioCurso>,
    anchorDate: Long,
    term: String? = null
): List<WeeklyScheduleBlock> {
    val weekDays = scheduleWeekDays(anchorDate)
    val result = mutableListOf<WeeklyScheduleBlock>()

    weekDays.forEachIndexed { dayIndex, dayDate ->
        val dayClasses = classesForScheduleDate(courses, dayDate, term)
        val rawBlocks = dayClasses.mapNotNull { scheduled ->
            val start = parseTimeMinutes(scheduled.bloque.horaInicio)
                ?: parseTimeMinutes(scheduled.bloque.horaInicio12h)
                ?: return@mapNotNull null
            val parsedEnd = parseTimeMinutes(scheduled.bloque.horaFin)
                ?: parseTimeMinutes(scheduled.bloque.horaFin12h)
            val end = if (parsedEnd != null && parsedEnd > start) parsedEnd else (start + 50).coerceAtMost(23 * 60 + 59)
            WeeklyScheduleBlock(
                dayIndex = dayIndex,
                dayDate = dayDate,
                curso = scheduled.curso,
                bloque = scheduled.bloque,
                startMinutes = start,
                endMinutes = end
            )
        }
        result += assignOverlappingLanes(rawBlocks)
    }
    return result
}

internal fun assignOverlappingLanes(blocks: List<WeeklyScheduleBlock>): List<WeeklyScheduleBlock> {
    if (blocks.isEmpty()) return emptyList()
    val sorted = blocks.sortedWith(compareBy<WeeklyScheduleBlock> { it.startMinutes }.thenBy { it.endMinutes })
    val clusters = mutableListOf<MutableList<WeeklyScheduleBlock>>()
    var currentCluster = mutableListOf<WeeklyScheduleBlock>()
    var clusterEnd = -1

    for (block in sorted) {
        if (currentCluster.isEmpty() || block.startMinutes < clusterEnd) {
            currentCluster.add(block)
            clusterEnd = maxOf(clusterEnd, block.endMinutes)
        } else {
            clusters.add(currentCluster)
            currentCluster = mutableListOf(block)
            clusterEnd = block.endMinutes
        }
    }
    if (currentCluster.isNotEmpty()) clusters.add(currentCluster)

    val output = mutableListOf<WeeklyScheduleBlock>()
    for (cluster in clusters) {
        val laneEnds = mutableListOf<Int>()
        val assigned = cluster.map { item ->
            var lane = laneEnds.indexOfFirst { it <= item.startMinutes }
            if (lane == -1) {
                lane = laneEnds.size
                laneEnds.add(item.endMinutes)
            } else {
                laneEnds[lane] = item.endMinutes
            }
            item to lane
        }
        val totalLanes = laneEnds.size.coerceAtLeast(1)
        assigned.forEach { (item, lane) ->
            output.add(item.copy(laneIndex = lane, laneCount = totalLanes))
        }
    }
    return output
}
