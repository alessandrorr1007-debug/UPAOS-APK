package com.example.upaos.service

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.work.ListenableWorker
import com.example.upaos.data.local.TokenManager
import com.example.upaos.data.upao.UpaoSessionExpiredException
import kotlinx.coroutines.CancellationException

/** Technical metadata only; shares the existing account-scoped cleanup policy. */
class SyncDiagnostics(private val context: Context) {
    private val prefs = context.getSharedPreferences(
        "upaos_notification_snapshots_${TokenManager(context).getSavedUser() ?: "anonymous"}", Context.MODE_PRIVATE)

    suspend fun run(source: String, id: String, attempt: Int, action: suspend () -> ListenableWorker.Result): ListenableWorker.Result {
        val started = SystemClock.elapsedRealtime()
        prefs.edit().putLong("diagnostic_${source}_attempt", System.currentTimeMillis()).remove("diagnostic_${source}_skip").apply()
        Log.i(TAG, "source=$source work=$id attempt=$attempt phase=start connected=${connected()}")
        var outcome = "interrupted"
        try {
            val result = action()
            outcome = result.toString()
            return result
        } catch (e: CancellationException) {
            outcome = "cancelled"
            throw e
        } finally {
            val duration = SystemClock.elapsedRealtime() - started
            prefs.edit().putString("diagnostic_${source}_result", outcome)
                .putLong("diagnostic_${source}_duration", duration).apply()
            Log.i(TAG, "source=$source work=$id durationMs=$duration result=$outcome")
        }
    }

    fun skipped(source: String, reason: String) {
        prefs.edit().putString("diagnostic_${source}_skip", reason).apply()
        Log.i(TAG, "source=$source skipped=$reason")
    }

    fun success(source: String) {
        prefs.edit().putLong("diagnostic_${source}_success", System.currentTimeMillis())
            .remove("diagnostic_${source}_error").apply()
    }

    fun error(source: String, error: Throwable) {
        val reason = when {
            error is UpaoSessionExpiredException || error is com.example.upaos.data.upao.UpaoInvalidCredentialsException -> "authentication"
            error is com.example.upaos.data.upao.UpaoUnexpectedFormatException -> "schema"
            error is com.example.upaos.data.upao.UpaoServiceUnavailableException -> "http_or_service"
            error is com.example.upaos.data.refresh.IncompleteRefreshException -> "incomplete_response"
            error is java.io.IOException -> "network"
            else -> "incomplete_or_api"
        }
        prefs.edit().putString("diagnostic_${source}_error", reason).apply()
        Log.w(TAG, "source=$source reason=$reason")
    }

    fun delivery(channel: String, emitted: Boolean) {
        val key = if (emitted) "diagnostic_emitted" else "diagnostic_blocked"
        synchronized(SyncDiagnostics::class.java) {
            prefs.edit().putInt(key, prefs.getInt(key, 0) + 1).apply()
        }
        Log.i(TAG, "channel=$channel delivery=${if (emitted) "emitted" else "blocked"}")
    }

    fun detected(eventKey: String) = synchronized(SyncDiagnostics::class.java) {
        val key = "diagnostic_detected_$eventKey"
        if (!prefs.getBoolean(key, false)) prefs.edit().putBoolean(key, true)
            .putInt("diagnostic_detected", prefs.getInt("diagnostic_detected", 0) + 1).apply()
    }

    /** WorkInfo describes eligibility, not a promise of execution or an OEM diagnosis. */
    suspend fun workSummary(): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val manager = androidx.work.WorkManager.getInstance(context)
        val names = listOf("Notas" to "grade_sync_backup", "Notas manual" to "grade_sync_now",
            "Asistencia" to "upao_attendance_periodic_v2", "Asistencia manual" to "upao_attendance_now_v2")
        names.joinToString("\n") { (label, name) ->
            try {
                val works = manager.getWorkInfosForUniqueWork(name).get(2, java.util.concurrent.TimeUnit.SECONDS)
                val info = works.firstOrNull { it.state == androidx.work.WorkInfo.State.RUNNING }
                    ?: works.firstOrNull { !it.state.isFinished } ?: works.firstOrNull()
                if (info == null) "$label: sin trabajo registrado"
                else {
                    val next = info.nextScheduleTimeMillis.takeIf { it > 0 && it < Long.MAX_VALUE }?.let {
                        java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.getDefault()).format(java.util.Date(it))
                    } ?: "no disponible"
                    "$label: ${info.state}; id=${info.id.toString().take(8)}; intento=${info.runAttemptCount}; " +
                        "red=${info.constraints.requiredNetworkType}; próxima elegibilidad=$next; parada=${info.stopReason}"
                }
            } catch (_: Exception) { "$label: WorkInfo no disponible" }
        } + "\nElegibilidad no significa ejecución exacta. La causa completa de un retraso no siempre está disponible."
    }

    private fun channels(): String {
        if (android.os.Build.VERSION.SDK_INT < 26) return "Canales: no aplican en esta versión"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        return listOf("Notas" to NotificationService.CHANNEL_NOTAS, "Asistencia" to NotificationService.CHANNEL_ASISTENCIA,
            "Clases" to NotificationService.CHANNEL_RECORDATORIO_CLASES, "Entregas" to AgendaReminderWorker.CHANNEL)
            .joinToString("; ") { (label, id) ->
                val channel = manager.getNotificationChannel(id)
                "$label: " + when { channel == null -> "sin crear"; channel.importance == 0 -> "bloqueado"; else -> "habilitado" }
            }
    }

    fun summary(): String {
        fun date(key: String): String = prefs.getLong(key, 0).takeIf { it > 0 }?.let {
            java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.getDefault()).format(java.util.Date(it))
        } ?: "Sin registro"
        return listOf("grades" to "Notas", "attendance" to "Asistencia").joinToString("\n") { (key, label) ->
            "$label: último intento ${date("diagnostic_${key}_attempt")}; éxito ${date("diagnostic_${key}_success")}. " +
                "Hace ${prefs.getLong("diagnostic_${key}_success", 0).takeIf { it > 0 }?.let { ((System.currentTimeMillis()-it).coerceAtLeast(0)/60_000).toString() } ?: "?"} min. Resultado: ${prefs.getString("diagnostic_${key}_result", "Sin registro")}; " +
                "error: ${prefs.getString("diagnostic_${key}_error", "ninguno")}; omisión: ${prefs.getString("diagnostic_${key}_skip", "ninguna")}."
        } + "\n${channels()}\nEventos detectados: ${prefs.getInt("diagnostic_detected", 0)}. Conexión actual: ${connected()}. Avisos emitidos: ${prefs.getInt("diagnostic_emitted", 0)}; bloqueados: ${prefs.getInt("diagnostic_blocked", 0)}."
    }

    @Suppress("DEPRECATION")
    private fun connected(): Boolean? = runCatching {
        (context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager)
            .activeNetworkInfo?.isConnected == true
    }.getOrNull()

    companion object { private const val TAG = "UPAO_SyncDiagnostic" }
}
