package com.motorola.launcher

import android.app.Application
import android.os.Process
import android.util.Log

/**
 * Custom Application class for Motorola Edge 50 Launcher.
 * Early-initializes thread priorities and process memory pools.
 */
class LauncherApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        try {
            // Setting negative nice priorities (like THREAD_PRIORITY_DISPLAY = -4) requires CAP_SYS_NICE
            // which is restricted to system/root apps. Normal user apps throw SecurityException.
            // Safely initialize with default priority.
            Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT)
            Log.i("LauncherApplication", "Motorola Edge 50 Launcher Initialized successfully.")

            // Initialize V2H Smart Hydration Reminder System
            com.motorola.launcher.system.hydration.HydrationNotificationHelper.createNotificationChannel(this)
            com.motorola.launcher.system.companion.FamilyCompanionOrchestrator.getInstance(this)
        } catch (e: Throwable) {
            Log.w("LauncherApplication", "Initialization warning: ${e.message}")
        }
    }
}
