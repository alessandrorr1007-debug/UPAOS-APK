package com.example.upaos.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.upaos.data.local.NotificationPreferences
import com.example.upaos.data.local.TokenManager
import com.example.upaos.data.refresh.ForegroundRefreshGate
import com.example.upaos.data.upao.UpaoAuth
import kotlinx.coroutines.CancellationException

class GradeBackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    private val diagnostics = SyncDiagnostics(applicationContext)

    override suspend fun doWork(): Result = diagnostics.run("grades", id.toString(), runAttemptCount) { performWork() }

    private suspend fun performWork(): Result {
        val tokens = TokenManager(applicationContext)
        if (!NotificationPreferences(applicationContext).checkNotasEnabled || !tokens.shouldKeepLoggedIn()) {
            diagnostics.skipped("grades", "disabled_or_no_saved_session")
            return Result.success()
        }
        // The persistent worker performs the check itself. Starting a dataSync service
        // here is restricted by Android and must not make the backup depend on UI.
        if (!ForegroundRefreshGate.lock.tryLock()) {
            diagnostics.skipped("grades", "busy_retry")
            return Result.retry()
        }
        try {
            val user = tokens.getSavedUser() ?: return Result.success()
            val owner = ReminderOwner(user, tokens.getSessionId() ?: return Result.success())
            val pass = tokens.getSavedPass() ?: run {
                diagnostics.skipped("grades", "missing_credentials")
                return Result.success()
            }
            if (!UpaoAuth.ensureSession(user, pass).success) {
                diagnostics.error("grades", com.example.upaos.data.upao.UpaoSessionExpiredException())
                return Result.failure()
            }
            if (!owner.matches(tokens.getSavedUser(), tokens.getSessionId())) throw CancellationException("Sesión cambiada")
            GradeSyncCheck(applicationContext).check(GradeSyncController.mode(applicationContext).name)
            diagnostics.success("grades")
            return Result.success()
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { diagnostics.error("grades", e); return if (isFatalSyncFailure(e)) Result.failure() else Result.retry() }
        finally { ForegroundRefreshGate.lock.unlock() }
    }
}
