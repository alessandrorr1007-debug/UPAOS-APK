package com.example.upaos.ui.payments

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.upaos.data.payments.*
import com.example.upaos.ui.theme.LocalUpaoDarkTheme

private val CardRadius = RoundedCornerShape(20.dp)
private val SubCardRadius = RoundedCornerShape(16.dp)
private val IconBoxRadius = RoundedCornerShape(13.dp)

@Composable
internal fun PaymentsHome(onOpenAccount: () -> Unit) {
    val palette = paymentsPalette(LocalUpaoDarkTheme.current)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp, vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "Pagos",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = palette.textPrimary
                )
                Text(
                    text = "Estado de cuenta y pagos universitarios",
                    fontSize = 13.5.sp,
                    color = palette.textSecondary
                )
            }
        }

        item {
            Surface(
                onClick = onOpenAccount,
                shape = CardRadius,
                color = palette.surface,
                border = BorderStroke(1.dp, palette.border),
                shadowElevation = if (palette.isDark) 0.dp else 1.5.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("payments_entry_account")
            ) {
                EntryRow(
                    icon = Icons.Outlined.EventAvailable,
                    title = "Estado de cuenta",
                    description = "Consulta tu resumen, períodos y movimientos",
                    palette = palette,
                    solidIconBadge = true,
                    showArrow = true
                )
            }
        }

        item {
            Surface(
                shape = CardRadius,
                color = palette.surface,
                border = BorderStroke(1.dp, palette.border),
                shadowElevation = if (palette.isDark) 0.dp else 1.5.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("payments_entry_certificates")
            ) {
                EntryRow(
                    icon = Icons.Outlined.Description,
                    title = "Constancias y otros",
                    description = "Constancias de pago, boletas y más",
                    palette = palette,
                    solidIconBadge = false,
                    upcoming = true,
                    showArrow = false
                )
            }
        }
    }
}

