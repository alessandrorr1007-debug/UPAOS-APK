package com.example.upaos.ui.grades

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.upaos.data.model.ComponenteDetalle
import com.example.upaos.data.upao.Calculators
import com.example.upaos.data.upao.UpaoRepository
import com.example.upaos.ui.components.gradeValue
import com.example.upaos.ui.components.isPendiente
import com.example.upaos.ui.components.toTitleCase
import com.example.upaos.ui.theme.LocalUpaoDarkTheme
import com.example.upaos.ui.theme.MinimalSectionPalette
import com.example.upaos.ui.theme.minimalSectionPalette
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailScreen(
    token: String,
    periodo: String,
    carrera: String,
    crn: String,
    courseName: String,
    onBack: () -> Unit
) {
    val palette = minimalSectionPalette(LocalUpaoDarkTheme.current)
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var componentes by remember { mutableStateOf<List<ComponenteDetalle>>(emptyList()) }
    var notaProyectada by remember { mutableStateOf<Any?>(null) }
    var pesosPendientes by remember { mutableStateOf<List<String>>(emptyList()) }

    val context = LocalContext.current
    val upaoRepository = remember { UpaoRepository(context) }

    fun load() {
        isLoading = true
        errorMessage = null
        scope.launch {
            try {
                val body = upaoRepository.getDetalleCurso(periodo, carrera, crn)
                isLoading = false
                if (body.success && body.detalles.isNotEmpty()) {
                    componentes = body.detalles
                    notaProyectada = body.notaProyectada
                    pesosPendientes = body.pesosPendientes
                } else if (body.success) {
                    componentes = body.detalles
                    notaProyectada = body.notaProyectada
                    pesosPendientes = body.pesosPendientes
                } else {
                    errorMessage = "Sin desglose disponible"
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                isLoading = false
                errorMessage = "En espera de desgloses"
            }
        }
    }

    LaunchedEffect(Unit) { load() }

    Scaffold(
        containerColor = palette.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.background)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 58.dp)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("course_detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = palette.textPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        text = toTitleCase(courseName).ifBlank { "Detalle del curso" },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                    )
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(palette.softBlueBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.School,
                            contentDescription = null,
                            tint = palette.accentBlue,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            CourseDetailAmbientCurve(
                tint = palette.softBlueBg.copy(alpha = if (palette.isDark) 0.22f else 0.65f),
                modifier = Modifier.fillMaxSize()
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                when {
                    isLoading -> {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = palette.surface,
                            border = BorderStroke(1.dp, palette.border),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = palette.accentBlue
                                )
                                Text(
                                    text = "Consultando componentes...",
                                    fontSize = 13.sp,
                                    color = palette.textSecondary
                                )
                            }
                        }
                    }

                    errorMessage != null && componentes.isEmpty() -> {
                        CourseDetailEmptyCard(
                            title = "Sin componentes disponibles",
                            subtitle = errorMessage.orEmpty(),
                            palette = palette
                        )
                    }

                    componentes.isEmpty() -> {
                        CourseDetailEmptyCard(
                            title = "Sin componentes registrados",
                            subtitle = "No hay componentes de evaluación para este curso.",
                            palette = palette
                        )
                    }

                    else -> {
                        val progreso = Calculators.progreso(componentes, periodo)
                        val puntosAcumulados = progreso?.acumulado
                            ?: Calculators.toDouble(notaProyectada)
                            ?: 0.0
                        val promedioParcial = progreso?.promedio

                        if (promedioParcial != null || notaProyectada != null) {
                            val partialValue = promedioParcial ?: puntosAcumulados
                            val partialTextColor = semanticGradeTextColor(partialValue, palette)
                            val acumuladoRef = progreso?.minimoAcumulado ?: 10.5
                            val acumuladoPassed = (progreso?.acumulado ?: puntosAcumulados) >= acumuladoRef
                            val acumuladoTextColor = if (acumuladoPassed) palette.greenText else palette.dangerText

                            Surface(
                                color = palette.surface,
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, palette.border),
                                shadowElevation = if (palette.isDark) 0.dp else 1.5.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("course_detail_summary_card")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .weight(0.95f)
                                            .padding(horizontal = 4.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(palette.softBlueBg),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.BarChart,
                                                contentDescription = null,
                                                tint = palette.accentBlue,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                text = "Promedio parcial",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = palette.textSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = formatNota(partialValue),
                                                fontSize = 22.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = partialTextColor
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Box(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .height(40.dp)
                                            .background(palette.border)
                                    )

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = palette.surfaceSoftBlue,
                                        modifier = Modifier.weight(1.05f)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(RoundedCornerShape(11.dp))
                                                    .background(palette.softBlueBg),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Layers,
                                                    contentDescription = null,
                                                    tint = palette.accentBlue,
                                                    modifier = Modifier.size(19.dp)
                                                )
                                            }
                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(
                                                    text = "Acumulado",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = palette.textSecondary
                                                )
                                                Text(
                                                    text = "${formatNota(puntosAcumulados)} / 20 pts",
                                                    fontSize = 14.5.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = acumuladoTextColor,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        Text(
                            text = "Componentes",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = palette.textPrimary,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            color = palette.surface,
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, palette.border),
                            shadowElevation = if (palette.isDark) 0.dp else 1.5.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("course_detail_components_card")
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                componentes.forEachIndexed { index, componente ->
                                    if (index > 0) {
                                        HorizontalDivider(
                                            color = palette.border,
                                            thickness = 1.dp,
                                            modifier = Modifier.padding(horizontal = 14.dp)
                                        )
                                    }
                                    CourseDetailComponentRow(
                                        componente = componente,
                                        periodo = periodo,
                                        palette = palette
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CourseDetailComponentRow(
    componente: ComponenteDetalle,
    periodo: String?,
    palette: MinimalSectionPalette
) {
    var expanded by remember { mutableStateOf(false) }
    val hasSub = componente.hasSubComponents && componente.subcomponentes.isNotEmpty()
    val pesoTxt = formatPesoTexto(Calculators.pesoComponente(componente, periodo), componente.displayNombre)
    val puntaje = componente.displayPuntaje
    val detalleTxt = listOfNotNull(
        puntaje,
        pesoTxt?.let { if (it.startsWith("Peso", ignoreCase = true)) it else "Peso $it" }
    ).joinToString(" · ")

    val upperName = componente.displayNombre.uppercase()
    val rowIcon = when {
        upperName.contains("ASISTENCIA") -> Icons.Outlined.Group
        upperName.contains("FINAL") -> Icons.Outlined.School
        else -> Icons.AutoMirrored.Outlined.Assignment
    }
    val (pillBg, pillText) = semanticGradePillColors(componente.displayNota, palette)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .clickable(enabled = hasSub) { expanded = !expanded }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.softBlueBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = rowIcon,
                    contentDescription = null,
                    tint = palette.accentBlue,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = componente.displayNombre,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (detalleTxt.isNotBlank()) {
                    Text(
                        text = detalleTxt,
                        fontSize = 12.sp,
                        color = palette.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "NOTA",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.textMuted,
                    letterSpacing = 0.5.sp
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = pillBg,
                    modifier = Modifier.widthIn(min = 56.dp)
                ) {
                    Text(
                        text = displayGrade(componente.displayNota),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = pillText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            if (hasSub) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Expandir sub-componentes",
                    tint = palette.textSecondary,
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(if (expanded) 180f else 0f)
                )
            }
        }

        if (hasSub) {
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    componente.subcomponentes.forEach { sub ->
                        val subPesoTxt = formatPesoTexto(sub.displayPeso, sub.displayNombre)
                        val subPuntaje = if (sub.puntajeObtenido != null && sub.puntajeSobre != null) {
                            "${sub.puntajeObtenido}/${sub.puntajeSobre}"
                        } else null
                        val (subBg, subText) = semanticGradePillColors(sub.displayNota, palette)

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = palette.surfaceSoft,
                            border = BorderStroke(1.dp, palette.border),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = sub.displayNombre,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = palette.textPrimary
                                    )
                                    if (subPuntaje != null || subPesoTxt != null) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            if (subPuntaje != null) {
                                                Text(
                                                    text = subPuntaje,
                                                    fontSize = 11.sp,
                                                    color = palette.textSecondary
                                                )
                                            }
                                            if (subPesoTxt != null) {
                                                Surface(
                                                    color = palette.softBlueBg,
                                                    shape = RoundedCornerShape(50)
                                                ) {
                                                    Text(
                                                        text = if (subPesoTxt.startsWith("Peso", ignoreCase = true)) subPesoTxt else "Peso $subPesoTxt",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = palette.softBlueText,
                                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Surface(
                                    color = subBg,
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.widthIn(min = 48.dp)
                                ) {
                                    Text(
                                        text = displayGrade(sub.displayNota),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = subText,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CourseDetailEmptyCard(
    title: String,
    subtitle: String,
    palette: MinimalSectionPalette
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
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
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.softBlueBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = palette.accentBlue,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = title,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = palette.textSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CourseDetailAmbientCurve(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val bottomRight = Path().apply {
            moveTo(w, h)
            lineTo(w * 0.48f, h)
            cubicTo(w * 0.58f, h * 0.88f, w * 0.78f, h * 0.79f, w, h * 0.76f)
            close()
        }
        drawPath(path = bottomRight, color = tint)

        val bottomLeft = Path().apply {
            moveTo(0f, h)
            lineTo(w * 0.44f, h)
            cubicTo(w * 0.32f, h * 0.90f, w * 0.14f, h * 0.87f, 0f, h * 0.88f)
            close()
        }
        drawPath(path = bottomLeft, color = tint.copy(alpha = tint.alpha * 0.65f))
    }
}

private fun semanticGradeTextColor(value: Any?, palette: MinimalSectionPalette): Color {
    if (isPendiente(value)) return palette.textMuted
    val numeric = gradeValue(value) ?: return palette.textMuted
    return if (numeric >= 10.5) palette.greenText else palette.dangerText
}

private fun semanticGradePillColors(value: Any?, palette: MinimalSectionPalette): Pair<Color, Color> {
    if (isPendiente(value)) return palette.pillBg to palette.textMuted
    val numeric = gradeValue(value) ?: return palette.pillBg to palette.textMuted
    return if (numeric >= 10.5) {
        palette.greenBg to palette.greenText
    } else {
        palette.dangerBg to palette.dangerText
    }
}
