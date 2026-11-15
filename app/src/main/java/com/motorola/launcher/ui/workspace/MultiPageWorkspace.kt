package com.motorola.launcher.ui.workspace

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.motorola.launcher.system.SystemTelemetryState
import com.motorola.launcher.ui.model.AppItem
import kotlinx.collections.immutable.ImmutableList

/**
 * Multi-page home screen.
 * Currently wraps the existing WorkspaceScreen on every page.
 * Ready for per-page layouts and drag & drop.
 */
@Composable
fun MultiPageWorkspace(
    pages: List<ImmutableList<AppItem>>,
    dockApps: ImmutableList<AppItem>,
    telemetry: SystemTelemetryState,
    progressProvider: () -> Float,
    onAppClick: (AppItem) -> Unit,
    onAppLongClick: (AppItem) -> Unit,
    onOpenDrawerClick: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onOpenQuickControls: () -> Unit,
    onOpenCustomization: () -> Unit,
    modifier: Modifier = Modifier,
    onPageChanged: (Int) -> Unit = {},
    onReorder: (pageIndex: Int, from: Int, to: Int) -> Unit = { _, _, _ -> }
) {
    val pageCount = pages.size.coerceAtLeast(1)
    val pagerState = rememberPagerState(pageCount = { pageCount })

    LaunchedEffect(pagerState.currentPage) {
        onPageChanged(pagerState.currentPage)
    }

    HorizontalPager(
        state = pagerState,
        modifier = modifier
    ) { page ->
        val pageApps = pages.getOrElse(page) { kotlinx.collections.immutable.persistentListOf() }
        if (page == 0) {
            WorkspaceScreen(
                dockApps = dockApps,
                telemetry = telemetry,
                progressProvider = progressProvider,
                onAppClick = onAppClick,
                onAppLongClick = onAppLongClick,
                onOpenDrawerClick = onOpenDrawerClick,
                onOpenDiagnostics = onOpenDiagnostics,
                onOpenQuickControls = onOpenQuickControls,
                onOpenCustomization = onOpenCustomization
            )
        } else {
            DraggableHomeGrid(
                apps = pageApps,
                onReorder = { from, to -> onReorder(page, from, to) },
                onAppClick = onAppClick,
                onAppLongClick = onAppLongClick
            )
        }
    }
}
