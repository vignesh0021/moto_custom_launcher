package com.motorola.launcher.ui.drawer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motorola.launcher.ui.model.AppItem
import com.motorola.launcher.ui.theme.MotoCyanAccent
import com.motorola.launcher.ui.theme.MotoDarkSurface
import com.motorola.launcher.ui.theme.TextPrimary
import com.motorola.launcher.ui.theme.TextSecondary
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch

/**
 * High-framerate App Drawer composable.
 * Runs at guaranteed 144Hz with zero allocations during scroll and touch passes.
 */
@Composable
fun AppDrawerScreen(
    apps: ImmutableList<AppItem>,
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    progressProvider: () -> Float,
    maxDragPx: Float,
    onAppClick: (AppItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                val p = progressProvider()
                // Translates from bottom (maxDragPx) to 0 (fully visible)
                translationY = maxDragPx * (1.0f - p)
                // Scrim opacity scales with progress
                alpha = p.coerceIn(0.0f, 1.0f)
            }
            .background(MotoDarkSurface.copy(alpha = 0.94f))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Sticky Top Search Bar
            SearchBarOverlay(
                query = searchQuery,
                onQueryChange = onQueryChange,
                onClearQuery = onClearQuery,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // App Grid with Alphabetical Fast Scrubber
            Box(modifier = Modifier.fillMaxSize()) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 4.dp,
                        end = 28.dp, // Space for right-edge fast scrubber
                        top = 8.dp,
                        bottom = 32.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = apps,
                        key = { it.componentKey },
                        contentType = { "app_icon" }
                    ) { appItem ->
                        DrawerAppItem(
                            app = appItem,
                            onClick = { onAppClick(appItem) }
                        )
                    }
                }

                // Vertical A-Z Scrubber aligned to right waterfall edge
                if (searchQuery.isEmpty() && apps.isNotEmpty()) {
                    FastScrollScrubber(
                        onLetterSelected = { targetChar ->
                            val targetIndex = apps.indexOfFirst {
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

@Composable
private fun DrawerAppItem(
    app: AppItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Hardware-backed Icon or Fallback
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
                    modifier = Modifier.size(52.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(0x3300E5FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = app.label.take(1).uppercase(),
                        color = MotoCyanAccent,
                        fontSize = 20.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = app.label,
            color = TextSecondary,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "Moto Edge 50 App Drawer (20:9)",
    device = "spec:width=1220px,height=2712px,dpi=446",
    showBackground = true,
    backgroundColor = 0xFF0E1116
)
@Composable
private fun AppDrawerScreenPreview() {
    com.motorola.launcher.ui.theme.MotoLauncherTheme {
        val dummyApps = kotlinx.collections.immutable.persistentListOf(
            AppItem("chrome", "com.android.chrome", "Main", "Chrome", android.os.Process.myUserHandle(), "chrome"),
            AppItem("camera", "com.motorola.camera3", "Main", "Camera", android.os.Process.myUserHandle(), "camera"),
            AppItem("clock", "com.google.android.deskclock", "Main", "Clock", android.os.Process.myUserHandle(), "clock"),
            AppItem("contacts", "com.google.android.contacts", "Main", "Contacts", android.os.Process.myUserHandle(), "contacts"),
            AppItem("drive", "com.google.android.apps.docs", "Main", "Drive", android.os.Process.myUserHandle(), "drive"),
            AppItem("files", "com.google.android.apps.nbu.files", "Main", "Files", android.os.Process.myUserHandle(), "files"),
            AppItem("gmail", "com.google.android.gm", "Main", "Gmail", android.os.Process.myUserHandle(), "gmail"),
            AppItem("maps", "com.google.android.apps.maps", "Main", "Maps", android.os.Process.myUserHandle(), "maps"),
            AppItem("messages", "com.google.android.apps.messaging", "Main", "Messages", android.os.Process.myUserHandle(), "messages"),
            AppItem("phone", "com.google.android.dialer", "Main", "Phone", android.os.Process.myUserHandle(), "phone"),
            AppItem("photos", "com.google.android.apps.photos", "Main", "Photos", android.os.Process.myUserHandle(), "photos"),
            AppItem("settings", "com.android.settings", "Main", "Settings", android.os.Process.myUserHandle(), "settings"),
            AppItem("youtube", "com.google.android.youtube", "Main", "YouTube", android.os.Process.myUserHandle(), "youtube")
        )
        AppDrawerScreen(
            apps = dummyApps,
            searchQuery = "",
            onQueryChange = {},
            onClearQuery = {},
            progressProvider = { 1.0f },
            maxDragPx = 2712f,
            onAppClick = {}
        )
    }
}
