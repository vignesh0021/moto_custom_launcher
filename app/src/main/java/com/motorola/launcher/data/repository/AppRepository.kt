package com.motorola.launcher.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.util.Log
import com.motorola.launcher.data.icon.HardwareIconManager
import com.motorola.launcher.ui.model.AppItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * High-performance, crash-resilient Repository managing installed launcher apps
 * and hardware-backed icon hydration via LauncherApps.Callback.
 */
class AppRepository(
    private val context: Context,
    val iconManager: HardwareIconManager = HardwareIconManager(context)
) {
    companion object {
        private const val TAG = "AppRepository"
    }

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Coroutine exception in AppRepository", throwable)
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default + exceptionHandler)
    private val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
    private val userManager = context.getSystemService(Context.USER_SERVICE) as? UserManager

    private val _appsState = MutableStateFlow<ImmutableList<AppItem>>(emptyList<AppItem>().toImmutableList())
    val appsState: StateFlow<ImmutableList<AppItem>> = _appsState.asStateFlow()

    private val launcherAppsCallback = object : LauncherApps.Callback() {
        override fun onPackageAdded(packageName: String, user: UserHandle) {
            scope.launch { reloadAll() }
        }

        override fun onPackageRemoved(packageName: String, user: UserHandle) {
            scope.launch { removePackage(packageName, user) }
        }

        override fun onPackageChanged(packageName: String, user: UserHandle) {
            scope.launch { reloadAll() }
        }

        override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) {
            scope.launch { reloadAll() }
        }

        override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) {
            scope.launch { reloadAll() }
        }

        override fun onShortcutsChanged(packageName: String, shortcuts: MutableList<android.content.pm.ShortcutInfo>, user: UserHandle) {
            // Shortcuts update hook
        }
    }

    init {
        try {
            val mainHandler = Handler(Looper.getMainLooper())
            launcherApps?.registerCallback(launcherAppsCallback, mainHandler)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed registering LauncherApps callback", e)
        }

        // Cold start initial load
        scope.launch {
            reloadAll()
        }
    }

    /**
     * Re-indexes all installed activities across all user profiles (personal, work profile).
     */
    suspend fun reloadAll() = withContext(Dispatchers.Default) {
        try {
            val userProfiles = try {
                userManager?.userProfiles ?: listOf(Process.myUserHandle())
            } catch (e: Throwable) {
                Log.w(TAG, "Could not fetch userProfiles, falling back to current user: ${e.message}")
                listOf(Process.myUserHandle())
            }

            val appItems = mutableListOf<AppItem>()

            for (user in userProfiles) {
                val userSerial = try {
                    userManager?.getSerialNumberForUser(user) ?: 0L
                } catch (e: Throwable) {
                    0L
                }

                val activities: List<LauncherActivityInfo> = try {
                    launcherApps?.getActivityList(null, user) ?: emptyList()
                } catch (e: Throwable) {
                    Log.e(TAG, "Failed reading activities for user $user: ${e.message}")
                    emptyList()
                }

                for (info in activities) {
                    try {
                        val component = info.componentName
                        val componentKey = "${component.packageName}/${component.className}#$userSerial"
                        val label = info.label?.toString() ?: component.packageName
                        val installTime = try { info.firstInstallTime } catch (e: Throwable) { 0L }
                        val iconKey = "icon_${component.packageName}_${component.className}_$installTime"

                        // Check instant memory cache hit for icon; if absent, will load in background
                        val cachedBitmap = iconManager.peekMemory(iconKey)
                        appItems.add(
                            AppItem(
                                componentKey = componentKey,
                                packageName = component.packageName,
                                activityName = component.className,
                                label = label,
                                userHandle = user,
                                iconCacheKey = iconKey,
                                iconBitmap = cachedBitmap
                            )
                        )
                    } catch (e: Throwable) {
                        Log.e(TAG, "Error indexing activity: ${e.message}")
                    }
                }
            }

            // Sort alphabetically NOCASE
            appItems.sortBy { it.label.lowercase() }
            _appsState.value = appItems.toImmutableList()

            // Pre-warm icons asynchronously
            hydrateIconsInBackground(appItems)
        } catch (e: Throwable) {
            Log.e(TAG, "Fatal error in reloadAll: ${e.message}", e)
        }
    }

    private suspend fun removePackage(packageName: String, user: UserHandle) = withContext(Dispatchers.Default) {
        try {
            val current = _appsState.value.filterNot { it.packageName == packageName && it.userHandle == user }
            _appsState.value = current.toImmutableList()
        } catch (e: Throwable) {
            Log.e(TAG, "Error in removePackage: ${e.message}")
        }
    }

    /**
     * Sub-millisecond prefix-ranked search query executed against in-memory index.
     */
    fun searchApps(query: String): Flow<ImmutableList<AppItem>> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            return flowOf(_appsState.value)
        }
        val currentApps = _appsState.value
        val filtered = currentApps.filter {
            it.label.contains(trimmed, ignoreCase = true) ||
            it.packageName.contains(trimmed, ignoreCase = true)
        }.sortedWith(compareBy(
            { !it.label.startsWith(trimmed, ignoreCase = true) }, // Prefix matches first
            { it.label.lowercase() }
        )).toImmutableList()

        return flowOf(filtered)
    }

    private fun hydrateIconsInBackground(items: List<AppItem>) {
        scope.launch(Dispatchers.Default) {
            try {
                var updated = false
                val hydratedList = items.map { item ->
                    if (item.iconBitmap != null) {
                        item
                    } else {
                        val bmp = iconManager.loadIcon(item.iconCacheKey) {
                            try {
                                val activities = launcherApps?.getActivityList(item.packageName, item.userHandle)
                                val actInfo = activities?.firstOrNull { it.componentName == item.componentName }
                                actInfo?.getIcon(0)
                            } catch (e: Throwable) {
                                null
                            }
                        }
                        if (bmp != null) {
                            updated = true
                            item.copy(iconBitmap = bmp)
                        } else {
                            item
                        }
                    }
                }

                if (updated) {
                    _appsState.value = hydratedList.toImmutableList()
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Error in hydrateIconsInBackground: ${e.message}")
            }
        }
    }

    /**
     * Launches selected application with low-latency system binder handoff.
     */
    fun launchApp(item: AppItem, sourceBounds: Rect? = null) {
        try {
            val opts = if (sourceBounds != null) {
                val b = Bundle()
                b.putParcelable(Intent.EXTRA_REFERRER, null)
                b
            } else null

            launcherApps?.startMainActivity(
                item.componentName,
                item.userHandle,
                sourceBounds,
                opts
            )
        } catch (t: Throwable) {
            Log.e(TAG, "Failed launching ${item.componentKey}: ${t.message}")
        }
    }

    fun onDestroy() {
        try {
            launcherApps?.unregisterCallback(launcherAppsCallback)
        } catch (e: Throwable) {
            // ignored
        }
    }
}
