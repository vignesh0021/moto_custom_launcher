package com.motorola.launcher.ui.drawer

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motorola.launcher.ui.model.AppItem
import com.motorola.launcher.ui.theme.MotoCyanAccent
import com.motorola.launcher.ui.theme.MotoDarkSurface
import com.motorola.launcher.ui.theme.MotoElectricViolet
import com.motorola.launcher.ui.theme.TextPrimary
import com.motorola.launcher.ui.theme.TextSecondary
import com.motorola.launcher.ui.theme.TextTertiary
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch

private val CATEGORIES = listOf("All", "Favorites", "Communication", "Media", "Tools")

/**
 * High-framerate App Drawer composable.
 * Features unblocked 144Hz scroll, customizable Quick Launch shelf, category filter chips,
 * and holographic A-Z edge fast scrubber with long-press App Info support.
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
    var selectedCategory by remember { mutableStateOf("All") }

    // Filter apps based on category if search query is empty
    val categorizedApps = remember(apps, selectedCategory, searchQuery, quickAccessApps) {
        if (searchQuery.isNotEmpty()) {
            apps
        } else {
            when (selectedCategory) {
                "Favorites" -> quickAccessApps.ifEmpty { apps.take(8).toImmutableList() }
                "Communication" -> apps.filter { app ->
                    val k = (app.packageName + " " + app.label).lowercase()
                    k.contains("phone") || k.contains("dialer") || k.contains("message") ||
                    k.contains("contact") || k.contains("chat") || k.contains("mail") ||
                    k.contains("whatsapp") || k.contains("telegram")
                }.ifEmpty { apps }.toImmutableList()
                "Media" -> apps.filter { app ->
                    val k = (app.packageName + " " + app.label).lowercase()
                    k.contains("camera") || k.contains("photo") || k.contains("gallery") ||
                    k.contains("music") || k.contains("video") || k.contains("youtube") ||
                    k.contains("spotify") || k.contains("audio") || k.contains("sound")
                }.ifEmpty { apps }.toImmutableList()
                "Tools" -> apps.filter { app ->
                    val k = (app.packageName + " " + app.label).lowercase()
                    k.contains("calc") || k.contains("clock") || k.contains("file") ||
                    k.contains("drive") || k.contains("setting") || k.contains("browser") ||
                    k.contains("chrome") || k.contains("map") || k.contains("calendar")
                }.ifEmpty { apps }.toImmutableList()
                else -> apps
            }
        }
    }

    // NestedScroll connection to detect pull-down dismiss when at top of app grid
    val nestedScrollConnection = remember(onCloseDrawer) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // Downward overscroll when at top of grid triggers drawer close
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
                val p = progressProvider()
                translationY = maxDragPx * (1.0f - p)
                alpha = p.coerceIn(0.0f, 1.0f)
            }
            .background(MotoDarkSurface.copy(alpha = 0.96f))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Motorola Curved Waterfall Edge Ambient Glow (Left)
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight(0.85f)
                .width(3.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(MotoCyanAccent.copy(alpha = 0.4f), Color.Transparent)
                    )
                )
        )

        // Motorola Curved Waterfall Edge Ambient Glow (Right)
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight(0.85f)
                .width(3.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, MotoElectricViolet.copy(alpha = 0.4f))
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp)
                .nestedScroll(nestedScrollConnection)
        ) {
            // Sleek Top Pull Handle (Swipe down or tap to dismiss)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { _, dragAmount ->
                            if (dragAmount > 15f) {
                                onCloseDrawer()
                            }
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
                        .background(MotoCyanAccent.copy(alpha = 0.45f))
                )
            }

            // Cyber Frosted Glass Search Bar
            SearchBarOverlay(
                query = searchQuery,
                onQueryChange = onQueryChange,
                onClearQuery = onClearQuery,
                appCount = apps.size,
                modifier = Modifier.fillMaxWidth()
            )

            // Category Filter Chips (visible when not searching)
            if (searchQuery.isEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (category in CATEGORIES) {
                        val isSelected = category == selectedCategory
                        CategoryChip(
                            label = category,
                            isSelected = isSelected,
                            onClick = {
                                selectedCategory = category
                                scope.launch { gridState.scrollToItem(0) }
                            }
                        )
                    }
                }
            }

            // Quick Access / Favorite Apps Shelf (visible when search is empty and category is "All")
            if (searchQuery.isEmpty() && selectedCategory == "All") {
                Spacer(modifier = Modifier.height(14.dp))
                QuickLaunchShelf(
                    apps = quickAccessApps,
                    onAppClick = onAppClick,
                    onAppLongClick = onAppLongClick,
                    onCustomizeClick = onCustomizeQuickAccessClick
                )
                Spacer(modifier = Modifier.height(10.dp))
            } else {
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Unblocked 144Hz Adaptive App Grid with Alphabetical Fast Scrubber
            Box(modifier = Modifier.fillMaxSize()) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 76.dp),
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 4.dp,
                        end = 32.dp, // Space for right-edge fast scrubber
                        top = 4.dp,
                        bottom = 36.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = categorizedApps,
                        key = { it.componentKey },
                        contentType = { "app_icon" }
                    ) { appItem ->
                        DrawerAppItem(
                            app = appItem,
                            onClick = { onAppClick(appItem) },
                            onLongClick = { onAppLongClick(appItem) }
                        )
                    }
                }

                // Vertical A-Z Fast Scrubber aligned to right waterfall edge
                if (searchQuery.isEmpty() && selectedCategory == "All" && categorizedApps.isNotEmpty()) {
                    FastScrollScrubber(
                        onLetterSelected = { targetChar ->
                            val targetIndex = categorizedApps.indexOfFirst {
                                it.label.startsWith(targetChar, ignoreCase = true)
                            }
                            if (targetIndex >= 0) {
                                scope.launch {
                                    gridState.scrollToItem(targetIndex)
                                }
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(bottom = 24.dp)
                    )
                }
            }
        }
    }
}

/**
 * Category Filter Pill Chip with subtle neon border.
 */
