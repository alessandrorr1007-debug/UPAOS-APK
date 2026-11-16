package com.example.upaos.ui.canvas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.upaos.data.model.CanvasCourse
import com.example.upaos.ui.components.RefreshableContent

@Composable
internal fun CanvasCoursesOverview(courses: List<CanvasCourse>, loading: Boolean, refreshing: Boolean,
    refreshCourses: () -> Unit, open: (CanvasCourse) -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        RefreshableContent(refreshing, refreshCourses, Modifier.fillMaxSize()) {
            LazyColumn(contentPadding = PaddingValues(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    Text("${courses.size} cursos activos", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                items(courses, key = { it.id }) { course -> CanvasCourseCard(course) { open(course) } }
                if (!loading && courses.isEmpty()) item { Text("Sin cursos activos") }
            }
        }
    }
}
