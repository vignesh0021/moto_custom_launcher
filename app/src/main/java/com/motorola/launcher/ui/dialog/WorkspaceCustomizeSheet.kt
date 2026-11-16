package com.motorola.launcher.ui.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motorola.launcher.data.db.WorkspaceItemEntity
import com.motorola.launcher.ui.components.chamferedShape
import com.motorola.launcher.ui.theme.LocalMarvelTheme
import com.motorola.launcher.ui.theme.TextPrimary
import com.motorola.launcher.ui.theme.TextSecondary
import com.motorola.launcher.ui.workspace.HudTextStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceCustomizeSheet(
    targetPage: Int,
    targetCellX: Int,
    targetCellY: Int,
    selectedItem: WorkspaceItemEntity? = null,
    onAddNativeWidget: (page: Int, cellX: Int, cellY: Int) -> Unit,
    onAddBuiltinWidget: (page: Int, cellX: Int, cellY: Int, type: String, title: String) -> Unit,
    onAddAppShortcut: (page: Int, cellX: Int, cellY: Int) -> Unit,
    onRemoveItem: (id: Long) -> Unit,
    onOpenIconPacks: () -> Unit = {},
    onOpenNotificationSettings: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val marvelTheme = LocalMarvelTheme.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xF00A101D),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedItem != null) "WORKSPACE ITEM" else "CUSTOMIZE WORKSPACE",
                    color = marvelTheme.colors.bright,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    style = HudTextStyle
                )

                Text(
                    text = "PAGE ${targetPage + 1} ($targetCellX,$targetCellY)",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedItem != null) {
                // Option: Remove item
                WorkspaceActionRow(
                    title = "REMOVE FROM WORKSPACE",
                    subtitle = "Delete this shortcut or widget from the page",
                    isDestructive = true,
                    onClick = {
                        onRemoveItem(selectedItem.id)
                        onDismiss()
                    }
                )
            } else {
                // Option 1: Add Android Widget
                WorkspaceActionRow(
                    title = "ADD ANDROID WIDGET",
                    subtitle = "Select any installed app widget (Clock, Weather, Spotify, etc.)",
                    onClick = {
                        onAddNativeWidget(targetPage, targetCellX, targetCellY)
                        onDismiss()
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Option 2: Add Built-in Cyber Widgets
                WorkspaceActionRow(
                    title = "ADD WEATHER HUD WIDGET",
                    subtitle = "Holographic atmosphere and weather tracker",
                    onClick = {
                        onAddBuiltinWidget(targetPage, targetCellX, targetCellY, WorkspaceItemEntity.BUILTIN_WEATHER, "SECTOR WEATHER")
                        onDismiss()
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                WorkspaceActionRow(
                    title = "ADD AUDIO SYSTEM WIDGET",
                    subtitle = "Cyberpunk media player with playback controls",
                    onClick = {
                        onAddBuiltinWidget(targetPage, targetCellX, targetCellY, WorkspaceItemEntity.BUILTIN_MEDIA, "STARK AUDIO SYSTEM")
                        onDismiss()
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                WorkspaceActionRow(
                    title = "ADD MISSION AGENDA WIDGET",
                    subtitle = "Tactical calendar events and schedule timeline",
                    onClick = {
                        onAddBuiltinWidget(targetPage, targetCellX, targetCellY, WorkspaceItemEntity.BUILTIN_CALENDAR, "MISSION AGENDA")
                        onDismiss()
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                WorkspaceActionRow(
                    title = "ADD TELEMETRY CORE WIDGET",
                    subtitle = "Mini Arc Reactor, live battery, temp & RAM gauges",
                    onClick = {
                        onAddBuiltinWidget(targetPage, targetCellX, targetCellY, WorkspaceItemEntity.BUILTIN_TELEMETRY, "QUANTUM TELEMETRY")
                        onDismiss()
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Option 3: Add App Shortcut
                WorkspaceActionRow(
                    title = "ADD APP SHORTCUT",
                    subtitle = "Place an installed application icon onto this grid cell",
                    onClick = {
                        onAddAppShortcut(targetPage, targetCellX, targetCellY)
                        onDismiss()
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Option 4: Apply Icon Pack
                WorkspaceActionRow(
                    title = "APPLY ICON PACK",
                    subtitle = "Select third-party icon packs (Nova, Apex, Total Launcher compatible)",
                    onClick = {
                        onOpenIconPacks()
                        onDismiss()
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Option 5: Notification Badges & Dots
                WorkspaceActionRow(
                    title = "NOTIFICATION BADGES & ACCESS",
                    subtitle = "Configure live unread notification badges on apps and grant access",
                    onClick = {
                        onOpenNotificationSettings()
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun WorkspaceActionRow(
    title: String,
    subtitle: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    val marvelTheme = LocalMarvelTheme.current
    val shape = chamferedShape(cutSize = 8f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (isDestructive) Color(0x33FF1744) else Color(0x33000000))
            .border(
                width = 1.dp,
                color = if (isDestructive) Color(0x88FF1744) else marvelTheme.colors.hudBorder,
                shape = shape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Column {
            Text(
                text = title,
                color = if (isDestructive) Color(0xFFFF5252) else marvelTheme.colors.bright,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                style = HudTextStyle
            )
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
