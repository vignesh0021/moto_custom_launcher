package com.motorola.launcher.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.provider.Settings
import android.util.Log
import com.motorola.launcher.data.icon.HardwareIconManager
import com.motorola.launcher.ui.model.AppItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * High-performance, crash-resilient Repository managing installed launcher apps
 * and hardware-backed icon hydration via LauncherApps.Callback.
 *
 * Guarantees:
 * - Event coalescing: Batches rapid package add/remove/change events within a 250ms debounce window.
 * - Non-swallowed cancellations: CancellationException is rethrown to maintain coroutine structure.
 * - Deterministic lifecycle: Cancels background scopes and unregisters callbacks on destroy.
 * - Responsive search: Offloaded to Dispatchers.Default with prefix ranking.
 */
class AppRepository(
    private val context: Context,
    val iconManager: HardwareIconManager = HardwareIconManager(context)
) {
    val preferences = LauncherPreferences(context)

    companion object {
        private const val TAG = "AppRepository"
        private const val PACKAGE_COALESCE_DELAY_MS = 250L
    }

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        if (throwable !is CancellationException) {
            Log.e(TAG, "Coroutine exception in AppRepository", throwable)
        }
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default + exceptionHandler)
    private val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
    private val userManager = context.getSystemService(Context.USER_SERVICE) as? UserManager

    private val _appsState = MutableStateFlow<ImmutableList<AppItem>>(emptyList<AppItem>().toImmutableList())
    val appsState: StateFlow<ImmutableList<AppItem>> = _appsState.asStateFlow()

    private var reloadJob: Job? = null
    private var hydrationJob: Job? = null
    private val reloadMutex = Mutex()

    private val launcherAppsCallback = object : LauncherApps.Callback() {
        override fun onPackageAdded(packageName: String, user: UserHandle) {
            scheduleReload()
        }

        override fun onPackageRemoved(packageName: String, user: UserHandle) {
            scheduleReload()
        }

        override fun onPackageChanged(packageName: String, user: UserHandle) {
            scheduleReload()
        }

        override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) {
            scheduleReload()
        }

        override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) {
            scheduleReload()
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

        // Initial load without debounce delay
        scheduleReload(delayMs = 0L)
    }

    /**
     * Coalesces rapid package change callbacks into a single reload pass.
     */
    fun scheduleReload(delayMs: Long = PACKAGE_COALESCE_DELAY_MS) {
        scope.launch {
            reloadMutex.withLock {
                reloadJob?.cancel()
                reloadJob = scope.launch {
                    try {
                        if (delayMs > 0) {
                            delay(delayMs)
                        }
                        reloadAll()
                    } catch (c: CancellationException) {
                        throw c
                    } catch (e: Throwable) {
                        Log.e(TAG, "Error executing scheduled reload: ${e.message}", e)
                    }
                }
            }
        }
    }

    /**
     * Re-indexes all installed activities across all user profiles (personal, work profile).
     */
    suspend fun reloadAll() = withContext(Dispatchers.Default) {
        try {
            val userProfiles = try {
                userManager?.userProfiles ?: listOf(Process.myUserHandle())
            } catch (c: CancellationException) {
                throw c
            } catch (e: Throwable) {
                Log.w(TAG, "Could not fetch userProfiles, falling back to current user: ${e.message}")
                listOf(Process.myUserHandle())
            }

            val appItems = mutableListOf<AppItem>()

            for (user in userProfiles) {
                val userSerial = try {
                    userManager?.getSerialNumberForUser(user) ?: 0L
                } catch (c: CancellationException) {
                    throw c
                } catch (e: Throwable) {
                    0L
                }

                val activities: List<LauncherActivityInfo> = try {
                    launcherApps?.getActivityList(null, user) ?: emptyList()
                } catch (c: CancellationException) {
                    throw c
                } catch (e: Throwable) {
                    Log.e(TAG, "Failed reading activities for user $user: ${e.message}")
                    emptyList()
                }

                for (info in activities) {
                    try {
                        val component = info.componentName
                        val componentKey = "${component.packageName}/${component.className}#$userSerial"
                        val label = info.label?.toString() ?: component.packageName
                        val installTime = try { info.firstInstallTime } catch (_: Throwable) { 0L }
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
                    } catch (c: CancellationException) {
                        throw c
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
        } catch (c: CancellationException) {
            throw c
        } catch (e: Throwable) {
            Log.e(TAG, "Fatal error in reloadAll: ${e.message}", e)
        }
    }

    /**
     * Sub-millisecond prefix-ranked search query executed against in-memory index on Dispatchers.Default.
     */
    fun searchApps(query: String): Flow<ImmutableList<AppItem>> = flow {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            emit(_appsState.value)
            return@flow
        }
        val currentApps = _appsState.value
        val filtered = currentApps.filter {
            it.label.contains(trimmed, ignoreCase = true) ||
            it.packageName.contains(trimmed, ignoreCase = true)
        }.sortedWith(compareBy(
            { !it.label.startsWith(trimmed, ignoreCase = true) }, // Prefix matches first
            { it.label.lowercase() }
        )).toImmutableList()

        emit(filtered)
    }.flowOn(Dispatchers.Default)

    private fun hydrateIconsInBackground(items: List<AppItem>) {
        hydrationJob?.cancel()
        hydrationJob = scope.launch(Dispatchers.Default) {
            try {
                val currentList = items.toMutableList()
                var pendingBatchUpdates = 0

                for (i in currentList.indices) {
                    val item = currentList[i]
                    if (item.iconBitmap == null) {
                        val bmp = iconManager.loadIcon(item.iconCacheKey) {
                            try {
                                val activities = launcherApps?.getActivityList(item.packageName, item.userHandle)
                                val actInfo = activities?.firstOrNull { it.componentName == item.componentName }
                                actInfo?.getIcon(0)
                            } catch (_: Throwable) {
                                null
                            }
                        }
                        if (bmp != null) {
                            currentList[i] = item.copy(iconBitmap = bmp)
                            pendingBatchUpdates++
                        }
                    }

                    // Flush batch updates every 8 items or on initial few items for instant display
                    if (pendingBatchUpdates > 0 && (i < 8 || pendingBatchUpdates >= 12 || i == currentList.lastIndex)) {
                        _appsState.value = currentList.toImmutableList()
                        pendingBatchUpdates = 0
                    }
                }

                if (pendingBatchUpdates > 0) {
                    _appsState.value = currentList.toImmutableList()
                }
            } catch (c: CancellationException) {
                throw c
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

    /**
     * Opens system App Info page for the given app.
     */
    fun openAppInfo(item: AppItem) {
        try {
            launcherApps?.startAppDetailsActivity(
                item.componentName,
                item.userHandle,
                null,
                null
            )
        } catch (e: Throwable) {
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", item.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to open app info: ${t.message}")
            }
        }
    }

    /**
     * Requests application uninstallation.
     */
    fun requestUninstall(item: AppItem) {
        try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.fromParts("package", item.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to request uninstall: ${t.message}")
        }
    }

    fun onDestroy() {
        try {
            launcherApps?.unregisterCallback(launcherAppsCallback)
        } catch (e: Throwable) {
            // ignored
        }
        scope.cancel()
    }
}
