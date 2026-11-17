package com.example.upaos.ui.horario

import com.example.upaos.ui.home.DashboardViewModel
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Room
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.upaos.data.model.AsistenciaCurso
import com.example.upaos.data.model.HorarioBloque
import com.example.upaos.data.model.HorarioCurso
import com.example.upaos.ui.components.EmptyState
import com.example.upaos.ui.components.ErrorView
import com.example.upaos.ui.components.RefreshableContent
import com.example.upaos.ui.components.SkeletonBox
import com.example.upaos.ui.components.toTitleCase
import com.example.upaos.ui.theme.LocalUpaoDarkTheme
import com.example.upaos.ui.theme.UpaoBlue

private fun normalizarNombre(nombre: String): String =
    nombre.uppercase()
        .replace("Á", "A").replace("É", "E").replace("Í", "I")
        .replace("Ó", "O").replace("Ú", "U")
        .replace(Regex("[^A-Z0-9\\s]"), "")
        .replace(Regex("\\s+"), " ")
        .trim()

internal data class InfoBloqueComponente(
    val tipo: String, // "Teoría", "Laboratorio", "Práctica" o "Clase"
    val nrc: String?,
    val seccion: String?,
    val docente: String?
)

internal data class TipoClaseInfo(
    val letra: String,           // "T", "L", "P"
    val nombreCompleto: String,  // "Teoría", "Laboratorio", "Práctica"
    val color: Color,
    val esLab: Boolean,
    val esPrac: Boolean
)

internal fun resolverTipoClase(
    curso: HorarioCurso,
    bloque: HorarioBloque,
    infoComponente: InfoBloqueComponente
): TipoClaseInfo {
    val tipoComp = infoComponente.tipo.trim().uppercase()
    val tipoBloque = (bloque.displayTipo ?: bloque.tipo ?: bloque.tipoComponente ?: bloque.tipoHorario)?.trim()?.uppercase() ?: ""
    val secComp = (infoComponente.seccion ?: bloque.seccion ?: curso.seccion)?.trim()?.uppercase() ?: ""
    val aula = bloque.aula?.trim()?.uppercase() ?: ""

    val bloquesMismoDia = curso.bloques.filter { it.dia == bloque.dia }.sortedBy { it.horaInicio ?: "" }
    val indiceEnDia = bloquesMismoDia.indexOf(bloque).let { if (it >= 0) it else 0 }
    val esSegundoBloqueDelDia = bloquesMismoDia.size >= 2 && indiceEnDia > 0

    // Detectar si explícitamente es Laboratorio
    val esLab = tipoComp == "L" || tipoComp.contains("LAB") ||
            tipoBloque == "L" || tipoBloque.contains("LAB") ||
            secComp == "L" || secComp.endsWith("L") || secComp.contains("LAB") ||
            aula.contains("LAB") ||
            esSegundoBloqueDelDia

    // Detectar si explícitamente es Práctica
    val esPrac = !esLab && (tipoComp == "P" || tipoComp.contains("PRAC") ||
            tipoBloque == "P" || tipoBloque.contains("PRAC") ||
            secComp == "P" || secComp.endsWith("P") || secComp.contains("PRAC"))

    return when {
        esLab -> TipoClaseInfo(
            letra = "L",
            nombreCompleto = "Laboratorio",
            color = Color(0xFF7C3AED),
            esLab = true,
            esPrac = false
        )
        esPrac -> TipoClaseInfo(
            letra = "P",
            nombreCompleto = "Práctica",
            color = Color(0xFF0D9488),
            esLab = false,
            esPrac = true
        )
        else -> TipoClaseInfo(
            letra = "T",
            nombreCompleto = "Teoría",
            color = UpaoBlue,
            esLab = false,
            esPrac = false
        )
    }
}

private fun formatDocente(docente: String?): String {
    if (docente.isNullOrBlank()) return "Docente no asignado en el sistema"
    return if (docente.contains(" / ")) {
        docente.split(" / ").map { toTitleCase(it.trim()) }.filter { it.isNotBlank() }.joinToString(" / ")
    } else {
        toTitleCase(docente)
    }
}

/**
 * Deduce el componente correspondiente a un bloque específico de un curso,
 * priorizando los datos directos del horario (docente, tipo, aula, NRC) extraídos por el backend,
 * y complementando con los registros de asistencia.
 */
