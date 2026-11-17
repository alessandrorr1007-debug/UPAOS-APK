package com.example.upaos.ui.settings

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.upaos.data.api.CanvasApiClient
import com.example.upaos.data.canvas.DefaultCanvasSetupRepository
import com.example.upaos.data.local.TokenManager
import com.example.upaos.ui.canvas.CanvasSetupViewModel
import com.example.upaos.ui.canvas.canvasViewModelFactory
import com.example.upaos.ui.canvas.canvasViewModelKey
import com.example.upaos.ui.theme.LocalUpaoDarkTheme

@Composable
internal fun SettingsIntegrationsContent(
    usuario: String,
    onTokenSaved: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val palette = settingsPalette(LocalUpaoDarkTheme.current)

    var tokenInput by remember(usuario) { mutableStateOf("") }
    val tokens = remember(context) { TokenManager(context.applicationContext) }
    val session = tokens.getSessionId()
    val setupRepository = remember(context, usuario, session) {
        DefaultCanvasSetupRepository(
            CanvasApiClient.apiService,
            validateOwner = { tokens.getSavedUser() == usuario && tokens.getSessionId() == session },
            save = { tokens.saveCanvasToken(usuario, it) }
        )
    }
    val setup: CanvasSetupViewModel = viewModel(
        key = canvasViewModelKey("setup", usuario, session, ""),
        factory = canvasViewModelFactory { CanvasSetupViewModel(setupRepository) }
    )
    val setupState by setup.state.collectAsState()
    val isValidating = setupState.loading
    val errorText = setupState.error

    LaunchedEffect(setupState.savedToken) {
        setupState.savedToken?.let {
            setup.consumeSavedToken()
            tokenInput = ""
            Toast.makeText(context, "Canvas conectado", Toast.LENGTH_SHORT).show()
            onTokenSaved()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Sub-header: Cap + Canvas
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.School,
                contentDescription = null,
                tint = palette.accentBlue,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = "Canvas",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary
            )
        }

        // Centered icon + title + description
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(palette.softBlueBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Link,
                contentDescription = null,
                tint = palette.accentBlue,
                modifier = Modifier.size(30.dp)
            )
        }

        Spacer(Modifier.height(14.dp))

        Text(
            text = "Conectar Canvas LMS",
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = palette.textPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "Conecta tu cuenta de Canvas para consultar tus tareas en Hoy.",
            fontSize = 13.5.sp,
            lineHeight = 19.sp,
            color = palette.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(Modifier.height(18.dp))

        // Instructions card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = palette.surface,
            border = BorderStroke(1.dp, palette.border),
            shadowElevation = if (palette.isDark) 0.dp else 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.surfaceSoftBlue)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
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
                        text = "¿Cómo obtener tu token?",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.accentBlue
                    )
                }

                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TokenStepRow("1", "Abre Canvas UPAO en tu navegador web.", palette)
                    TokenStepRow("2", "Entra a Cuenta (menú lateral) → Configuración.", palette)
                    TokenStepRow("3", "En 'Tokens de acceso aprobados', pulsa + Nuevo token de acceso.", palette)
                    TokenStepRow("4", "Copia el token generado y pégalo abajo.", palette)

                    Spacer(Modifier.height(4.dp))

                    Surface(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://upao.instructure.com/profile/settings"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                if (e is kotlinx.coroutines.CancellationException) throw e
                                Log.w("Canvas", "No se pudo completar la operación de Canvas")
                            }
                        },
                        shape = RoundedCornerShape(50),
                        color = palette.softBlueBg,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 46.dp)
                            .testTag("integrations_open_canvas_button")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.OpenInNew,
                                contentDescription = null,
                                tint = palette.accentBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Ir a Configuración Canvas",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.accentBlue
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Token input box with Paste button
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = palette.surface,
            border = BorderStroke(
                width = 1.dp,
                color = if (errorText != null) palette.dangerText else palette.border
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 54.dp)
                    .padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (tokenInput.isEmpty()) {
                        Text(
                            text = "Token de Acceso de Canvas",
                            fontSize = 14.sp,
                            color = palette.textMuted
                        )
                    }
                    BasicTextField(
                        value = tokenInput,
                        onValueChange = {
                            tokenInput = it
                            setup.clearError()
                        },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        textStyle = TextStyle(
                            color = palette.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = SolidColor(palette.accentBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("integrations_token_input")
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(24.dp)
                        .background(palette.border)
                )

                TextButton(
                    onClick = {
                        val clip = clipboardManager.getText()?.text
                        if (!clip.isNullOrBlank()) {
                            tokenInput = clip.trim()
                            setup.clearError()
                        }
                    },
                    modifier = Modifier
                        .defaultMinSize(minHeight = 44.dp)
                        .testTag("integrations_paste_button")
                ) {
                    Text(
                        text = "Pegar",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.accentBlue
                    )
                }
            }
        }

        if (errorText != null) {
            Text(
                text = errorText,
                fontSize = 12.sp,
                color = palette.dangerText,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        // Validate and connect primary button
        Button(
            onClick = { setup.validate(tokenInput) },
            enabled = !isValidating,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = palette.accentBlue,
                contentColor = Color.White,
                disabledContainerColor = palette.accentBlue.copy(alpha = 0.6f),
                disabledContentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 50.dp)
                .testTag("integrations_validate_button")
        ) {
            if (isValidating) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(19.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Validar y Conectar",
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TokenStepRow(
    number: String,
    text: String,
    palette: SettingsPalette
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(palette.softBlueBg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = palette.accentBlue
            )
        }
        Text(
            text = text,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = palette.textPrimary,
            modifier = Modifier.weight(1f)
        )
    }
}
