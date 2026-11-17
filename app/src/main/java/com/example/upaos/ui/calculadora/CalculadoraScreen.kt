package com.example.upaos.ui.calculadora

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.upaos.data.local.GradesCache
import com.example.upaos.data.local.TokenManager
import com.example.upaos.data.model.GradesResponse
import com.example.upaos.ui.components.CampusTopBar
import com.example.upaos.ui.theme.LocalUpaoDarkTheme
import com.example.upaos.ui.theme.MinimalSectionPalette
import com.example.upaos.ui.theme.minimalSectionPalette
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

// Nota mínima aprobatoria oficial UPAO
private const val NOTA_MINIMA = 10.5

private data class ComponentePeso(val id: String, val nombre: String, val peso: Int)

private val componentes = listOf(
    ComponentePeso("ep1", "EP1", 20),
    ComponentePeso("parcial", "Parcial", 30),
    ComponentePeso("ep2", "EP2", 20),
    ComponentePeso("final", "Final", 30)
)

private fun formatNota(v: Double): String {
    val r = (v * 100).roundToInt() / 100.0
    return if (r % 1.0 == 0.0) r.toInt().toString() else String.format(java.util.Locale.US, "%.2f", r)
}

private enum class EstadoCalculo { NEUTRO, PARCIAL, APROBADO, DESAPROBADO, ALCANZABLE, IMPOSIBLE }

private data class ResultadoCalculo(
    val titulo: String,
    val valorDestacado: String,
    val mensaje: String,
    val estado: EstadoCalculo
)

