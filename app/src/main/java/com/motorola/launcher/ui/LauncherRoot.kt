package com.motorola.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import com.motorola.launcher.data.repository.AppRepository
import com.motorola.launcher.system.SystemTelemetryManager
import com.motorola.launcher.system.WallpaperZoomController
import com.motorola.launcher.ui.components.MarvelHydrationCompanion
import com.motorola.launcher.ui.dialog.AppContextDialog
import com.motorola.launcher.ui.dialog.JarvisDashboardSheet
import com.motorola.launcher.ui.dialog.QuickAccessCustomizerDialog
import com.motorola.launcher.ui.dialog.QuickControlsSheet
import com.motorola.launcher.ui.dialog.SettingsCustomizationDialog
import com.motorola.launcher.ui.drawer.AppDrawerScreen
import com.motorola.launcher.ui.gesture.drawerSwipeGesture
import com.motorola.launcher.ui.gesture.rememberDrawerGestureState
import com.motorola.launcher.ui.model.AppItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.motorola.launcher.ui.theme.MotoLauncherTheme
import com.motorola.launcher.ui.workspace.StarkWallpaperLayer
import com.motorola.launcher.ui.workspace.WorkspaceScreen
import kotlinx.collections.immutable.toImmutableList

/**
 * Root Composable orchestrating the 5 STARK OS views:
 * 1. Home Workspace with procedural Arc Reactor
 * 2. Adaptive Holographic App Drawer with categories & search
 * 3. System Dashboard with real device measurements
 * 4. Radial Quick Controls Menu & Toggles
 * 5. Settings & Customization (Theme, Wallpapers, Icons, Animations, Toggles)
 */
