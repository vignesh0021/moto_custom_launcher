package com.motorola.launcher.ui.workspace

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motorola.launcher.ui.model.AppItem
import com.motorola.launcher.ui.theme.DockGlass
import com.motorola.launcher.ui.theme.MotoCyanAccent
import com.motorola.launcher.ui.theme.MotoGlassBorder
import com.motorola.launcher.ui.theme.TextPrimary
import com.motorola.launcher.ui.theme.TextTertiary
import kotlinx.collections.immutable.ImmutableList

/**
 * Main Home Workspace composable.
 * Uses lambda-based graphicsLayer transforms to eliminate recomposition during 360Hz swipes.
 */
@Composable
fun WorkspaceScreen(
    dockApps: ImmutableList<AppItem>,
    progressProvider: () -> Float,
    onAppClick: (AppItem) -> Unit,
    onOpenDrawerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                val p = progressProvider()
                // Layer-only transform: 0 recompositions, 0 layout passes
                val scale = 1.0f - (0.08f * p)
                scaleX = scale
                scaleY = scale
                alpha = (1.0f - (p * 1.8f)).coerceIn(0.0f, 1.0f)
            }
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Clock & Glanceable Information
        ClockWidget(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 28.dp)
        )

        // Bottom Workspace Dock
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterVertically
        ) {
            // Swipe Up Pill Indicator
            Box(
                modifier = Modifier
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(TextTertiary)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onOpenDrawerClick
                    )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Glassmorphic Dock Container
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(DockGlass)
                    .border(1.dp, MotoGlassBorder, RoundedCornerShape(28.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (item in dockApps.take(5)) {
                    DockIconItem(
                        app = item,
                        onClick = { onAppClick(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DockIconItem(
    app: AppItem,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(54.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (app.iconBitmap != null) {
            Image(
                bitmap = app.iconBitmap,
                contentDescription = app.label,
                modifier = Modifier.size(46.dp)
            )
        } else {
            // High-contrast fallback avatar
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0x3300E5FF)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = app.label.take(1).uppercase(),
                    color = MotoCyanAccent,
                    fontSize = 18.sp
                )
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "Moto Edge 50 Workspace (20:9)",
    device = "spec:width=1220px,height=2712px,dpi=446",
    showBackground = true,
    backgroundColor = 0xFF0D1117
)
@Composable
private fun WorkspaceScreenPreview() {
    com.motorola.launcher.ui.theme.MotoLauncherTheme {
        val dummyApps = kotlinx.collections.immutable.persistentListOf(
            AppItem("com.google.android.dialer/.DialtactsActivity#0", "com.google.android.dialer", ".DialtactsActivity", "Phone", android.os.Process.myUserHandle(), "phone"),
            AppItem("com.google.android.apps.messaging/.ui.ConversationListActivity#0", "com.google.android.apps.messaging", ".ConversationListActivity", "Messages", android.os.Process.myUserHandle(), "messages"),
            AppItem("com.android.chrome/com.google.android.apps.chrome.Main#0", "com.android.chrome", "Main", "Chrome", android.os.Process.myUserHandle(), "chrome"),
            AppItem("com.motorola.camera3/.MainActivity#0", "com.motorola.camera3", ".MainActivity", "Camera", android.os.Process.myUserHandle(), "camera"),
            AppItem("com.android.settings/.Settings#0", "com.android.settings", ".Settings", "Settings", android.os.Process.myUserHandle(), "settings")
        )
        WorkspaceScreen(
            dockApps = dummyApps,
            progressProvider = { 0.0f },
            onAppClick = {},
            onOpenDrawerClick = {}
        )
    }
}
