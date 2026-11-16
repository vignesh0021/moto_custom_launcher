package com.motorola.launcher

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.motorola.launcher.data.db.WorkspaceItemEntity
import com.motorola.launcher.data.repository.AppRepository
import com.motorola.launcher.data.repository.WorkspaceRepository
import com.motorola.launcher.system.CurvedEdgeGestureController
import com.motorola.launcher.system.WallpaperZoomController
import com.motorola.launcher.ui.LauncherRoot
import com.motorola.launcher.ui.theme.MotoLauncherTheme
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

/**
 * Host Activity for the Motorola Edge 50 Custom Launcher.
 * Plumbs system hardware wallpaper, locks 144Hz refresh rate, hosts native Android widgets via AppWidgetHost,
 * and manages curved palm rejection.
 */
class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
        const val EXTRA_OPEN_HYDRATION_DASHBOARD = "OPEN_HYDRATION_DASHBOARD"
        const val APPWIDGET_HOST_ID = 2048
    }

    private val homePressEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val hydrationDashboardEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private var wallpaperZoomController: WallpaperZoomController? = null
    private var curvedEdgeGestureController: CurvedEdgeGestureController? = null
    private var repository: AppRepository? = null
    private var workspaceRepository: WorkspaceRepository? = null
    private var telemetryManager: com.motorola.launcher.system.SystemTelemetryManager? = null

    private var appWidgetHost: AppWidgetHost? = null
    private var appWidgetManager: AppWidgetManager? = null

    private data class PendingWidgetPlacement(
        val appWidgetId: Int,
        val page: Int,
        val cellX: Int,
        val cellY: Int
    )
    private var pendingPlacement: PendingWidgetPlacement? = null

    private val widgetConfigLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val placement = pendingPlacement ?: return@registerForActivityResult
        if (result.resultCode == RESULT_OK) {
            completeAddWidget(placement.appWidgetId, placement.page, placement.cellX, placement.cellY)
        } else {
            appWidgetHost?.deleteAppWidgetId(placement.appWidgetId)
        }
        pendingPlacement = null
    }

    private val widgetPickLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val placement = pendingPlacement ?: return@registerForActivityResult
        if (result.resultCode == RESULT_OK) {
            val appWidgetId = result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, placement.appWidgetId) ?: placement.appWidgetId
            val info = try { appWidgetManager?.getAppWidgetInfo(appWidgetId) } catch (_: Throwable) { null }
            if (info?.configure != null) {
                // Widget requires custom configuration activity
                val configureIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                    component = info.configure
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                pendingPlacement = placement.copy(appWidgetId = appWidgetId)
                widgetConfigLauncher.launch(configureIntent)
            } else {
                completeAddWidget(appWidgetId, placement.page, placement.cellX, placement.cellY)
                pendingPlacement = null
            }
        } else {
            appWidgetHost?.deleteAppWidgetId(placement.appWidgetId)
            pendingPlacement = null
        }
    }

    private fun completeAddWidget(appWidgetId: Int, page: Int, cellX: Int, cellY: Int) {
        val repo = workspaceRepository ?: return
        lifecycleScope.launch {
            repo.addWorkspaceItem(
                pageIndex = page,
                cellX = cellX,
                cellY = cellY,
                spanX = 5,
                spanY = 2,
                itemType = WorkspaceItemEntity.TYPE_WIDGET_NATIVE,
                widgetId = appWidgetId
            )
        }
    }

    fun selectAppWidget(page: Int, cellX: Int, cellY: Int) {
        val host = appWidgetHost ?: return
        try {
            val appWidgetId = host.allocateAppWidgetId()
            pendingPlacement = PendingWidgetPlacement(appWidgetId, page, cellX, cellY)
            val pickIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            widgetPickLauncher.launch(pickIntent)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed launching widget picker: ${e.message}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            // Enforce edge-to-edge transparent system bars
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
                navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
            )
        } catch (e: Throwable) {
            Log.w(TAG, "enableEdgeToEdge failed: ${e.message}")
        }

        super.onCreate(savedInstanceState)

        try {
            // 1. Hardware Wallpaper Window Plumbing
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
            WindowCompat.setDecorFitsSystemWindows(window, false)
        } catch (e: Throwable) {
            Log.w(TAG, "Failed setting window wallpaper flags: ${e.message}")
        }

        // 2. Hardware Wallpaper Zoom & Parallax Controller
        val wpController = WallpaperZoomController(this).also {
            wallpaperZoomController = it
        }

        // 3. Curved Edge Palm Exclusion Controller
        try {
            curvedEdgeGestureController = CurvedEdgeGestureController(window.decorView)
        } catch (e: Throwable) {
            Log.w(TAG, "Curved edge exclusion initialization failed: ${e.message}")
        }

        // 4. Reactive Repository & Icon Hydration
        val repo = AppRepository(applicationContext).also {
            repository = it
        }

        // 5. Workspace Multi-Page & Widget Repository
        val wpRepo = WorkspaceRepository(applicationContext).also {
            workspaceRepository = it
        }

        // 6. Native Android AppWidgetHost & Manager
        try {
            appWidgetManager = AppWidgetManager.getInstance(applicationContext)
            appWidgetHost = AppWidgetHost(applicationContext, APPWIDGET_HOST_ID)
        } catch (e: Throwable) {
            Log.w(TAG, "Failed initializing AppWidgetHost: ${e.message}")
        }

        // 7. Live System Telemetry Engine (Lifecycle-aware)
        val telem = com.motorola.launcher.system.SystemTelemetryManager(applicationContext).also {
            telemetryManager = it
        }

        // 8. Initialize Hydration Notification Channel & Background Alarm Orchestrator
        try {
            com.motorola.launcher.system.hydration.HydrationNotificationHelper.createNotificationChannel(applicationContext)
            com.motorola.launcher.system.companion.FamilyCompanionOrchestrator.getInstance(applicationContext)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 9001)
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Hydration notification init failed: ${e.message}")
        }

        if (intent.getBooleanExtra(EXTRA_OPEN_HYDRATION_DASHBOARD, false)) {
            hydrationDashboardEvents.tryEmit(Unit)
        }

        setContent {
            MotoLauncherTheme {
                LauncherRoot(
                    repository = repo,
                    workspaceRepository = wpRepo,
                    appWidgetHost = appWidgetHost,
                    onRequestAddNativeWidget = { page, cx, cy -> selectAppWidget(page, cx, cy) },
                    wallpaperZoomController = wpController,
                    telemetryManager = telem,
                    homePressEvents = homePressEvents,
                    hydrationDashboardEvents = hydrationDashboardEvents
                )
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        wallpaperZoomController?.onWindowAttached(window.decorView)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        wallpaperZoomController?.onWindowDetached()
    }

    private val closeSystemDialogsReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: Intent?) {
            if (Intent.ACTION_CLOSE_SYSTEM_DIALOGS == intent?.action) {
                wallpaperZoomController?.updateDrawerProgress(0.0f)
                homePressEvents.tryEmit(Unit)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        telemetryManager?.startPeriodicCollection()
        try {
            appWidgetHost?.startListening()
        } catch (e: Throwable) {
            Log.w(TAG, "Error in appWidgetHost.startListening: ${e.message}")
        }
    }

    override fun onResume() {
        super.onResume()
        telemetryManager?.startPeriodicCollection()
        try {
            val filter = android.content.IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(closeSystemDialogsReceiver, filter, android.content.Context.RECEIVER_EXPORTED)
            } else {
                registerReceiver(closeSystemDialogsReceiver, filter)
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Registering closeSystemDialogsReceiver failed: ${e.message}")
        }
    }

    override fun onPause() {
        super.onPause()
        telemetryManager?.stopPeriodicCollection()
        try {
            unregisterReceiver(closeSystemDialogsReceiver)
        } catch (_: Throwable) {}
    }

    override fun onStop() {
        super.onStop()
        telemetryManager?.stopPeriodicCollection()
        try {
            appWidgetHost?.stopListening()
        } catch (e: Throwable) {
            Log.w(TAG, "Error in appWidgetHost.stopListening: ${e.message}")
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        homePressEvents.tryEmit(Unit)
    }

    /**
     * Intercepts Home button presses (intent action MAIN) to reset state and return to main home screen.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (Intent.ACTION_MAIN == intent.action) {
            wallpaperZoomController?.updateDrawerProgress(0.0f)
            homePressEvents.tryEmit(Unit)
        }
        if (intent.getBooleanExtra(EXTRA_OPEN_HYDRATION_DASHBOARD, false)) {
            hydrationDashboardEvents.tryEmit(Unit)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        curvedEdgeGestureController?.detach()
        telemetryManager?.release()
        repository?.onDestroy()
    }
}