internal fun deducirInfoComponente(
    curso: HorarioCurso,
    bloque: HorarioBloque,
    asistenciaCursos: List<AsistenciaCurso>
): InfoBloqueComponente {
    val nomNormalizado = normalizarNombre(curso.displayNombre)
    val codMateria = curso.codigoMateria?.trim()?.uppercase() ?: ""

    // 1. Datos directos del bloque o del curso en horario (extraídos por el backend)
    val docBloque = bloque.displayDocente?.takeIf { it.isNotBlank() }
    val docCurso = curso.displayDocente?.takeIf { it.isNotBlank() }
    val tipoDirecto = bloque.displayTipo?.takeIf { it.isNotBlank() }
    val nrcDirecto = (bloque.nrc ?: bloque.crn)?.takeIf { it.isNotBlank() }
    val secDirecta = (bloque.seccion ?: curso.seccion)?.takeIf { it.isNotBlank() }

    // Buscar el curso en la lista de asistencia
    val asisMatch = asistenciaCursos.firstOrNull { a ->
        val crnA = a.crn?.trim()
        val crnH = curso.crn?.trim()
        if (!crnA.isNullOrBlank() && !crnH.isNullOrBlank() && (crnA == crnH || a.componentes.any { it.crn?.trim() == crnH })) return@firstOrNull true
        val codA = a.codigoMateria?.trim()?.uppercase() ?: ""
        val nomA = normalizarNombre(a.displayNombre)
        if (codMateria.isNotBlank() && codA.isNotBlank() && codMateria == codA) {
            nomA == nomNormalizado || nomA.contains(nomNormalizado) || nomNormalizado.contains(nomA)
        } else {
            nomA == nomNormalizado || nomA.contains(nomNormalizado) || nomNormalizado.contains(nomA)
        }
    }

    val componentes = asisMatch?.componentes ?: emptyList()
    val totalBloques = curso.bloques.size
    val indiceBloque = curso.bloques.indexOf(bloque).let { if (it >= 0) it else 0 }

    val bloquesMismoDia = curso.bloques.filter { it.dia == bloque.dia }.sortedBy { it.horaInicio ?: "" }
    val indiceEnDia = bloquesMismoDia.indexOf(bloque).let { if (it >= 0) it else indiceBloque }
    val esSegundoBloqueDelDia = bloquesMismoDia.size >= 2 && indiceEnDia > 0

    if (componentes.size == 2) {
        val c1 = componentes[0]
        val c2 = componentes[1]
        val sec1 = c1.seccion?.uppercase() ?: ""
        val sec2 = c2.seccion?.uppercase() ?: ""
        val tip1 = (c1.tipo ?: c1.tipoComponente)?.uppercase() ?: ""
        val tip2 = (c2.tipo ?: c2.tipoComponente)?.uppercase() ?: ""

        val esLab1 = sec1.contains("LAB") || sec1.endsWith("L") || tip1.contains("LAB") || tip1 == "L"
        val esLab2 = sec2.contains("LAB") || sec2.endsWith("L") || tip2.contains("LAB") || tip2 == "L"
        val esTeor1 = sec1.contains("TEOR") || sec1.endsWith("T") || tip1.contains("TEOR") || tip1 == "T"
        val esTeor2 = sec2.contains("TEOR") || sec2.endsWith("T") || tip2.contains("TEOR") || tip2 == "T"

        val (teoriaComp, labComp) = when {
            esLab2 || esTeor1 -> c1 to c2
            esLab1 || esTeor2 -> c2 to c1
            else -> if (sec1 <= sec2) c1 to c2 else c2 to c1
        }

        val esLabPorPosicion = esSegundoBloqueDelDia || (indiceBloque > 0)
        val compElegido = if (esLabPorPosicion) labComp else teoriaComp

        val tipoNombre = when {
            esLabPorPosicion -> {
                val t = (compElegido.tipo ?: compElegido.tipoComponente)?.uppercase() ?: ""
                val s = compElegido.seccion?.uppercase() ?: ""
                if (t.contains("PRAC") || t == "P" || s.contains("PRAC") || s.endsWith("P")) "Práctica"
                else "Laboratorio"
            }
            tipoDirecto?.contains("LAB", ignoreCase = true) == true || tipoDirecto.equals("L", ignoreCase = true) -> "Laboratorio"
            tipoDirecto?.contains("PRAC", ignoreCase = true) == true || tipoDirecto.equals("P", ignoreCase = true) -> "Práctica"
            tipoDirecto?.contains("TEOR", ignoreCase = true) == true || tipoDirecto.equals("T", ignoreCase = true) -> "Teoría"
            else -> "Teoría"
        }

        val docenteFinal = docBloque
            ?: compElegido.displayDocente
            ?: docCurso
            ?: asisMatch?.displayDocente

        return InfoBloqueComponente(
            tipo = tipoNombre,
            nrc = nrcDirecto ?: compElegido.crn?.takeIf { it.isNotBlank() } ?: curso.crn,
            seccion = secDirecta ?: compElegido.seccion?.takeIf { it.isNotBlank() } ?: asisMatch?.seccion,
            docente = docenteFinal
        )
    } else if (componentes.isNotEmpty()) {
        val targetIndex = if (esSegundoBloqueDelDia && componentes.size > 1) 1 else indiceBloque.coerceAtMost(componentes.size - 1)
        val comp = componentes[targetIndex]
        val tipoComp = (comp.tipo ?: comp.tipoComponente)?.uppercase() ?: ""
        val secComp = comp.seccion?.uppercase() ?: ""
        val tipoNombre = when {
            esSegundoBloqueDelDia || tipoComp.contains("LAB") || tipoComp == "L" || secComp.contains("LAB") || secComp.endsWith("L") || bloque.aula?.contains("LAB", ignoreCase = true) == true -> "Laboratorio"
            tipoComp.contains("PRAC") || tipoComp == "P" || secComp.contains("PRAC") || secComp.endsWith("P") -> "Práctica"
            tipoComp.contains("TEOR") || tipoComp == "T" || secComp.contains("TEOR") || secComp.endsWith("T") -> "Teoría"
            tipoDirecto?.contains("LAB", ignoreCase = true) == true || tipoDirecto.equals("L", ignoreCase = true) -> "Laboratorio"
            tipoDirecto?.contains("PRAC", ignoreCase = true) == true || tipoDirecto.equals("P", ignoreCase = true) -> "Práctica"
            tipoDirecto?.contains("TEOR", ignoreCase = true) == true || tipoDirecto.equals("T", ignoreCase = true) -> "Teoría"
            indiceBloque == 0 -> "Teoría"
            else -> "Laboratorio"
        }
        val docenteFinal = docBloque
            ?: comp.displayDocente
            ?: docCurso
            ?: asisMatch?.displayDocente

        return InfoBloqueComponente(
            tipo = tipoNombre,
            nrc = nrcDirecto ?: comp.crn?.takeIf { it.isNotBlank() } ?: curso.crn,
            seccion = secDirecta ?: comp.seccion?.takeIf { it.isNotBlank() } ?: asisMatch?.seccion,
            docente = docenteFinal
        )
    }

    // Si no hay desglose en asistencia, deducir por la información directa del horario o por la posición del bloque
    val tipoDefecto = when {
        esSegundoBloqueDelDia -> "Laboratorio"
        bloque.aula?.contains("LAB", ignoreCase = true) == true -> "Laboratorio"
        (bloque.seccion ?: curso.seccion)?.contains("LAB", ignoreCase = true) == true -> "Laboratorio"
        (bloque.seccion ?: curso.seccion)?.endsWith("L", ignoreCase = true) == true -> "Laboratorio"
        (bloque.seccion ?: curso.seccion)?.contains("PRAC", ignoreCase = true) == true -> "Práctica"
        (bloque.seccion ?: curso.seccion)?.endsWith("P", ignoreCase = true) == true -> "Práctica"
        tipoDirecto?.contains("LAB", ignoreCase = true) == true || tipoDirecto.equals("L", ignoreCase = true) -> "Laboratorio"
        tipoDirecto?.contains("PRAC", ignoreCase = true) == true || tipoDirecto.equals("P", ignoreCase = true) -> "Práctica"
        tipoDirecto?.contains("TEOR", ignoreCase = true) == true || tipoDirecto.equals("T", ignoreCase = true) -> "Teoría"
        totalBloques >= 2 && indiceBloque > 0 -> "Laboratorio"
        else -> "Teoría"
    }
    val docenteFinal = docBloque
        ?: docCurso
        ?: asisMatch?.displayDocente

    return InfoBloqueComponente(
        tipo = tipoDefecto,
        nrc = nrcDirecto ?: curso.crn,
        seccion = secDirecta ?: asisMatch?.seccion,
        docente = docenteFinal
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HorarioContent(
    token: String,
    dashboard: DashboardViewModel,
    usuario: String? = null,
    onSesionExpirada: () -> Unit = {}
) {
    val context = LocalContext.current
    val isDark = LocalUpaoDarkTheme.current
    val palette = remember(isDark) { horarioThemePalette(isDark) }

    var selectedDate by rememberSaveable { mutableLongStateOf(scheduleDateStart(System.currentTimeMillis())) }
    var viewModeName by rememberSaveable { mutableStateOf(HorarioViewMode.DAILY.name) }
    val viewMode = remember(viewModeName) {
        HorarioViewMode.entries.firstOrNull { it.name == viewModeName } ?: HorarioViewMode.DAILY
    }

    val shared by dashboard.state.collectAsState()
    val selectedPeriodo = shared.schedulePeriod
    val cursos = shared.schedules[selectedPeriodo]?.listaCursos ?: emptyList()
    var ultimoHorarioValido by remember(selectedPeriodo) { mutableStateOf(cursos) }
    if (cursos.isNotEmpty()) {
        ultimoHorarioValido = cursos
    }
    val cursosAMostrar = if (cursos.isNotEmpty()) cursos else ultimoHorarioValido

    val asistenciaCursos = shared.attendance?.asistencia ?: emptyList()
    var bloqueSeleccionado by remember { mutableStateOf<Pair<HorarioCurso, HorarioBloque>?>(null) }
    var mostrarDialogCompartir by remember { mutableStateOf(false) }
    val isLoading = shared.busy
    val isRefreshing = shared.busy
    fun claveCache() = dashboard.scheduleCacheKey(selectedPeriodo)
    val errorMessage = shared.errors[claveCache()]
    val offline = claveCache() !in shared.fresh
    fun loadHorario() = dashboard.refresh()

    val today = scheduleDateStart(System.currentTimeMillis())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        if (offline || isLoading || isRefreshing) {
            val cacheKey = claveCache()
            if (shared.times[cacheKey] != null || isLoading || isRefreshing) {
                com.example.upaos.ui.home.Freshness(shared.times[cacheKey], !offline, isLoading || isRefreshing)
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        if (offline && cursosAMostrar.isNotEmpty()) {
            Surface(
                color = palette.surface,
                border = BorderStroke(1.dp, palette.border),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            ) {
                Text(
                    text = "Horario guardado · pendiente de actualizar",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.textSecondary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        HorarioHeaderAndToggle(
            viewMode = viewMode,
            onViewModeChange = { viewModeName = it.name },
            isToday = selectedDate == today,
            onSelectToday = { selectedDate = today },
            onExportCalendar = {
                com.example.upaos.data.upao.IcsCalendarExporter.exportarAlCalendario(
                    context = context,
                    cursos = cursosAMostrar,
                    term = selectedPeriodo
                )
            },
            onShare = {
                if (cursosAMostrar.isEmpty()) {
                    Toast.makeText(context, "No hay horario cargado todavía para compartir", Toast.LENGTH_SHORT).show()
                } else {
                    mostrarDialogCompartir = true
                }
            },
            palette = palette
        )

        if (viewMode == HorarioViewMode.DAILY) {
            HorarioDailyCalendarBar(
                selectedDate = selectedDate,
                onDateSelected = { selectedDate = it },
                palette = palette
            )
        }

        // Pull-to-refresh envolviendo el contenido académico
        RefreshableContent(
            isRefreshing = isRefreshing,
            onRefresh = { loadHorario() },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when {
                isLoading && cursosAMostrar.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 8.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        repeat(3) { SkeletonHorarioCard(palette) }
                    }
                }
                errorMessage != null && cursosAMostrar.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        contentAlignment = Alignment.Center
                    ) {
                        ErrorView(
                            message = errorMessage!!,
                            onRetry = { loadHorario() }
                        )
                    }
                }
                cursosAMostrar.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyState(
                            icon = Icons.Filled.Schedule,
                            title = "Sin horario publicado",
                            subtitle = "No hay horario para el periodo $selectedPeriodo."
                        )
                    }
                }
                viewMode == HorarioViewMode.DAILY -> {
                    HorarioDailyTimeline(
                        cursos = cursosAMostrar,
                        asistenciaCursos = asistenciaCursos,
                        selectedDate = selectedDate,
                        selectedPeriodo = selectedPeriodo,
                        palette = palette,
                        onBloqueClick = { curso, bloque ->
                            bloqueSeleccionado = curso to bloque
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    HorarioWeeklyGrid(
                        cursos = cursosAMostrar,
                        asistenciaCursos = asistenciaCursos,
                        selectedDate = selectedDate,
                        selectedPeriodo = selectedPeriodo,
                        palette = palette,
                        onDateSelected = { selectedDate = it },
                        onBloqueClick = { curso, bloque ->
                            bloqueSeleccionado = curso to bloque
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    bloqueSeleccionado?.let { (curso, bloque) ->
        val infoComp = deducirInfoComponente(curso, bloque, asistenciaCursos)
        HorarioDetalleModal(
            curso = curso,
            bloque = bloque,
            infoComponente = infoComp,
            palette = palette,
            onDismiss = { bloqueSeleccionado = null }
        )
    }

    if (mostrarDialogCompartir) {
        com.example.upaos.ui.share.CompartirHorarioDialog(
            cursos = cursosAMostrar,
            periodo = selectedPeriodo,
            onDismiss = { mostrarDialogCompartir = false }
        )
    }
}

@Composable
private fun SkeletonHorarioCard(palette: HorarioThemePalette) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = palette.surface,
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            SkeletonBox(modifier = Modifier.fillMaxWidth(0.7f).height(14.dp), corner = 7.dp)
            Spacer(modifier = Modifier.height(6.dp))
            SkeletonBox(modifier = Modifier.fillMaxWidth(0.4f).height(10.dp), corner = 5.dp)
            Spacer(modifier = Modifier.height(10.dp))
            SkeletonBox(modifier = Modifier.fillMaxWidth().height(28.dp), corner = 8.dp)
        }
    }
}

/**
 * Modal BottomSheet interactivo que se despliega al pulsar un bloque del horario.
 * Muestra el desglose detallado con distinción de Teoría / Laboratorio,
 * NRC específico del componente, aula, horario y nombre del docente.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HorarioDetalleModal(
    curso: HorarioCurso,
    bloque: HorarioBloque,
    infoComponente: InfoBloqueComponente,
    palette: HorarioThemePalette,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val tipoInfo = resolverTipoClase(curso, bloque, infoComponente)
    val compColor = when {
        tipoInfo.esLab -> palette.labText
        tipoInfo.esPrac -> palette.pracText
        else -> palette.teoriaText
    }
    val compBg = when {
        tipoInfo.esLab -> palette.labBg
        tipoInfo.esPrac -> palette.pracBg
        else -> palette.teoriaBg
    }
    val compIcon = when {
        tipoInfo.esLab -> Icons.Filled.Science
        tipoInfo.esPrac -> Icons.Filled.Schedule
        else -> Icons.Filled.MenuBook
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header del Curso
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = toTitleCase(curso.displayNombre),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = curso.displayCodigo.ifBlank { "Curso Universitario" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = compBg
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = compIcon,
                            contentDescription = null,
                            tint = compColor,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tipoInfo.nombreCompleto.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = compColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tarjeta de Información Detallada del Componente
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Detalles de la Clase",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // NRC y Sección
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                Text(
                                    text = "NRC COMPONENTE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = infoComponente.nrc ?: curso.crn ?: "—",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        if (!infoComponente.seccion.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                    Text(
                                        text = "SECCIÓN",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Sec. ${infoComponente.seccion}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Horario y Día
                    val horaTxt = listOfNotNull(
                        bloque.horaInicio12h ?: bloque.horaInicio,
                        bloque.horaFin12h ?: bloque.horaFin
                    ).joinToString(" - ")

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccessTime,
                            contentDescription = "Horario",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Día y Horario",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = "${bloque.diaNombre ?: "Día programado"} · $horaTxt",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Aula
                    if (!bloque.aula.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Room,
                                contentDescription = "Aula",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Ubicación / Aula",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = bloque.aula!!,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Nombre del Profesor / Docente
                    Spacer(modifier = Modifier.height(8.dp))
                    val docNombre = infoComponente.docente?.takeIf { it.isNotBlank() }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (docNombre != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (docNombre != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = "Docente",
                                    tint = if (docNombre != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "DOCENTE / PROFESOR",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = formatDocente(docNombre),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (docNombre != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

