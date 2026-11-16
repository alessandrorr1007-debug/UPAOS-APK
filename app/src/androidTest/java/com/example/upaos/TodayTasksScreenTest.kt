package com.example.upaos

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.upaos.data.agenda.PendingTask
import com.example.upaos.data.agenda.PendingTaskResult
import com.example.upaos.ui.home.AllPendingTasks
import com.example.upaos.ui.home.PendingTasksPreview
import com.example.upaos.ui.home.TodayTasksState
import com.example.upaos.ui.theme.UPAOSTheme
import java.time.Instant
import org.junit.Rule
import org.junit.Test

class TodayTasksScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun opensAllTasksAndReturnsAcrossThemeChange() {
        val now = Instant.parse("2026-11-12T16:00:00Z")
        val state = TodayTasksState().apply {
            loading = false
            snapshot = PendingTaskResult((1L..4L).map {
                PendingTask(1, it, "Curso de prueba", "Actividad $it", now.plusSeconds(it * 3600), null, null)
            }, partial = false)
        }
        val opened = mutableStateOf(false)
        val dark = mutableStateOf(false)
        compose.setContent {
            UPAOSTheme(darkTheme = dark.value) {
                if (opened.value) AllPendingTasks(state, now.toEpochMilli(), {}) { opened.value = false }
                else PendingTasksPreview(state, now.toEpochMilli(), {}) { opened.value = true }
            }
        }
        compose.onNodeWithText("4 tareas pendientes").assertIsDisplayed()
        compose.onNodeWithText("Ver todas las tareas").performClick()
        compose.onNodeWithText("Volver a Hoy").assertIsDisplayed()
        compose.runOnIdle { dark.value = true }
        compose.onNodeWithText("4 tareas pendientes").assertIsDisplayed()
        compose.onNodeWithText("Volver a Hoy").performClick()
        compose.onNodeWithText("Ver todas las tareas").assertIsDisplayed()
    }
}
