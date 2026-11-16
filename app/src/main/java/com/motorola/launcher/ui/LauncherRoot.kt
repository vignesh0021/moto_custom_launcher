package com.motorola.launcher.ui

import android.appwidget.AppWidgetHost
import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import com.motorola.launcher.data.db.AppFolderEntity
import com.motorola.launcher.data.db.WorkspaceItemEntity
import com.motorola.launcher.data.repository.AppRepository
import com.motorola.launcher.data.repository.WorkspaceRepository
import com.motorola.launcher.system.ContextualLauncherEngine
import com.motorola.launcher.system.LauncherAccessibilityService
import com.motorola.launcher.system.LauncherSoundManager
import com.motorola.launcher.system.SystemTelemetryManager
import com.motorola.launcher.system.WallpaperZoomController
import com.motorola.launcher.system.companion.FamilyCompanionOrchestrator
import com.motorola.launcher.ui.components.AnimatedWaterGlassIndicator
import com.motorola.launcher.ui.components.MarvelHydrationCompanion
import com.motorola.launcher.ui.dialog.AppContextDialog
import com.motorola.launcher.ui.dialog.AppShortcutPickerDialog
import com.motorola.launcher.ui.dialog.CreateFolderDialog
import com.motorola.launcher.ui.dialog.FolderDialog
import com.motorola.launcher.ui.dialog.HydrationDashboardSheet
import com.motorola.launcher.ui.dialog.HydrationSettingsDialog
import com.motorola.launcher.ui.dialog.JarvisDashboardSheet
import com.motorola.launcher.ui.dialog.QuickAccessCustomizerDialog
import com.motorola.launcher.ui.dialog.QuickControlsSheet
import com.motorola.launcher.system.notification.NotificationBadgeManager
import com.motorola.launcher.ui.dialog.IconPackPickerDialog
import com.motorola.launcher.ui.dialog.SettingsCustomizationDialog
import com.motorola.launcher.ui.dialog.WorkspaceCustomizeSheet
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
 * 1. Home Workspace with procedural Arc Reactor and multi-page widget grid
 * 2. Adaptive Holographic App Drawer with categories, folders & search
 * 3. System Dashboard with real device measurements
 * 4. Radial Quick Controls Menu & Toggles
 * 5. Settings & Customization (Theme, Wallpapers, Icons, Animations, Toggles)
 */
