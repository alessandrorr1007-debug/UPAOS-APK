package com.example.upaos.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.example.upaos.data.agenda.*
import com.example.upaos.data.api.CanvasApiClient
import com.example.upaos.data.api.TrustedServers
import com.example.upaos.data.local.TokenManager
import com.example.upaos.ui.components.CampusCard
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
                    state.snapshot?.partial == false && state.failure == null) return
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
            try { kotlinx.coroutines.awaitCancellation() }
            finally { state.refresh = {}; state.loading = false }
        }
    }
    return state
}

@Composable
internal fun PendingTasksPreview(state: TodayTasksState, nowMillis: Long, connect: () -> Unit, open: () -> Unit) {
    val now = Instant.ofEpochMilli(nowMillis)
    val tasks = visiblePendingTasks(state.snapshot?.tasks.orEmpty(), now)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TaskHeader(state, tasks.size, connect)
        tasks.take(3).forEach { TaskCard(it, now) }
        if (tasks.isNotEmpty()) TextButton(onClick = open) { Text("Ver todas las tareas") }
    }
}

@Composable
internal fun AllPendingTasks(state: TodayTasksState, nowMillis: Long, connect: () -> Unit, back: () -> Unit) {
    BackHandler(onBack = back)
    val now = Instant.ofEpochMilli(nowMillis)
    val tasks = visiblePendingTasks(state.snapshot?.tasks.orEmpty(), now)
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { TextButton(onClick = back) { Text("Volver a Hoy") } }
        item { TaskHeader(state, tasks.size, connect) }
        items(tasks.filter { it.due != null }, key = { it.key }) { TaskCard(it, now) }
        val undated = tasks.filter { it.due == null }
        if (undated.isNotEmpty()) item { Text("Sin fecha de entrega", style = MaterialTheme.typography.titleMedium) }
        items(undated, key = { it.key }) { TaskCard(it, now) }
    }
}

@Composable
private fun TaskHeader(state: TodayTasksState, count: Int, connect: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text("Tareas por hacer", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Actividades pendientes de tus cursos", style = MaterialTheme.typography.bodyMedium)
        if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        when (state.failure) {
            TaskFailure.CONFIGURATION, TaskFailure.AUTHENTICATION -> {
                Text(if (state.failure == TaskFailure.CONFIGURATION) "Conecta Canvas para consultar tus tareas."
                    else "Necesitas volver a conectar Canvas.")
                TextButton(onClick = connect) { Text("Configurar Canvas") }
            }
            TaskFailure.CONNECTION -> {
                Text("No pudimos actualizar tus tareas.")
                TextButton(onClick = state.refresh) { Text("Reintentar") }
            }
            null -> state.snapshot?.let {
                if (it.partial) {
                    Text("Algunas tareas no pudieron verificarse.")
                    Text("$count tareas verificadas")
                } else if (!state.loading && count == 0) Text("¡Estás al día! No tienes tareas pendientes.")
                else Text("$count tareas pendientes")
                TextButton(onClick = state.refresh, enabled = !state.loading) { Text("Actualizar tareas") }
            }
        }
    }
}

private val deadlineFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm", Locale.forLanguageTag("es-PE"))

@Composable
private fun TaskCard(task: PendingTask, now: Instant) {
    val uriHandler = LocalUriHandler.current
    val url = task.url?.toHttpUrlOrNull()?.takeIf(TrustedServers::isCanvas)
    var openFailed by remember(task.key) { mutableStateOf(false) }
    CampusCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(task.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(task.course, style = MaterialTheme.typography.bodyMedium)
            Text(task.timing(now), color = if (task.timing(now) == "Vence hoy")
                MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            task.due?.let { Text("Entrega · ${deadlineFormat.format(it.atZone(taskZone))}") }
            if (url != null) TextButton(onClick = {
                openFailed = runCatching { uriHandler.openUri(url.toString()) }.isFailure
            }) { Text("Abrir actividad") }
            if (openFailed) Text("No se pudo abrir la actividad.")
        }
    }
}
