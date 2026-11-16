package com.motorola.launcher.data.db

/**
 * Represents an item positioned on a home screen workspace grid page.
 *
 * @property id Unique database ID.
 * @property pageIndex Workspace page (0 = Left/Overview, 1 = Center/Core, 2 = Right/Media & Widgets).
 * @property cellX Horizontal grid column (0..4 for a 5-column grid).
 * @property cellY Vertical grid row (0..5 for a 6-row grid).
 * @property spanX Width in grid cells (default 1, widgets can be 2..5).
 * @property spanY Height in grid cells (default 1, widgets can be 2..4).
 * @property itemType "APP", "WIDGET_NATIVE", "WIDGET_BUILTIN", or "FOLDER".
 * @property componentKey Component identifier for apps ("packageName/className#userSerial").
 * @property widgetId Android AppWidget ID allocated via AppWidgetHost (-1 for non-native items).
 * @property customTitle Display title or label override.
 * @property extraData Extra payload (e.g. built-in widget subtype: "WEATHER", "MEDIA", "CALENDAR", "TELEMETRY").
 */
data class WorkspaceItemEntity(
    val id: Long = 0,
    val pageIndex: Int,
    val cellX: Int,
    val cellY: Int,
    val spanX: Int = 1,
    val spanY: Int = 1,
    val itemType: String,
    val componentKey: String = "",
    val widgetId: Int = -1,
    val customTitle: String = "",
    val extraData: String = ""
) {
    companion object {
        const val TYPE_APP = "APP"
        const val TYPE_WIDGET_NATIVE = "WIDGET_NATIVE"
        const val TYPE_WIDGET_BUILTIN = "WIDGET_BUILTIN"
        const val TYPE_FOLDER = "FOLDER"

        const val BUILTIN_WEATHER = "WEATHER"
        const val BUILTIN_MEDIA = "MEDIA"
        const val BUILTIN_CALENDAR = "CALENDAR"
        const val BUILTIN_TELEMETRY = "TELEMETRY"
    }
}
