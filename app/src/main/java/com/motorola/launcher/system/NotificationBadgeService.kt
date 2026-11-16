package com.motorola.launcher.system

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Real notification badge provider.
 * User must grant Notification Access in system settings for this to receive updates.
 */
class NotificationBadgeService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        refreshBadges()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        if (instance === this) instance = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        refreshBadges()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        refreshBadges()
    }

    private fun refreshBadges() {
        try {
            val active = activeNotifications ?: return
            val counts = active
                .filter { it.packageName != packageName }
                .groupBy { it.packageName }
                .mapValues { it.value.size }
            _badgeCounts.value = counts
        } catch (_: Throwable) {
            // Service may not be fully connected
        }
    }

    companion object {
        @Volatile
        var instance: NotificationBadgeService? = null
            private set

        private val _badgeCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
        val badgeCounts: StateFlow<Map<String, Int>> = _badgeCounts.asStateFlow()

        fun isEnabled(): Boolean = instance != null
    }
}
