package com.example.upaos.ui.settings

import android.app.Activity
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
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
import com.example.upaos.data.local.BiometricHelper
import com.example.upaos.data.local.NotificationPreferences
import com.example.upaos.data.local.SecurityPreferences
import com.example.upaos.data.local.TokenManager
import com.example.upaos.data.profile.StudentProfile
import com.example.upaos.service.ClassReminderScheduler
import com.example.upaos.service.GradeSyncController
import com.example.upaos.service.SyncPolicy
import com.example.upaos.service.SyncScheduler
import com.example.upaos.ui.theme.LocalUpaoDarkTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class SettingsSection(val title: String) {
    GENERAL("Ajustes"),
    PRIVACY("Privacidad y datos"),
    NOTIFICATIONS("Notificaciones"),
    HELP("Ayuda y soporte"),
    INTEGRATIONS("Integraciones")
}

internal data class FrequencyOption(
    val preference: SyncPolicy.Preference,
    val title: String,
    val description: String
)

private val frequencyOptions = listOf(
    FrequencyOption(
        SyncPolicy.Preference.SIEMPRE_INTENSIVO,
        "Frecuente",
        "Revisa las notas aproximadamente cada 2 minutos. Puede consumir más batería y mostrar un aviso de revisión en segundo plano. Android puede retrasar o limitar las revisiones; en ese caso se mantiene el respaldo de revisión normal."
    ),
    FrequencyOption(
        SyncPolicy.Preference.NORMAL,
        "Normal",
        "Revisa las notas en segundo plano aproximadamente cada 15 minutos. Android puede retrasar las revisiones por batería o conexión."
    ),
    FrequencyOption(
        SyncPolicy.Preference.AUTOMATICO,
        "Automático",
        "Usa revisión frecuente en semanas de evaluaciones (7–9 y 15–17), de 07:00 a 23:00. El resto del tiempo utiliza el modo normal. Android puede retrasar o limitar las revisiones."
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    usuario: String?,
    onBack: () -> Unit,
    openIntegrations: Boolean = false,
    onAccountRemoved: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val palette = settingsPalette(LocalUpaoDarkTheme.current)

    var section by rememberSaveable(usuario, openIntegrations) {
        mutableStateOf(if (openIntegrations) SettingsSection.INTEGRATIONS else SettingsSection.GENERAL)
    }
    val pageScroll = key(section) { rememberScrollState() }
    fun back() {
        if (section == SettingsSection.GENERAL) onBack() else section = SettingsSection.GENERAL
    }
    BackHandler(enabled = section != SettingsSection.GENERAL) { back() }

    val notificationPrefs = remember { NotificationPreferences(context) }
    val securityPrefs = remember { SecurityPreferences(context) }
    val puedeUsarBiometria = remember { BiometricHelper.puedeAutenticar(context) }
    var biometricLockEnabled by remember { mutableStateOf(securityPrefs.isBiometricLockEnabled) }

    var checkNotasEnabled by remember { mutableStateOf(notificationPrefs.checkNotasEnabled) }
    var gradeSyncPreference by remember { mutableStateOf(notificationPrefs.gradeSyncPreference) }
    var pendingFrequency by remember { mutableStateOf<FrequencyOption?>(null) }
    var checkAsistenciaEnabled by remember { mutableStateOf(notificationPrefs.checkAsistenciaEnabled) }
    var recordatorioClasesEnabled by remember { mutableStateOf(notificationPrefs.recordatorioClasesEnabled) }
    var cargando by remember { mutableStateOf(true) }
    var guardando by remember { mutableStateOf(false) }

    var nombreEstudiante by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(usuario) {
        if (usuario == null) {
            cargando = false
            return@LaunchedEffect
        }
        nombreEstudiante = withContext(Dispatchers.IO) {
            val tokens = TokenManager(context)
            val savedName = tokens.getCuentas().firstOrNull { it.usuario == usuario }?.nombre
            val profileName = try {
                tokens.getProfileJson(usuario)?.let { StudentProfile.parse(it).fullName }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
            StudentProfile.normalizeName(profileName ?: savedName)
        }
        checkAsistenciaEnabled = notificationPrefs.checkAsistenciaEnabled
        checkNotasEnabled = notificationPrefs.checkNotasEnabled
        recordatorioClasesEnabled = notificationPrefs.recordatorioClasesEnabled
        if (checkAsistenciaEnabled || checkNotasEnabled) {
            SyncScheduler.start(context)
        }
        if (recordatorioClasesEnabled) {
            ClassReminderScheduler.reprogramar(context)
        }
        cargando = false
    }

    fun guardar(
        cambiarNotas: Boolean? = null,
        cambiarAsistencia: Boolean? = null,
        cambiarRecordatorioClases: Boolean? = null
    ) {
        if (usuario == null) return
        guardando = true
        scope.launch {
            try {
                if (cambiarNotas != null) {
                    checkNotasEnabled = cambiarNotas
                    notificationPrefs.checkNotasEnabled = cambiarNotas
                    if (cambiarNotas || checkAsistenciaEnabled) {
                        SyncScheduler.start(context)
                        if (cambiarNotas) SyncScheduler.runNow(context)
                    } else {
                        SyncScheduler.cancel(context)
                    }
                    if (cambiarAsistencia == null && cambiarRecordatorioClases == null) {
                        val msg = if (cambiarNotas) "Notificaciones de notas activadas" else "Notificaciones de notas desactivadas"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                }
                if (cambiarAsistencia != null) {
                    checkAsistenciaEnabled = cambiarAsistencia
                    notificationPrefs.checkAsistenciaEnabled = cambiarAsistencia
                    if (cambiarAsistencia || checkNotasEnabled) {
                        SyncScheduler.start(context)
                        SyncScheduler.runNow(context)
                    } else {
                        SyncScheduler.cancel(context)
                    }
                    if (cambiarNotas == null && cambiarRecordatorioClases == null) {
                        Toast.makeText(
                            context,
                            if (cambiarAsistencia) "Notificaciones de asistencia activadas"
                            else "Notificaciones de asistencia desactivadas",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                if (cambiarRecordatorioClases != null) {
                    recordatorioClasesEnabled = cambiarRecordatorioClases
                    notificationPrefs.recordatorioClasesEnabled = cambiarRecordatorioClases
                    if (cambiarRecordatorioClases) {
                        ClassReminderScheduler.reprogramar(context)
                    } else {
                        ClassReminderScheduler.cancelarTodos(context)
                    }
                    if (cambiarNotas == null && cambiarAsistencia == null) {
                        Toast.makeText(
                            context,
                            if (cambiarRecordatorioClases) "Recordatorios de clase activados"
                            else "Recordatorios de clase desactivados",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                if (cambiarNotas != null && cambiarAsistencia != null && cambiarRecordatorioClases != null) {
                    Toast.makeText(
                        context,
                        if (cambiarNotas) "Notificaciones activadas" else "Notificaciones desactivadas",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error al guardar: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
            guardando = false
        }
    }

    pendingFrequency?.let { option ->
        AlertDialog(
            onDismissRequest = { pendingFrequency = null },
            containerColor = palette.surface,
            titleContentColor = palette.textPrimary,
            textContentColor = palette.textSecondary,
            title = { Text("Revisión ${option.title.lowercase()}") },
            text = { Text(option.description) },
            confirmButton = {
                TextButton(
                    onClick = {
                        notificationPrefs.gradeSyncPreference = option.preference
                        gradeSyncPreference = option.preference
                        pendingFrequency = null
                        GradeSyncController.reconcile(context)
                    }
                ) {
                    Text("Aceptar", color = palette.accentBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingFrequency = null }) {
                    Text("Cancelar", color = palette.textSecondary)
                }
            }
        )
    }

    Scaffold(
        containerColor = palette.background,
        topBar = {
            Column(Modifier.background(palette.background)) {
                TopAppBar(
                    title = {
                        Text(
                            text = section.title,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = palette.background,
                        titleContentColor = palette.textPrimary,
                        navigationIconContentColor = palette.textPrimary
                    ),
                    navigationIcon = {
                        IconButton(
                            onClick = { back() },
                            modifier = Modifier.testTag("settings_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver"
                            )
                        }
                    }
                )
                HorizontalDivider(color = palette.border, thickness = 1.dp)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.background)
                .padding(padding)
                .verticalScroll(pageScroll)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            if (cargando) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = palette.accentBlue,
                        modifier = Modifier.padding(top = 48.dp)
                    )
                }
                return@Column
            }

            if (usuario == null) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = palette.surface,
                    border = BorderStroke(1.dp, palette.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(palette.softBlueBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = null,
                                tint = palette.accentBlue
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Sin sesión guardada",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.textPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Inicia sesión para activar la revisión automática de notas en segundo plano.",
                                fontSize = 12.5.sp,
                                color = palette.textSecondary
                            )
                        }
                    }
                }
                return@Column
            }

            when (section) {
                SettingsSection.GENERAL -> {
                    SettingsGeneralContent(
                        usuario = usuario,
                        studentName = nombreEstudiante,
                        biometricAvailable = puedeUsarBiometria,
                        biometricEnabled = biometricLockEnabled,
                        palette = palette,
                        onOpenPrivacy = { section = SettingsSection.PRIVACY },
                        onOpenNotifications = { section = SettingsSection.NOTIFICATIONS },
                        onOpenHelp = { section = SettingsSection.HELP },
                        onOpenIntegrations = { section = SettingsSection.INTEGRATIONS },
                        onToggleBiometric = { nuevo ->
                            if (nuevo && !puedeUsarBiometria) {
                                Toast.makeText(
                                    context,
                                    "Tu dispositivo no tiene biometría ni PIN/patrón configurado en los Ajustes del sistema.",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                biometricLockEnabled = nuevo
                                securityPrefs.isBiometricLockEnabled = nuevo
                                (context as? Activity)?.let { act ->
                                    if (nuevo) {
                                        act.window.setFlags(
                                            WindowManager.LayoutParams.FLAG_SECURE,
                                            WindowManager.LayoutParams.FLAG_SECURE
                                        )
                                    } else {
                                        act.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                                    }
                                }
                            }
                        }
                    )
                }

                SettingsSection.PRIVACY -> {
                    PrivacyControls(usuario = usuario, onAccountRemoved = onAccountRemoved)
                }

                SettingsSection.NOTIFICATIONS -> {
                    NotificationsSettingsContent(
                        usuario = usuario,
                        checkNotasEnabled = checkNotasEnabled,
                        gradeSyncPreference = gradeSyncPreference,
                        checkAsistenciaEnabled = checkAsistenciaEnabled,
                        recordatorioClasesEnabled = recordatorioClasesEnabled,
                        guardando = guardando,
                        frequencyOptions = frequencyOptions,
                        onToggleAll = { enabled ->
                            guardar(
                                cambiarNotas = enabled,
                                cambiarAsistencia = enabled,
                                cambiarRecordatorioClases = enabled
                            )
                        },
                        onToggleNotas = { nuevo ->
                            checkNotasEnabled = nuevo
                            guardar(cambiarNotas = nuevo)
                        },
                        onSelectFrequency = { pendingFrequency = it },
                        onToggleAsistencia = { nuevo ->
                            checkAsistenciaEnabled = nuevo
                            guardar(cambiarAsistencia = nuevo)
                        },
                        onToggleClases = { nuevo ->
                            recordatorioClasesEnabled = nuevo
                            guardar(cambiarRecordatorioClases = nuevo)
                        }
                    )
                }

                SettingsSection.HELP -> {
                    HelpSupportContent()
                }

                SettingsSection.INTEGRATIONS -> {
                    SettingsIntegrationsContent(
                        usuario = usuario,
                        onTokenSaved = { back() }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsGeneralContent(
    usuario: String,
    studentName: String?,
    biometricAvailable: Boolean,
    biometricEnabled: Boolean,
    palette: SettingsPalette,
    onOpenPrivacy: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenIntegrations: () -> Unit,
    onToggleBiometric: (Boolean) -> Unit
) {
    val initial = (studentName ?: usuario).trim().firstOrNull()?.uppercase() ?: "?"

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Student Profile Card
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = palette.surfaceSoftBlue,
            border = BorderStroke(1.dp, palette.borderBlue),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(palette.accentBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initial,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(32.dp)
                        .background(palette.borderBlue)
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = studentName ?: "Nombre no disponible",
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary
                    )
                    Text(
                        text = "Código: $usuario",
                        fontSize = 13.sp,
                        color = palette.textSecondary
                    )
                }
            }
        }

        SettingsAccentRowCard(
            title = "Privacidad y datos",
            subtitle = "Cuentas y datos del dispositivo",
            icon = Icons.Outlined.Person,
            palette = palette,
            testTag = "settings_row_privacy",
            onClick = onOpenPrivacy
        )

        SettingsAccentRowCard(
            title = "Activar notificaciones",
            subtitle = "Notas, asistencias y recordatorios",
            icon = Icons.Outlined.Notifications,
            palette = palette,
            testTag = "settings_row_notifications",
            onClick = onOpenNotifications
        )

        SettingsAccentRowCard(
            title = "Ayuda y soporte",
            subtitle = "Preguntas y reporte de problemas",
            icon = Icons.AutoMirrored.Outlined.HelpOutline,
            palette = palette,
            testTag = "settings_row_help",
            onClick = onOpenHelp
        )

        SettingsAccentRowCard(
            title = "Integraciones",
            subtitle = "Configuración de Canvas",
            icon = Icons.Outlined.Link,
            palette = palette,
            testTag = "settings_row_integrations",
            onClick = onOpenIntegrations
        )

        // Bloqueo de la app card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = palette.surface,
            border = BorderStroke(1.dp, palette.border),
            shadowElevation = if (palette.isDark) 0.dp else 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(3.5.dp)
                        .fillMaxHeight()
                        .background(palette.accentBlue)
                )
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 14.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(palette.softBlueBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = palette.accentBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Bloqueo de la app",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        Text(
                            text = if (biometricAvailable) {
                                "Solicitar huella, rostro o PIN al abrir o tras 1 min en segundo plano"
                            } else {
                                "No disponible: configura un PIN, patrón o huella en tu teléfono para activar el bloqueo"
                            },
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = if (biometricAvailable) palette.textSecondary else palette.dangerText
                        )
                    }

                    Switch(
                        checked = biometricEnabled,
                        onCheckedChange = onToggleBiometric,
                        enabled = biometricAvailable || biometricEnabled,
                        colors = settingsSwitchColors(palette),
                        modifier = Modifier.testTag("settings_biometric_switch")
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsAccentRowCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    palette: SettingsPalette,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = palette.surface,
        border = BorderStroke(1.dp, palette.border),
        shadowElevation = if (palette.isDark) 0.dp else 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .fillMaxHeight()
                    .background(palette.accentBlue)
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.softBlueBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = palette.accentBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary
                    )
                    Text(
                        text = subtitle,
                        fontSize = 12.5.sp,
                        color = palette.textSecondary
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = palette.textMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
