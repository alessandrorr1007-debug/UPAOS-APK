package com.example.upaos.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.upaos.ui.theme.LocalUpaoDarkTheme
import com.example.upaos.util.AppUpdater

@Composable
fun HelpSupportContent() {
    val context = LocalContext.current
    val palette = settingsPalette(LocalUpaoDarkTheme.current)
    var expanded by rememberSaveable { mutableStateOf(-1) }
    var reportOpen by rememberSaveable { mutableStateOf(false) }
    var report by rememberSaveable { mutableStateOf("") }

    val questions = listOf(
        "¿Cómo actualizo mis datos?" to "Desliza hacia abajo en Cursos, Horario o Asistencia. Necesitas conexión para consultar la información más reciente. Si la consulta falla, la app conserva lo último guardado.",
        "¿Por qué no recibo avisos?" to "Revisa que los avisos estén activados en Ajustes > Activar notificaciones y que Android permita las notificaciones de UPAO S. La revisión en segundo plano necesita una sesión y contraseña guardadas, conexión y permiso para trabajar con la pantalla apagada.",
        "¿La calculadora cambia mis notas?" to "No. Las notas que ingresas o completas son una simulación dentro de la calculadora. Tus calificaciones en la universidad se conservan.",
        "¿Qué significa una asistencia pendiente?" to "Indica que todavía no hay un registro disponible. Toca Ver desglose en el curso para consultar sus componentes.",
        "¿Cómo elimino mis datos del celular?" to "En Ajustes > Privacidad y datos puedes borrar la caché o cerrar sesión y borrar los datos locales de una cuenta. Esto no elimina tu cuenta universitaria."
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(palette.accentBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = "Preguntas frecuentes",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary
            )
        }

        questions.forEachIndexed { index, (question, answer) ->
            val isExpanded = expanded == index
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = palette.surface,
                border = BorderStroke(1.dp, palette.border),
                shadowElevation = if (palette.isDark) 0.dp else 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp)
                        .clickable(role = Role.Button) {
                            expanded = if (isExpanded) -1 else index
                        }
                        .testTag("help_faq_item_$index")
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = question,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Contraer respuesta" else "Ver respuesta",
                            tint = palette.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    AnimatedVisibility(visible = isExpanded) {
                        Column {
                            Spacer(Modifier.height(10.dp))
                            HorizontalDivider(color = palette.border, thickness = 1.dp)
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = answer,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                color = palette.textSecondary
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        Button(
            onClick = { reportOpen = true },
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = palette.accentBlue,
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 50.dp)
                .testTag("help_report_problem_button")
        ) {
            Icon(
                imageVector = Icons.Outlined.ChatBubbleOutline,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Reportar un problema",
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    if (reportOpen) {
        AlertDialog(
            onDismissRequest = { reportOpen = false },
            containerColor = palette.surface,
            titleContentColor = palette.textPrimary,
            textContentColor = palette.textSecondary,
            title = { Text("Reportar un problema") },
            text = {
                OutlinedTextField(
                    value = report,
                    onValueChange = { if (it.length <= 2000) report = it },
                    label = { Text("¿Qué ocurrió?") },
                    placeholder = { Text("En qué pantalla estabas y qué hiciste antes del problema") },
                    minLines = 4,
                    maxLines = 7,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    enabled = report.isNotBlank(),
                    onClick = {
                        val version = AppUpdater.getCurrentVersionName(context)
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Problema en UPAO S")
                            putExtra(Intent.EXTRA_TEXT, "UPAO S $version · Android ${Build.VERSION.RELEASE}\n\n${report.trim()}")
                        }
                        try {
                            context.startActivity(Intent.createChooser(send, "Compartir reporte"))
                            reportOpen = false
                        } catch (_: ActivityNotFoundException) {
                            Toast.makeText(context, "No hay una aplicación disponible para compartir el reporte", Toast.LENGTH_LONG).show()
                        }
                    }
                ) {
                    Text("Compartir reporte", color = palette.accentBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { reportOpen = false }) {
                    Text("Cancelar", color = palette.textSecondary)
                }
            }
        )
    }
}