@Composable
fun LauncherRoot(
    repository: AppRepository,
    workspaceRepository: WorkspaceRepository? = null,
    appWidgetHost: AppWidgetHost? = null,
    onRequestAddNativeWidget: ((page: Int, cellX: Int, cellY: Int) -> Unit)? = null,
    wallpaperZoomController: WallpaperZoomController,
    telemetryManager: SystemTelemetryManager,
    homePressEvents: kotlinx.coroutines.flow.SharedFlow<Unit>? = null,
    hydrationDashboardEvents: kotlinx.coroutines.flow.SharedFlow<Unit>? = null,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val allApps by repository.appsState.collectAsState()
    val quickAccessKeys by repository.preferences.quickAccessKeys.collectAsState()
    val dockKeys by repository.preferences.dockKeys.collectAsState()
    val customization by repository.preferences.customization.collectAsState()
    val telemetry by telemetryManager.telemetryState.collectAsState()
    val notificationCounts by NotificationBadgeManager.badgeCounts.collectAsState()

    // Workspace and Folders repository
    val workspaceRepo = remember(context) { workspaceRepository ?: WorkspaceRepository(context) }
    val workspaceItems by workspaceRepo.workspaceItems.collectAsState()
    val folders by workspaceRepo.appFolders.collectAsState()

    // Seed defaults if empty
    LaunchedEffect(allApps) {
        if (allApps.isNotEmpty()) {
            workspaceRepo.seedDefaultsIfEmpty(allApps)
        }
    }

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

    var launchingApp by remember { mutableStateOf<AppItem?>(null) }
    val launchAnimationProgress = remember { Animatable(0f) }
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

    // Workspace & Folder Dialog States
    var showCustomizeSheet by remember { mutableStateOf(false) }
    var customizeTargetPage by remember { mutableStateOf(0) }
    var customizeTargetCellX by remember { mutableStateOf(0) }
    var customizeTargetCellY by remember { mutableStateOf(0) }
    var selectedWorkspaceItem by remember { mutableStateOf<WorkspaceItemEntity?>(null) }
    var showFolderDialog by remember { mutableStateOf<AppFolderEntity?>(null) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showShortcutPicker by remember { mutableStateOf(false) }
    var showIconPackDialog by remember { mutableStateOf(false) }

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

    // Initialize hydration timestamp on first launch so it never triggers immediately
    LaunchedEffect(Unit) {
        if (customization.lastHydrationHour == 0L) {
            repository.preferences.recordWaterRefused()
        }
    }

    val gestureState = rememberDrawerGestureState(wallpaperZoomController)

    // Pillar 1: Gesture-Driven World State & Orientation Sensing
    val gestureWorldState = rememberGestureWorldState(
        currentHero = customization.heroProtocol,
        currentColor = customization.themeColor
    )
    GestureWorldRotationEffect(gestureWorldState)

    // Pillar 2: Autonomous Context Adaptation
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

    // Pillar 3: Daily Family Story Mode
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
        enabled = showIconPackDialog ||
                showHydrationSettings ||
                showHydrationDashboard ||
                showHydrationReminder ||
                selectedAppContextApp != null ||
                showQuickAccessCustomizer ||
                showJarvisDashboard ||
                showQuickControls ||
                showCustomization ||
                showCustomizeSheet ||
                showFolderDialog != null ||
                showCreateFolderDialog ||
                showShortcutPicker ||
                gestureState.isDrawerVisible
    ) {
        when {
            showIconPackDialog -> showIconPackDialog = false
            showHydrationSettings -> showHydrationSettings = false
            showHydrationDashboard -> showHydrationDashboard = false
            showHydrationReminder -> showHydrationReminder = false
            selectedAppContextApp != null -> selectedAppContextApp = null
            showQuickAccessCustomizer -> showQuickAccessCustomizer = false
            showJarvisDashboard -> showJarvisDashboard = false
            showQuickControls -> showQuickControls = false
            showCustomization -> showCustomization = false
            showCustomizeSheet -> showCustomizeSheet = false
            showFolderDialog != null -> showFolderDialog = null
            showCreateFolderDialog -> showCreateFolderDialog = false
            showShortcutPicker -> showShortcutPicker = false
            searchQuery.isNotEmpty() -> searchQuery = ""
            else -> gestureState.close()
        }
    }

    // Reset back to main workspace when system Home button is pressed
    LaunchedEffect(homePressEvents) {
        homePressEvents?.collect {
            showIconPackDialog = false
            showHydrationSettings = false
            showHydrationDashboard = false
            showHydrationReminder = false
            selectedAppContextApp = null
            showQuickAccessCustomizer = false
            showJarvisDashboard = false
            showQuickControls = false
            showCustomization = false
            showCustomizeSheet = false
            showFolderDialog = null
            showCreateFolderDialog = false
            showShortcutPicker = false
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
                .drawerSwipeGesture(
                    state = gestureState,
                    maxDragPx = maxDragPx,
                    onSwipeDown = {
                        LauncherAccessibilityService.expandNotifications(context)
                    }
                )

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

            // Background: selected HUD wallpaper
            StarkWallpaperLayer(
                preset = customization.wallpaper,
                accent = customization.themeColor.primary,
                modifier = Modifier.fillMaxSize()
            )

            // Screen 1: Multi-Page Workspace Layer
            WorkspaceScreen(
                dockApps = dockApps,
                workspaceItems = workspaceItems,
                allApps = allApps,
                folders = folders,
                telemetry = telemetry,
                appWidgetHost = appWidgetHost,
                notificationCounts = notificationCounts,
                progressProvider = { gestureState.progress },
                onAppClick = { item -> launchAppWithAnimation(item) },
                onAppLongClick = { item -> selectedAppContextApp = item },
                onFolderClick = { folder -> showFolderDialog = folder },
                onItemLongClick = { item ->
                    selectedWorkspaceItem = item
                    customizeTargetPage = item.pageIndex
                    customizeTargetCellX = item.cellX
                    customizeTargetCellY = item.cellY
                    showCustomizeSheet = true
                },
                onEmptySpaceLongClick = { page, cx, cy ->
                    selectedWorkspaceItem = null
                    customizeTargetPage = page
                    customizeTargetCellX = cx
                    customizeTargetCellY = cy
                    showCustomizeSheet = true
                },
                onEmptySpaceDoubleTap = {
                    LauncherAccessibilityService.lockScreen(context)
                },
                onOpenDrawerClick = { gestureState.open() },
                onOpenDiagnostics = { showJarvisDashboard = true },
                onOpenQuickControls = { showQuickControls = true },
                onOpenCustomization = { showCustomization = true },
                onOpenSystemSettings = {
                    try {
                        val intent = Intent(Settings.ACTION_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                        }
                        context.startActivity(intent)
                    } catch (_: Throwable) {}
                },
                modifier = workspaceGestureModifier
            )

            // Screen 2: App Drawer Layer
            if (gestureState.isDrawerVisible) {
                AppDrawerScreen(
                    apps = displayedDrawerApps,
                    quickAccessApps = quickAccessApps,
                    folders = folders,
                    notificationCounts = notificationCounts,
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
                    onFolderClick = { folder -> showFolderDialog = folder },
                    onCreateFolderClick = { showCreateFolderDialog = true },
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

            // Screen 4: Quick Controls Radial HUD
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
                    onOpenIconPacks = { showIconPackDialog = true },
                    onDismiss = { showCustomization = false },
                    onTriggerHydrationTest = {
                        showCustomization = false
                        showHydrationReminder = true
                    }
                )
            }

            // Workspace Customize Sheet (Add widgets, app shortcuts, remove items)
            if (showCustomizeSheet) {
                WorkspaceCustomizeSheet(
                    targetPage = customizeTargetPage,
                    targetCellX = customizeTargetCellX,
                    targetCellY = customizeTargetCellY,
                    selectedItem = selectedWorkspaceItem,
                    onAddNativeWidget = { page, cx, cy ->
                        onRequestAddNativeWidget?.invoke(page, cx, cy)
                    },
                    onAddBuiltinWidget = { page, cx, cy, type, title ->
                        coroutineScope.launch {
                            workspaceRepo.addWorkspaceItem(
                                pageIndex = page,
                                cellX = cx,
                                cellY = cy,
                                spanX = 5,
                                spanY = 2,
                                itemType = WorkspaceItemEntity.TYPE_WIDGET_BUILTIN,
                                extraData = type,
                                customTitle = title
                            )
                        }
                    },
                    onAddAppShortcut = { page, cx, cy ->
                        customizeTargetPage = page
                        customizeTargetCellX = cx
                        customizeTargetCellY = cy
                        showShortcutPicker = true
                    },
                    onRemoveItem = { id ->
                        coroutineScope.launch {
                            workspaceRepo.removeWorkspaceItem(id)
                        }
                    },
                    onOpenIconPacks = {
                        showCustomizeSheet = false
                        showIconPackDialog = true
                    },
                    onOpenNotificationSettings = {
                        NotificationBadgeManager.openNotificationAccessSettings(context)
                    },
                    onDismiss = { showCustomizeSheet = false }
                )
            }

            // Icon Pack Picker Dialog
            if (showIconPackDialog) {
                val iconPacks = remember { repository.iconPackManager.getInstalledIconPacks() }
                IconPackPickerDialog(
                    iconPacks = iconPacks,
                    selectedPackage = customization.iconPackPackage,
                    onSelectIconPack = { pkg ->
                        repository.applyIconPack(pkg)
                        showIconPackDialog = false
                    },
                    onDismiss = { showIconPackDialog = false }
                )
            }

            // Folder Viewer Dialog
            showFolderDialog?.let { folder ->
                FolderDialog(
                    folder = folder,
                    allApps = allApps,
                    onAppClick = { item ->
                        launchAppWithAnimation(item)
                        showFolderDialog = null
                    },
                    onAppLongClick = { item ->
                        selectedAppContextApp = item
                    },
                    onDeleteFolder = {
                        coroutineScope.launch {
                            workspaceRepo.deleteFolder(folder.id)
                        }
                    },
                    onDismiss = { showFolderDialog = null }
                )
            }

            // Create Folder Dialog
            if (showCreateFolderDialog) {
                CreateFolderDialog(
                    onCreateFolder = { name ->
                        coroutineScope.launch {
                            workspaceRepo.createFolder(name)
                        }
                    },
                    onDismiss = { showCreateFolderDialog = false }
                )
            }

            // App Shortcut Picker Dialog
            if (showShortcutPicker) {
                AppShortcutPickerDialog(
                    apps = allApps.filter { !it.isHidden },
                    onSelectApp = { app ->
                        coroutineScope.launch {
                            workspaceRepo.addWorkspaceItem(
                                pageIndex = customizeTargetPage,
                                cellX = customizeTargetCellX,
                                cellY = customizeTargetCellY,
                                spanX = 1,
                                spanY = 1,
                                itemType = WorkspaceItemEntity.TYPE_APP,
                                componentKey = app.componentKey,
                                customTitle = app.label
                            )
                        }
                    },
                    onDismiss = { showShortcutPicker = false }
                )
            }

            // Small Animated Water Glass Indicator
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
                    showHydrationReminder = false
                },
                onRefuseWater = {
                    orchestrator.snoozeReminder()
                    repository.preferences.recordWaterRefused()
                    showHydrationReminder = false
                },
                onDismiss = {
                    repository.preferences.recordWaterRefused()
                    showHydrationReminder = false
                }
            )

            // App Context Menu (App Info, Quick Access, Dock, Hide, Pin to Workspace, Uninstall)
            selectedAppContextApp?.let { app ->
                AppContextDialog(
                    app = app,
                    isQuickAccess = repository.preferences.isQuickAccess(app.componentKey),
                    isDockPinned = repository.preferences.isDockPinned(app.componentKey),
                    isHidden = repository.preferences.isHiddenApp(app.componentKey),
                    onOpenAppInfo = { repository.openAppInfo(app) },
                    onToggleQuickAccess = { repository.preferences.toggleQuickAccess(app.componentKey) },
                    onToggleDock = { repository.preferences.toggleDock(app.componentKey) },
                    onToggleHideApp = {
                        repository.preferences.toggleHideApp(app.componentKey)
                        repository.scheduleReload(0)
                    },
                    onPinToWorkspace = {
                        coroutineScope.launch {
                            workspaceRepo.addWorkspaceItem(
                                pageIndex = 0,
                                cellX = 0,
                                cellY = 4,
                                spanX = 1,
                                spanY = 1,
                                itemType = WorkspaceItemEntity.TYPE_APP,
                                componentKey = app.componentKey,
                                customTitle = app.label
                            )
                        }
                    },
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
            if (launchAnimationProgress.value > 0f) {
                val p = launchAnimationProgress.value
                val primaryColor = customization.heroProtocol.primary
                val secondaryColor = customization.heroProtocol.secondary

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val maxRadius = kotlin.math.max(size.width, size.height)

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
