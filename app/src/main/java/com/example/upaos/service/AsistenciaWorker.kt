package com.example.upaos.service

import android.content.Context
import com.example.upaos.data.refresh.*
import android.util.Log
import androidx.work.*
import com.example.upaos.data.local.ApiCache
import com.example.upaos.data.local.NotificationPreferences
import com.example.upaos.data.local.TokenManager
import com.example.upaos.data.model.AsistenciaCurso
import com.example.upaos.data.upao.UpaoAuth
import com.example.upaos.data.upao.UpaoRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AsistenciaWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private var notificationPrefs = NotificationPreferences(context)
    private val tokenManager = TokenManager(context)
    private val apiCache = ApiCache(context)
    private val upaoRepository = UpaoRepository(context)
    private val gson = Gson()
    private var owner: ReminderOwner? = null

    private fun requireCurrentSession() {
        if (owner?.matches(tokenManager.getSavedUser(), tokenManager.getSessionId()) != true)
            throw CancellationException("La sesión del trabajo cambió")
    }

    private fun publishForCurrentSession(publish: () -> Unit) {
        val expected = owner ?: throw CancellationException("No hay sesión del trabajo")
        if (!com.example.upaos.data.local.SessionCleanup.publishIfCurrent(expected,
                { tokenManager.getSavedUser() to tokenManager.getSessionId() }, publish))
            throw CancellationException("La sesión del trabajo cambió")
    }

    private val diagnostics = SyncDiagnostics(applicationContext)

    override suspend fun doWork(): Result = diagnostics.run("attendance", id.toString(), runAttemptCount) { performWork() }

    private suspend fun performWork(): Result = withContext(Dispatchers.IO) {
        if (!syncMutex.tryLock()) {
            diagnostics.skipped("attendance", "busy_retry")
            return@withContext Result.retry()
        }
        try {
            if (!tokenManager.shouldKeepLoggedIn()) {
                diagnostics.skipped("attendance", "no_saved_session")
                return@withContext Result.success()
            }
            val checkAsistencia = notificationPrefs.checkAsistenciaEnabled
            val checkNotas = notificationPrefs.checkNotasEnabled

            if (!checkAsistencia && !checkNotas) {
                Log.d(TAG, "Notificaciones de asistencia y notas desactivadas. Omitiendo revisión.")
                return@withContext Result.success()
            }

            if (!checkAsistencia) {
                SyncScheduler.scheduleNext(applicationContext)
                return@withContext Result.success()
            }

            val user = tokenManager.getSavedUser()
            val pass = tokenManager.getSavedPass()

            if (user.isNullOrBlank() || pass.isNullOrBlank()) {
                diagnostics.skipped("attendance", "missing_credentials")
                Log.d(TAG, "Sin credenciales guardadas, omitiendo chequeo.")
                return@withContext Result.success()
            }
            val sessionId = tokenManager.getSessionId() ?: return@withContext Result.success()
            owner = ReminderOwner(user, sessionId)
            requireCurrentSession()
            notificationPrefs = NotificationPreferences(applicationContext, user)
            if (!inputData.getBoolean("manual", false) && System.currentTimeMillis() < notificationPrefs.nextAttendanceAttempt) {
                diagnostics.skipped("attendance", "pending_adaptive_deadline")
                return@withContext Result.success()
            }

            try {
                // Asegurar sesión directa en UPAO SSO
                val loginRes = UpaoAuth.ensureSession(user, pass)
                if (!loginRes.success) {
                    diagnostics.error("attendance", com.example.upaos.data.upao.UpaoSessionExpiredException())
                    Log.w(TAG, "No se pudo asegurar sesión directa con UPAO SSO.")
                    val fallos = notificationPrefs.syncConsecutiveFailures + 1
                    publishForCurrentSession {
                        notificationPrefs.syncConsecutiveFailures = fallos
                        SyncScheduler.scheduleNext(applicationContext, fallosConsecutivos = fallos)
                    }
                    return@withContext Result.success()
                }

                // Resolve active metadata even when only attendance checks are enabled.
                upaoRepository.getPeriodos()
                val checks = mutableListOf<suspend () -> RefreshResult<Unit>>()
                if (checkAsistencia) checks += { revisarAsistencia(user) }
                runSyncChecks(checks).requireComplete()

                // Reprograma la siguiente verificación periódica con éxito (resetea fallos a 0)
                publishForCurrentSession {
                    notificationPrefs.syncConsecutiveFailures = 0
                    SyncScheduler.scheduleNext(applicationContext, fallosConsecutivos = 0)
                }
                diagnostics.success("attendance")
                Log.d(TAG, "Sincronización en segundo plano completada con éxito. Próximo ciclo adaptativo programado.")
                Result.success()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                requireCurrentSession()
                diagnostics.error("attendance", e)
                Log.e(TAG, "No se pudo completar la sincronización")
                publishForCurrentSession {
                    val fallos = notificationPrefs.syncConsecutiveFailures + 1
                    notificationPrefs.syncConsecutiveFailures = fallos
                    SyncScheduler.scheduleNext(applicationContext, fallosConsecutivos = fallos,
                        esErrorFatal = isFatalSyncFailure(e))
                }
                // The adaptive successor owns retries; never add WorkManager's second retry clock.
                Result.success()
            }
        } finally {
            syncMutex.unlock()
        }
    }

    private suspend fun revisarAsistencia(user: String): RefreshResult<Unit> = captureRefresh("Asistencia") {
        publishComplete(upaoRepository.getAsistenciaResult()) { response ->
            requireCurrentSession()
            compararYNotificarAsistencia(response.asistencia, user)
            apiCache.guardar("asistencia_$user", gson.toJson(response))
            notificationPrefs.ultimaRevisionAsistencia = System.currentTimeMillis()
        }
        Unit
    }

    private suspend fun compararYNotificarAsistencia(nuevosCursos: List<AsistenciaCurso>, user: String) {
        val snapshotPrevioRaw = notificationPrefs.ultimoSnapshotAsistencia
        val type = object : TypeToken<Map<String, SnapshotCurso>>() {}.type
        val snapshotPrevio: Map<String, SnapshotCurso> = if (!snapshotPrevioRaw.isNullOrBlank()) {
            try {
                gson.fromJson(snapshotPrevioRaw, type) ?: emptyMap()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                emptyMap()
            }
        } else {
            emptyMap()
        }

        val nuevoSnapshot = mutableMapOf<String, SnapshotCurso>()

        for (curso in nuevosCursos) {
            val key = curso.crn?.takeIf { it.isNotBlank() }
                ?: "${curso.displayNombre}_${curso.seccion ?: ""}_${curso.periodo ?: ""}"

            val faltasActuales = curso.faltas ?: 0
            val pctActual = curso.porcentaje ?: 0.0
            val asistenciasActuales = curso.vecesAsistidas

            val tipoTag = when {
                !curso.tipo.isNullOrBlank() -> " (${curso.tipo})"
                !curso.tipoComponente.isNullOrBlank() -> " (${curso.tipoComponente})"
                !curso.seccion.isNullOrBlank() -> " (Sec. ${curso.seccion})"
                else -> ""
            }
            val nombreConTipo = "${curso.displayNombre}$tipoTag"

            nuevoSnapshot[key] = SnapshotCurso(
                nombre = nombreConTipo,
                faltas = curso.faltas,
                asistencias = asistenciasActuales,
                porcentaje = curso.porcentaje
            )

            // Solo comparamos si ya existía un snapshot previo
            if (snapshotPrevio.isNotEmpty()) {
                val previo = snapshotPrevio[key]
                if (previo != null) {
                    val faltasPrevias = previo.faltas ?: 0
                    val asistPrevias = previo.asistencias

                    if (faltasActuales > faltasPrevias) {
                        val dif = faltasActuales - faltasPrevias
                        val plural = if (dif == 1) "una nueva falta" else "$dif faltas nuevas"
                        val titulo = "⚠️ Falta registrada"
                        val mensaje = "Te registraron $plural (Total: $faltasActuales). Asistencia: ${formatearPct(pctActual)}%."
                        var eventKey: String? = null
                        publishForCurrentSession { eventKey = emitAttendance(user, key, titulo, mensaje) }
                        upaoRepository.registrarNotificacionLocal(user, mensaje, curso = nombreConTipo, componente = "Asistencia", eventKey = eventKey)
                    } else if (asistenciasActuales != null && asistPrevias != null && asistenciasActuales > asistPrevias) {
                        val dif = asistenciasActuales - asistPrevias
                        val plural = if (dif == 1) "asistencia" else "$dif asistencias"
                        val titulo = "✅ Asistencia registrada"
                        val mensaje = "¡Se registró tu $plural! Llevas $asistenciasActuales asistencias."
                        var eventKey: String? = null
                        publishForCurrentSession { eventKey = emitAttendance(user, key, titulo, mensaje) }
                        upaoRepository.registrarNotificacionLocal(user, mensaje, curso = nombreConTipo, componente = "Asistencia", eventKey = eventKey)
                    } else if (previo.porcentaje != null && pctActual > previo.porcentaje && faltasActuales == faltasPrevias) {
                        val titulo = "✅ Asistencia registrada"
                        val mensaje = "¡Se registró una nueva asistencia! (${formatearPct(pctActual)}%)."
                        var eventKey: String? = null
                        publishForCurrentSession { eventKey = emitAttendance(user, key, titulo, mensaje) }
                        upaoRepository.registrarNotificacionLocal(user, mensaje, curso = nombreConTipo, componente = "Asistencia", eventKey = eventKey)
                    }
                }
            }
        }

        publishForCurrentSession { notificationPrefs.commitAttendanceSnapshot(gson.toJson(nuevoSnapshot)) }
    }

    private fun emitAttendance(user: String, courseKey: String, title: String, body: String): String {
        val prefs = applicationContext.getSharedPreferences("upaos_notification_snapshots_$user", Context.MODE_PRIVATE)
        val fingerprint = com.example.upaos.data.local.notificationEventKey(user, "attendance",
            notificationPrefs.syncAcademicPeriod, notificationPrefs.attendanceSnapshotGeneration, "$courseKey/$title/$body")
        diagnostics.detected(fingerprint)
        if (prefs.contains("attendance_notified_$fingerprint")) return fingerprint
        if (!NotificationService.mostrarNotificacionAsistencia(applicationContext, title, body, fingerprint.hashCode())) return fingerprint
        check(prefs.edit().putBoolean("attendance_notified_$fingerprint", true).commit()) { "No se pudo persistir dedupe" }
        return fingerprint
    }

    private fun formatearPct(pct: Double): String =
        if (pct % 1.0 == 0.0) pct.toInt().toString() else "%.1f".format(pct)

    private data class SnapshotCurso(
        val nombre: String,
        val faltas: Int?,
        val asistencias: Int?,
        val porcentaje: Double?
    )

    companion object {
        private const val TAG = "UPAO_BackgroundSync"
        private val syncMutex = com.example.upaos.data.refresh.ForegroundRefreshGate.lock

        fun schedule(context: Context) {
            SyncScheduler.start(context)
        }

        fun scheduleFiveMinuteCheck(context: Context) {
            SyncScheduler.scheduleNext(context)
        }

        fun cancel(context: Context) {
            SyncScheduler.cancel(context)
        }

        fun runOnce(context: Context) {
            SyncScheduler.runNow(context)
        }
    }
}
