package com.example.upaos.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.repeatOnLifecycle
import com.example.upaos.data.model.*
import com.example.upaos.ui.theme.LocalUpaoDarkTheme
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun Freshness(updated: Long?, current: Boolean, loading: Boolean = false) {
    val palette = todayPalette(LocalUpaoDarkTheme.current)
    if (loading) {
        Text("Actualizando…", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
    } else if (!current && updated != null) {
        Text(
            "Última actualización: " + SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(updated)),
            style = MaterialTheme.typography.labelSmall,
            color = palette.textSecondary
        )
    }
}

data class NextClass(val name: String, val room: String, val start: Long, val end: Long)

fun nextClass(courses: List<HorarioCurso>, now: Long): NextClass? {
    val zone = TimeZone.getTimeZone("America/Lima")
    val calendar = Calendar.getInstance(zone).apply { timeInMillis = now }
    val today = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7
    fun minutes(text: String?): Int? {
        val parts = text?.split(':') ?: return null
        val h = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val m = parts.getOrNull(1)?.toIntOrNull() ?: return null
        return if (h in 0..23 && m in 0..59) h * 60 + m else null
    }
    return courses.flatMap { course ->
        course.bloques.mapNotNull { block ->
            val day = block.dia?.takeIf { it in 0..6 } ?: return@mapNotNull null
            val startMinutes = minutes(block.horaInicio) ?: return@mapNotNull null
            val endMinutes = minutes(block.horaFin) ?: return@mapNotNull null
            val start = (calendar.clone() as Calendar).apply {
                add(Calendar.DATE, (day - today + 7) % 7)
                set(Calendar.HOUR_OF_DAY, startMinutes / 60)
                set(Calendar.MINUTE, startMinutes % 60)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            var end = start.timeInMillis + (endMinutes - startMinutes) * 60_000L
            if (end <= now) {
                start.add(Calendar.DATE, 7)
                end += 7 * 86_400_000L
            }
            NextClass(course.displayNombre, block.aula?.trim()?.takeIf { it.isNotEmpty() } ?: "Virtual", start.timeInMillis, end)
        }
    }.minByOrNull { it.start }
}

@Composable
fun TodayContent(
    token: String,
    dashboard: DashboardViewModel,
    usuario: String?,
    onSchedule: () -> Unit,
    onConnectCanvas: () -> Unit,
    onNotifications: () -> Unit,
    openAgenda: Boolean = false,
    onAgendaOpened: () -> Unit = {}
) {
    val palette = todayPalette(LocalUpaoDarkTheme.current)

    if (usuario.isNullOrBlank()) {
        Column(
            Modifier
                .fillMaxSize()
                .background(palette.background)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("No se pudo recuperar la sesión", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
            Text("Vuelve a iniciar sesión desde el menú para cargar tu información.", color = palette.textSecondary)
        }
        return
    }
    val tasks = rememberTodayTasks(usuario, token)
    var allTasks by rememberSaveable(usuario, token) { mutableStateOf(false) }
    val shared by dashboard.state.collectAsState()
    LaunchedEffect(openAgenda) { if (openAgenda) onAgendaOpened() }
    val timetable = shared.schedules[shared.currentPeriod]
    val notices = shared.notices?.notificaciones ?: emptyList()
    val scheduleTime = shared.times[dashboard.scheduleCacheKey(shared.currentPeriod)]
    val noticesTime = shared.times["notificaciones_$usuario"]
    val scheduleFresh = dashboard.scheduleCacheKey(shared.currentPeriod) in shared.fresh
    val noticesFresh = "notificaciones_$usuario" in shared.fresh
    val error = listOfNotNull(
        shared.errors[dashboard.scheduleCacheKey(shared.currentPeriod)],
        shared.errors["notificaciones_$usuario"]
    ).joinToString("\n").takeIf { it.isNotBlank() }
    val semanaEtiqueta = shared.week?.takeIf { it.configurada && it.fueraDeCiclo != true }?.etiqueta

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
            while (true) {
                now = System.currentTimeMillis()
                val untilMidnight = campusDay(now).last + 1 - System.currentTimeMillis()
                kotlinx.coroutines.delay(minOf(60_000L, untilMidnight).coerceAtLeast(1L))
                now = System.currentTimeMillis()
            }
        }
    }
    val dayClasses = remember(timetable, now) { classesToday(timetable?.listaCursos ?: emptyList(), now) }
    val dayNotices = remember(notices, now) { notificationsToday(notices, now) }
    var showDetails by remember { mutableStateOf(false) }

    if (allTasks) {
        AllPendingTasks(tasks, now, onConnectCanvas) { allTasks = false }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        semanaEtiqueta?.let { label ->
            item { TodayWeekBanner(label = label) }
        }
        item {
            TodayHero(
                now = now,
                classes = if (timetable != null) dayClasses.size else null,
                tasks = tasks.snapshot?.takeUnless { it.partial }?.let {
                    com.example.upaos.data.agenda.visiblePendingTasks(it.tasks, java.time.Instant.ofEpochMilli(now)).size
                },
                news = if (noticesTime != null) dayNotices.size else null,
                onSchedule = onSchedule,
                onTasks = { allTasks = true },
                onNotifications = onNotifications
            )
        }
        error?.let { detail ->
            item {
                Surface(
                    color = palette.surface,
                    border = BorderStroke(1.dp, palette.border),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Actualización pendiente", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                        Text(
                            "Algunos datos no se pudieron consultar. Mostramos lo último guardado.",
                            fontSize = 12.sp,
                            color = palette.textSecondary
                        )
                        TextButton(onClick = { showDetails = !showDetails }) {
                            Text(if (showDetails) "Ocultar detalle" else "Ver detalle", color = palette.accentBlue)
                        }
                        if (showDetails) Text(detail, fontSize = 11.5.sp, color = palette.textSecondary)
                    }
                }
            }
        }
        if (dayClasses.isNotEmpty()) {
            item { TodaySchedule(dayClasses, timetable != null, now, scheduleTime, scheduleFresh, onSchedule) }
        }
        if (dayNotices.isNotEmpty()) {
            item {
                TodayNewsCard(
                    items = dayNotices,
                    now = now,
                    updated = noticesTime,
                    current = noticesFresh,
                    onNotifications = onNotifications
                )
            }
        }
        item { PendingTasksPreview(tasks, now, onConnectCanvas) { allTasks = true } }
    }
}
