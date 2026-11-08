package com.motorola.launcher

import android.app.Application
import android.os.Process
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.util.Date

/**
 * Custom Application class for Motorola Edge 50 Launcher.
 * Early-initializes thread priorities, crash instrumentation, and process memory pools.
 */
class LauncherApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Install emergency crash file logger for immediate diagnostics
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                val crashText = "Timestamp: ${Date()}\nThread: ${thread.name}\nStacktrace:\n$sw\n"
                try {
                    File(getExternalFilesDir(null), "crash.log").writeText(crashText)
                } catch (_: Throwable) {}
                try {
                    File("/sdcard/Download/moto_launcher_crash.log").writeText(crashText)
                } catch (_: Throwable) {}
            } catch (_: Throwable) {}
            defaultHandler?.uncaughtException(thread, throwable)
        }

        try {
            // Setting negative nice priorities requires CAP_SYS_NICE
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
