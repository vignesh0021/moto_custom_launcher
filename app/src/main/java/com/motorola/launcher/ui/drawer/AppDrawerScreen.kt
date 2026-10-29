package com.motorola.launcher.ui.drawer

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motorola.launcher.ui.components.FuturisticAppIcon
import com.motorola.launcher.ui.model.AppItem
import com.motorola.launcher.ui.theme.LocalCustomization
import com.motorola.launcher.ui.theme.StarkDarkSurface
import com.motorola.launcher.ui.theme.StarkHudBorder
import com.motorola.launcher.ui.theme.TextPrimary
import com.motorola.launcher.ui.theme.TextSecondary
import androidx.compose.animation.core.FastOutSlowInEasing
import com.motorola.launcher.ui.workspace.StarkWallpaperLayer
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch

private val DRAWER_TABS = listOf("ALL", "FAVORITES", "RECENT")

/**
 * Holographic Application Drawer for STARK OS — JARVIS Edition.
 * Features top APPS header, category tabs (ALL, FAVORITES, RECENT),
 * voice-enabled search bar, 4-column adaptive cybernetic app grid, and edge fast scrubber.
 */
@Composable
fun AppDrawerScreen(
    apps: ImmutableList<AppItem>,
    quickAccessApps: ImmutableList<AppItem>,
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    progressProvider: () -> Float,
    maxDragPx: Float,
    onAppClick: (AppItem) -> Unit,
    onAppLongClick: (AppItem) -> Unit,
    onCustomizeQuickAccessClick: () -> Unit,
    onCloseDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val customization = LocalCustomization.current
    val marvelTheme = com.motorola.launcher.ui.theme.LocalMarvelTheme.current
    val themeColor = customization.themeColor
    val view = LocalView.current
    var selectedTab by remember { mutableStateOf("ALL") }

    // Filter apps based on active tab
    val displayedApps = remember(apps, selectedTab, searchQuery, quickAccessApps) {
        if (searchQuery.isNotEmpty()) {
            apps
        } else {
            when (selectedTab) {
                "FAVORITES" -> quickAccessApps.ifEmpty { apps.take(8).toImmutableList() }
                "RECENT" -> apps.take(12).toImmutableList()
                else -> apps
            }
        }
    }

    val nestedScrollConnection = remember(onCloseDrawer) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (available.y > 22f && source == NestedScrollSource.UserInput) {
                    onCloseDrawer()
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                val rawP = progressProvider().coerceIn(0f, 1f)
                // Marvel smooth cinematic easing (0% idle battery drain when resting)
                val smoothP = FastOutSlowInEasing.transform(rawP)
                translationY = maxDragPx * (1.0f - smoothP)
                val scale = 0.94f + (0.06f * smoothP)
                scaleX = scale
                scaleY = scale
                alpha = smoothP
            }
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Marvel-Themed Wallpaper Layer for All Apps Screen
        StarkWallpaperLayer(
            preset = customization.wallpaper,
            accent = marvelTheme.colors.primary,
            modifier = Modifier.fillMaxSize()
        )
        // High-legibility frosted HUD glass scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xB3030710))
        )

        // Motorola Curved Waterfall Edge Ambient Glow
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight(0.85f)
                .width(3.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(marvelTheme.colors.glow, Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight(0.85f)
                .width(3.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, marvelTheme.colors.glow)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .nestedScroll(nestedScrollConnection)
        ) {
            // Pull Down Dismiss Handle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { _, dragAmount ->
                            if (dragAmount > 15f) onCloseDrawer()
                        }
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onCloseDrawer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(marvelTheme.colors.primary.copy(alpha = 0.5f))
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Category Tabs: ALL | FAVORITES | RECENT
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(marvelTheme.colors.subtle)
                    .border(1.dp, marvelTheme.colors.hudBorder, RoundedCornerShape(20.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (tab in DRAWER_TABS) {
                    val isSelected = tab == selectedTab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) marvelTheme.drawerStyle.activeTabIndicator else Color.Transparent)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    com.motorola.launcher.system.LauncherFeedback.performFeedback(
                                        view = view,
                                        hapticEnabled = customization.hapticFeedback,
                                        soundEnabled = customization.soundEffectsEnabled
                                    )
                                    selectedTab = tab
                                    scope.launch { gridState.scrollToItem(0) }
                                }
                            )
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab,
                            color = if (isSelected) Color.Black else TextSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            letterSpacing = 1.0.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Holographic Search Bar
            SearchBarOverlay(
                query = searchQuery,
                onQueryChange = onQueryChange,
                onClearQuery = onClearQuery,
                appCount = apps.size,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 4-Column Adaptive App Grid with Fast Scrubber
            Box(modifier = Modifier.fillMaxSize()) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 4.dp,
                        end = 28.dp, // Space for right-side A-Z scrubber
                        top = 4.dp,
                        bottom = 32.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = displayedApps,
                        key = { it.componentKey },
                        contentType = { "app_icon" }
                    ) { appItem ->
                        FuturisticAppIcon(
                            app = appItem,
                            onClick = { onAppClick(appItem) },
                            onLongClick = { onAppLongClick(appItem) },
                            iconSize = 48.dp,
                            showLabel = true
                        )
                    }
                }

                // Vertical A-Z Fast Scrubber
                if (searchQuery.isEmpty() && selectedTab == "ALL" && displayedApps.isNotEmpty()) {
                    FastScrollScrubber(
                        onLetterSelected = { targetChar ->
                            val targetIndex = displayedApps.indexOfFirst {
                                it.label.startsWith(targetChar, ignoreCase = true)
                            }
                            if (targetIndex >= 0) {
                                scope.launch { gridState.scrollToItem(targetIndex) }
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(bottom = 44.dp)
                    )
                }

                // Bottom Center Back To Workspace Pill
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 6.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(marvelTheme.colors.surface)
                        .border(1.dp, marvelTheme.colors.hudBorderBright, RoundedCornerShape(16.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                com.motorola.launcher.system.LauncherFeedback.performFeedback(
                                    view = view,
                                    hapticEnabled = customization.hapticFeedback,
                                    soundEnabled = customization.soundEffectsEnabled
                                )
                                onCloseDrawer()
                            }
                        )
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "▼ BACK TO WORKSPACE",
                        color = marvelTheme.colors.bright,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                }
            }
        }
    }
}
