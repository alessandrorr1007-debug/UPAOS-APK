package com.example.upaos.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.upaos.data.local.AccountPrivacy
import com.example.upaos.data.local.TokenManager
import com.example.upaos.ui.theme.LocalUpaoDarkTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun PrivacyControls(usuario: String, onAccountRemoved: () -> Unit) {
    val context = LocalContext.current
    val tokens = remember { TokenManager(context) }
    val scope = rememberCoroutineScope()
    val palette = settingsPalette(LocalUpaoDarkTheme.current)

    var accounts by remember { mutableStateOf((tokens.getCuentas().map { it.usuario } + usuario).distinct()) }
    var selected by remember { mutableStateOf(usuario) }
    var action by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = palette.surface,
        border = BorderStroke(1.dp, palette.border),
        shadowElevation = if (palette.isDark) 0.dp else 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.accentBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(21.dp)
                    )
                }
                Text(
                    text = "Privacidad y datos",
                    fontSize = 17.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
            }

            Text(
                text = "Elige una cuenta guardada. Esto solo afecta a UPAO S; tu cuenta universitaria y Canvas se conservan.",
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = palette.textSecondary
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                accounts.forEach { user ->
                    val isSelected = selected == user
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) palette.selectedAccountBg else palette.surface,
                        border = if (isSelected) null else BorderStroke(1.dp, palette.border),
                        modifier = Modifier
                            .then(if (isSelected) Modifier.fillMaxWidth() else Modifier)
                            .defaultMinSize(minHeight = 46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(enabled = !busy, role = Role.RadioButton) {
                                selected = user
                                message = null
                            }
                            .testTag("privacy_account_$user")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = user,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) palette.selectedAccountText else palette.textPrimary
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Cuenta seleccionada",
                                    tint = palette.selectedAccountText,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            OutlinedButton(
                enabled = !busy,
                onClick = { action = "cache" },
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.4.dp, palette.accentBlue),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = palette.surface,
                    contentColor = palette.accentBlue
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("privacy_clear_cache_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.CleaningServices,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Borrar caché de esta cuenta",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                enabled = !busy,
                onClick = { action = "local" },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.dangerBg,
                    contentColor = palette.dangerText
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("privacy_remove_local_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Cerrar sesión y borrar datos locales",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = palette.surfaceSoftBlue,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = palette.accentBlue,
                        modifier = Modifier
                            .padding(top = 1.dp)
                            .size(19.dp)
                    )
                    Text(
                        text = "Eliminar del dispositivo borra las credenciales guardadas, el token de Canvas, la caché, las notificaciones locales y los recordatorios.",
                        fontSize = 12.5.sp,
                        lineHeight = 17.5.sp,
                        color = palette.textSecondary
                    )
                }
            }

            if (busy) {
                LinearProgressIndicator(
                    color = palette.accentBlue,
                    trackColor = palette.border,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            message?.let {
                Text(
                    text = it,
                    fontSize = 12.5.sp,
                    color = palette.accentBlue,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    action?.let { mode ->
        AlertDialog(
            onDismissRequest = { if (!busy) action = null },
            containerColor = palette.surface,
            titleContentColor = palette.textPrimary,
            textContentColor = palette.textSecondary,
            title = {
                Text(
                    if (mode == "cache") "¿Borrar datos guardados?"
                    else "¿Cerrar sesión y borrar datos de $selected?"
                )
            },
            text = {
                Text(
                    if (mode == "cache") {
                        "Se eliminará la información descargada de $selected. Necesitarás conexión para volver a cargarla. La cuenta seguirá guardada."
                    } else {
                        "Se eliminarán las credenciales, notas, horarios, asistencias en caché y recordatorios de $selected en este dispositivo."
                    }
                )
            },
            dismissButton = {
                TextButton(enabled = !busy, onClick = { action = null }) {
                    Text("Cancelar", color = palette.textSecondary)
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !busy,
                    onClick = {
                        busy = true
                        message = null
                        scope.launch {
                            val deletingUser = selected
                            if (mode != "cache") AccountPrivacy.markDeleting(deletingUser, true)
                            try {
                                if (mode == "cache") {
                                    AccountPrivacy.clearCache(context, selected)
                                    message = "Caché eliminada. La próxima consulta descargará los datos de nuevo."
                                } else {
                                    AccountPrivacy.removeLocal(context, selected)
                                    accounts = accounts.filter { it != selected }
                                    if (selected == usuario) {
                                        action = null
                                        onAccountRemoved()
                                    } else {
                                        message = "Cuenta eliminada del dispositivo."
                                        selected = usuario
                                    }
                                }
                                action = null
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                action = null
                                message = e.message ?: "No se pudieron eliminar los datos."
                            } finally {
                                AccountPrivacy.markDeleting(deletingUser, false)
                                busy = false
                            }
                        }
                    }
                ) {
                    Text(
                        text = if (busy) "Eliminando…" else "Eliminar",
                        color = if (mode == "cache") palette.accentBlue else palette.dangerText,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }
}