@Composable
private fun EntryRow(
    icon: ImageVector,
    title: String,
    description: String,
    palette: PaymentsPalette,
    solidIconBadge: Boolean,
    upcoming: Boolean = false,
    showArrow: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(IconBoxRadius)
                .background(if (solidIconBadge) palette.accentBlue else palette.softBlueBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (solidIconBadge) Color.White else palette.softBlueText,
                modifier = Modifier.size(23.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary
            )
            Text(
                text = description,
                fontSize = 12.5.sp,
                lineHeight = 17.sp,
                color = palette.textSecondary
            )
            if (upcoming) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = palette.softBlueBg,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = "Próximamente",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.softBlueText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }
        }

        if (showArrow) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = palette.textMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PaymentsTopBar(onBack: () -> Unit, onRefresh: (() -> Unit)?, refreshing: Boolean) {
    val palette = paymentsPalette(LocalUpaoDarkTheme.current)
    TopAppBar(
        title = {
            Text(
                text = "Estado de cuenta",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = palette.textPrimary
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = palette.background,
            titleContentColor = palette.textPrimary,
            navigationIconContentColor = palette.textPrimary,
            actionIconContentColor = palette.textPrimary
        ),
        navigationIcon = {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("payments_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver"
                )
            }
        },
        actions = {
            IconButton(
                onClick = { onRefresh?.invoke() },
                enabled = onRefresh != null && !refreshing,
                modifier = Modifier.testTag("payments_refresh_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = "Actualizar estado de cuenta"
                )
            }
        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PaymentsContent(
    state: PaymentsState,
    onSelectPeriod: (PaymentPeriod) -> Unit,
    onRetry: () -> Unit,
    onReauthenticate: () -> Unit
) {
    var academic by rememberSaveable { mutableStateOf(false) }
    var expandedHistory by rememberSaveable { mutableStateOf(false) }
    var selectedMovement by remember { mutableStateOf<PaymentMovement?>(null) }

    val overview = state.overview
    val period = state.period
    val allTerms = period == null || period.code == ALL_TERMS
    val general = state.general ?: state.movements.takeIf { allTerms }
    val palette = paymentsPalette(LocalUpaoDarkTheme.current)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        AccountTabs(academic = academic, palette = palette) { academic = it }

        if (state.loading) {
            LinearProgressIndicator(
                color = palette.accentBlue,
                trackColor = palette.border,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
            )
        }

        key(academic) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.loading && (overview == null || academic)) {
                    item {
                        Text(
                            text = "Consultando estado de cuenta…",
                            fontSize = 13.5.sp,
                            color = palette.textSecondary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }

                state.error?.let { error ->
                    if (academic || overview == null || general == null) {
                        item {
                            MessagePanel(
                                icon = Icons.Outlined.Info,
                                message = error.message,
                                palette = palette
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = onRetry) { Text("Reintentar") }
                                    if (error == PaymentError.EXPIRED) {
                                        TextButton(onClick = onReauthenticate) { Text("Volver a iniciar sesión") }
                                    }
                                }
                            }
                        }
                    }
                }

                if (overview != null && !academic) {
                    item {
                        BalanceHero(
                            title = "Saldo total",
                            amount = overview.balance,
                            date = overview.date,
                            palette = palette
                        )
                    }

                    item {
                        DueAmountCard(
                            due = overview.due,
                            currency = overview.currency,
                            holds = overview.holds,
                            palette = palette
                        )
                    }

                    general?.totals?.let { totals ->
                        item { Metrics(totals = totals, palette = palette) }
                    }

                    val rows = general?.items.orEmpty()
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            SectionTitle("Movimientos generales", palette)
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.FormatListBulleted,
                                contentDescription = null,
                                tint = palette.textMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (rows.isEmpty() && !state.loading && state.error == null) {
                        item {
                            MessagePanel(
                                icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                                message = "No hay movimientos en la cuenta.",
                                palette = palette
                            )
                        }
                    }

                    items(if (expandedHistory) rows else rows.take(3)) { movement ->
                        MovementRow(
                            movement = movement,
                            palette = palette,
                            onClick = { selectedMovement = movement }
                        )
                    }

                    if (rows.size > 3) {
                        item {
                            OutlinedButton(
                                onClick = { expandedHistory = !expandedHistory },
                                shape = SubCardRadius,
                                border = BorderStroke(1.dp, palette.border),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = palette.surface,
                                    contentColor = palette.accentBlue
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("payments_toggle_movements")
                            ) {
                                Text(
                                    text = if (expandedHistory) "Mostrar menos movimientos" else "Ver todos los movimientos (${rows.size})",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.5.sp
                                )
                            }
                        }
                    }
                }

                if (overview != null && academic) {
                    val terms = overview.periods.filter { it.code != ALL_TERMS }
                    if (overview.periods.isNotEmpty()) {
                        item {
                            TermSelector(
                                periods = overview.periods,
                                current = period,
                                palette = palette,
                                select = onSelectPeriod
                            )
                        }
                    }

                    if (allTerms) {
                        item {
                            CompactBalanceCard(
                                title = "Saldo de todos los periodos",
                                amount = overview.balance,
                                palette = palette
                            )
                        }
                        item {
                            SectionTitle(
                                title = "Periodos académicos",
                                palette = palette,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        if (terms.isEmpty()) {
                            item {
                                MessagePanel(
                                    icon = Icons.Outlined.CalendarToday,
                                    message = "No hay periodos disponibles.",
                                    palette = palette
                                )
                            }
                        } else {
                            items(terms.size) { index ->
                                val term = terms[index]
                                PeriodItemCard(
                                    term = term,
                                    isCurrent = index == 0,
                                    palette = palette,
                                    onClick = { onSelectPeriod(term) }
                                )
                            }
                        }
                    } else {
                        item {
                            CompactBalanceCard(
                                title = "Saldo del periodo",
                                amount = period.balance,
                                caption = period.label,
                                palette = palette
                            )
                        }
                        state.movements?.totals?.let { totals ->
                            item { Metrics(totals = totals, palette = palette) }
                        }
                        item {
                            SectionTitle(
                                title = "Movimientos del periodo",
                                palette = palette,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        val rows = state.movements?.items.orEmpty()
                        if (!state.loading && state.error == null && rows.isEmpty()) {
                            item {
                                MessagePanel(
                                    icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                                    message = if (period.canOpen) "No hay movimientos para este periodo."
                                    else "Este periodo solo muestra el saldo.",
                                    palette = palette
                                )
                            }
                        }
                        items(rows) { movement ->
                            MovementRow(
                                movement = movement,
                                palette = palette,
                                onClick = { selectedMovement = movement }
                            )
                        }
                    }
                }
            }
        }
    }

    selectedMovement?.let { movement ->
        MovementDetailSheet(
            movement = movement,
            palette = palette,
            onDismiss = { selectedMovement = null }
        )
    }
}

@Composable
private fun AccountTabs(
    academic: Boolean,
    palette: PaymentsPalette,
    select: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(false, true).forEach { isAcademicTab ->
            val selected = academic == isAcademicTab
            val bgColor by animateColorAsState(
                targetValue = if (selected) palette.accentBlue else palette.surface,
                label = "paymentsTabBg"
            )
            val contentColor = if (selected) Color.White else palette.textPrimary
            val borderColor = if (selected) palette.accentBlue else palette.border

            Row(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(SubCardRadius)
                    .background(bgColor)
                    .border(1.dp, borderColor, SubCardRadius)
                    .selectable(
                        selected = selected,
                        role = Role.Tab,
                        onClick = { select(isAcademicTab) }
                    )
                    .testTag(if (isAcademicTab) "payments_tab_academic" else "payments_tab_summary")
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isAcademicTab) Icons.Outlined.CalendarToday else Icons.Outlined.School,
                    contentDescription = null,
                    tint = if (selected) Color.White else palette.textMuted,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = if (isAcademicTab) "Periodo académico" else "Resumen de cuenta",
                    color = contentColor,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun BalanceHero(
    title: String,
    amount: String?,
    date: String? = null,
    palette: PaymentsPalette
) {
    Surface(
        shape = CardRadius,
        border = BorderStroke(1.dp, palette.heroBorder),
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardRadius)
            .background(Brush.horizontalGradient(listOf(palette.heroStart, palette.heroEnd)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.heroLabel
            )
            Text(
                text = amount ?: "Saldo no disponible",
                fontSize = if (amount == null) 20.sp else 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = palette.heroValue,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            date?.let {
                Text(
                    text = "Actualizado al $it",
                    fontSize = 12.sp,
                    color = palette.heroCaption
                )
            }
        }
    }
}

@Composable
private fun CompactBalanceCard(
    title: String,
    amount: String?,
    caption: String? = null,
    palette: PaymentsPalette
) {
    Surface(
        shape = CardRadius,
        border = BorderStroke(1.dp, palette.heroBorder),
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardRadius)
            .background(Brush.horizontalGradient(listOf(palette.heroStart, palette.heroEnd)))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(IconBoxRadius)
                    .background(
                        if (palette.isDark) Color.White.copy(alpha = 0.14f) else palette.accentBlue.copy(alpha = 0.12f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Layers,
                    contentDescription = null,
                    tint = if (palette.isDark) Color.White else palette.accentBlue,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                caption?.let {
                    Text(
                        text = it,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.heroCaption,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = title,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = palette.heroLabel
                )
                Text(
                    text = amount ?: "Saldo no disponible",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = palette.heroValue,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun DueAmountCard(
    due: String?,
    currency: String?,
    holds: Int?,
    palette: PaymentsPalette
) {
    val holdLabel = when (holds) {
        null -> "Sin retenciones"
        0 -> "Sin retenciones"
        else -> "$holds retenciones"
    }
    val hasHolds = holds != null && holds > 0

    Surface(
        shape = SubCardRadius,
        color = palette.surface,
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.softBlueBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.AccountBalanceWallet,
                    contentDescription = null,
                    tint = palette.softBlueText,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Importe pendiente",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = palette.textSecondary
                )
                Text(
                    text = due ?: "Importe no disponible",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                shape = RoundedCornerShape(50),
                color = if (hasHolds) palette.warningBg else palette.softBlueBg
            ) {
                val badgeText = buildString {
                    if (!currency.isNullOrBlank()) {
                        append(currency)
                        append(" · ")
                    }
                    append(holdLabel)
                }
                Text(
                    text = badgeText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (hasHolds) palette.warningText else palette.softBlueText,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun Metrics(totals: MovementAmounts, palette: PaymentsPalette) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth < 330.dp || LocalDensity.current.fontScale > 1.25f) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Metric("Total de cargos", totals.charge, paid = false, palette = palette, modifier = Modifier.fillMaxWidth())
                Metric("Total de pagos", totals.payment, paid = true, palette = palette, modifier = Modifier.fillMaxWidth())
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Metric("Total de cargos", totals.charge, paid = false, palette = palette, modifier = Modifier.weight(1f))
                Metric("Total de pagos", totals.payment, paid = true, palette = palette, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Metric(
    label: String,
    amount: String?,
    paid: Boolean,
    palette: PaymentsPalette,
    modifier: Modifier
) {
    val bgColor = if (paid) palette.greenBg else palette.surface
    val borderColor = if (paid) palette.greenBorder else palette.border
    val iconBg = if (paid) {
        palette.greenAccent.copy(alpha = if (palette.isDark) 0.18f else 0.14f)
    } else {
        palette.softBlueBg
    }
    val iconTint = if (paid) palette.greenAccent else palette.softBlueText

    Surface(
        modifier = modifier,
        shape = SubCardRadius,
        color = bgColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (paid) Icons.Outlined.CheckCircle else Icons.Outlined.Layers,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = label,
                    fontSize = 11.5.sp,
                    color = palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = amount ?: "No disponible",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun MovementRow(
    movement: PaymentMovement,
    palette: PaymentsPalette,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CardRadius,
        color = palette.surface,
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.softBlueBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = null,
                    tint = palette.softBlueText,
                    modifier = Modifier.size(21.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = movement.label ?: movement.code ?: "Movimiento",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                movement.code?.let {
                    Text(
                        text = "Código: $it",
                        fontSize = 12.sp,
                        color = palette.textSecondary
                    )
                }
                movement.date?.let {
                    Text(
                        text = "Fecha: $it",
                        fontSize = 12.sp,
                        color = palette.textSecondary
                    )
                }
                movement.amounts.charge?.let {
                    MovementAmountLine(label = "Cargo:", value = it, valueColor = palette.textPrimary, labelColor = palette.textSecondary)
                }
                movement.amounts.payment?.let {
                    MovementAmountLine(label = "Pago:", value = it, valueColor = palette.greenAccent, labelColor = palette.greenAccent)
                }
                movement.amounts.balance?.let {
                    MovementAmountLine(label = "Saldo:", value = it, valueColor = palette.textPrimary, labelColor = palette.textSecondary)
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Ver detalle del movimiento",
                tint = palette.textMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun MovementAmountLine(label: String, value: String, valueColor: Color, labelColor: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = labelColor,
            modifier = Modifier.widthIn(min = 42.dp)
        )
        Text(
            text = value,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MovementDetailSheet(
    movement: PaymentMovement,
    palette: PaymentsPalette,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = movement.label ?: movement.code ?: "Detalle del movimiento",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary
            )
            Surface(
                shape = SubCardRadius,
                color = palette.background,
                border = BorderStroke(1.dp, palette.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    movement.code?.let { DetailSheetRow("Código", it, palette.textPrimary, palette) }
                    movement.date?.let { DetailSheetRow("Fecha", it, palette.textPrimary, palette) }
                    movement.amounts.charge?.let { DetailSheetRow("Cargo", it, palette.textPrimary, palette) }
                    movement.amounts.payment?.let { DetailSheetRow("Pago", it, palette.greenAccent, palette) }
                    movement.amounts.balance?.let { DetailSheetRow("Saldo", it, palette.accentBlue, palette) }
                }
            }
        }
    }
}

@Composable
private fun DetailSheetRow(label: String, value: String, valueColor: Color, palette: PaymentsPalette) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 13.sp, color = palette.textSecondary)
        Text(text = value, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

@Composable
private fun TermSelector(
    periods: List<PaymentPeriod>,
    current: PaymentPeriod?,
    palette: PaymentsPalette,
    select: (PaymentPeriod) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Seleccionar periodo académico",
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium,
            color = palette.textSecondary
        )
        Box {
            Surface(
                onClick = { expanded = true },
                shape = SubCardRadius,
                color = palette.surface,
                border = BorderStroke(1.dp, palette.border),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("payments_term_selector")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = current?.label ?: "Todos los periodos",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowDown,
                        contentDescription = "Seleccionar periodo académico",
                        tint = palette.textMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.heightIn(max = 380.dp)
            ) {
                periods.forEach { period ->
                    DropdownMenuItem(
                        text = { Text(period.label, fontSize = 13.5.sp) },
                        onClick = {
                            expanded = false
                            select(period)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PeriodItemCard(
    term: PaymentPeriod,
    isCurrent: Boolean,
    palette: PaymentsPalette,
    onClick: () -> Unit
) {
    val activeBalance = hasPositiveBalance(term.balance)
    Surface(
        onClick = onClick,
        shape = CardRadius,
        color = palette.surface,
        border = BorderStroke(1.dp, palette.border),
        shadowElevation = if (palette.isDark) 0.dp else 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
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
                    imageVector = Icons.Outlined.CalendarToday,
                    contentDescription = null,
                    tint = palette.softBlueText,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = term.label,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = term.balance ?: "Saldo no disponible",
                    fontSize = 13.sp,
                    fontWeight = if (activeBalance) FontWeight.Bold else FontWeight.Medium,
                    color = if (activeBalance) palette.accentBlue else palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isCurrent) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = palette.accentBlue
                ) {
                    Text(
                        text = "Actual",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = palette.textMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    palette: PaymentsPalette,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
        color = palette.textPrimary,
        modifier = modifier
    )
}

@Composable
private fun MessagePanel(
    icon: ImageVector,
    message: String,
    palette: PaymentsPalette,
    actions: (@Composable ColumnScope.() -> Unit)? = null
) {
    Surface(
        shape = CardRadius,
        color = palette.surface,
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                    imageVector = icon,
                    contentDescription = null,
                    tint = palette.softBlueText,
                    modifier = Modifier.size(21.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = message,
                    fontSize = 13.5.sp,
                    color = palette.textPrimary
                )
                actions?.invoke(this)
            }
        }
    }
}
