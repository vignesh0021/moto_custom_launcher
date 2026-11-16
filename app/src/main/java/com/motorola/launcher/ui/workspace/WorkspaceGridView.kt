package com.motorola.launcher.ui.workspace

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motorola.launcher.data.db.AppFolderEntity
import com.motorola.launcher.data.db.WorkspaceItemEntity
import com.motorola.launcher.system.SystemTelemetryState
import com.motorola.launcher.ui.components.FuturisticAppIcon
import com.motorola.launcher.ui.model.AppItem
import com.motorola.launcher.ui.theme.LocalMarvelTheme
import com.motorola.launcher.ui.theme.TextSecondary
import com.motorola.launcher.ui.widgets.CyberCalendarWidget
import com.motorola.launcher.ui.widgets.CyberMediaWidget
import com.motorola.launcher.ui.widgets.CyberTelemetryWidget
import com.motorola.launcher.ui.widgets.CyberWeatherWidget
import com.motorola.launcher.ui.widgets.NativeAppWidgetContainer

/**
 * Free-form 5x6 coordinate grid for Home Screen Pages.
 * Accurately maps (cellX, cellY, spanX, spanY) to spatial coordinates.
 */
@Composable
fun WorkspaceGridView(
    pageIndex: Int,
    items: List<WorkspaceItemEntity>,
    allApps: List<AppItem>,
    folders: List<AppFolderEntity>,
    telemetry: SystemTelemetryState,
    appWidgetHost: AppWidgetHost?,
    onAppClick: (AppItem) -> Unit,
    onAppLongClick: (AppItem) -> Unit,
    onFolderClick: (AppFolderEntity) -> Unit,
    onOpenDiagnostics: () -> Unit,
    onItemLongClick: (WorkspaceItemEntity) -> Unit,
    onEmptySpaceLongClick: (page: Int, cellX: Int, cellY: Int) -> Unit,
    onEmptySpaceDoubleTap: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val marvelTheme = LocalMarvelTheme.current
    val appWidgetManager = remember { AppWidgetManager.getInstance(context) }
    val appMap = remember(allApps) { allApps.associateBy { it.componentKey } }
    val folderMap = remember(folders) { folders.associateBy { it.id } }

    val columns = 5
    val rows = 6

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(pageIndex) {
                detectTapGestures(
                    onDoubleTap = { onEmptySpaceDoubleTap() },
                    onLongPress = { offset ->
                        val cellW = size.width / columns
                        val cellH = size.height / rows
                        val cx = (offset.x / cellW).toInt().coerceIn(0, columns - 1)
                        val cy = (offset.y / cellH).toInt().coerceIn(0, rows - 1)
                        onEmptySpaceLongClick(pageIndex, cx, cy)
                    }
                )
            }
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        val cellWidth = maxWidth / columns
        val cellHeight = maxHeight / rows

        for (item in items) {
            val itemX = cellWidth * item.cellX
            val itemY = cellHeight * item.cellY
            val itemW = cellWidth * item.spanX
            val itemH = cellHeight * item.spanY

            Box(
                modifier = Modifier
                    .offset(x = itemX, y = itemY)
                    .size(width = itemW, height = itemH)
                    .padding(4.dp)
                    .pointerInput(item.id) {
                        detectTapGestures(
                            onLongPress = { onItemLongClick(item) }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                when (item.itemType) {
                    WorkspaceItemEntity.TYPE_APP -> {
                        val app = appMap[item.componentKey]
                        if (app != null) {
                            FuturisticAppIcon(
                                app = app,
                                onClick = { onAppClick(app) },
                                onLongClick = { onAppLongClick(app) },
                                showLabel = true,
                                iconSize = 48.dp
                            )
                        }
                    }

                    WorkspaceItemEntity.TYPE_WIDGET_BUILTIN -> {
                        when (item.extraData) {
                            WorkspaceItemEntity.BUILTIN_WEATHER -> {
                                CyberWeatherWidget(
                                    modifier = Modifier.fillMaxSize(),
                                    onClick = {}
                                )
                            }
                            WorkspaceItemEntity.BUILTIN_MEDIA -> {
                                CyberMediaWidget(
                                    modifier = Modifier.fillMaxSize(),
                                    onLaunchMusicApp = {}
                                )
                            }
                            WorkspaceItemEntity.BUILTIN_CALENDAR -> {
                                CyberCalendarWidget(
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            WorkspaceItemEntity.BUILTIN_TELEMETRY -> {
                                CyberTelemetryWidget(
                                    telemetry = telemetry,
                                    modifier = Modifier.fillMaxSize(),
                                    onOpenDiagnostics = onOpenDiagnostics
                                )
                            }
                        }
                    }

                    WorkspaceItemEntity.TYPE_WIDGET_NATIVE -> {
                        if (item.widgetId != -1) {
                            val info = remember(item.widgetId) {
                                try {
                                    appWidgetManager.getAppWidgetInfo(item.widgetId)
                                } catch (_: Throwable) {
                                    null
                                }
                            }
                            NativeAppWidgetContainer(
                                appWidgetHost = appWidgetHost,
                                appWidgetId = item.widgetId,
                                appWidgetInfo = info,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    WorkspaceItemEntity.TYPE_FOLDER -> {
                        val folderId = item.componentKey.toLongOrNull() ?: -1L
                        val folder = folderMap[folderId]
                        val shape = RoundedCornerShape(12.dp)
                        Column(
                            modifier = Modifier
                                .clip(shape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { if (folder != null) onFolderClick(folder) }
                                )
                                .padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(shape)
                                    .background(Color(0x3300E5FF))
                                    .border(1.dp, marvelTheme.colors.hudBorder, shape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = folder?.keyList?.size?.toString() ?: "0",
                                    color = marvelTheme.colors.bright,
                                    fontSize = 16.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = item.customTitle.ifEmpty { folder?.name ?: "Folder" },
                                color = Color.White,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
