package com.example.upaos.data.agenda

import com.example.upaos.data.api.CanvasApiService
import com.example.upaos.data.model.CanvasAssignment
import com.example.upaos.data.model.CanvasSubmission
import java.io.IOException
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import retrofit2.Response

internal val taskZone: ZoneId = ZoneId.of("America/Lima")

internal data class PendingTask(
    val courseId: Long,
    val id: Long,
    val course: String,
    val name: String,
    val due: Instant?,
    val closes: Instant?,
    val url: String?
) {
    val key get() = "$courseId:$id"
    fun timing(now: Instant): String = due?.atZone(taskZone)?.toLocalDate()?.let { date ->
        val today = now.atZone(taskZone).toLocalDate()
        when (date) {
            today -> "Vence hoy"
            today.plusDays(1) -> "Vence mañana"
            else -> "Próximamente"
        }
    } ?: "Sin fecha de entrega"
}

internal data class PendingTaskResult(val tasks: List<PendingTask>, val partial: Boolean)

internal fun canvasInstant(value: String?): Instant? =
    value?.let { OffsetDateTime.parse(it).toInstant() }

private val offlineSubmissionTypes = setOf("none", "on_paper", "not_graded")

internal fun CanvasAssignment.needsSubmission(): Boolean =
    published != false && submissionTypes.orEmpty().any { it !in offlineSubmissionTypes }

internal fun pendingTask(
    assignment: CanvasAssignment, submission: CanvasSubmission, courseId: Long, course: String, now: Instant
): PendingTask? {
    if (!assignment.needsSubmission() || submission.excused == true) return null
    if (!submission.submittedAt.isNullOrBlank() ||
        submission.workflowState in setOf("submitted", "pending_review")) return null
    if (submission.workflowState !in setOf("unsubmitted", "graded")) {
        throw IOException("Estado de entrega desconocido")
    }
    val due = canvasInstant(assignment.dueAt)
    val closes = canvasInstant(assignment.lockAt)
    val opens = canvasInstant(assignment.unlockAt)
    if (due != null && due < now || closes != null && closes <= now) return null
    if (opens != null && due != null && opens > due) return null
    if (assignment.lockedForUser == true && (due == null || opens == null || opens <= now)) return null
    if (due == null && opens != null && opens > now) return null
    return PendingTask(courseId, assignment.id, course, assignment.name?.takeIf { it.isNotBlank() } ?: "Actividad",
        due, closes, assignment.htmlUrl)
}

internal fun visiblePendingTasks(tasks: List<PendingTask>, now: Instant): List<PendingTask> = tasks
    .filter { (it.due == null || it.due >= now) && (it.closes == null || it.closes > now) }
    .distinctBy { it.key }
    .sortedWith(compareBy<PendingTask> { it.due == null }.thenBy { it.due }
        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.course }
        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }.thenBy { it.key })

internal class PendingTasksRepository(private val api: CanvasApiService) {
    suspend fun load(token: String, now: Instant = Instant.now()): PendingTaskResult {
        val auth = "Bearer $token"
        val courses = api.getActiveCourses(auth, enrollmentType = "student").requiredBody()
        val tasks = mutableListOf<PendingTask>()
        var partial = false
        for (course in courses.distinctBy { it.id }) {
            if (course.workflowState in setOf("completed", "deleted", "unpublished")) continue
            val assignments = try {
                api.getCourseAssignments(auth, course.id).requiredBody()
            } catch (error: Exception) {
                error.rethrowAuthenticationOrCancellation()
                partial = true
                continue
            }
            for (assignment in assignments.distinctBy { it.id }) {
                if (assignment.published != false && assignment.submissionTypes.isNullOrEmpty()) {
                    partial = true
                    continue
                }
                if (!assignment.needsSubmission()) continue
                if (assignment.id <= 0) {
                    partial = true
                    continue
                }
                try {
                    if (canvasInstant(assignment.dueAt)?.let { it < now } == true) continue
                    val submission = assignment.submission
                        ?: api.getSubmission(auth, course.id, assignment.id, emptyList()).requiredBody()
                    pendingTask(assignment, submission, course.id, course.displayTitle, now)?.let(tasks::add)
                } catch (error: Exception) {
                    error.rethrowAuthenticationOrCancellation()
                    partial = true
                }
            }
        }
        return PendingTaskResult(visiblePendingTasks(tasks, now), partial)
    }
}

private fun Exception.rethrowAuthenticationOrCancellation() {
    if (this is CancellationException || this is HttpException && code() == 401) throw this
}

private fun <T> Response<T>.requiredBody(): T {
    if (!isSuccessful) {
        errorBody()?.close()
        throw HttpException(this)
    }
    return body() ?: throw IOException("Canvas devolvió una respuesta vacía")
}
