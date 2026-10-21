package com.motorola.launcher.ui.model

import android.content.ComponentName
import android.os.UserHandle
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap

/**
 * 100% Immutable representation of a launcher activity.
 * Strict immutability guarantees zero unnecessary recompositions in Compose 1.12.
 */
@Immutable
data class AppItem(
    val componentKey: String,
    val packageName: String,
    val activityName: String,
    val label: String,
    val userHandle: UserHandle,
    val iconCacheKey: String,
    val iconBitmap: ImageBitmap? = null
) {
    val componentName: ComponentName
        get() = ComponentName(packageName, activityName)
}
