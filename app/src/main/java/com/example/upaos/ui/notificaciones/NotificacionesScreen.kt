package com.example.upaos.ui.notificaciones

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.upaos.data.model.NotificacionItem
import com.example.upaos.data.upao.UpaoRepository
import com.example.upaos.ui.components.CampusTopBar
import com.example.upaos.ui.components.ErrorView
import com.example.upaos.ui.components.NotificationCard
import com.example.upaos.ui.components.SkeletonBox
import com.example.upaos.ui.home.notificationMillis
import com.example.upaos.ui.theme.LocalUpaoDarkTheme
import com.example.upaos.ui.theme.MinimalSectionPalette
import com.example.upaos.ui.theme.minimalSectionPalette
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class NotifFilter(val label: String) {
    TODAS("Todas"),
    SIN_LEER("Sin leer"),
    NOTAS("Notas"),
    ASISTENCIA("Asistencias")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificacionesScreen(
    usuario: String?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val palette = minimalSectionPalette(LocalUpaoDarkTheme.current)
    val upaoRepository = remember { UpaoRepository(context) }
    val scope = rememberCoroutineScope()
    var notificaciones by remember { mutableStateOf<List<NotificacionItem>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedFilter by remember { mutableStateOf(NotifFilter.TODAS) }

    suspend fun cargar() {
        if (usuario == null) return
        try {
            val res = upaoRepository.getNotificaciones(usuario)
            notificaciones = res.notificaciones
            errorMessage = null
        } catch (e: Exception) {
            errorMessage = "Error al cargar notificaciones: ${e.localizedMessage}"
        } finally {
            cargando = false
        }
    }

    fun marcarLeida(notif: NotificacionItem) {
        if (notif.leida) return
        scope.launch {
            notificaciones = notificaciones.map {
                if (it.id == notif.id) it.copy(leida = true) else it
            }
            if (usuario != null) {
                upaoRepository.marcarNotificacionLeida(notif.id, usuario)
            }
        }
    }

    fun marcarTodasLeidas() {
        val noLeidas = notificaciones.filter { !it.leida }
        if (noLeidas.isEmpty() || usuario == null) return
        scope.launch {
            notificaciones = notificaciones.map { it.copy(leida = true) }
            upaoRepository.marcarTodasNotificacionesLeidas(usuario)
        }
    }

    LaunchedEffect(usuario) {
        cargar()
        cargando = false
    }

    val noLeidasTotal = notificaciones.count { !it.leida }

    val notificacionesFiltradas = remember(notificaciones, selectedFilter) {
        when (selectedFilter) {
            NotifFilter.TODAS -> notificaciones
            NotifFilter.SIN_LEER -> notificaciones.filter { !it.leida }
            NotifFilter.NOTAS -> notificaciones.filter { notif ->
                val esAsist = notif.componente?.contains("asistencia", ignoreCase = true) == true ||
                    notif.mensaje.contains("asistencia", ignoreCase = true) ||
                    notif.mensaje.contains("falta", ignoreCase = true)
                !esAsist
            }
            NotifFilter.ASISTENCIA -> notificaciones.filter { notif ->
                notif.componente?.contains("asistencia", ignoreCase = true) == true ||
                    notif.mensaje.contains("asistencia", ignoreCase = true) ||
                    notif.mensaje.contains("falta", ignoreCase = true)
            }
        }
    }

    Scaffold(
        containerColor = palette.background,
        topBar = {
            Column(modifier = Modifier.background(palette.background)) {
                CampusTopBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Notificaciones",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = palette.textPrimary
                            )
                            if (noLeidasTotal > 0) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = palette.softBlueBg
                                ) {
                                    Text(
                                        text = if (noLeidasTotal > 99) "99+" else "$noLeidasTotal",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        color = palette.softBlueText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = palette.background
                    ),
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("notifications_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver",
                                tint = palette.textPrimary
                            )
                        }
                    },
                    actions = {
                        if (noLeidasTotal > 0) {
                            IconButton(
                                onClick = { marcarTodasLeidas() },
                                modifier = Modifier.testTag("notifications_mark_all_read_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DoneAll,
                                    contentDescription = "Marcar todas como leídas",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (notificaciones.isNotEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 600.dp)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = palette.surfaceSoftBlue,
                            border = BorderStroke(1.dp, palette.borderBlue),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(palette.softBlueBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Notifications,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = palette.accentBlue
                                    )
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = if (noLeidasTotal > 0) "Tienes $noLeidasTotal avisos sin leer" else "Estás al día",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.textPrimary
                                    )
                                    Text(
                                        text = "Tu actividad académica, en un solo lugar",
                                        fontSize = 12.sp,
                                        color = palette.textSecondary
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            NotifFilter.entries.forEach { filtro ->
                                val selected = selectedFilter == filtro
                                Surface(
                                    onClick = { selectedFilter = filtro },
                                    shape = RoundedCornerShape(50),
                                    color = if (selected) palette.accentBlue else palette.surface,
                                    border = if (selected) null else BorderStroke(1.dp, palette.border),
                                    modifier = Modifier
                                        .heightIn(min = 36.dp)
                                        .testTag("notifications_filter_${filtro.name.lowercase()}")
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                    ) {
                                        Text(
                                            text = filtro.label,
                                            fontSize = 12.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (selected) Color.White else palette.textSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            when {
                cargando -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 600.dp)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            repeat(4) { SkeletonNotificacion(palette) }
                        }
                    }
                }

                errorMessage != null && notificaciones.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        ErrorView(
                            message = errorMessage!!,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                }

                notificacionesFiltradas.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyNotificationsCard(
                            title = when (selectedFilter) {
                                NotifFilter.TODAS -> "Aún no hay notificaciones"
                                NotifFilter.SIN_LEER -> "Todo leído"
                                else -> "Sin notificaciones en esta categoría"
                            },
                            subtitle = if (selectedFilter == NotifFilter.SIN_LEER) {
                                "Ya revisaste todos tus avisos. Puedes consultarlos en Todas."
                            } else {
                                "Aquí aparecerán los cambios de notas y asistencias. Puedes elegir la frecuencia de revisión en Ajustes."
                            },
                            palette = palette
                        )
                    }
                }

                else -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 600.dp)
                                .fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val today = LocalDate.now(ZoneId.of("America/Lima"))
                            val groups = notificacionesFiltradas.sortedByDescending {
                                notificationMillis(it.fechaCreacion) ?: Long.MIN_VALUE
                            }.groupBy { notif ->
                                notificationMillis(notif.fechaCreacion)?.let {
                                    Instant.ofEpochMilli(it).atZone(ZoneId.of("America/Lima")).toLocalDate()
                                }
                            }
                            groups.forEach { (date, notices) ->
                                item(key = "date_$date") {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp, bottom = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = when (date) {
                                                today -> "Hoy"
                                                today.minusDays(1) -> "Ayer"
                                                null -> "Sin fecha"
                                                else -> date.format(
                                                    DateTimeFormatter.ofPattern(
                                                        "d MMM yyyy",
                                                        Locale.forLanguageTag("es-PE")
                                                    )
                                                )
                                            },
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = palette.textPrimary
                                        )
                                        HorizontalDivider(
                                            modifier = Modifier.weight(1f),
                                            thickness = 1.dp,
                                            color = palette.border
                                        )
                                        Text(
                                            text = notices.size.toString(),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = palette.textMuted
                                        )
                                    }
                                }
                                items(notices, key = { it.id }) { notif ->
                                    NotificacionCard(notif = notif, onClick = { marcarLeida(notif) })
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
private fun EmptyNotificationsCard(
    title: String,
    subtitle: String,
    palette: MinimalSectionPalette
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = palette.surface,
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 420.dp)
            .testTag("notifications_empty_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(palette.softBlueBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = null,
                    tint = if (palette.isDark) palette.textPrimary else palette.accentBlue,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = palette.textSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun NotificacionCard(notif: NotificacionItem, onClick: () -> Unit) {
    NotificationCard(notice = notif, onClick = onClick)
}

@Composable
private fun SkeletonNotificacion(palette: MinimalSectionPalette) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = palette.surface,
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            SkeletonBox(modifier = Modifier.size(40.dp), corner = 12.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                SkeletonBox(modifier = Modifier.fillMaxWidth(0.35f).height(13.dp), corner = 6.dp)
                Spacer(modifier = Modifier.height(8.dp))
                SkeletonBox(modifier = Modifier.fillMaxWidth().height(15.dp), corner = 6.dp)
                Spacer(modifier = Modifier.height(8.dp))
                SkeletonBox(modifier = Modifier.fillMaxWidth(0.3f).height(11.dp), corner = 5.dp)
            }
        }
    }
}
