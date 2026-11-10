package com.motorola.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.motorola.launcher.data.repository.AppRepository
import com.motorola.launcher.system.ContextualLauncherEngine
import com.motorola.launcher.system.LauncherSoundManager
import com.motorola.launcher.system.SystemTelemetryManager
import com.motorola.launcher.system.WallpaperZoomController
import com.motorola.launcher.system.companion.FamilyCompanionOrchestrator
import com.motorola.launcher.ui.components.AnimatedWaterGlassIndicator
import com.motorola.launcher.ui.components.MarvelHydrationCompanion
import com.motorola.launcher.ui.dialog.AppContextDialog
import com.motorola.launcher.ui.dialog.HydrationDashboardSheet
import com.motorola.launcher.ui.dialog.HydrationSettingsDialog
import com.motorola.launcher.ui.dialog.JarvisDashboardSheet
import com.motorola.launcher.ui.dialog.QuickAccessCustomizerDialog
import com.motorola.launcher.ui.dialog.QuickControlsSheet
import com.motorola.launcher.ui.dialog.SettingsCustomizationDialog
import com.motorola.launcher.ui.drawer.AppDrawerScreen
import com.motorola.launcher.ui.gesture.GestureWorldRotationEffect
import com.motorola.launcher.ui.gesture.drawerSwipeGesture
import com.motorola.launcher.ui.gesture.edgeSwipeGesture
import com.motorola.launcher.ui.gesture.heroGestureTransform
import com.motorola.launcher.ui.gesture.rememberDrawerGestureState
import com.motorola.launcher.ui.gesture.rememberGestureWorldState
import com.motorola.launcher.ui.model.AppItem
import com.motorola.launcher.ui.theme.MotoLauncherTheme
import com.motorola.launcher.ui.workspace.StarkWallpaperLayer
import com.motorola.launcher.ui.workspace.WorkspaceScreen
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    hydrationDashboardEvents: kotlinx.coroutines.flow.SharedFlow<Unit>? = null,
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val isReducedMotion = remember(customization, context) {
        com.motorola.launcher.system.LauncherFeedback.isReducedMotion(context, customization)
    }

    val launchAppWithAnimation: (AppItem) -> Unit = { item ->
        launchingApp = item
        if (customization.soundEffectsEnabled) {
            LauncherSoundManager.playSound(
                context,
                LauncherSoundManager.SoundEvent.APP_CLICK,
                soundEnabled = true
            )
        }
        coroutineScope.launch {
            if (!isReducedMotion) {
                launchAnimationProgress.snapTo(0f)
                launchAnimationProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 200, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                )
            }
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
    var showHydrationDashboard by remember { mutableStateOf(false) }
    var showHydrationSettings by remember { mutableStateOf(false) }

    val orchestrator = remember(context) { FamilyCompanionOrchestrator.getInstance(context) }
    val todayIntakeMl by orchestrator.todayTotalMl.collectAsState()
    val progressRatio by orchestrator.progressRatio.collectAsState()
    val hydrationSettings by orchestrator.settings.collectAsState()
    val pendingCompanionPrompt by orchestrator.pendingCompanionPrompt.collectAsState()

    LaunchedEffect(hydrationDashboardEvents) {
        hydrationDashboardEvents?.collect {
            showHydrationDashboard = true
        }
    }

    LaunchedEffect(pendingCompanionPrompt) {
        if (pendingCompanionPrompt) {
            showHydrationReminder = true
            orchestrator.consumeCompanionPrompt()
        }
    }

    // Marvel Hydration Companion check with user-configurable interval (works both locked & unlocked)
    LaunchedEffect(customization.hydrationReminderEnabled, customization.hydrationIntervalMinutes, hydrationSettings.enabled) {
        if (!customization.hydrationReminderEnabled && !hydrationSettings.enabled) return@LaunchedEffect
        while (true) {
            val now = System.currentTimeMillis()
            val intervalMin = if (hydrationSettings.enabled) hydrationSettings.reminderIntervalMinutes else customization.hydrationIntervalMinutes
            val intervalMillis = intervalMin.coerceAtLeast(1) * 60 * 1000L
            if (!showHydrationReminder && now - customization.lastHydrationHour >= intervalMillis) {
                val keyguard = context.getSystemService(android.content.Context.KEYGUARD_SERVICE) as? android.app.KeyguardManager
                val isLocked = keyguard?.isKeyguardLocked == true
                if (isLocked) {
                    // Phone is locked: post high-priority lock-screen public notification immediately
                    val dialogue = orchestrator.getNextDialogue()
                    com.motorola.launcher.system.hydration.HydrationNotificationHelper.showReminderNotification(
                        context = context,
                        dialogue = dialogue,
                        consumedMl = todayIntakeMl,
                        targetMl = hydrationSettings.dailyGoalMl
                    )
                }
                showHydrationReminder = true
            }
            delay(30000L) // Lightweight 30-second ticker, zero battery drain
        }
    }

    val gestureState = rememberDrawerGestureState(wallpaperZoomController)

    // Pillar 1: Gesture-Driven World State & Orientation Sensing
    val gestureWorldState = rememberGestureWorldState(
        currentHero = customization.heroProtocol,
        currentColor = customization.themeColor
    )
    GestureWorldRotationEffect(gestureWorldState)

    // Pillar 2: Autonomous Context Adaptation (Time, Battery, In-Transit)
    val contextualEngine = remember { ContextualLauncherEngine(context) }
    DisposableEffect(contextualEngine) {
        contextualEngine.start()
        onDispose { contextualEngine.stop() }
    }
    val contextSnapshot by contextualEngine.contextState.collectAsState()
    LaunchedEffect(customization.autonomousContextEnabled, contextSnapshot) {
        if (customization.autonomousContextEnabled) {
            val suggestedHero = contextSnapshot.suggestedHero
            if (suggestedHero != customization.heroProtocol) {
                repository.preferences.updateHeroProtocol(suggestedHero)
            }
            val suggestedColor = contextSnapshot.suggestedColor
            if (suggestedColor != customization.themeColor) {
                repository.preferences.updateThemeColor(suggestedColor)
            }
        }
    }

    // Pillar 3: Daily Family Story Mode (Mithra & Mithra Mom story rotation, skipping non-family)
    LaunchedEffect(customization.dailyStoryMode) {
        if (customization.dailyStoryMode) {
            val cal = java.util.Calendar.getInstance()
            val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
            val expectedCompanion = if (hour in 6..16) {
                com.motorola.launcher.ui.model.CompanionStyle.MITHRA
            } else {
                com.motorola.launcher.ui.model.CompanionStyle.MITHRA_MOM
            }
            if (customization.companionStyle != expectedCompanion &&
                customization.companionStyle != com.motorola.launcher.ui.model.CompanionStyle.CUSTOM_SHEET) {
                repository.preferences.updateCompanionStyle(expectedCompanion)
            }
        }
    }

    // Back button closes active sheets/dialogs or app drawer in priority order
    BackHandler(
        enabled = showHydrationSettings ||
                showHydrationDashboard ||
                showHydrationReminder ||
                selectedAppContextApp != null ||
                showQuickAccessCustomizer ||
                showJarvisDashboard ||
                showQuickControls ||
                showCustomization ||
                gestureState.isDrawerVisible
    ) {
        when {
            showHydrationSettings -> showHydrationSettings = false
            showHydrationDashboard -> showHydrationDashboard = false
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
            showHydrationSettings = false
            showHydrationDashboard = false
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

            val workspaceGestureModifier = gestureModifier
                .heroGestureTransform(
                    state = gestureWorldState,
                    enabled = customization.gestureWorldEnabled && !gestureState.isDrawerVisible,
                    onHeroChanged = { newHero ->
                        repository.preferences.updateHeroProtocol(newHero)
                        if (customization.soundEffectsEnabled) {
                            LauncherSoundManager.playSound(
                                context,
                                LauncherSoundManager.SoundEvent.HERO_CYCLE,
                                soundEnabled = true
                            )
                        }
                    }
                )
                .edgeSwipeGesture(
                    state = gestureWorldState,
                    enabled = customization.gestureWorldEnabled && !gestureState.isDrawerVisible,
                    onColorChanged = { newColor ->
                        repository.preferences.updateThemeColor(newColor)
                        if (customization.soundEffectsEnabled) {
                            LauncherSoundManager.playSound(
                                context,
                                LauncherSoundManager.SoundEvent.COLOR_SWIPE,
                                soundEnabled = true
                            )
                        }
                    }
                )

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
                onOpenSystemSettings = {
                    try {
                        val intent = android.content.Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                        }
                        context.startActivity(intent)
                    } catch (_: Throwable) {}
                },
                modifier = workspaceGestureModifier
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

            // Small Animated Water Glass Indicator (Beside Companion on Home Screen)
            if (customization.hydrationReminderEnabled && !gestureState.isDrawerVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 118.dp, end = 16.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    AnimatedWaterGlassIndicator(
                        progressRatio = progressRatio,
                        consumedMl = todayIntakeMl,
                        targetMl = hydrationSettings.dailyGoalMl,
                        accentColor = customization.themeColor.primary,
                        onClick = { showHydrationDashboard = true },
                        onQuickAdd = {
                            coroutineScope.launch {
                                orchestrator.recordWaterIntake(250, "HOME_GLASS")
                            }
                        }
                    )
                }
            }

            // Hydration Dashboard Modal
            if (showHydrationDashboard) {
                HydrationDashboardSheet(
                    orchestrator = orchestrator,
                    onDismiss = { showHydrationDashboard = false },
                    onOpenSettings = {
                        showHydrationDashboard = false
                        showHydrationSettings = true
                    }
                )
            }

            // Hydration Settings Modal
            if (showHydrationSettings) {
                HydrationSettingsDialog(
                    orchestrator = orchestrator,
                    onDismiss = { showHydrationSettings = false }
                )
            }

            // Marvel Character Hourly Hydration Companion
            MarvelHydrationCompanion(
                visible = showHydrationReminder,
                glassesCount = customization.waterGlassesCount,
                onAcceptWater = {
                    coroutineScope.launch {
                        orchestrator.recordWaterIntake(250, "COMPANION_PROMPT")
                    }
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
