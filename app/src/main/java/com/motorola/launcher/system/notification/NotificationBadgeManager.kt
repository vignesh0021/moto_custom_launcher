package com.motorola.launcher.system.notification

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages reactive notification badge counts for installed applications.
 * Coalesces status bar notifications per package name into an observable StateFlow.
 */
object NotificationBadgeManager {

    private val _badgeCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val badgeCounts: StateFlow<Map<String, Int>> = _badgeCounts.asStateFlow()

    private val _isServiceConnected = MutableStateFlow(false)
    val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

    fun setServiceConnected(connected: Boolean) {
        _isServiceConnected.value = connected
        if (!connected) {
            _badgeCounts.value = emptyMap()
        }
    }

    /**
     * Updates badge counts from a snapshot of active StatusBarNotifications.
     * Groups by packageName, ignoring ongoing foreground system notifications if needed.
     */
    fun updateFromNotifications(activeNotifications: Array<StatusBarNotification>?) {
        if (activeNotifications == null || activeNotifications.isEmpty()) {
            _badgeCounts.value = emptyMap()
            return
        }

        val counts = mutableMapOf<String, Int>()
        for (sbn in activeNotifications) {
            val pkg = sbn.packageName ?: continue
            // Count non-ongoing or dismissible user notifications
            if (sbn.isClearable) {
                counts[pkg] = (counts[pkg] ?: 0) + 1
            } else if (!sbn.notification.flags.and(android.app.Notification.FLAG_ONGOING_EVENT.toInt()).let { it != 0 }) {
                counts[pkg] = (counts[pkg] ?: 0) + 1
            }
        }

        _badgeCounts.value = counts
    }

    /**
     * Checks if this launcher has been granted Notification Access in Android Settings.
     */
    fun isNotificationAccessGranted(context: Context): Boolean {
        val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
        return enabledPackages.contains(context.packageName)
    }

    /**
     * Opens system Notification Access Settings screen.
     */
    fun openNotificationAccessSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Throwable) {
            val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
        }
    }
}
