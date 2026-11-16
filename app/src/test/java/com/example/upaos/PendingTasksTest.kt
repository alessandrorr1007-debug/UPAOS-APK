package com.example.upaos

import com.example.upaos.data.agenda.PendingTasksRepository
import com.example.upaos.data.agenda.pendingTask
import com.example.upaos.data.agenda.visiblePendingTasks
import com.example.upaos.data.api.CanvasApiService
import com.example.upaos.data.api.CanvasPagedApi
import com.example.upaos.data.model.CanvasAssignment
import com.example.upaos.data.model.CanvasAssignmentDate
import com.example.upaos.data.model.CanvasSubmission
import java.io.IOException
import java.time.Instant
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class PendingTasksTest {
    private val now = Instant.parse("2026-11-12T16:00:00Z")
    private val open = CanvasSubmission(workflowState = "unsubmitted")
    private val base = CanvasAssignment(1, "Actividad", submissionTypes = listOf("online_upload"))
    private fun task(assignment: CanvasAssignment = base, submission: CanvasSubmission = open) =
        pendingTask(assignment, submission, 2, "Curso de prueba", now)

    @Test
    fun ordersTodayTomorrowAndDistantDeadlinesBeforeUndated() {
        val dates = listOf("2026-11-12T23:59:00-05:00", "2026-11-13T09:00:00-05:00",
            "2026-12-12T12:00:00-05:00", "2027-02-12T12:00:00-05:00", null)
        val tasks = dates.mapIndexed { i, date -> task(base.copy(id = i + 1L, dueAt = date))!! }
        assertEquals(tasks, visiblePendingTasks(tasks.reversed() + tasks.first(), now))
        assertEquals(listOf("Vence hoy", "Vence mañana", "Próximamente", "Próximamente", "Sin fecha de entrega"),
            tasks.map { it.timing(now) })
    }

    @Test
    fun usesEffectiveDeadlineEvenWhenBaseDateDiffersOrEffectiveDateIsNull() {
        val old = CanvasAssignmentDate(base = true, dueAt = "2026-11-11T12:00:00Z")
        val future = base.copy(dueAt = "2026-12-01T12:00:00Z", allDates = listOf(old))
        assertEquals(Instant.parse("2026-12-01T12:00:00Z"), task(future)?.due)
        assertNotNull(task(future.copy(dueAt = null)))
        assertNull(task(future.copy(dueAt = null))?.due)
    }

    @Test
    fun distinguishesGradingFromDeliveryAndExcludesIneligibleActivities() {
        assertNull(task(submission = open.copy(workflowState = "submitted")))
        assertNull(task(submission = open.copy(workflowState = "pending_review")))
        assertNull(task(submission = open.copy(workflowState = "graded", submittedAt = now.toString())))
        assertNotNull(task(submission = open.copy(workflowState = "graded", grade = "0")))
        assertNull(task(submission = open.copy(excused = true)))
        assertNull(task(base.copy(dueAt = "2026-11-11T12:00:00Z")))
        assertNull(task(base.copy(lockAt = now.toString())))
        assertNull(task(base.copy(lockedForUser = true)))
        assertNull(task(base.copy(published = false)))
        assertNull(task(base.copy(submissionTypes = listOf("none", "on_paper"))))
    }

    @Test
    fun loadsAllPagesWithDuplicatesEmptyCourseAndMissingSubmissionFallback() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""[{"id":1,"name":"Curso A"},{"id":2,"name":"Curso B"}]"""))
            val row = """{"id":8,"name":"Actividad","due_at":"2027-02-12T12:00:00Z","submission_types":["online_upload"]}"""
            server.enqueue(MockResponse().setHeader("Link", "</api/v1/courses/1/assignments?page=2>; rel=next").setBody("[$row]"))
            server.enqueue(MockResponse().setBody("[$row]"))
            server.enqueue(MockResponse().setBody("""{"workflow_state":"unsubmitted"}"""))
            server.enqueue(MockResponse().setBody("[]"))
            val result = repository(server).load("synthetic", now)
            assertEquals(listOf("1:8"), result.tasks.map { it.key })
            assertFalse(result.partial)
            assertEquals("student", server.takeRequest().requestUrl!!.queryParameter("enrollment_type"))
            assertEquals("true", server.takeRequest().requestUrl!!.queryParameter("override_assignment_dates"))
            assertEquals("2", server.takeRequest().requestUrl!!.queryParameter("page"))
            assertTrue(server.takeRequest().path!!.contains("/submissions/self"))
            assertEquals(5, server.requestCount)
        }
    }

    @Test
    fun reportsPartialCoverageAndPropagatesAuthenticationAndConnectionErrors() = runBlocking {
        MockWebServer().use { server ->
            val courses = """[{"id":1,"name":"Curso A"},{"id":2,"name":"Curso B"}]"""
            server.enqueue(MockResponse().setBody(courses))
            server.enqueue(MockResponse().setResponseCode(403))
            server.enqueue(MockResponse().setBody("[]"))
            assertTrue(repository(server).load("synthetic", now).partial)
            server.enqueue(MockResponse().setBody(courses))
            server.enqueue(MockResponse().setResponseCode(401))
            try {
                repository(server).load("synthetic", now)
                fail("Debe informar expiración")
            } catch (error: HttpException) { assertEquals(401, error.code()) }
        }
        val closed = MockWebServer()
        val repository = repository(closed)
        closed.shutdown()
        try {
            repository.load("synthetic", now)
            fail("Debe informar fallo de conexión")
        } catch (_: IOException) { }
    }

    private fun repository(server: MockWebServer): PendingTasksRepository {
        val baseUrl = server.url("/")
        val raw = Retrofit.Builder().baseUrl(baseUrl).addConverterFactory(GsonConverterFactory.create())
            .build().create(CanvasApiService::class.java)
        return PendingTasksRepository(CanvasPagedApi(raw, baseUrl))
    }
}
