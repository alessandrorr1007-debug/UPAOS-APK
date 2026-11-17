package com.example.upaos.ui.login

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.upaos.data.local.CuentaGuardada
import com.example.upaos.data.local.TokenManager
import com.example.upaos.data.profile.StudentProfile
import com.example.upaos.data.upao.UpaoAuth
import com.example.upaos.data.upao.UpaoInvalidCredentialsException
import com.example.upaos.data.upao.UpaoNetworkException
import com.example.upaos.data.upao.UpaoServiceUnavailableException
import com.example.upaos.ui.theme.LocalUpaoDarkTheme
import com.example.upaos.ui.theme.MinimalSectionPalette
import com.example.upaos.ui.theme.minimalSectionPalette
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun EligeCuentaScreen(
    onLoginSuccess: (String) -> Unit,
    onGoToLogin: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val palette = minimalSectionPalette(LocalUpaoDarkTheme.current)
    val tokenManager = remember { TokenManager(context) }
    val scope = rememberCoroutineScope()

    var cuentas by remember { mutableStateOf(tokenManager.getCuentas()) }
    var autenticando by remember { mutableStateOf<String?>(null) }
    var eliminarCuenta by remember { mutableStateOf<CuentaGuardada?>(null) }
    val initialNombres = remember(cuentas) {
        cuentas.associate { cuenta ->
            val cachedName = try {
                tokenManager.getProfileJson(cuenta.usuario)?.let { StudentProfile.parse(it).shortName }
            } catch (_: Exception) {
                null
            }
            cuenta.usuario to (cachedName ?: cuenta.nombreCorto ?: StudentProfile.shortNameFromFullName(cuenta.nombre))
        }
    }
    var nombres by remember { mutableStateOf(initialNombres) }
    LaunchedEffect(cuentas) {
        nombres = withContext(Dispatchers.IO) {
            cuentas.associate { cuenta ->
                val cachedName = try {
                    tokenManager.getProfileJson(cuenta.usuario)?.let { StudentProfile.parse(it).shortName }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    null
                }
                cuenta.usuario to (cachedName ?: cuenta.nombreCorto ?: StudentProfile.shortNameFromFullName(cuenta.nombre))
            }
        }
    }
    BackHandler { if (autenticando == null) onBack() }

    fun iniciarSesionCon(cuenta: CuentaGuardada) {
        if (autenticando != null) return
        autenticando = cuenta.usuario
        scope.launch {
            try {
                val result = UpaoAuth.login(cuenta.usuario, cuenta.password)
                if (result.success) {
                    val nombreFinal = result.studentName ?: cuenta.nombre
                    tokenManager.saveLogin(cuenta.usuario, cuenta.password, true, nombreFinal)
                    autenticando = null
                    onLoginSuccess(cuenta.usuario)
                } else {
                    autenticando = null
                    Toast.makeText(context, result.errorMessage ?: "No se pudo iniciar sesión", Toast.LENGTH_LONG).show()
                }
            } catch (e: UpaoInvalidCredentialsException) {
                autenticando = null
                Toast.makeText(context, "ID o contraseña incorrectos. Revisa tus datos.", Toast.LENGTH_LONG).show()
            } catch (e: UpaoServiceUnavailableException) {
                autenticando = null
                Toast.makeText(context, "El portal de la UPAO no está disponible, intenta más tarde.", Toast.LENGTH_LONG).show()
            } catch (e: UpaoNetworkException) {
                autenticando = null
                Toast.makeText(context, "No se pudo conectar. Revisa tu conexión a internet.", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                autenticando = null
                Toast.makeText(context, e.localizedMessage ?: "No se pudo iniciar sesión.", Toast.LENGTH_LONG).show()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background),
        contentAlignment = Alignment.TopCenter
    ) {
        AccountPickerAmbientBackground(
            tint = palette.softBlueBg.copy(alpha = if (palette.isDark) 0.25f else 0.7f),
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 560.dp)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            IconButton(
                onClick = onBack,
                enabled = autenticando == null,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("choose_account_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = palette.textPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Elige una cuenta",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.textPrimary
                    )
                    Text(
                        text = "Cuentas guardadas en este dispositivo",
                        fontSize = 13.5.sp,
                        color = palette.textSecondary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.softBlueBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.School,
                        contentDescription = null,
                        tint = palette.accentBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (cuentas.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = palette.surface,
                        border = BorderStroke(1.dp, palette.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(palette.softBlueBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = null,
                                    tint = palette.accentBlue,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Text(
                                text = "No hay cuentas guardadas",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.textPrimary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Guarda tu contraseña al iniciar sesión para verla aquí.",
                                fontSize = 13.sp,
                                color = palette.textSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(cuentas, key = { it.usuario }) { cuenta ->
                        val nombreMostrar = nombres[cuenta.usuario]
                            ?: cuenta.nombreCorto
                            ?: StudentProfile.shortNameFromFullName(cuenta.nombre)
                            ?: "Cuenta guardada"
                        AccountCardItem(
                            nombreMostrar = nombreMostrar,
                            codigo = cuenta.usuario,
                            isAuthenticating = autenticando == cuenta.usuario,
                            actionsEnabled = autenticando == null,
                            palette = palette,
                            onSelect = { iniciarSesionCon(cuenta) },
                            onDelete = { eliminarCuenta = cuenta }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onGoToLogin,
                enabled = autenticando == null,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.accentBlue,
                    contentColor = Color.White,
                    disabledContainerColor = palette.pillBg,
                    disabledContentColor = palette.textMuted
                ),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
                    .testTag("add_account_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.PersonAdd,
                    contentDescription = null,
                    modifier = Modifier.size(19.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Agregar otra cuenta",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    eliminarCuenta?.let { cuenta ->
        AlertDialog(
            onDismissRequest = { eliminarCuenta = null },
            containerColor = palette.surface,
            titleContentColor = palette.textPrimary,
            textContentColor = palette.textSecondary,
            title = {
                Text(
                    text = "¿Eliminar esta cuenta guardada?",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
            },
            text = {
                Text(
                    text = "Se quitará la cuenta ${cuenta.usuario} de las cuentas guardadas de este dispositivo.",
                    fontSize = 14.sp,
                    color = palette.textSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        tokenManager.removeCuenta(cuenta.usuario)
                        cuentas = tokenManager.getCuentas()
                        eliminarCuenta = null
                    },
                    modifier = Modifier.testTag("confirm_delete_account_button")
                ) {
                    Text(
                        text = "Eliminar",
                        fontWeight = FontWeight.Bold,
                        color = palette.dangerText
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { eliminarCuenta = null }) {
                    Text(
                        text = "Cancelar",
                        fontWeight = FontWeight.SemiBold,
                        color = palette.textSecondary
                    )
                }
            }
        )
    }
}

@Composable
private fun AccountCardItem(
    nombreMostrar: String,
    codigo: String,
    isAuthenticating: Boolean,
    actionsEnabled: Boolean,
    palette: MinimalSectionPalette,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val initial = nombreMostrar.trim().firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "?"

    Surface(
        onClick = onSelect,
        enabled = actionsEnabled,
        shape = RoundedCornerShape(18.dp),
        color = palette.surface,
        border = BorderStroke(1.dp, palette.border),
        shadowElevation = if (palette.isDark) 0.dp else 1.5.dp,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .testTag("account_card_$codigo")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(palette.softBlueBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    color = palette.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(30.dp)
                    .background(palette.border)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = nombreMostrar,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = codigo,
                    fontSize = 13.sp,
                    color = palette.textSecondary
                )
                if (isAuthenticating) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 1.8.dp,
                            color = palette.accentBlue
                        )
                        Text(
                            text = "Iniciando sesión...",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = palette.accentBlue
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(
                        enabled = actionsEnabled,
                        role = Role.Button,
                        onClick = onDelete
                    )
                    .testTag("delete_account_button_$codigo"),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(palette.dangerBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Eliminar cuenta guardada",
                        tint = palette.dangerText,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountPickerAmbientBackground(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val topLeft = Path().apply {
            moveTo(0f, 0f)
            lineTo(w * 0.44f, 0f)
            cubicTo(w * 0.30f, h * 0.06f, w * 0.14f, h * 0.09f, 0f, h * 0.11f)
            close()
        }
        drawPath(path = topLeft, color = tint)

        val bottomRight = Path().apply {
            moveTo(w, h)
            lineTo(w * 0.38f, h)
            cubicTo(w * 0.56f, h * 0.84f, w * 0.78f, h * 0.75f, w, h * 0.73f)
            close()
        }
        drawPath(path = bottomRight, color = tint)
    }
}
