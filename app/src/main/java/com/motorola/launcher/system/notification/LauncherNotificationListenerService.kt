package com.motorola.launcher.system.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

/**
 * Android NotificationListenerService that notifies NotificationBadgeManager
 * whenever notifications are posted or dismissed.
 */
class LauncherNotificationListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "LauncherNotifListener"
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "Notification listener connected")
        NotificationBadgeManager.setServiceConnected(true)
        refreshActiveNotifications()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.i(TAG, "Notification listener disconnected")
        NotificationBadgeManager.setServiceConnected(false)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        refreshActiveNotifications()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        refreshActiveNotifications()
    }

    private fun refreshActiveNotifications() {
        try {
            val active = activeNotifications
            NotificationBadgeManager.updateFromNotifications(active)
        } catch (t: Throwable) {
            Log.e(TAG, "Error fetching active notifications: ${t.message}")
        }
    }
}
