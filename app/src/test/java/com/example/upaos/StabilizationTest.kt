package com.example.upaos

import com.example.upaos.data.local.*
import com.example.upaos.data.model.*
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

class StabilizationTest {
    private fun event(generation: Long, value: Int) = notificationEventKey("student", "grades", "202620", generation, "course/average/$value")
    private fun item(key: String, read: Boolean = false) = NotificacionItem(id = notificationHistoryId(key), mensaje = "Synthetic", eventKey = key, leida = read)

    @Test fun retryAfterHistoryCommitDoesNotInsertTwiceOrResetReadReceipt() {
        val key = event(1, 14)
        val original = insertNotification(NotificacionesResponse(), item(key, true))
        // Simulate disk/process recreation before the retry.
        val restored = Gson().fromJson(Gson().toJson(original), NotificacionesResponse::class.java)
        val retried = insertNotification(restored, item(key))
        assertEquals(1, retried.total)
        assertEquals(0, retried.noLeidas)
        assertTrue(retried.notificaciones.single().leida)
    }

    @Test fun twelveFourteenTwelveAndFourteenAgainRemainDistinctChanges() {
        var history = NotificacionesResponse()
        for ((generation, value) in listOf(0L to 12, 1L to 14, 2L to 12, 3L to 14)) {
            val next = item(event(generation, value))
            history = insertNotification(history, next)
            history = insertNotification(history, next) // retry in same generation
        }
        assertEquals(4, history.total)
        assertEquals(4, history.notificaciones.map { it.id }.toSet().size)
    }

    @Test fun identicalValuesDoNotCollideAcrossAccountsSourcesOrPeriods() {
        val keys = listOf(notificationEventKey("a", "grades", "202620", 1, "14"),
            notificationEventKey("b", "grades", "202620", 1, "14"),
            notificationEventKey("a", "attendance", "202620", 1, "14"),
            notificationEventKey("a", "grades", "202610", 1, "14"))
        assertEquals(4, keys.toSet().size)
    }

    @Test fun legacyHistoryWithoutEventKeysIsRetained() {
        val legacy = Gson().fromJson("""{"notificaciones":[{"id":7,"mensaje":"Synthetic","leida":true}],"total":1,"no_leidas":0}""", NotificacionesResponse::class.java)
        assertNull(legacy.notificaciones.single().eventKey)
        val combined = insertNotification(legacy, item(event(1, 14)))
        assertEquals(2, combined.total)
        assertEquals(1, combined.noLeidas)
        assertEquals(7L, combined.notificaciones.last().id)
    }

    @Test fun coursePeriodAndCareerMustAllMatchHistoricalSelection() {
        val json = Gson().toJson(GradesResponse(periodo = "202610", carrera = "UG"))
        assertNotNull(restoreGradeVariant(json, "202610", "UG"))
        assertNull(restoreGradeVariant(json, "202620", "UG"))
        assertNull(restoreGradeVariant(json, "202610", "PG"))
    }

    @Test fun malformedOrUnidentifiedHistoryCannotBePresentedUnderAnotherCareer() {
        for (json in listOf(null, "invalid", "{}", "{\"periodo\":\"202610\"}"))
            assertNull(restoreGradeVariant(json, "202610", "UG"))
    }

    @Test fun historicalCacheSeparatesAccountTermAndCareer() {
        val keys = listOf(gradeVariantKey("a", "202610", "UG"), gradeVariantKey("a", "202610", "PG"),
            gradeVariantKey("a", "202620", "UG"), gradeVariantKey("b", "202610", "UG"))
        assertEquals(4, keys.toSet().size)
    }

    @Test fun newlyCachedResourceRetentionDoesNotIncludeMessagesSubmissionsOrDocuments() {
        for (flow in listOf("inbox:inbox", "submission:1:2", "fileDetail:3", "discussion:1:2"))
            assertFalse(CanvasResourceRetention.managed(flow))
        for (flow in listOf("navigation:1", "files:1", "topics:1")) assertTrue(CanvasResourceRetention.managed(flow))
    }

    @Test fun expiredAndFutureDatedResourcesDoNotCountAsFresh() {
        val now = CanvasResourceRetention.MAX_AGE + 100
        assertTrue(CanvasResourceRetention.fresh(now, now))
        assertFalse(CanvasResourceRetention.fresh(99, now))
        assertFalse(CanvasResourceRetention.fresh(now + 1, now))
        assertFalse(CanvasResourceRetention.fresh(null, now))
    }

    @Test fun retentionEvictsOldestBeyondBoundAndKeepsRecentlyAccessedResource() {
        val times = (0..40).associate { "files:$it" to (1000L + it) }
        val removed = CanvasResourceRetention.evict(times, 1100)
        assertEquals(9, removed.size)
        assertTrue("files:0" in removed)
        assertFalse("files:40" in removed)
    }
}
