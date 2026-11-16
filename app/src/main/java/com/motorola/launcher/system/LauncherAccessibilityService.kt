package com.motorola.launcher.system

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent

/**
 * High-performance Accessibility Service for Moto Marvel Launcher.
 * Enables zero-permission rootless system actions:
 * - Double-tap empty workspace to sleep/lock screen (API 28+)
 * - Swipe-down to expand notifications shade reliably across all Android OS versions
 */
class LauncherAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "LauncherA11yService"

        @Volatile
        private var instance: LauncherAccessibilityService? = null

        val isRunning: Boolean
            get() = instance != null

        fun lockScreen(context: Context): Boolean {
            val service = instance
            if (service != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                return service.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            }
            // Guide user to enable accessibility if not granted
            try {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Throwable) {
                Log.w(TAG, "Failed launching accessibility settings: ${e.message}")
            }
            return false
        }

        fun expandNotifications(context: Context): Boolean {
            val service = instance
            if (service != null) {
                return service.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
            }
            // Fallback via StatusBarManager reflection
            return try {
                val statusBarService = context.getSystemService("statusbar")
                val statusBarManagerClass = Class.forName("android.app.StatusBarManager")
                val method = statusBarManagerClass.getMethod("expandNotificationsPanel")
                method.invoke(statusBarService)
                true
            } catch (e: Throwable) {
                Log.w(TAG, "Notification shade fallback invocation failed: ${e.message}")
                false
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "LauncherAccessibilityService connected and ready for lock & gestures")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Zero work in event loop to avoid any CPU/battery drain
    }

    override fun onInterrupt() {
        // Required callback
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }
}