private fun calcular(notas: List<Double?>): ResultadoCalculo {
    val llenas = notas.count { it != null }
    val sumaPonderada = componentes.indices.sumOf { i ->
        notas[i]?.let { it * componentes[i].peso / 100.0 } ?: 0.0
    }

    if (llenas == 4) {
        val promedio = sumaPonderada
        val redondeo = promedio.roundToInt()
        return if (promedio >= NOTA_MINIMA) {
            ResultadoCalculo(
                titulo = "Promedio Final",
                valorDestacado = formatNota(promedio),
                mensaje = "¡Felicidades! Curso aprobado",
                estado = EstadoCalculo.APROBADO
            )
        } else {
            ResultadoCalculo(
                titulo = "Promedio Final",
                valorDestacado = formatNota(promedio),
                mensaje = "Desaprobado (por debajo de ${formatNota(NOTA_MINIMA)}) · Nota en acta: $redondeo",
                estado = EstadoCalculo.DESAPROBADO
            )
        }
    }

    if (llenas == 3) {
        val missingIndex = componentes.indices.first { notas[it] == null }
        val missingComp = componentes[missingIndex]
        val pesoFaltante = missingComp.peso / 100.0
        val necesaria = (NOTA_MINIMA - sumaPonderada) / pesoFaltante

        return when {
            necesaria <= 0.0 -> ResultadoCalculo(
                titulo = "Curso Aprobado",
                valorDestacado = formatNota(sumaPonderada),
                mensaje = "Ya acumulaste ${formatNota(sumaPonderada)} pts. Tienes el curso aprobado sin importar tu nota en ${missingComp.nombre}.",
                estado = EstadoCalculo.APROBADO
            )
            necesaria > 20.0 -> ResultadoCalculo(
                titulo = "Fuera de alcance",
                valorDestacado = formatNota(necesaria),
                mensaje = "Se requiere más de 20 pts en ${missingComp.nombre} para alcanzar 10.5.",
                estado = EstadoCalculo.IMPOSIBLE
            )
            else -> ResultadoCalculo(
                titulo = "Necesitas en ${missingComp.nombre}",
                valorDestacado = formatNota(necesaria),
                mensaje = "Sacando ${formatNota(necesaria)} o más en ${missingComp.nombre} apruebas el curso con ${formatNota(NOTA_MINIMA)}.",
                estado = EstadoCalculo.ALCANZABLE
            )
        }
    }

    if (llenas in 1..2) {
        val sumaPesos = componentes.indices.sumOf { if (notas[it] != null) componentes[it].peso else 0 }
        val parcial = sumaPonderada / (sumaPesos / 100.0)
        val faltanParaAprobar = (NOTA_MINIMA - sumaPonderada).coerceAtLeast(0.0)
        return ResultadoCalculo(
            titulo = "Puntos asegurados",
            valorDestacado = formatNota(sumaPonderada),
            mensaje = if (faltanParaAprobar > 0) {
                "Llevas ${formatNota(sumaPonderada)} pts asegurados (promedio parcial ${formatNota(parcial)}). Faltan ${formatNota(faltanParaAprobar)} pts para aprobar."
            } else {
                "¡Ya tienes ${formatNota(sumaPonderada)} pts asegurados! Ya alcanzaste la nota aprobatoria."
            },
            estado = if (faltanParaAprobar <= 0) EstadoCalculo.APROBADO else EstadoCalculo.PARCIAL
        )
    }

    return ResultadoCalculo(
        titulo = "Calculadora UPAO",
        valorDestacado = "0.0",
        mensaje = "Ingresa tus notas para ver tu promedio acumulado y saber cuánto necesitas para aprobar.",
        estado = EstadoCalculo.NEUTRO
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculadoraScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val palette = minimalSectionPalette(LocalUpaoDarkTheme.current)
    val user = remember(context) { TokenManager(context).getSavedUser() }
    var valores by rememberSaveable(user) { mutableStateOf(listOf("", "", "", "")) }
    var selectedCourse by rememberSaveable(user) { mutableStateOf(-1) }
    var savedGrades by remember { mutableStateOf<GradesResponse?>(null) }
    var loadingCourses by remember { mutableStateOf(true) }
    var courseMenu by remember { mutableStateOf(false) }
    var cacheError by remember { mutableStateOf(false) }

    LaunchedEffect(user) {
        try {
            savedGrades = withContext(Dispatchers.IO) {
                user?.let { GradesCache(context).cargar("notas_$it") }
                    ?.let { Gson().fromJson(it, GradesResponse::class.java) }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            cacheError = true
        } finally {
            loadingCourses = false
        }
    }

    val courses = savedGrades?.cursos.orEmpty()
    val course = courses.getOrNull(selectedCourse)
    val original = course?.let(::simulationGrades) ?: listOf("", "", "", "")

    val notas = componentes.indices.map { i ->
        valores[i].toDoubleOrNull()?.takeIf { it in 0.0..20.0 }
    }
    val resultado = calcular(notas)
    val llenas = notas.count { it != null }
    val sumaPonderada = componentes.indices.sumOf { i ->
        notas[i]?.let { it * componentes[i].peso / 100.0 } ?: 0.0
    }
    val hayNotasIngresadas = valores.any { it.isNotBlank() }
    val completedGrades = completePassingGrades(valores, componentes.map { it.peso }, NOTA_MINIMA)
    val missingCount = valores.count { it.isBlank() }
    val hasInvalidGrade = valores.any {
        it.isNotBlank() && it.toDoubleOrNull()?.let { n -> n.isFinite() && n in 0.0..20.0 } != true
    }

    Scaffold(
        containerColor = palette.background,
        topBar = {
            Column(modifier = Modifier.background(palette.background)) {
                CampusTopBar(
                    title = {
                        Text(
                            text = "Calculadora",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = palette.textPrimary
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = palette.background
                    ),
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("calculator_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver",
                                tint = palette.textPrimary
                            )
                        }
                    },
                    actions = {
                        AnimatedVisibility(visible = hayNotasIngresadas, enter = fadeIn(), exit = fadeOut()) {
                            IconButton(
                                onClick = { valores = listOf("", "", "", "") },
                                modifier = Modifier.testTag("calculator_reset_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = "Limpiar notas",
                                    tint = palette.accentBlue
                                )
                            }
                        }
                    }
                )
                HorizontalDivider(thickness = 1.dp, color = palette.border)
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = palette.surface,
                    border = BorderStroke(1.dp, palette.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 14.dp),
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
                                imageVector = Icons.Filled.Calculate,
                                contentDescription = null,
                                tint = palette.accentBlue,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "Simula tus notas",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.textPrimary
                            )
                            Text(
                                text = "Calcula tu promedio y la nota que necesitas para aprobar.",
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                color = palette.textSecondary
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Curso",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = palette.textSecondary
                        )
                        if (course != null) {
                            TextButton(
                                onClick = { valores = original },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                modifier = Modifier.testTag("restore_saved_grades_button")
                            ) {
                                Text(
                                    text = "Restaurar notas guardadas",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = palette.accentBlue
                                )
                            }
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = courseMenu,
                        onExpandedChange = { if (!loadingCourses) courseMenu = !courseMenu }
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = palette.surface,
                            border = BorderStroke(
                                1.dp,
                                if (courseMenu) palette.accentBlue else palette.border
                            ),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("course_selector_dropdown")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = when {
                                        loadingCourses -> "Cargando cursos guardados…"
                                        else -> course?.displayNombre ?: "Ingreso manual"
                                    },
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (loadingCourses) palette.textMuted else palette.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = if (courseMenu) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                    contentDescription = "Seleccionar curso",
                                    tint = palette.textSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        ExposedDropdownMenu(
                            expanded = courseMenu,
                            onDismissRequest = { courseMenu = false },
                            modifier = Modifier.background(palette.surface)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Ingreso manual",
                                        color = palette.textPrimary,
                                        fontWeight = if (selectedCourse == -1) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    selectedCourse = -1
                                    valores = listOf("", "", "", "")
                                    courseMenu = false
                                }
                            )
                            courses.forEachIndexed { index, item ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = item.displayNombre,
                                            color = palette.textPrimary,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            fontWeight = if (selectedCourse == index) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        selectedCourse = index
                                        valores = simulationGrades(item)
                                        courseMenu = false
                                    }
                                )
                            }
                        }
                    }

                    if (cacheError) {
                        Text(
                            text = "No se pudieron leer las notas guardadas. Puedes ingresar notas manualmente.",
                            fontSize = 12.sp,
                            color = palette.dangerText
                        )
                    } else if (course != null && !savedGrades?.periodo.isNullOrBlank()) {
                        Text(
                            text = "Notas guardadas · ${savedGrades?.periodo.orEmpty()}",
                            fontSize = 12.sp,
                            color = palette.textMuted
                        )
                    }
                }

                Text(
                    text = "Evaluaciones",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NotaCard(
                        componente = componentes[0],
                        value = valores[0],
                        source = gradeSource(valores[0], original[0]),
                        palette = palette,
                        onValueChange = { nuevo ->
                            valores = valores.toMutableList().also { it[0] = nuevo }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    NotaCard(
                        componente = componentes[1],
                        value = valores[1],
                        source = gradeSource(valores[1], original[1]),
                        palette = palette,
                        onValueChange = { nuevo ->
                            valores = valores.toMutableList().also { it[1] = nuevo }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NotaCard(
                        componente = componentes[2],
                        value = valores[2],
                        source = gradeSource(valores[2], original[2]),
                        palette = palette,
                        onValueChange = { nuevo ->
                            valores = valores.toMutableList().also { it[2] = nuevo }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    NotaCard(
                        componente = componentes[3],
                        value = valores[3],
                        source = gradeSource(valores[3], original[3]),
                        palette = palette,
                        onValueChange = { nuevo ->
                            valores = valores.toMutableList().also { it[3] = nuevo }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (missingCount > 0) {
                    Button(
                        onClick = { completedGrades?.let { valores = it } },
                        enabled = completedGrades != null,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.accentBlue,
                            contentColor = Color.White,
                            disabledContainerColor = palette.pillBg,
                            disabledContentColor = palette.textMuted
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("complete_passing_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Calculate,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Completar para aprobar",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (hasInvalidGrade) {
                        Text(
                            text = "Revisa las notas: deben estar entre 0 y 20.",
                            fontSize = 12.sp,
                            color = palette.dangerText
                        )
                    } else if (completedGrades == null) {
                        Text(
                            text = "Con estas notas no se llega a 10.5, ni con 20 en lo pendiente.",
                            fontSize = 12.sp,
                            color = palette.dangerText
                        )
                    }
                }

                AnimatedVisibility(visible = llenas > 0) {
                    ResultadoBanner(
                        resultado = resultado,
                        puntosAcumulados = sumaPonderada,
                        llenas = llenas,
                        palette = palette
                    )
                }

                Surface(
                    color = palette.surface,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, palette.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = palette.textPrimary.copy(alpha = 0.85f),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "La nota mínima aprobatoria UPAO es 10.5.",
                            fontSize = 13.sp,
                            color = palette.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun ResultadoBanner(
    resultado: ResultadoCalculo,
    puntosAcumulados: Double,
    llenas: Int,
    palette: MinimalSectionPalette
) {
    val accentColor = when (resultado.estado) {
        EstadoCalculo.APROBADO -> palette.greenText
        EstadoCalculo.DESAPROBADO, EstadoCalculo.IMPOSIBLE -> palette.dangerText
        EstadoCalculo.ALCANZABLE, EstadoCalculo.PARCIAL -> palette.accentBlue
        EstadoCalculo.NEUTRO -> palette.textMuted
    }
    val badgeBg = when (resultado.estado) {
        EstadoCalculo.APROBADO -> palette.greenBg
        EstadoCalculo.DESAPROBADO, EstadoCalculo.IMPOSIBLE -> palette.dangerBg
        EstadoCalculo.ALCANZABLE, EstadoCalculo.PARCIAL -> palette.softBlueBg
        EstadoCalculo.NEUTRO -> palette.pillBg
    }

    Surface(
        color = palette.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("calculator_result_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = resultado.titulo.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.7.sp,
                    color = palette.textMuted,
                    modifier = Modifier.weight(1f)
                )
                if (resultado.estado != EstadoCalculo.NEUTRO) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = badgeBg,
                        contentColor = accentColor
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(accentColor)
                            )
                            Text(
                                text = when (resultado.estado) {
                                    EstadoCalculo.APROBADO -> "Aprobado"
                                    EstadoCalculo.DESAPROBADO -> "Desaprobado"
                                    EstadoCalculo.ALCANZABLE -> "Meta alcanzable"
                                    EstadoCalculo.IMPOSIBLE -> "Crítico"
                                    EstadoCalculo.PARCIAL -> "$llenas de 4 notas"
                                    else -> ""
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = resultado.valorDestacado,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = if (resultado.estado == EstadoCalculo.NEUTRO) palette.textMuted else accentColor
                )
                if (resultado.estado != EstadoCalculo.NEUTRO) {
                    Text(
                        text = if (resultado.estado == EstadoCalculo.ALCANZABLE) "requerida" else "/ 20 pts",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.textSecondary,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }
            }

            if (llenas > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                val fraction = (puntosAcumulados / 20.0).toFloat().coerceIn(0f, 1f)
                val animatedFraction by animateFloatAsState(
                    targetValue = fraction,
                    animationSpec = tween(350),
                    label = "barProgreso"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(palette.pillBg)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedFraction)
                            .clip(RoundedCornerShape(3.dp))
                            .background(accentColor)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = resultado.mensaje,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = palette.textSecondary
            )
        }
    }
}

@Composable
private fun NotaCard(
    componente: ComponentePeso,
    value: String,
    source: String,
    palette: MinimalSectionPalette,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val d = value.toDoubleOrNull()
    val tieneNota = d != null
    val esInvalido = value.isNotEmpty() && (d == null || d < 0.0 || d > 20.0)
    val aporte = if (tieneNota && !esInvalido) (d!! * componente.peso / 100.0) else null
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        color = palette.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (esInvalido) palette.dangerText else palette.border),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = componente.nombre,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = palette.pillBg
                ) {
                    Text(
                        text = "${componente.peso} %",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.textSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = palette.surfaceSoft,
                border = BorderStroke(
                    1.dp,
                    when {
                        esInvalido -> palette.dangerText
                        isFocused -> palette.accentBlue
                        else -> palette.border
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BasicTextField(
                        value = value,
                        onValueChange = { input ->
                            if (input.length <= 5) {
                                onValueChange(input.replace(',', '.').filter { it.isDigit() || it == '.' })
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        cursorBrush = SolidColor(palette.accentBlue),
                        textStyle = TextStyle(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = when {
                                esInvalido -> palette.dangerText
                                tieneNota -> if (d!! >= NOTA_MINIMA) palette.greenText else palette.dangerText
                                else -> palette.textPrimary
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = if (value.isNotEmpty()) 22.dp else 0.dp)
                            .onFocusChanged { isFocused = it.isFocused }
                            .testTag("nota_input_${componente.id}"),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (value.isEmpty()) {
                                    Text(
                                        text = "0 - 20",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = palette.textMuted,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )

                    if (value.isNotEmpty()) {
                        IconButton(
                            onClick = { onValueChange("") },
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = "Borrar ${componente.nombre}",
                                tint = palette.textMuted,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }

            if (esInvalido) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Debe ser de 0 a 20",
                    color = palette.dangerText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            } else if (aporte != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "$source · +${formatNota(aporte)} pts",
                    color = palette.greenText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private fun gradeSource(value: String, original: String): String =
    if (original.isNotBlank() && value.toDoubleOrNull() == original.toDoubleOrNull()) "Guardada" else "Simulada"
