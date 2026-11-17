package com.example.upaos.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.upaos.data.model.NotificacionItem
import com.example.upaos.ui.components.toTitleCase
import com.example.upaos.ui.theme.LocalUpaoDarkTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val TodayCardShape = RoundedCornerShape(20.dp)
private val TodayInnerCardShape = RoundedCornerShape(14.dp)

private fun campusFormat(time: Long, pattern: String) =
    SimpleDateFormat(pattern, Locale.forLanguageTag("es-PE"))
        .apply { timeZone = campusTimeZone }
        .format(Date(time))

@Composable
internal fun TodayWeekBanner(label: String) {
    val palette = todayPalette(LocalUpaoDarkTheme.current)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(palette.weekGradientStart, palette.weekGradientMid, palette.weekGradientEnd)
                )
            )
            .testTag("today_week_banner")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.20f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarToday,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun TodayHero(
    now: Long,
    classes: Int?,
    tasks: Int?,
    news: Int?,
    onSchedule: () -> Unit = {},
    onTasks: () -> Unit = {},
    onNotifications: () -> Unit = {}
) {
    val palette = todayPalette(LocalUpaoDarkTheme.current)

    Surface(
        shape = TodayCardShape,
        color = palette.surface,
        border = BorderStroke(1.dp, palette.border),
        shadowElevation = if (palette.isDark) 0.dp else 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = campusFormat(now, "EEEE, d 'de' MMMM").replaceFirstChar { it.titlecase() },
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = palette.textSecondary
                    )
                    Text(
                        text = "Tu día, a la vista",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.textPrimary
                    )
                    Text(
                        text = "Clases, pendientes y novedades de hoy",
                        fontSize = 12.5.sp,
                        color = palette.textSecondary
                    )
                }

                Spacer(Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(50),
                    color = palette.hoyBadgeBg
                ) {
                    Text(
                        text = "HOY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.hoyBadgeText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HeroMetricTile(
                    count = classes,
                    label = "Clases",
                    icon = Icons.Outlined.Schedule,
                    style = palette.classesMetric,
                    testTag = "today_metric_classes",
                    modifier = Modifier.weight(1f),
                    onClick = onSchedule
                )
                HeroMetricTile(
                    count = tasks,
                    label = "Pendientes",
                    icon = Icons.Outlined.CalendarToday,
                    style = palette.pendingMetric,
                    testTag = "today_metric_pending",
                    modifier = Modifier.weight(1f),
                    onClick = onTasks
                )
                HeroMetricTile(
                    count = news,
                    label = "Novedades",
                    icon = Icons.Outlined.Notifications,
                    style = palette.newsMetric,
                    testTag = "today_metric_news",
                    modifier = Modifier.weight(1f),
                    onClick = onNotifications
                )
            }
        }
    }
}

