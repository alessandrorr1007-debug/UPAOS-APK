package com.example.upaos.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.upaos.data.model.NotificacionItem
import com.example.upaos.ui.theme.LocalUpaoDarkTheme
import com.example.upaos.ui.theme.minimalSectionPalette

/** Same visual language for today's preview and the notification inbox. */
@Composable
fun NotificationCard(
    notice: NotificacionItem,
    onClick: () -> Unit,
    compact: Boolean = false,
    timeLabel: String = tiempoRelativo(notice.fechaCreacion)
) {
    val palette = minimalSectionPalette(LocalUpaoDarkTheme.current)
    val absence = notice.mensaje.contains("falta", ignoreCase = true)
    val attendance = absence || notice.componente?.contains("asistencia", ignoreCase = true) == true ||
        notice.mensaje.contains("asistencia", ignoreCase = true)

    val accent = when {
        absence -> palette.dangerText
        attendance -> palette.greenText
        else -> palette.accentBlue
    }
    val iconContainerBg = when {
        absence -> palette.dangerBg
        attendance -> palette.greenBg
        else -> palette.softBlueBg
    }
    val unreadBorder = when {
        absence -> palette.dangerBorder
        attendance -> palette.greenText.copy(alpha = 0.35f)
        else -> palette.borderBlue
    }
    val category = when {
        absence -> "Falta registrada"
        attendance -> "Asistencia"
        else -> "Calificación"
    }
    val icon = when {
        absence -> Icons.Filled.WarningAmber
        attendance -> Icons.AutoMirrored.Filled.FactCheck
        else -> Icons.AutoMirrored.Filled.MenuBook
    }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        color = if (notice.leida) palette.surfaceSoft else palette.surface,
        border = BorderStroke(1.dp, if (notice.leida) palette.border else unreadBorder)
    ) {
        Column(
            modifier = Modifier.padding(if (compact) 12.dp else 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = iconContainerBg,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(9.dp)
                            .size(18.dp),
                        tint = accent
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = category,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = accent
                    )
                    if (timeLabel.isNotBlank()) {
                        Text(
                            text = timeLabel,
                            fontSize = 11.sp,
                            color = palette.textMuted
                        )
                    }
                }
                if (!notice.leida) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(accent)
                        )
                        Text(
                            text = "Sin leer",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = accent
                        )
                    }
                } else if (compact) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = palette.textMuted
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                notice.curso?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = toTitleCase(it),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary,
                        maxLines = if (compact) 2 else Int.MAX_VALUE,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!attendance) {
                    notice.componente?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = toTitleCase(it),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = accent
                        )
                    }
                }
                Text(
                    text = limpiarMensajeNotificacion(notice.mensaje, notice.curso),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = palette.textSecondary,
                    maxLines = if (compact) 3 else Int.MAX_VALUE,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!compact) {
                HorizontalDivider(color = palette.border, thickness = 1.dp)
                if (!notice.leida) {
                    TextButton(
                        onClick = onClick,
                        contentPadding = PaddingValues(horizontal = 0.dp),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = palette.accentBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Marcar como leída",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.accentBlue
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = palette.textMuted
                        )
                        Text(
                            text = "Leída",
                            fontSize = 11.sp,
                            color = palette.textMuted
                        )
                    }
                }
            }
        }
    }
}
