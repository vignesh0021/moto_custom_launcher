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
import com.motorola.launcher.ui.drawer.AppDrawerScreen
import com.motorola.launcher.ui.gesture.drawerSwipeGesture
import com.motorola.launcher.ui.gesture.rememberDrawerGestureState
import com.motorola.launcher.ui.model.AppItem
import com.motorola.launcher.ui.workspace.WorkspaceScreen
import kotlinx.collections.immutable.toImmutableList

/**
 * Root Composable orchestrating Workspace, Drawer, 360Hz gesture physics,
 * and wallpaper zoom pacing.
 */
@Composable
fun LauncherRoot(
    repository: AppRepository,
    wallpaperZoomController: WallpaperZoomController,
    modifier: Modifier = Modifier
) {
    val allApps by repository.appsState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    val searchResults by repository.searchApps(searchQuery).collectAsState(initial = emptyList<AppItem>().toImmutableList())

    val displayedDrawerApps = remember(searchQuery, allApps, searchResults) {
        if (searchQuery.isBlank()) allApps else searchResults
    }

    val gestureState = rememberDrawerGestureState(wallpaperZoomController)

    // Back button closes app drawer before allowing exit
    BackHandler(enabled = gestureState.progress > 0.01f) {
        if (searchQuery.isNotEmpty()) {
            searchQuery = ""
        } else {
            gestureState.close()
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
            dockApps = allApps,
            progressProvider = { gestureState.progress },
            onAppClick = { item -> repository.launchApp(item) },
            onOpenDrawerClick = { gestureState.open() },
            modifier = gestureModifier
        )

        // App Drawer Layer (Active when progress > 0.0f)
        if (gestureState.progress > 0.001f) {
            AppDrawerScreen(
                apps = displayedDrawerApps,
                searchQuery = searchQuery,
                onQueryChange = { searchQuery = it },
                onClearQuery = { searchQuery = "" },
                progressProvider = { gestureState.progress },
                maxDragPx = maxDragPx,
                onAppClick = { item ->
                    repository.launchApp(item)
                    gestureState.close()
                },
                onCloseDrawer = { gestureState.close() },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