@Composable
private fun HeroMetricTile(
    count: Int?,
    label: String,
    icon: ImageVector,
    style: TodayMetricStyle,
    testTag: String,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .defaultMinSize(minHeight = 52.dp)
            .clip(TodayInnerCardShape)
            .background(style.containerBg)
            .clickable(role = Role.Button, onClick = onClick)
            .testTag(testTag)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = style.iconTint,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = count?.toString() ?: "—",
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = style.countColor,
                maxLines = 1
            )
        }
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            color = style.labelColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun TodaySectionTitle(title: String, count: Int) {
    val palette = todayPalette(LocalUpaoDarkTheme.current)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = title,
            fontSize = 15.5.sp,
            fontWeight = FontWeight.Bold,
            color = palette.textPrimary,
            modifier = Modifier.weight(1f)
        )
        Surface(color = palette.softBlueBg, shape = CircleShape) {
            Text(
                text = count.toString(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = palette.softBlueText,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

data class TodayCardTheme(
    val cardBg: Color,
    val cardBorder: Color,
    val cardIconBg: Color,
    val cardAccent: Color,
    val cardTextColor: Color,
    val cardSubtextColor: Color,
    val innerCardBg: Color,
    val innerCardBorder: Color
)

@Composable
fun rememberTodayCardTheme(): TodayCardTheme {
    val palette = todayPalette(LocalUpaoDarkTheme.current)
    return TodayCardTheme(
        cardBg = palette.surface,
        cardBorder = palette.border,
        cardIconBg = palette.softBlueBg,
        cardAccent = palette.accentBlue,
        cardTextColor = palette.textPrimary,
        cardSubtextColor = palette.textSecondary,
        innerCardBg = palette.innerSurface,
        innerCardBorder = palette.innerBorder
    )
}

@Composable
fun SourceStamp(updated: Long?, current: Boolean, color: Color? = null) {
    val palette = todayPalette(LocalUpaoDarkTheme.current)
    if (!current && updated != null) {
        Text(
            text = "Última actualización · ${campusFormat(updated, "dd/MM HH:mm")}",
            style = MaterialTheme.typography.labelSmall,
            color = color ?: palette.textMuted
        )
    }
}

@Composable
fun TodayEmptyState(title: String, description: String) {
    val palette = todayPalette(LocalUpaoDarkTheme.current)
    Surface(
        color = palette.surface,
        border = BorderStroke(1.dp, palette.border),
        shape = TodayCardShape,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = palette.accentBlue,
                modifier = Modifier.size(22.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = title,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
                Text(
                    text = description,
                    fontSize = 12.5.sp,
                    color = palette.textSecondary
                )
            }
        }
    }
}

@Composable
fun TodaySchedule(
    classes: List<NextClass>,
    available: Boolean,
    now: Long,
    updated: Long?,
    current: Boolean,
    onSchedule: () -> Unit
) {
    if (classes.isEmpty()) return
    val next = classes.firstOrNull { it.end > now }
    val allDone = next == null
    val palette = todayPalette(LocalUpaoDarkTheme.current)

    Surface(
        shape = TodayCardShape,
        color = palette.surface,
        border = BorderStroke(1.dp, palette.border),
        shadowElevation = if (palette.isDark) 0.dp else 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(palette.accentBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        text = "Tus clases de hoy",
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary
                    )
                    Text(
                        text = when {
                            !available -> "Horario por actualizar"
                            classes.isEmpty() -> "Hoy no tienes clases programadas"
                            allDone -> "Clases de hoy finalizadas"
                            next.start <= now -> "Estás en horario de clase"
                            else -> "Tu siguiente clase de hoy"
                        },
                        fontSize = 12.sp,
                        color = palette.textSecondary
                    )
                }

                if (allDone) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(palette.finishedStatus.badgeBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Clases finalizadas",
                            tint = palette.finishedStatus.badgeText,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                classes.forEachIndexed { index, lesson ->
                    val finished = lesson.end <= now
                    val ongoing = !finished && lesson.start <= now
                    val isNext = lesson == next
                    val statusStyle = when {
                        finished -> palette.finishedStatus
                        ongoing -> palette.activeStatus
                        else -> palette.upcomingStatus
                    }
                    val statusLabel = when {
                        finished -> "Finalizada"
                        ongoing -> "En curso"
                        isNext -> "Próxima clase"
                        else -> null
                    }
                    val prevFinished = index > 0 && classes[index - 1].end <= now
                    val lineAboveColor = if (finished && prevFinished) palette.successGreen else palette.timelineNeutralTrack
                    val nextFinished = index < classes.lastIndex && classes[index + 1].end <= now
                    val lineBelowColor = if (finished && nextFinished) palette.successGreen else palette.timelineNeutralTrack

                    CompactClassRow(
                        start = campusFormat(lesson.start, "HH:mm"),
                        end = campusFormat(lesson.end, "HH:mm"),
                        title = toTitleCase(lesson.name),
                        room = lesson.room,
                        statusLabel = statusLabel,
                        statusStyle = statusStyle,
                        connectAbove = index > 0,
                        connectBelow = index < classes.lastIndex,
                        lineAboveColor = lineAboveColor,
                        lineBelowColor = lineBelowColor,
                        highlighted = isNext,
                        palette = palette
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!current && updated != null) {
                    Box(modifier = Modifier.weight(1f)) {
                        SourceStamp(updated, current, color = palette.textMuted)
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                }

                Row(
                    modifier = Modifier
                        .defaultMinSize(minHeight = 40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(role = Role.Button, onClick = onSchedule)
                        .testTag("today_view_schedule_button")
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Ver horario",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.accentBlue
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = palette.accentBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactClassRow(
    start: String,
    end: String,
    title: String,
    room: String,
    statusLabel: String?,
    statusStyle: TodayStatusStyle,
    connectAbove: Boolean,
    connectBelow: Boolean,
    lineAboveColor: Color,
    lineBelowColor: Color,
    highlighted: Boolean,
    palette: TodayPalette
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        Column(
            modifier = Modifier
                .width(42.dp)
                .padding(top = 10.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = start,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = palette.accentBlue
            )
            Text(
                text = end,
                fontSize = 11.sp,
                color = palette.textSecondary
            )
        }

        Box(
            modifier = Modifier
                .width(18.dp)
                .fillMaxHeight()
                .drawBehind {
                    val x = size.width / 2f
                    val y = 20.dp.toPx()
                    if (connectAbove) {
                        drawLine(lineAboveColor, Offset(x, 0f), Offset(x, y), 1.6.dp.toPx())
                    }
                    if (connectBelow) {
                        drawLine(lineBelowColor, Offset(x, y), Offset(x, size.height), 1.6.dp.toPx())
                    }
                    drawCircle(statusStyle.dotColor.copy(alpha = 0.22f), 6.5.dp.toPx(), Offset(x, y))
                    drawCircle(statusStyle.dotColor, 3.6.dp.toPx(), Offset(x, y))
                }
        )

        Surface(
            shape = TodayInnerCardShape,
            color = if (highlighted && !palette.isDark) palette.softBlueBg.copy(alpha = 0.45f) else palette.innerSurface,
            border = BorderStroke(
                1.dp,
                if (highlighted) palette.accentBlue.copy(alpha = 0.45f) else palette.innerBorder
            ),
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp, bottom = if (connectBelow) 8.dp else 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = palette.textMuted,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = room,
                            fontSize = 11.sp,
                            color = palette.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (!statusLabel.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = statusStyle.badgeBg
                    ) {
                        Text(
                            text = statusLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = statusStyle.badgeText,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.5.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TodayNewsCard(
    items: List<NotificacionItem>,
    now: Long,
    updated: Long?,
    current: Boolean,
    onNotifications: () -> Unit
) {
    if (items.isEmpty()) return
    val unread = items.count { !it.leida }
    val palette = todayPalette(LocalUpaoDarkTheme.current)

    Surface(
        shape = TodayCardShape,
        color = palette.surface,
        border = BorderStroke(1.dp, palette.border),
        shadowElevation = if (palette.isDark) 0.dp else 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(palette.accentBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        text = "Novedades de hoy",
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary
                    )
                    Text(
                        text = if (unread > 0) "$unread sin leer · ${items.size} avisos hoy"
                        else "Estás al día · ${items.size} avisos hoy",
                        fontSize = 12.sp,
                        color = palette.textSecondary
                    )
                }
            }

            items.take(3).forEach { notice ->
                androidx.compose.runtime.key(notice.id) {
                    com.example.upaos.ui.components.NotificationCard(
                        notice = notice,
                        onClick = onNotifications,
                        compact = true,
                        timeLabel = notificationMillis(notice.fechaCreacion)?.let { campusFormat(it, "HH:mm") }.orEmpty()
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!current && updated != null) {
                    Box(modifier = Modifier.weight(1f)) {
                        SourceStamp(updated, current)
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                }
                TextButton(onClick = onNotifications) {
                    Text(
                        text = if (items.size > 3) "Ver todas las novedades (${items.size})" else "Ver notificaciones",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = palette.accentBlue
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = palette.accentBlue,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}
