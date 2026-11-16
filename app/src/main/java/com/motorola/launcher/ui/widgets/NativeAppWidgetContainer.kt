package com.motorola.launcher.ui.widgets

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetProviderInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.motorola.launcher.ui.theme.LocalMarvelTheme

/**
 * High-performance Compose container for embedding native Android App Widgets (AppWidgetHostView).
 */
@Composable
fun NativeAppWidgetContainer(
    appWidgetHost: AppWidgetHost?,
    appWidgetId: Int,
    appWidgetInfo: AppWidgetProviderInfo?,
    modifier: Modifier = Modifier
) {
    val marvelTheme = LocalMarvelTheme.current
    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(shape)
            .background(Color(0x44000000))
            .border(1.dp, marvelTheme.colors.hudBorder, shape)
            .padding(4.dp)
    ) {
        if (appWidgetHost != null && appWidgetInfo != null) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    try {
                        appWidgetHost.createView(context, appWidgetId, appWidgetInfo).apply {
                            setAppWidget(appWidgetId, appWidgetInfo)
                        }
                    } catch (e: Throwable) {
                        android.widget.TextView(context).apply {
                            text = "Widget Error: ${e.message}"
                            setTextColor(android.graphics.Color.WHITE)
                        }
                    }
                }
            )
        }
    }
}
