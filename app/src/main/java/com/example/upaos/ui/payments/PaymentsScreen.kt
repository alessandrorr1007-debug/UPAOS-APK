package com.example.upaos.ui.payments

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.upaos.data.payments.*
import com.example.upaos.ui.theme.LocalUpaoDarkTheme

private val panel = RoundedCornerShape(18.dp)
private val inner = RoundedCornerShape(13.dp)

private data class PaymentColors(
    val background: Color, val surface: Color, val border: Color, val header: Color,
    val blue: Color, val accent: Color, val tint: Color, val green: Color, val greenTint: Color,
    val heroStart: Color, val heroEnd: Color
)

@Composable
private fun paymentColors(): PaymentColors = if (LocalUpaoDarkTheme.current) {
    PaymentColors(
        Color(0xFF050B17), Color(0xFF101C31), Color(0xFF273A56), Color(0xFF061A3B),
        Color(0xFF075ADC), Color(0xFF9BC9FF), Color(0xFF142A50), Color(0xFF77E5A0),
        Color(0xFF10362F), Color(0xFF092A66), Color(0xFF071B3B)
    )
} else {
    PaymentColors(
        Color(0xFFF7F9FC), Color.White, Color(0xFFDFE6F1), Color(0xFF073B87),
        Color(0xFF0754C9), Color(0xFF0754C9), Color(0xFFE4EEFF), Color(0xFF146C35),
        Color(0xFFE2F8EB), Color(0xFF0759CE), Color(0xFF084895)
    )
}

@Composable
internal fun PaymentsHome(onOpenAccount: () -> Unit) {
    val colors = paymentColors()
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.background),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(colors.heroStart, colors.heroEnd)))
                    .padding(horizontal = 22.dp, vertical = 23.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Pagos", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Estado de cuenta y pagos universitarios", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                }
                Icon(Icons.Outlined.CreditCard, null, Modifier.size(55.dp), tint = Color(0xFFB9D8FF))
            }
        }
        item {
            Surface(onClick = onOpenAccount, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = panel, color = colors.surface, border = BorderStroke(1.dp, colors.border), shadowElevation = 2.dp) {
                EntryRow(Icons.AutoMirrored.Outlined.ReceiptLong, "Estado de cuenta", "Consulta tu resumen, periodos y movimientos") {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
                }
            }
        }
        item {
            Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = panel,
                color = colors.surface, border = BorderStroke(1.dp, colors.border), shadowElevation = 2.dp) {
                EntryRow(Icons.Outlined.Description, "Constancias y otros", "Constancias de pago, boletas y más", upcoming = true)
            }
        }
    }
}

@Composable
private fun EntryRow(icon: ImageVector, title: String, description: String, upcoming: Boolean = false,
    action: (@Composable () -> Unit)? = null) {
    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        BadgeIcon(icon)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (upcoming) Text("Próximamente", Modifier.background(paymentColors().tint, inner).padding(horizontal = 9.dp, vertical = 3.dp),
                style = MaterialTheme.typography.labelMedium, color = paymentColors().accent)
        }
        action?.invoke()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PaymentsTopBar(onBack: () -> Unit, onRefresh: (() -> Unit)?, refreshing: Boolean) {
    TopAppBar(
        title = { Text("Estado de cuenta", fontWeight = FontWeight.Bold) },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = paymentColors().header,
            titleContentColor = Color.White, navigationIconContentColor = Color.White, actionIconContentColor = Color.White),
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } },
        actions = { IconButton(onClick = { onRefresh?.invoke() }, enabled = onRefresh != null && !refreshing) {
            Icon(Icons.Outlined.Refresh, "Actualizar estado de cuenta")
        } }
    )
}

@Composable
internal fun PaymentsScreen(model: PaymentsViewModel, state: PaymentsState, onReauthenticate: () -> Unit) {
    DisposableEffect(model) {
        model.refresh()
        onDispose { model.clear() }
    }
    PaymentsContent(state, model::selectPeriod, model::refresh, onReauthenticate)
}

