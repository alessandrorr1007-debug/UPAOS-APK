package com.example.upaos.ui.grades

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.upaos.ui.components.CourseIconBadgeLight
import com.example.upaos.ui.components.resolveCourseSubjectIcon

data class CourseCardStyle(
    val icon: ImageVector,
    val iconBgColor: Color,
    val lightPillBg: Color,
    val lightPillLabel: Color,
    val lightPillValue: Color,
    val darkPillBg: Color,
    val darkPillLabel: Color,
    val darkPillValue: Color
)

private val StyleBlue = CourseCardStyle(
    icon = Icons.Rounded.MenuBook,
    iconBgColor = CourseIconBadgeLight,
    lightPillBg = Color(0xFFEAF2FF),
    lightPillLabel = Color(0xFF3E5C8A),
    lightPillValue = Color(0xFF0A3A8A),
    darkPillBg = Color(0xFF132948),
    darkPillLabel = Color(0xFF8CAAD6),
    darkPillValue = Color(0xFFFFFFFF)
)

fun resolveCourseCardStyle(courseName: String, crn: String? = null, index: Int = 0): CourseCardStyle {
    return StyleBlue.copy(icon = resolveCourseSubjectIcon(courseName))
}