@Composable
private fun CategoryChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val view = LocalView.current
    val bgColor = if (isSelected) MotoCyanAccent.copy(alpha = 0.2f) else Color(0x14FFFFFF)
    val borderColor = if (isSelected) MotoCyanAccent else Color(0x22FFFFFF)
    val textColor = if (isSelected) MotoCyanAccent else TextSecondary

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onClick()
                }
            )
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

/**
 * Luminous Quick Launch shelf with customize button and long-press support.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QuickLaunchShelf(
    apps: List<AppItem>,
    onAppClick: (AppItem) -> Unit,
    onAppLongClick: (AppItem) -> Unit,
    onCustomizeClick: () -> Unit
) {
    val view = LocalView.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x14FFFFFF))
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        // Shelf Header: Title + Customize Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 2.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "QUICK ACCESS",
                color = MotoCyanAccent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )

            // Customize Quick Access Pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x1A00E5FF))
                    .border(1.dp, MotoCyanAccent.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .clickable {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onCustomizeClick()
                    }
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚙️ CUSTOMIZE",
                    color = MotoCyanAccent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (apps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCustomizeClick() }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+ Tap Customize to select Quick Access apps",
                    color = TextTertiary,
                    fontSize = 12.sp
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (app in apps.take(5)) {
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val scale by animateFloatAsState(
                        targetValue = if (isPressed) 0.88f else 1.0f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "quickAppScale"
                    )

                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .clip(CircleShape)
                            .border(
                                width = 1.5.dp,
                                brush = Brush.sweepGradient(
                                    listOf(MotoCyanAccent, MotoElectricViolet, MotoCyanAccent)
                                ),
                                shape = CircleShape
                            )
                            .combinedClickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    onAppClick(app)
                                },
                                onLongClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                    onAppLongClick(app)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (app.iconBitmap != null) {
                            Image(
                                bitmap = app.iconBitmap,
                                contentDescription = app.label,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Text(
                                text = app.label.take(1).uppercase(),
                                color = MotoCyanAccent,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tactile App Item with spring squeeze bounce and haptic click & long-press support.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DrawerAppItem(
    app: AppItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "drawerItemScale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onClick()
                },
                onLongClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    onLongClick()
                }
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Icon with Hardware Bitmap or Cyber Fallback
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (app.iconBitmap != null) {
                Image(
                    bitmap = app.iconBitmap,
                    contentDescription = app.label,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(Color(0x4D00E5FF), Color(0x1A7B2CBF))
                            )
                        )
                        .border(1.dp, MotoCyanAccent.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = app.label.take(1).uppercase(),
                        color = MotoCyanAccent,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = app.label,
            color = TextSecondary,
            fontSize = 11.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
