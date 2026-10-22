package com.motorola.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import com.motorola.launcher.data.repository.AppRepository
import com.motorola.launcher.system.WallpaperZoomController
import com.motorola.launcher.ui.dialog.AppContextDialog
import com.motorola.launcher.ui.dialog.QuickAccessCustomizerDialog
import com.motorola.launcher.ui.drawer.AppDrawerScreen
import com.motorola.launcher.ui.gesture.drawerSwipeGesture
import com.motorola.launcher.ui.gesture.rememberDrawerGestureState
import com.motorola.launcher.ui.model.AppItem
import com.motorola.launcher.ui.workspace.WorkspaceScreen
import kotlinx.collections.immutable.toImmutableList

/**
 * Root Composable orchestrating Workspace, Drawer, App Context Menus,
 * Quick Access Customizer, and gesture physics.
 */
@Composable
fun LauncherRoot(
    repository: AppRepository,
    wallpaperZoomController: WallpaperZoomController,
    modifier: Modifier = Modifier
) {
    val allApps by repository.appsState.collectAsState()
    val quickAccessKeys by repository.preferences.quickAccessKeys.collectAsState()
    val dockKeys by repository.preferences.dockKeys.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    val searchResults by remember(searchQuery) { repository.searchApps(searchQuery) }.collectAsState(initial = emptyList<AppItem>().toImmutableList())

    val displayedDrawerApps = remember(searchQuery, allApps, searchResults) {
        if (searchQuery.isBlank()) allApps else searchResults
    }

    // Resolve customized Dock apps
    val dockApps = remember(allApps, dockKeys) {
        if (dockKeys.isEmpty()) {
            allApps.take(5).toImmutableList()
        } else {
            val appMap = allApps.associateBy { it.componentKey }
            dockKeys.mapNotNull { appMap[it] }.ifEmpty { allApps.take(5) }.toImmutableList()
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

    // State for App Context Dialog (Long press menu: App Info, Quick Access, Dock, Uninstall)
    var selectedAppContextApp by remember { mutableStateOf<AppItem?>(null) }

    // State for Quick Access Customizer sheet
    var showQuickAccessCustomizer by remember { mutableStateOf(false) }

    val gestureState = rememberDrawerGestureState(wallpaperZoomController)

    // Back button closes dialogs or app drawer before allowing exit
    BackHandler(enabled = selectedAppContextApp != null || showQuickAccessCustomizer || gestureState.isDrawerVisible) {
        when {
            selectedAppContextApp != null -> selectedAppContextApp = null
            showQuickAccessCustomizer -> showQuickAccessCustomizer = false
            searchQuery.isNotEmpty() -> searchQuery = ""
            else -> gestureState.close()
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val maxDragPx = with(density) { maxHeight.toPx() }

        val gestureModifier = Modifier
            .fillMaxSize()
            .drawerSwipeGesture(gestureState, maxDragPx)

        // Workspace Layer
        WorkspaceScreen(
            dockApps = dockApps,
            progressProvider = { gestureState.progress },
            onAppClick = { item -> repository.launchApp(item) },
            onAppLongClick = { item -> selectedAppContextApp = item },
            onOpenDrawerClick = { gestureState.open() },
            modifier = gestureModifier
        )

        // App Drawer Layer (Active when drawer is visible or transitioning)
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
                    repository.launchApp(item)
                    gestureState.close()
                },
                onAppLongClick = { item -> selectedAppContextApp = item },
                onCustomizeQuickAccessClick = { showQuickAccessCustomizer = true },
                onCloseDrawer = { gestureState.close() },
                modifier = Modifier.fillMaxSize()
            )
        }

        // App Context Dialog (App Info, Quick Access, Dock, Uninstall)
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
    }
}
