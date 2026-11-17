package com.example.upaos.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import com.example.upaos.service.SyncDiagnostics
import com.example.upaos.service.SyncPolicy
import com.example.upaos.ui.theme.LocalUpaoDarkTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
internal fun NotificationsSettingsContent(
    usuario: String,
    checkNotasEnabled: Boolean,
    gradeSyncPreference: SyncPolicy.Preference,
    checkAsistenciaEnabled: Boolean,
    recordatorioClasesEnabled: Boolean,
    guardando: Boolean,
    frequencyOptions: List<FrequencyOption>,
    onToggleAll: (Boolean) -> Unit,
    onToggleNotas: (Boolean) -> Unit,
    onSelectFrequency: (FrequencyOption) -> Unit,
    onToggleAsistencia: (Boolean) -> Unit,
    onToggleClases: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val palette = settingsPalette(LocalUpaoDarkTheme.current)
    val anyEnabled = checkNotasEnabled || checkAsistenciaEnabled || recordatorioClasesEnabled
    var diagnosticExpanded by rememberSaveable { mutableStateOf(false) }

    val powerManager = remember {
        context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    }
    val isIgnoringBattery = remember(powerManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && powerManager != null) {
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else true
    }

    val diagnosticSummary by produceState(initialValue = "", usuario) {
        while (true) {
            value = withContext(Dispatchers.IO) {
                val diagnostic = SyncDiagnostics(context)
                diagnostic.summary() + "\n" + diagnostic.workSummary() + "\nNotificaciones de Android: " +
                    (if (NotificationManagerCompat.from(context).areNotificationsEnabled()) "habilitadas" else "deshabilitadas")
            }
            delay(5_000)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Master card: Activar notificaciones
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = palette.surfaceSoftBlue,
            border = BorderStroke(1.dp, palette.borderBlue),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                Text(
                    text = "Activar notificaciones",
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = anyEnabled,
                    onCheckedChange = onToggleAll,
                    enabled = !guardando,
                    colors = settingsSwitchColors(palette),
                    modifier = Modifier.testTag("notifications_master_switch")
                )
            }
        }

        // Main options card: Notas + Frecuencia + Asistencias + Clases
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = palette.surface,
            border = BorderStroke(1.dp, palette.border),
            shadowElevation = if (palette.isDark) 0.dp else 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NotificationToggleRow(
                    title = "Notificaciones de Notas",
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    iconBg = palette.softBlueBg,
                    iconTint = palette.softBlueText,
                    checked = checkNotasEnabled,
                    enabled = !guardando,
                    palette = palette,
                    testTag = "notifications_grades_switch",
                    onCheckedChange = onToggleNotas
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Frecuencia de revisión de notas",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                    Column(Modifier.selectableGroup()) {
                        frequencyOptions.forEach { option ->
                            val selected = gradeSyncPreference == option.preference
                            val enabled = checkNotasEnabled && !guardando
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .selectable(
                                        selected = selected,
                                        enabled = enabled,
                                        role = Role.RadioButton,
                                        onClick = { onSelectFrequency(option) }
                                    )
                                    .padding(horizontal = 4.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selected,
                                    onClick = null,
                                    enabled = enabled,
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = palette.accentBlue,
                                        unselectedColor = palette.textMuted
                                    )
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = option.title,
                                    fontSize = 14.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (enabled) palette.textPrimary else palette.textMuted
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = palette.border, thickness = 1.dp)

                NotificationToggleRow(
                    title = "Notificaciones de Asistencias",
                    icon = Icons.AutoMirrored.Filled.FactCheck,
                    iconBg = palette.amberBg,
                    iconTint = palette.amberText,
                    checked = checkAsistenciaEnabled,
                    enabled = !guardando,
                    palette = palette,
                    testTag = "notifications_attendance_switch",
                    onCheckedChange = onToggleAsistencia
                )

                HorizontalDivider(color = palette.border, thickness = 1.dp)

                NotificationToggleRow(
                    title = "Recordatorio de Clases",
                    icon = Icons.Filled.Schedule,
                    iconBg = palette.greenBg,
                    iconTint = palette.greenText,
                    checked = recordatorioClasesEnabled,
                    enabled = !guardando,
                    palette = palette,
                    testTag = "notifications_classes_switch",
                    onCheckedChange = onToggleClases
                )
            }
        }

        // Battery & sync guidance card
        if ((checkNotasEnabled || checkAsistenciaEnabled) && !isIgnoringBattery) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = palette.surfaceSoftBlue,
                border = BorderStroke(1.dp, palette.borderBlue),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = palette.accentBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Sincronización y batería",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.accentBlue
                        )
                    }
                    Text(
                        text = "Android puede aplazar revisiones durante el reposo o el ahorro de batería. Si observas retrasos, revisa los ajustes de la aplicación. No es necesario desactivar el ahorro para usar UPAO S.",
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp,
                        color = palette.textPrimary
                    )
                    OutlinedButton(
                        onClick = {
                            try {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                }
                            } catch (_: Exception) {
                                try {
                                    context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                                } catch (_: Exception) {
                                    Toast.makeText(context, "Revisa Ajustes > Aplicaciones > UPAO S", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.2.dp, palette.accentBlue),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = palette.surface,
                            contentColor = palette.accentBlue
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Revisar ajustes de la aplicación",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Collapsible technical sync diagnostic card
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = palette.surface,
            border = BorderStroke(1.dp, palette.border),
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp)
                        .clickable(role = Role.Button) { diagnosticExpanded = !diagnosticExpanded }
                        .testTag("notifications_diagnostic_toggle")
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(palette.softBlueBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MonitorHeart,
                            contentDescription = null,
                            tint = palette.softBlueText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "Diagnóstico de sincronización",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (diagnosticExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (diagnosticExpanded) "Contraer diagnóstico" else "Ver diagnóstico",
                        tint = palette.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                AnimatedVisibility(visible = diagnosticExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HorizontalDivider(color = palette.border, thickness = 1.dp)
                        Text(
                            text = diagnosticSummary.ifBlank { "Consultando estado de sincronización…" },
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = palette.textSecondary
                        )
                        Text(
                            text = "Para recibir avisos, permite las notificaciones de UPAO S y sus categorías en los ajustes de Android. La revisión requiere una sesión guardada y conexión.",
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp,
                            color = palette.textMuted
                        )
                    }
                }
            }
        }

        // Bottom primary action button
        Button(
            onClick = {
                val intent = if (Build.VERSION.SDK_INT >= 26) {
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                } else {
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                }
                context.startActivity(intent)
            },
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = palette.accentBlue,
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 50.dp)
                .testTag("notifications_permissions_button")
        ) {
            Icon(
                imageVector = Icons.Outlined.AdminPanelSettings,
                contentDescription = null,
                modifier = Modifier.size(19.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Revisar permisos de notificaciones",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun NotificationToggleRow(
    title: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    checked: Boolean,
    enabled: Boolean,
    palette: SettingsPalette,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = title,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Bold,
            color = palette.textPrimary,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = settingsSwitchColors(palette),
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
internal fun settingsSwitchColors(palette: SettingsPalette) = SwitchDefaults.colors(
    checkedThumbColor = Color.White,
    checkedTrackColor = palette.accentBlue,
    uncheckedThumbColor = Color.White,
    uncheckedTrackColor = if (palette.isDark) Color(0xFF28354D) else Color(0xFFCBD5E1),
    uncheckedBorderColor = Color.Transparent
)
