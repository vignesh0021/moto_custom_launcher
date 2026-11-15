package com.motorola.launcher.ui.workspace

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.motorola.launcher.ui.components.FuturisticAppIcon
import com.motorola.launcher.ui.model.AppItem
import kotlinx.collections.immutable.ImmutableList

/**
 * Home screen grid with long-press drag & drop reordering.
 * Updates the page order on drop.
 */
@Composable
fun DraggableHomeGrid(
    apps: ImmutableList<AppItem>,
    onReorder: (fromIndex: Int, toIndex: Int) -> Unit,
    onAppClick: (AppItem) -> Unit,
    onAppLongClick: (AppItem) -> Unit,
    modifier: Modifier = Modifier,
    badgeCounts: Map<String, Int> = emptyMap()
) {
    val gridState = rememberLazyGridState()
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        state = gridState,
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxSize()
    ) {
        itemsIndexed(apps, key = { _, item -> item.componentKey }) { index, app ->
            val isDragging = draggingIndex == index
            Box(
                modifier = Modifier
                    .zIndex(if (isDragging) 1f else 0f)
                    .graphicsLayer {
                        if (isDragging) {
                            translationX = dragOffset.x
                            translationY = dragOffset.y
                            scaleX = 1.1f
                            scaleY = 1.1f
                            alpha = 0.9f
                        }
                    }
                    .pointerInput(app.componentKey) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                draggingIndex = index
                                dragOffset = androidx.compose.ui.geometry.Offset.Zero
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragOffset += dragAmount
                            },
                            onDragEnd = {
                                val from = draggingIndex
                                if (from != null) {
                                    // Simple nearest-slot drop (can be refined with layout coordinates)
                                    val to = (from + (dragOffset.x / 80f).toInt()).coerceIn(0, apps.lastIndex)
                                    if (to != from) onReorder(from, to)
                                }
                                draggingIndex = null
                                dragOffset = androidx.compose.ui.geometry.Offset.Zero
                            },
                            onDragCancel = {
                                draggingIndex = null
                                dragOffset = androidx.compose.ui.geometry.Offset.Zero
                            }
                        )
                    }
            ) {
                FuturisticAppIcon(
                    app = app,
                    badgeCount = badgeCounts[app.packageName] ?: 0,
                    onClick = { onAppClick(app) },
                    onLongClick = { onAppLongClick(app) }
                )
            }
        }
    }
}
