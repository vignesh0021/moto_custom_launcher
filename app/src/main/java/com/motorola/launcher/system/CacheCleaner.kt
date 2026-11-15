package com.motorola.launcher.system

import android.content.Context
import android.util.Log
import com.motorola.launcher.data.icon.HardwareIconManager
import java.io.File

/**
 * Automatic cache and temporary file cleaner for the launcher.
 * Safe to call on startup, after uninstall, or when storage is low.
 */
object CacheCleaner {

    private const val TAG = "CacheCleaner"

    fun clearAll(context: Context, iconManager: HardwareIconManager? = null) {
        try {
            // 1. Clear icon caches (memory + disk)
            iconManager?.clearAll()

            // 2. Clear app cache directory
            clearDirectory(context.cacheDir)

            // 3. Clear code cache
            clearDirectory(context.codeCacheDir)

            // 4. Clear external cache if available
            context.externalCacheDir?.let { clearDirectory(it) }

            // 5. Clear any custom tmp directories
            val tmpDir = File(context.filesDir, "tmp")
            if (tmpDir.exists()) clearDirectory(tmpDir)

            Log.i(TAG, "Cache and temporary files cleared successfully")
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to clear cache: ${t.message}", t)
        }
    }

    private fun clearDirectory(dir: File?) {
        if (dir == null || !dir.exists()) return
        dir.listFiles()?.forEach { file ->
            try {
                if (file.isDirectory) {
                    file.deleteRecursively()
                } else {
                    file.delete()
                }
            } catch (_: Throwable) {
                // ignore individual file failures
            }
        }
    }
}
