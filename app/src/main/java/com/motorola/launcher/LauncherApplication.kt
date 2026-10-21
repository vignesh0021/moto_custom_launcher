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
        // Boost launcher main thread to high display priority
        Process.setThreadPriority(Process.THREAD_PRIORITY_DISPLAY)
        Log.i("LauncherApplication", "Motorola Edge 50 Launcher Initialized at DISPLAY thread priority.")
    }
}