@Composable
internal fun PaymentsContent(state: PaymentsState, onSelectPeriod: (PaymentPeriod) -> Unit, onRetry: () -> Unit, onReauthenticate: () -> Unit) {
    var academic by rememberSaveable { mutableStateOf(false) }
    var expandedHistory by rememberSaveable { mutableStateOf(false) }
    val overview = state.overview
    val period = state.period
    val allTerms = period == null || period.code == ALL_TERMS
    val general = state.general ?: state.movements.takeIf { allTerms }
    val colors = paymentColors()

    Column(Modifier.fillMaxSize().background(colors.background)) {
        AccountTabs(academic) { academic = it }
        if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        key(academic) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.loading && (overview == null || academic)) {
                    item { Text("Consultando estado de cuenta…", style = MaterialTheme.typography.bodyMedium) }
                }
                state.error?.let { error ->
                    if (academic || overview == null || general == null) item {
                        MessagePanel(Icons.Outlined.Info, error.message) {
                            TextButton(onClick = onRetry) { Text("Reintentar") }
                            if (error == PaymentError.EXPIRED) TextButton(onClick = onReauthenticate) { Text("Volver a iniciar sesión") }
                        }
                    }
                }
                if (overview != null && !academic) {
                    item { BalanceHero("Saldo total", overview.balance, overview.date) }
                    item {
                        PaymentPanel {
                            Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                BadgeIcon(Icons.Outlined.AccountBalanceWallet)
                                Column(Modifier.weight(1f)) {
                                    Text("Importe pendiente", style = MaterialTheme.typography.labelLarge)
                                    Text(overview.due ?: "Importe no disponible", style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    general?.totals?.let { item { Metrics(it) } }
                    item { SectionTitle("Movimientos generales") }
                    val rows = general?.items.orEmpty()
                    if (rows.isEmpty() && !state.loading && state.error == null) {
                        item { MessagePanel(Icons.AutoMirrored.Outlined.ReceiptLong, "No hay movimientos en la cuenta.") }
                    }
                    items(if (expandedHistory) rows else rows.take(3)) { MovementRow(it) }
                    if (rows.size > 3) item {
                        OutlinedButton(onClick = { expandedHistory = !expandedHistory }, Modifier.fillMaxWidth()) {
                            Text(if (expandedHistory) "Mostrar menos movimientos" else "Ver todos los movimientos")
                        }
                    }
                    item {
                        PaymentPanel {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                overview.currency?.let { Text("Moneda · $it") }
                                Text(when (val holds = overview.holds) {
                                    null -> "Retenciones · Sin información"
                                    0 -> "Sin retenciones"
                                    else -> "Retenciones · $holds"
                                })
                            }
                        }
                    }
                }
                if (overview != null && academic) {
                    val terms = overview.periods.filter { it.code != ALL_TERMS }
                    if (overview.periods.isNotEmpty()) item { TermSelector(overview.periods, period, onSelectPeriod) }
                    if (allTerms) {
                        item { BalanceHero("Saldo de todos los periodos", overview.balance) }
                        item { SectionTitle("Periodos académicos") }
                        if (terms.isEmpty()) item { MessagePanel(Icons.Outlined.CalendarToday, "No hay periodos disponibles.") }
                        if (terms.isNotEmpty()) item {
                            PaymentPanel {
                                Column {
                                    terms.forEachIndexed { index, term ->
                                        if (index > 0) HorizontalDivider(color = colors.border)
                                        Surface(onClick = { onSelectPeriod(term) }, color = colors.surface) {
                                            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                                BadgeIcon(Icons.Outlined.CalendarToday)
                                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                                    Text(term.label, fontWeight = FontWeight.SemiBold)
                                                    Text(term.balance ?: "Saldo no disponible", color = colors.accent)
                                                }
                                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        item { BalanceHero("Saldo del periodo", period.balance, caption = period.label) }
                        state.movements?.totals?.let { item { Metrics(it) } }
                        item { SectionTitle("Movimientos del periodo") }
                        val rows = state.movements?.items.orEmpty()
                        if (!state.loading && state.error == null && rows.isEmpty()) {
                            item { MessagePanel(Icons.AutoMirrored.Outlined.ReceiptLong, if (period.canOpen)
                                "No hay movimientos para este periodo." else "Este periodo solo muestra el saldo.") }
                        }
                        items(rows) { MovementRow(it) }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountTabs(academic: Boolean, select: (Boolean) -> Unit) {
    val colors = paymentColors()
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)
        .background(colors.surface, panel).padding(3.dp).selectableGroup()) {
        listOf(false, true).forEach { term ->
            val selected = academic == term
            val background by animateColorAsState(if (selected) colors.blue else colors.surface, label = "Pestaña de Pagos")
            val foreground = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
            Column(
                Modifier.weight(1f).clip(inner).background(background)
                    .selectable(selected, role = Role.Tab, onClick = { select(term) })
                    .heightIn(min = 60.dp).padding(horizontal = 5.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
            ) {
                Icon(if (term) Icons.Outlined.CalendarToday else Icons.Outlined.Layers, null, Modifier.size(22.dp), tint = foreground)
                Text(if (term) "Periodo académico" else "Resumen de cuenta", color = foreground,
                    style = MaterialTheme.typography.labelLarge, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
}

@Composable
private fun BalanceHero(title: String, amount: String?, date: String? = null, caption: String? = null) {
    val colors = paymentColors()
    Row(Modifier.fillMaxWidth().clip(panel)
        .background(Brush.horizontalGradient(listOf(colors.heroStart, colors.heroEnd))).padding(17.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            caption?.let { Text(it, color = Color(0xFFD4E5FF), style = MaterialTheme.typography.labelLarge) }
            Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium)
            Text(amount ?: "Saldo no disponible", color = Color.White,
                style = if (amount == null) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold)
            date?.let { Text("Actualizado al $it", color = Color(0xFFE2EDFF), style = MaterialTheme.typography.bodyMedium) }
        }
        Icon(Icons.Outlined.AccountBalanceWallet, null, Modifier.size(48.dp), tint = Color(0xFFB9D8FF))
    }
}

@Composable
private fun Metrics(totals: MovementAmounts) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth < 340.dp || androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.2f) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Metric("Total de cargos", totals.charge, false, Modifier.fillMaxWidth())
                Metric("Total de pagos", totals.payment, true, Modifier.fillMaxWidth())
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Metric("Total de cargos", totals.charge, false, Modifier.weight(1f))
                Metric("Total de pagos", totals.payment, true, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Metric(label: String, amount: String?, paid: Boolean, modifier: Modifier) {
    val colors = paymentColors()
    Surface(modifier, shape = panel, color = if (paid) colors.greenTint else colors.tint,
        contentColor = MaterialTheme.colorScheme.onSurface) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Icon(if (paid) Icons.Outlined.CheckCircle else Icons.AutoMirrored.Outlined.ReceiptLong, null,
                Modifier.size(21.dp), tint = if (paid) colors.green else colors.accent)
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(amount ?: "No disponible", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MovementRow(movement: PaymentMovement) {
    val colors = paymentColors()
    PaymentPanel {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top) {
            BadgeIcon(Icons.AutoMirrored.Outlined.ReceiptLong)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(movement.label ?: movement.code ?: "Movimiento", fontWeight = FontWeight.SemiBold)
                movement.code?.let { Text("Código · $it", style = MaterialTheme.typography.bodySmall) }
                movement.date?.let { Text("Fecha · $it", style = MaterialTheme.typography.bodySmall) }
                movement.amounts.charge?.let { Text("Cargo · $it", color = colors.accent) }
                movement.amounts.payment?.let { Text("Pago · $it", color = colors.green) }
                movement.amounts.balance?.let { Text("Saldo · $it", fontWeight = FontWeight.Medium) }
            }
        }
    }
}

@Composable
private fun TermSelector(periods: List<PaymentPeriod>, current: PaymentPeriod?, select: (PaymentPeriod) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Seleccionar periodo académico", style = MaterialTheme.typography.labelLarge)
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = inner, border = BorderStroke(1.dp, paymentColors().border)) {
                Text(current?.label ?: "Todos los periodos", Modifier.weight(1f))
                Icon(Icons.Outlined.KeyboardArrowDown, null)
            }
            DropdownMenu(expanded, onDismissRequest = { expanded = false }, modifier = Modifier.heightIn(max = 380.dp)) {
                periods.forEach { period ->
                    DropdownMenuItem(text = { Text(period.label) }, onClick = { expanded = false; select(period) })
                }
            }
        }
    }
}

@Composable
private fun PaymentPanel(content: @Composable () -> Unit) {
    val colors = paymentColors()
    Surface(Modifier.fillMaxWidth(), shape = panel, color = colors.surface, border = BorderStroke(1.dp, colors.border), content = content)
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
}

@Composable
private fun BadgeIcon(icon: ImageVector) {
    val colors = paymentColors()
    Box(Modifier.size(40.dp).background(colors.tint, inner), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(23.dp), tint = colors.accent)
    }
}

@Composable
private fun MessagePanel(icon: ImageVector, message: String, actions: (@Composable ColumnScope.() -> Unit)? = null) {
    PaymentPanel {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BadgeIcon(icon)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(message)
                actions?.invoke(this)
            }
        }
    }
}
