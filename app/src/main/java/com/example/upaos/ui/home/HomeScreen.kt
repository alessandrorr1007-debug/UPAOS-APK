package com.example.upaos.ui.home
import com.example.upaos.ui.components.CampusTopBar
import androidx.activity.compose.BackHandler

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.*
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.upaos.data.local.TokenManager
import com.example.upaos.ui.asistencia.AsistenciaContent
import com.example.upaos.ui.grades.GradesContent
import com.example.upaos.ui.horario.HorarioContent
import com.example.upaos.ui.payments.PaymentsHome
import com.example.upaos.ui.payments.PaymentsScreen
import com.example.upaos.ui.payments.PaymentsTopBar
import com.example.upaos.ui.payments.rememberPaymentsViewModel
import com.example.upaos.ui.theme.LocalUpaoDarkTheme
import com.example.upaos.ui.theme.minimalSectionPalette
import androidx.compose.material.icons.automirrored.filled.Assignment
import kotlinx.coroutines.launch

@Composable
private fun CampusNavigationIcon(selected: Boolean, active: ImageVector, inactive: ImageVector) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.12f else 1f,
        animationSpec = spring(dampingRatio = .7f, stiffness = 500f),
        label = "navigationSelection"
    )
    Icon(
        imageVector = if (selected) active else inactive,
        contentDescription = null,
        modifier = Modifier.size(22.dp).graphicsLayer { scaleX = scale; scaleY = scale }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    token: String,
    initialTab: String? = null,
    onTabHandled: () -> Unit = {},
    openAgenda: Boolean = false,
    onAgendaOpened: () -> Unit = {},
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onLogout: () -> Unit,
    onSwitchAccount: () -> Unit = onLogout,
    onOpenCalculator: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenIntegrations: () -> Unit,
    profileName: String? = null,
    profileFirstName: String? = null,
    onOpenProfile: () -> Unit = {},
    onOpenNotifications: () -> Unit,
    onOpenCourse: (String, String, String, String) -> Unit = { _, _, _, _ -> }
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val coroutineScope = rememberCoroutineScope()
    val initialPageIndex = remember {
        when (initialTab) {
            "notas" -> 1
            "horario" -> 2
            "asistencia" -> 3
            else -> 0
        }
    }
    val pagerState = rememberPagerState(initialPage = initialPageIndex, pageCount = { 5 })
    LaunchedEffect(openAgenda) { if (openAgenda) pagerState.scrollToPage(0) }
    LaunchedEffect(initialTab) {
        if (initialTab != null) {
            when (initialTab) {
                "notas" -> pagerState.scrollToPage(1)
                "horario" -> pagerState.scrollToPage(2)
                "asistencia" -> pagerState.scrollToPage(3)
            }
            onTabHandled()
        }
    }

    val usuario = tokenManager.getSavedUser()
    val session = remember(usuario, token) { tokenManager.getSessionId() }
    var accountOpen by rememberSaveable(session) { mutableStateOf(false) }
    var previousPage by rememberSaveable(session) { mutableIntStateOf(4) }
    val showingPayments = pagerState.currentPage == 4
    val showingAccount = showingPayments && accountOpen
    val paymentsModel = if (showingAccount && session != null && usuario != null) {
        rememberPaymentsViewModel(usuario, session)
    } else null
    val paymentsState = paymentsModel?.state?.collectAsState()?.value

    fun openPayments() {
        if (!showingPayments) previousPage = pagerState.currentPage
        coroutineScope.launch { pagerState.scrollToPage(4) }
    }

    fun leavePayments() {
        val destination = previousPage
        accountOpen = false
        coroutineScope.launch { pagerState.scrollToPage(destination) }
    }

    val savedAccount = remember(usuario) { tokenManager.getCuentas().firstOrNull { it.usuario == usuario } }
    val cachedProfile = remember(usuario) {
        usuario?.let { u ->
            try {
                tokenManager.getProfileJson(u)?.let { com.example.upaos.data.profile.StudentProfile.parse(it) }
            } catch (_: Exception) { null }
        }
    }
    val greetingName = profileFirstName
        ?: cachedProfile?.firstName
        ?: com.example.upaos.data.profile.StudentProfile.givenName(
            profileName ?: cachedProfile?.fullName ?: savedAccount?.nombreCorto ?: savedAccount?.nombre
        )

    fun cerrarSesion() {
        com.example.upaos.data.local.AccountPrivacy.logout(context)
        onLogout()
    }

    val dashboard: DashboardViewModel = viewModel(
        key = "dashboard_${usuario}_${token.hashCode()}",
        factory = DashboardViewModel.factory(context.applicationContext as android.app.Application, token, usuario ?: "anonimo")
    )
    val shared by dashboard.state.collectAsState()
    val unreadCount = shared.notices?.noLeidas ?: 0
    val lifecycle = androidx.compose.ui.platform.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(dashboard, lifecycle) {
        val observer = LifecycleEventObserver { _, _ ->
            dashboard.setForeground(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
        }
        lifecycle.addObserver(observer)
        dashboard.setForeground(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
        onDispose { lifecycle.removeObserver(observer); dashboard.setForeground(false) }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    BackHandler(enabled = showingPayments && drawerState.isClosed) { if (showingAccount) accountOpen = false else leavePayments() }
    var showAboutDialog by remember { mutableStateOf(false) }
    val appVersion = remember { com.example.upaos.util.AppUpdater.getCurrentVersionName(context) }

    val drawerPalette = minimalSectionPalette(LocalUpaoDarkTheme.current)
    val drawerDisplayName = remember(profileName, cachedProfile, savedAccount, usuario) {
        profileName?.takeIf { it.isNotBlank() }
            ?: cachedProfile?.fullName?.takeIf { it.isNotBlank() }
            ?: savedAccount?.nombre?.takeIf { it.isNotBlank() }
            ?: usuario
            ?: "Estudiante UPAO"
    }
    val drawerInitial = remember(drawerDisplayName) {
        drawerDisplayName.trim().firstOrNull { it.isLetter() }?.uppercaseChar()?.toString() ?: "U"
    }
    val drawerCode = usuario?.takeIf { it.isNotBlank() } ?: "Estudiante UPAO"

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .width(300.dp)
                    .testTag("navigation_drawer_sheet"),
                drawerContainerColor = drawerPalette.background,
                windowInsets = DrawerDefaults.windowInsets
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp, vertical = 18.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(drawerPalette.accentBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = drawerInitial,
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = drawerDisplayName,
                                fontSize = 15.sp,
                                lineHeight = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = drawerPalette.textPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = drawerPalette.pillBg
                            ) {
                                Text(
                                    text = drawerCode,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = drawerPalette.textPrimary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                )
                            }
                            Text(
                                text = "UPAO S",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = drawerPalette.textSecondary
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 14.dp),
                        thickness = 1.dp,
                        color = drawerPalette.border
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        DrawerMenuCard(
                            label = "Mi perfil",
                            icon = Icons.Filled.Person,
                            containerColor = drawerPalette.surface,
                            borderColor = drawerPalette.border,
                            iconColor = drawerPalette.textSecondary,
                            textColor = drawerPalette.textPrimary,
                            trailingColor = drawerPalette.textSecondary,
                            showArrow = true,
                            testTag = "drawer_profile_item",
                            onClick = {
                                coroutineScope.launch { drawerState.close(); onOpenProfile() }
                            }
                        )

                        DrawerMenuCard(
                            label = "Cambiar de cuenta",
                            icon = Icons.Filled.SwapHoriz,
                            containerColor = drawerPalette.surface,
                            borderColor = drawerPalette.border,
                            iconColor = drawerPalette.textSecondary,
                            textColor = drawerPalette.textPrimary,
                            trailingColor = drawerPalette.textSecondary,
                            showArrow = true,
                            testTag = "drawer_switch_account_item",
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                onSwitchAccount()
                            }
                        )

                        DrawerMenuCard(
                            label = "Acerca de",
                            icon = Icons.Filled.Info,
                            containerColor = drawerPalette.surface,
                            borderColor = drawerPalette.border,
                            iconColor = drawerPalette.textSecondary,
                            textColor = drawerPalette.textPrimary,
                            trailingColor = drawerPalette.textSecondary,
                            showArrow = true,
                            testTag = "drawer_about_item",
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                showAboutDialog = true
                            }
                        )

                        DrawerMenuCard(
                            label = "Cerrar sesión",
                            icon = Icons.AutoMirrored.Filled.Logout,
                            containerColor = drawerPalette.dangerBg,
                            borderColor = drawerPalette.dangerBorder,
                            iconColor = drawerPalette.dangerText,
                            textColor = drawerPalette.dangerText,
                            trailingColor = drawerPalette.dangerText,
                            showArrow = false,
                            testTag = "drawer_logout_item",
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                cerrarSesion()
                            }
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = drawerPalette.textMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "v.$appVersion",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = drawerPalette.textMuted
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                if (showingAccount) {
                    PaymentsTopBar(
                        onBack = { accountOpen = false },
                        onRefresh = paymentsModel?.let { it::refresh },
                        refreshing = paymentsState?.loading == true
                    )
                } else CampusTopBar(
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = "Menú principal",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    title = {
                        Text(
                            text = when (pagerState.currentPage) {
                                0 -> "Hoy"
                                1 -> "Mis Cursos"
                                2 -> "Horario"
                                3 -> "Asistencia"
                                else -> "Pagos"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
                    actions = {
                        IconButton(onClick = onToggleTheme) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                contentDescription = if (isDarkTheme) "Modo claro" else "Modo oscuro",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(onClick = onOpenCalculator) {
                            Icon(
                                imageVector = Icons.Filled.Calculate,
                                contentDescription = "Calculadora",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        BadgedBox(
                            badge = {
                                if (unreadCount > 0) {
                                    Badge {
                                        Text(if (unreadCount > 9) "9+" else "$unreadCount")
                                    }
                                }
                            }
                        ) {
                            IconButton(onClick = onOpenNotifications) {
                                Icon(
                                    imageVector = Icons.Filled.Notifications,
                                    contentDescription = "Notificaciones",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "Ajustes",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                )
            },
            bottomBar = {
                if (!showingAccount) {
                val navBarColors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primary,
                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f)),
                    shadowElevation = 6.dp
                ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    windowInsets = WindowInsets(0, 0, 0, 0)
                ) {
                    NavigationBarItem(
                        selected = pagerState.currentPage == 0,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } },
                        icon = { CampusNavigationIcon(pagerState.currentPage == 0, Icons.Filled.EventAvailable, Icons.Outlined.EventAvailable) },
                        label = { Text("Hoy", fontSize = 10.sp, maxLines = 1, softWrap = false) },
                        colors = navBarColors
                    )
                    NavigationBarItem(
                        selected = pagerState.currentPage == 1,
                        onClick = {
                            coroutineScope.launch { pagerState.animateScrollToPage(1) }
                        },
                        icon = { CampusNavigationIcon(pagerState.currentPage == 1, Icons.Filled.School, Icons.Outlined.School) },
                        label = { Text("Cursos", fontSize = 10.sp, maxLines = 1, softWrap = false) },
                        colors = navBarColors
                    )
                    NavigationBarItem(
                        selected = pagerState.currentPage == 2,
                        onClick = {
                            coroutineScope.launch { pagerState.animateScrollToPage(2) }
                        },
                        icon = { CampusNavigationIcon(pagerState.currentPage == 2, Icons.Filled.Schedule, Icons.Outlined.Schedule) },
                        label = { Text("Horario", fontSize = 10.sp, maxLines = 1, softWrap = false) },
                        colors = navBarColors
                    )
                    NavigationBarItem(
                        selected = pagerState.currentPage == 3,
                        onClick = {
                            coroutineScope.launch { pagerState.animateScrollToPage(3) }
                        },
                        icon = { CampusNavigationIcon(pagerState.currentPage == 3, Icons.Filled.FactCheck, Icons.Outlined.FactCheck) },
                        label = { Text("Asistencia", fontSize = 10.sp, maxLines = 1, softWrap = false) },
                        colors = navBarColors
                    )
                    NavigationBarItem(
                        selected = pagerState.currentPage == 4,
                        onClick = ::openPayments,
                        icon = { Icon(Icons.AutoMirrored.Filled.ReceiptLong, null, Modifier.size(22.dp)) },
                        label = { Text("Pagos", fontSize = 10.sp, maxLines = 1, softWrap = false) },
                        colors = navBarColors
                    )
                }
                }
                }
            }
        ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = !showingAccount,
                beyondViewportPageCount = 2,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) { page ->
                when (page) {
                    0 -> if (pagerState.currentPage == 0) TodayContent(token, dashboard, usuario,
                        openAgenda = openAgenda, onAgendaOpened = onAgendaOpened,
                        onSchedule = { coroutineScope.launch { pagerState.animateScrollToPage(2) } },
                        onConnectCanvas = onOpenIntegrations,
                        onNotifications = onOpenNotifications)
                    1 -> GradesContent(
                        token = token,
                        dashboard = dashboard,
                        usuario = usuario,
                        onOpenCourse = { periodo, carrera, crn, nombre ->
                            onOpenCourse(periodo, carrera, crn, nombre)
                        }
                    )
                    2 -> HorarioContent(
                        token = token,
                        dashboard = dashboard,
                        usuario = usuario
                    )
                    3 -> AsistenciaContent(
                        token = token,
                        dashboard = dashboard,
                        usuario = usuario
                    )
                    else -> if (!accountOpen) {
                        PaymentsHome(onOpenAccount = { accountOpen = true })
                    } else if (paymentsModel != null && paymentsState != null) {
                        PaymentsScreen(paymentsModel, paymentsState, onReauthenticate = ::cerrarSesion)
                    } else if (showingPayments) {
                        Text("Inicia sesión para consultar Estado de cuenta.", modifier = Modifier.padding(16.dp))
                    }
                }
            }
        }
    }
}

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            containerColor = drawerPalette.surface,
            titleContentColor = drawerPalette.textPrimary,
            textContentColor = drawerPalette.textSecondary,
            icon = {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(drawerPalette.softBlueBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.School,
                        contentDescription = null,
                        tint = drawerPalette.accentBlue,
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "UPAO S",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = drawerPalette.textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "v.$appVersion",
                        style = MaterialTheme.typography.bodySmall,
                        color = drawerPalette.textMuted
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Aplicación estudiantil no oficial diseñada para optimizar el acceso a cursos, horarios, notas, asistencias, pagos y tareas UPAO.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = drawerPalette.textSecondary
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = drawerPalette.surfaceSoftBlue,
                        border = BorderStroke(1.dp, drawerPalette.borderBlue),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Desarrollado para la comunidad universitaria UPAO.",
                                style = MaterialTheme.typography.labelMedium,
                                color = drawerPalette.textPrimary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Cerrar", color = drawerPalette.accentBlue, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
}

@Composable
private fun DrawerMenuCard(
    label: String,
    icon: ImageVector,
    containerColor: Color,
    borderColor: Color,
    iconColor: Color,
    textColor: Color,
    trailingColor: Color,
    showArrow: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                modifier = Modifier.weight(1f)
            )
            if (showArrow) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = trailingColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

