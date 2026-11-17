package com.example.upaos.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.upaos.data.agenda.*
import com.example.upaos.data.api.CanvasApiClient
import com.example.upaos.data.api.TrustedServers
import com.example.upaos.data.local.TokenManager
import com.example.upaos.ui.theme.LocalUpaoDarkTheme
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import retrofit2.HttpException

internal class TodayTasksState {
    var snapshot by mutableStateOf<PendingTaskResult?>(null)
    var loading by mutableStateOf(true)
    var failure by mutableStateOf<TaskFailure?>(null)
    var refresh by mutableStateOf<() -> Unit>({})
}

internal enum class TaskFailure { CONFIGURATION, AUTHENTICATION, CONNECTION }

@Composable
internal fun rememberTodayTasks(user: String, login: String): TodayTasksState {
    val context = LocalContext.current.applicationContext
    val tokens = remember(context) { TokenManager(context) }
    val session = remember(user, login) { tokens.getSessionId() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val repository = remember { PendingTasksRepository(CanvasApiClient.pagedApiService) }
    val state = remember(user, login, session) { TodayTasksState() }

    LaunchedEffect(state, lifecycle) {
        var loadedToken: String? = null
        var updatedAt = 0L
        fun isCurrent() = session != null && tokens.getSavedUser() == user && tokens.getSessionId() == session
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var request: Job? = null
            fun load(force: Boolean) {
                if (!isCurrent() || request?.isActive == true) return
                val token = tokens.getCanvasToken(user)?.takeIf { it.isNotBlank() }
                if (token != loadedToken) {
                    state.snapshot = null
                    loadedToken = token
                    updatedAt = 0L
                }
                if (token == null) {
                    state.loading = false
                    state.failure = TaskFailure.CONFIGURATION
                    return
                }
                if (!force && android.os.SystemClock.elapsedRealtime() - updatedAt in 0 until 300_000 &&
                    state.snapshot?.partial == false && state.failure == null
                ) return
                state.snapshot = null
                state.loading = true
                state.failure = null
                request = launch {
                    fun canPublish() = isCurrent() && tokens.getCanvasToken(user) == token
                    try {
                        val result = repository.load(token)
                        if (canPublish()) {
                            state.snapshot = result
                            updatedAt = android.os.SystemClock.elapsedRealtime()
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        if (canPublish()) {
                            state.snapshot = null
                            state.failure = if (error is HttpException && error.code() in setOf(401, 403))
                                TaskFailure.AUTHENTICATION else TaskFailure.CONNECTION
                        }
                    } finally {
                        if (canPublish()) state.loading = false
                    }
                }
            }
            state.refresh = { load(true) }
            load(false)
            try {
                kotlinx.coroutines.awaitCancellation()
            } finally {
                state.refresh = {}
                state.loading = false
            }
        }
    }
    return state
}

private fun taskCountLabel(count: Int): String = when (count) {
    0 -> "Al día"
    1 -> "1 pendiente"
    else -> "$count tareas pendientes"
}

@Composable
internal fun PendingTasksPreview(
    state: TodayTasksState,
    nowMillis: Long,
    connect: () -> Unit,
    open: () -> Unit
) {
    val now = Instant.ofEpochMilli(nowMillis)
    val tasks = visiblePendingTasks(state.snapshot?.tasks.orEmpty(), now)
    val palette = todayPalette(LocalUpaoDarkTheme.current)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = palette.surface,
        border = BorderStroke(1.dp, palette.border),
        shadowElevation = if (palette.isDark) 0.dp else 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TasksHeaderRow(state = state, count = tasks.size, palette = palette)

            if (state.loading) {
                LinearProgressIndicator(
                    color = palette.accentBlue,
                    trackColor = palette.border,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp)
                        .clip(RoundedCornerShape(50))
                )
            }

            TaskStateNotice(state = state, count = tasks.size, palette = palette, connect = connect)

            tasks.take(3).forEach { task ->
                TaskCard(task = task, now = now, palette = palette)
            }

            if (state.snapshot != null || tasks.isNotEmpty()) {
                HorizontalDivider(color = palette.border, thickness = 1.dp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (state.snapshot != null) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(
                                    enabled = !state.loading,
                                    role = Role.Button,
                                    onClick = state.refresh
                                )
                                .testTag("today_refresh_tasks_button")
                                .padding(horizontal = 6.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = null,
                                tint = if (state.loading) palette.textMuted else palette.accentBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Actualizar tareas",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (state.loading) palette.textMuted else palette.accentBlue,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (state.snapshot != null && tasks.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(20.dp)
                                .background(palette.border)
                        )
                    }

                    if (tasks.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(role = Role.Button, onClick = open)
                                .testTag("today_view_all_tasks_button")
                                .padding(horizontal = 6.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally)
                        ) {
                            Text(
                                text = "Ver todas las tareas",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.accentBlue,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = palette.accentBlue,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun AllPendingTasks(
    state: TodayTasksState,
    nowMillis: Long,
    connect: () -> Unit,
    back: () -> Unit
) {
    BackHandler(onBack = back)
    val now = Instant.ofEpochMilli(nowMillis)
    val tasks = visiblePendingTasks(state.snapshot?.tasks.orEmpty(), now)
    val palette = todayPalette(LocalUpaoDarkTheme.current)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(role = Role.Button, onClick = back)
                    .testTag("today_back_from_tasks_button")
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = palette.accentBlue,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Volver a Hoy",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.accentBlue
                )
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = palette.surface,
                border = BorderStroke(1.dp, palette.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TasksHeaderRow(state = state, count = tasks.size, palette = palette)
                    if (state.loading) {
                        LinearProgressIndicator(
                            color = palette.accentBlue,
                            trackColor = palette.border,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.5.dp)
                                .clip(RoundedCornerShape(50))
                        )
                    }
                    TaskStateNotice(state = state, count = tasks.size, palette = palette, connect = connect)
                    if (state.snapshot != null) {
                        OutlinedButton(
                            onClick = state.refresh,
                            enabled = !state.loading,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, palette.border),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = palette.accentBlue
                            ),
                            modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Actualizar tareas", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        items(tasks.filter { it.due != null }, key = { it.key }) { task ->
            TaskCard(task = task, now = now, palette = palette)
        }

        val undated = tasks.filter { it.due == null }
        if (undated.isNotEmpty()) {
            item {
                Text(
                    text = "Sin fecha de entrega",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                )
            }
        }
        items(undated, key = { it.key }) { task ->
            TaskCard(task = task, now = now, palette = palette)
        }
    }
}

@Composable
private fun TasksHeaderRow(
    state: TodayTasksState,
    count: Int,
    palette: TodayPalette
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(palette.accentBlue),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.EventAvailable,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = "Tareas por hacer",
                fontSize = 15.5.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary
            )
            Text(
                text = "Actividades pendientes de tus cursos",
                fontSize = 12.sp,
                color = palette.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (state.failure == null && state.snapshot != null) {
            Surface(
                shape = RoundedCornerShape(50),
                color = palette.softBlueBg
            ) {
                Text(
                    text = taskCountLabel(count),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.softBlueText,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun TaskStateNotice(
    state: TodayTasksState,
    count: Int,
    palette: TodayPalette,
    connect: () -> Unit
) {
    when (state.failure) {
        TaskFailure.CONFIGURATION, TaskFailure.AUTHENTICATION -> {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = palette.innerSurface,
                border = BorderStroke(1.dp, palette.innerBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (state.failure == TaskFailure.CONFIGURATION)
                            "Conecta Canvas para consultar tus tareas."
                        else "Necesitas volver a conectar Canvas.",
                        fontSize = 13.sp,
                        color = palette.textPrimary
                    )
                    TextButton(
                        onClick = connect,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Configurar Canvas", color = palette.accentBlue, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        TaskFailure.CONNECTION -> {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = palette.innerSurface,
                border = BorderStroke(1.dp, palette.innerBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "No pudimos actualizar tus tareas.",
                        fontSize = 13.sp,
                        color = palette.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = state.refresh) {
                        Text("Reintentar", color = palette.accentBlue, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        null -> state.snapshot?.let { snapshot ->
            if (snapshot.partial) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = palette.textMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Algunas tareas no pudieron verificarse ($count verificadas).",
                        fontSize = 12.sp,
                        color = palette.textSecondary
                    )
                }
            } else if (!state.loading && count == 0) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = palette.innerSurface,
                    border = BorderStroke(1.dp, palette.innerBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = palette.successGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "¡Estás al día! No tienes tareas pendientes.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = palette.textPrimary
                        )
                    }
                }
            }
        }
    }
}

private val deadlineFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm", Locale.forLanguageTag("es-PE"))

@Composable
private fun TaskCard(
    task: PendingTask,
    now: Instant,
    palette: TodayPalette
) {
    val uriHandler = LocalUriHandler.current
    val url = task.url?.toHttpUrlOrNull()?.takeIf(TrustedServers::isCanvas)
    var openFailed by remember(task.key) { mutableStateOf(false) }
    val timingLabel = task.timing(now)
    val isUrgent = timingLabel == "Vence hoy"
    val dateLabel = task.due?.let {
        "$timingLabel · ${deadlineFormat.format(it.atZone(taskZone))}"
    } ?: "Sin fecha de entrega"

    Surface(
        shape = RoundedCornerShape(15.dp),
        color = palette.innerSurface,
        border = BorderStroke(1.dp, palette.innerBorder),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (url != null) {
                    Modifier
                        .clip(RoundedCornerShape(15.dp))
                        .clickable(role = Role.Button) {
                            openFailed = runCatching { uriHandler.openUri(url.toString()) }.isFailure
                        }
                } else Modifier
            )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(palette.softBlueBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Description,
                        contentDescription = null,
                        tint = palette.softBlueText,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = task.name,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = task.course,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = palette.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarToday,
                            contentDescription = null,
                            tint = if (isUrgent) palette.urgentText else palette.softBlueText,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = dateLabel,
                            fontSize = 11.5.sp,
                            fontWeight = if (isUrgent) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (isUrgent) palette.urgentText else palette.softBlueText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (url != null) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Abrir actividad",
                        tint = palette.textMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (openFailed) {
                Text(
                    text = "No se pudo abrir la actividad.",
                    fontSize = 11.5.sp,
                    color = palette.urgentText,
                    modifier = Modifier.padding(start = 61.dp, end = 12.dp, bottom = 10.dp)
                )
            }
        }
    }
}
