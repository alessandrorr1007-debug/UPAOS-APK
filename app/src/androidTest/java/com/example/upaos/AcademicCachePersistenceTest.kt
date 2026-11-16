package com.example.upaos

import android.content.Context
import android.content.ContextWrapper
import androidx.test.platform.app.InstrumentationRegistry
import com.example.upaos.data.local.*
import com.example.upaos.data.model.CanvasCourse
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
import org.junit.Assert.*
import org.junit.Test

/** Synthetic accounts only. Can write/read across separate instrumentation processes. */
class AcademicCachePersistenceTest {
    companion object {
        private const val USER = "000000071"
        private const val OTHER = "000000072"
        private val stores by lazy {
            listOf("api", "grades", "canvas").associateWith { name ->
                androidx.datastore.preferences.core.PreferenceDataStoreFactory.create {
                    java.io.File(context.filesDir, "$name.preferences_pb")
                }
            }
        }
        private val context = object : ContextWrapper(InstrumentationRegistry.getInstrumentation().targetContext) {
            override fun getApplicationContext(): Context = this
            override fun getFilesDir() = java.io.File(super.getFilesDir(), "academic_cache_regression").apply { mkdirs() }
            override fun getSharedPreferences(name: String, mode: Int) = super.getSharedPreferences("academic_cache_regression_$name", mode)
        }
    }

    private val apiStore get() = stores.getValue("api")
    private val gradesStore get() = stores.getValue("grades")
    private val canvasStore get() = stores.getValue("canvas")

    @Test fun diskSnapshotAndTimestampSurviveRecreationOrProcessRestart() = runBlocking {
        val phase = InstrumentationRegistry.getArguments().getString("cachePhase")
        if (phase != "read") {
            TokenManager(context).saveCuenta(USER, "synthetic")
            GradesCache(context, gradesStore).guardar("notas_$USER", "{\"periodo\":\"202620\",\"cursos\":[]}")
            ApiCache(context, apiStore).guardar("horario_${USER}_202620", "{\"success\":true,\"cursos\":[]}")
            CanvasCache(context, canvasStore).guardarCursos(USER, listOf(CanvasCourse(id=71, name="Synthetic")))
        }
        if (phase != "write") {
            assertTrue(GradesCache(context, gradesStore).cargar("notas_$USER")!!.contains("202620"))
            assertNotNull(ApiCache(context, apiStore).actualizado("horario_${USER}_202620"))
            assertEquals(71L, CanvasCache(context, canvasStore).cargarCursos(USER).single().id)
        }
    }

    @Test fun deletingOneAccountKeepsOtherAccountAndLegacyKeys() = runBlocking {
        val tokens = TokenManager(context)
        for (user in listOf(USER, OTHER)) {
            tokens.saveCuenta(user, "synthetic")
            ApiCache(context, apiStore).guardar("asistencia_$user", "[]")
            GradesCache(context, gradesStore).guardar("notas_$user", "{\"cursos\":[]}")
        }
        ApiCache(context, apiStore).limpiar(USER)
        GradesCache(context, gradesStore).limpiar(USER)
        assertNull(ApiCache(context, apiStore).cargar("asistencia_$USER"))
        assertNull(GradesCache(context, gradesStore).cargar("notas_$USER"))
        assertEquals("[]", ApiCache(context, apiStore).cargar("asistencia_$OTHER"))
        assertNotNull(GradesCache(context, gradesStore).cargar("notas_$OTHER"))
    }

    @Test fun adaptiveDeadlineAndSnapshotsAreAccountScoped() {
        NotificationPreferences(context, USER).nextAttendanceAttempt = 123L
        NotificationPreferences(context, OTHER).nextAttendanceAttempt = 456L
        assertEquals(123L, NotificationPreferences(context, USER).nextAttendanceAttempt)
        assertEquals(456L, NotificationPreferences(context, OTHER).nextAttendanceAttempt)
    }
    @Test fun concurrentNotificationUpdatesAreAtomicAndRetriesIdempotent() = runBlocking {
        TokenManager(context).saveCuenta(USER, "synthetic")
        val cache = ApiCache(context, apiStore)
        val key = "notificaciones_$USER"
        cache.guardar(key, com.google.gson.Gson().toJson(com.example.upaos.data.model.NotificacionesResponse()))
        kotlinx.coroutines.coroutineScope {
            repeat(40) { index ->
                launch(kotlinx.coroutines.Dispatchers.Default) {
                    repeat(2) {
                        cache.actualizar(key) { json ->
                            val gson = com.google.gson.Gson()
                            val history = gson.fromJson(json, com.example.upaos.data.model.NotificacionesResponse::class.java)
                            val event = notificationEventKey(USER, "grades", "202620", 1, "$index")
                            gson.toJson(insertNotification(history,
                                com.example.upaos.data.model.NotificacionItem(id = notificationHistoryId(event), eventKey = event)))
                        }
                    }
                }
            }
        }
        val result = com.google.gson.Gson().fromJson(cache.cargar(key), com.example.upaos.data.model.NotificacionesResponse::class.java)
        assertEquals(40, result.total)
        assertEquals(40, result.notificaciones.map { it.eventKey }.toSet().size)
    }

    @Test fun newlyCachedFileListingsAreRestoredWithoutStoringDocumentBytes() = runBlocking {
        TokenManager(context).saveLogin(USER, "synthetic", true)
        val first = CanvasCache(context, canvasStore)
        first.saveCanvasList(USER, "files:71", "[]")
        val second = CanvasCache(context, canvasStore)
        assertEquals("[]", second.readCanvasList(USER, "files:71"))
        assertNotNull(second.canvasListUpdatedAt(USER, "files:71"))
        second.saveCanvasList(USER, "files:71", "x".repeat(CanvasResourceRetention.MAX_BYTES + 1))
        assertEquals("[]", second.readCanvasList(USER, "files:71"))
    }

}
