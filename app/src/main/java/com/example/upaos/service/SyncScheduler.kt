package com.example.upaos.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.work.*
import com.example.upaos.data.local.NotificationPreferences
import com.example.upaos.data.local.SessionCleanup
import com.example.upaos.data.local.TokenManager
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/** Persistent periodic checks with an adaptive due time. No self-rescheduling chain. */
object SyncScheduler {
    const val ACTION_SYNC = "com.example.upaos.ACTION_SYNC_10MIN"
    private const val WORK_NAME = "upao_attendance_periodic_v2"
    private const val IMMEDIATE = "upao_attendance_now_v2"

    fun start(context: Context) = synchronized(SessionCleanup.lock) {
        GradeSyncController.reconcile(context)
        removeLegacySchedules(context)
        enqueue(context)
    }

    /** Periodic work survives interruption; this timestamp only controls adaptive polling. */
    fun scheduleNext(
        context: Context,
        fallosConsecutivos: Int = NotificationPreferences(context).syncConsecutiveFailures,
        esErrorFatal: Boolean = false
    ) {
        val prefs = NotificationPreferences(context)
        val delay = if (esErrorFatal) java.time.Duration.ofHours(1) else SyncIntervalPolicy.proximoIntervalo(
            ZonedDateTime.now(SyncIntervalPolicy.ZONE_LIMA),
            SyncIntervalPolicy.SyncContext(fallosConsecutivos = fallosConsecutivos,
                periodoAcademico = prefs.syncAcademicPeriod))
        prefs.nextAttendanceAttempt = System.currentTimeMillis() + delay.toMillis()
    }

    private fun enqueue(context: Context) {
        val prefs = NotificationPreferences(context)
        val work = WorkManager.getInstance(context)
        if (!TokenManager(context).shouldKeepLoggedIn() || !prefs.checkAsistenciaEnabled) {
            work.cancelUniqueWork(WORK_NAME)
            return
        }
        val request = PeriodicWorkRequestBuilder<AsistenciaWorker>(15, TimeUnit.MINUTES, 5, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        work.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun runNow(context: Context) = synchronized(SessionCleanup.lock) {
        removeLegacySchedules(context)
        GradeSyncController.ensureBackup(context)
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val work = WorkManager.getInstance(context)
        if (NotificationPreferences(context).checkNotasEnabled) work.enqueueUniqueWork(
            "grade_sync_now", ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<GradeBackupWorker>().setConstraints(constraints).build())
        if (NotificationPreferences(context).checkAsistenciaEnabled) work.enqueueUniqueWork(
            IMMEDIATE, ExistingWorkPolicy.KEEP, OneTimeWorkRequestBuilder<AsistenciaWorker>()
                .setInputData(workDataOf("manual" to true)).setConstraints(constraints).build())
    }

    fun cancel(context: Context) = synchronized(SessionCleanup.lock) {
        GradeSyncController.cancel(context)
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        WorkManager.getInstance(context).cancelUniqueWork(IMMEDIATE)
        removeLegacySchedules(context)
    }

    /** Migration from releases with three independent polling schedules. */
    private fun removeLegacySchedules(context: Context) {
        val alarms = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val intent = Intent(context, BackgroundSyncReceiver::class.java).apply { action = ACTION_SYNC }
        PendingIntent.getBroadcast(context, 9988, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.let {
            alarms?.cancel(it)
            it.cancel()
        }
        for (name in listOf("upao_adaptive_sync", "upao_unified_periodic_sync", "upao_unified_onetime_sync",
            "upao_immediate_sync", "upao_asistencia_check", "upao_asistencia_5min_check")) {
            WorkManager.getInstance(context).cancelUniqueWork(name)
        }
    }
}