@Composable
fun LauncherRoot(
    repository: AppRepository,
    wallpaperZoomController: WallpaperZoomController,
    telemetryManager: SystemTelemetryManager,
    homePressEvents: kotlinx.coroutines.flow.SharedFlow<Unit>? = null,
    modifier: Modifier = Modifier
) {
    val allApps by repository.appsState.collectAsState()
    val quickAccessKeys by repository.preferences.quickAccessKeys.collectAsState()
    val dockKeys by repository.preferences.dockKeys.collectAsState()
    val customization by repository.preferences.customization.collectAsState()
    val telemetry by telemetryManager.telemetryState.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    val searchResults by remember(searchQuery) { repository.searchApps(searchQuery) }.collectAsState(initial = emptyList<AppItem>().toImmutableList())

    val displayedDrawerApps = remember(searchQuery, allApps, searchResults) {
        if (searchQuery.isBlank()) allApps else searchResults
    }

    // Resolve customized Dock apps (up to 6 apps)
    val dockApps = remember(allApps, dockKeys) {
        if (dockKeys.isEmpty()) {
            allApps.take(6).toImmutableList()
        } else {
            val appMap = allApps.associateBy { it.componentKey }
            dockKeys.mapNotNull { appMap[it] }.ifEmpty { allApps.take(6) }.toImmutableList()
        }
    }

    // Resolve customized Quick Access apps
    val quickAccessApps = remember(allApps, quickAccessKeys) {
        if (quickAccessKeys.isEmpty()) {
            allApps.take(5).toImmutableList()
        } else {
            val appMap = allApps.associateBy { it.componentKey }
            quickAccessKeys.mapNotNull { appMap[it] }.ifEmpty { allApps.take(5) }.toImmutableList()
        }
    }

    val coroutineScope = rememberCoroutineScope()
    var launchingApp by remember { mutableStateOf<AppItem?>(null) }
    val launchAnimationProgress = remember { Animatable(0f) }

    val launchAppWithAnimation: (AppItem) -> Unit = { item ->
        launchingApp = item
        coroutineScope.launch {
            launchAnimationProgress.snapTo(0f)
            launchAnimationProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 240, easing = androidx.compose.animation.core.FastOutSlowInEasing)
            )
            repository.launchApp(item)
            launchAnimationProgress.snapTo(0f)
            launchingApp = null
        }
    }

    // Modal, Sheet, and Companion States
    var selectedAppContextApp by remember { mutableStateOf<AppItem?>(null) }
    var showQuickAccessCustomizer by remember { mutableStateOf(false) }
    var showJarvisDashboard by remember { mutableStateOf(false) }
    var showQuickControls by remember { mutableStateOf(false) }
    var showCustomization by remember { mutableStateOf(false) }
    var showHydrationReminder by remember { mutableStateOf(false) }

    // Marvel Hydration Companion check with user-configurable interval
    LaunchedEffect(customization.hydrationReminderEnabled, customization.hydrationIntervalMinutes) {
        if (!customization.hydrationReminderEnabled) return@LaunchedEffect
        while (true) {
            val now = System.currentTimeMillis()
            val intervalMillis = customization.hydrationIntervalMinutes.coerceAtLeast(1) * 60 * 1000L
            if (now - customization.lastHydrationHour >= intervalMillis) {
                showHydrationReminder = true
            }
            delay(30000L) // Lightweight 30-second ticker, zero battery drain
        }
    }

    val gestureState = rememberDrawerGestureState(wallpaperZoomController)

    // Back button closes active sheets/dialogs or app drawer in priority order
    BackHandler(
        enabled = showHydrationReminder ||
                selectedAppContextApp != null ||
                showQuickAccessCustomizer ||
                showJarvisDashboard ||
                showQuickControls ||
                showCustomization ||
                gestureState.isDrawerVisible
    ) {
        when {
            showHydrationReminder -> showHydrationReminder = false
            selectedAppContextApp != null -> selectedAppContextApp = null
            showQuickAccessCustomizer -> showQuickAccessCustomizer = false
            showJarvisDashboard -> showJarvisDashboard = false
            showQuickControls -> showQuickControls = false
            showCustomization -> showCustomization = false
            searchQuery.isNotEmpty() -> searchQuery = ""
            else -> gestureState.close()
        }
    }

    // Reset back to main workspace when system Home button (center click / nav bar) is pressed
    LaunchedEffect(homePressEvents) {
        homePressEvents?.collect {
            showHydrationReminder = false
            selectedAppContextApp = null
            showQuickAccessCustomizer = false
            showJarvisDashboard = false
            showQuickControls = false
            showCustomization = false
            searchQuery = ""
            gestureState.close()
        }
    }

    MotoLauncherTheme(customization = customization) {
        BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val maxDragPx = with(density) { maxHeight.toPx() }

            val gestureModifier = Modifier
                .fillMaxSize()
                .drawerSwipeGesture(gestureState, maxDragPx)

            // Background: selected HUD wallpaper, or readability scrim over the user's photo
            StarkWallpaperLayer(
                preset = customization.wallpaper,
                accent = customization.themeColor.primary,
                modifier = Modifier.fillMaxSize()
            )

            // Screen 1: Workspace Layer (STARK OS Command Center)
            WorkspaceScreen(
                dockApps = dockApps,
                telemetry = telemetry,
                progressProvider = { gestureState.progress },
                onAppClick = { item -> launchAppWithAnimation(item) },
                onAppLongClick = { item -> selectedAppContextApp = item },
                onOpenDrawerClick = { gestureState.open() },
                onOpenDiagnostics = { showJarvisDashboard = true },
                onOpenQuickControls = { showQuickControls = true },
                onOpenCustomization = { showCustomization = true },
                modifier = gestureModifier
            )

            // Screen 2: App Drawer Layer (Active when drawer is visible or transitioning)
            if (gestureState.isDrawerVisible) {
                AppDrawerScreen(
                    apps = displayedDrawerApps,
                    quickAccessApps = quickAccessApps,
                    searchQuery = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onClearQuery = { searchQuery = "" },
                    progressProvider = { gestureState.progress },
                    maxDragPx = maxDragPx,
                    onAppClick = { item ->
                        launchAppWithAnimation(item)
                        gestureState.close()
                    },
                    onAppLongClick = { item -> selectedAppContextApp = item },
                    onCustomizeQuickAccessClick = { showQuickAccessCustomizer = true },
                    onCloseDrawer = { gestureState.close() },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Screen 3: JARVIS Holographic Diagnostic Modal
            if (showJarvisDashboard) {
                JarvisDashboardSheet(
                    telemetry = telemetry,
                    onDismiss = { showJarvisDashboard = false }
                )
            }

            // Screen 4: Quick Controls Radial HUD & Toggles Modal
            if (showQuickControls) {
                QuickControlsSheet(
                    onDismiss = { showQuickControls = false },
                    onTriggerHydrationTest = {
                        showQuickControls = false
                        showHydrationReminder = true
                    }
                )
            }

            // Screen 5: Settings & Customization Modal
            if (showCustomization) {
                SettingsCustomizationDialog(
                    preferences = repository.preferences,
                    customization = customization,
                    onDismiss = { showCustomization = false },
                    onTriggerHydrationTest = {
                        showCustomization = false
                        showHydrationReminder = true
                    }
                )
            }

            // Marvel Character Hourly Hydration Companion
            MarvelHydrationCompanion(
                visible = showHydrationReminder,
                glassesCount = customization.waterGlassesCount,
                onAcceptWater = {
                    repository.preferences.recordWaterAccepted()
                },
                onRefuseWater = {
                    repository.preferences.recordWaterRefused()
                },
                onDismiss = {
                    showHydrationReminder = false
                }
            )

            // App Context Menu (App Info, Quick Access, Dock, Uninstall)
            selectedAppContextApp?.let { app ->
                AppContextDialog(
                    app = app,
                    isQuickAccess = repository.preferences.isQuickAccess(app.componentKey),
                    isDockPinned = repository.preferences.isDockPinned(app.componentKey),
                    onOpenAppInfo = { repository.openAppInfo(app) },
                    onToggleQuickAccess = { repository.preferences.toggleQuickAccess(app.componentKey) },
                    onToggleDock = { repository.preferences.toggleDock(app.componentKey) },
                    onRequestUninstall = { repository.requestUninstall(app) },
                    onDismiss = { selectedAppContextApp = null }
                )
            }

            // Quick Access Customizer Modal
            if (showQuickAccessCustomizer) {
                QuickAccessCustomizerDialog(
                    allApps = allApps,
                    initialSelectedKeys = quickAccessKeys.ifEmpty { allApps.take(5).map { it.componentKey }.toSet() },
                    onSave = { newKeys -> repository.preferences.setQuickAccessKeys(newKeys) },
                    onDismiss = { showQuickAccessCustomizer = false }
                )
            }

            // High-Impact Quantum Holographic App Launch Transition
            // Fires strictly on tap (0% CPU at idle, zero battery consumption when not launching)
            if (launchAnimationProgress.value > 0f) {
                val p = launchAnimationProgress.value
                val primaryColor = customization.heroProtocol.primary
                val secondaryColor = customization.heroProtocol.secondary

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val maxRadius = kotlin.math.max(size.width, size.height)

                    // Quantum warp expanding shockwave
                    val ringRadius = maxRadius * p
                    val alpha = (1f - p).coerceIn(0f, 1f)

                    drawCircle(
                        color = primaryColor.copy(alpha = alpha * 0.7f),
                        radius = ringRadius,
                        center = center,
                        style = Stroke(width = (6f * (1f - p)).coerceAtLeast(1f))
                    )

                    drawCircle(
                        color = secondaryColor.copy(alpha = alpha * 0.4f),
                        radius = ringRadius * 0.85f,
                        center = center,
                        style = Stroke(width = (3f * (1f - p)).coerceAtLeast(1f))
                    )

                    // Iris shutter flash
                    drawCircle(
                        color = Color.Black.copy(alpha = (p * 0.65f).coerceIn(0f, 0.65f)),
                        radius = maxRadius,
                        center = center
                    )
                }
            }
        }
    }
}
