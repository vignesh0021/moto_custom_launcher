package com.motorola.launcher

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import com.motorola.launcher.data.repository.AppRepository
import com.motorola.launcher.system.CurvedEdgeGestureController
import com.motorola.launcher.system.Display144HzManager
import com.motorola.launcher.system.WallpaperZoomController
import com.motorola.launcher.ui.LauncherRoot
import com.motorola.launcher.ui.theme.MotoLauncherTheme

/**
 * Host Activity for the Motorola Edge 50 Custom Launcher.
 * Plumbs system hardware wallpaper, locks 144Hz refresh rate, and manages curved palm rejection.
 */
class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private var display144HzManager: Display144HzManager? = null
    private var wallpaperZoomController: WallpaperZoomController? = null
    private var curvedEdgeGestureController: CurvedEdgeGestureController? = null
    private var repository: AppRepository? = null

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

        // 3. Lock 144Hz Display Mode (6.944ms VSYNC cadence)
        display144HzManager = Display144HzManager(this).also {
            it.enforce144HzMode()
        }

        // 4. Curved Edge Palm Exclusion Controller
        try {
            curvedEdgeGestureController = CurvedEdgeGestureController(window.decorView)
        } catch (e: Throwable) {
            Log.w(TAG, "Curved edge exclusion initialization failed: ${e.message}")
        }

        // 5. Reactive Repository & Icon Hydration
        val repo = AppRepository(applicationContext).also {
            repository = it
        }

        setContent {
            MotoLauncherTheme {
                LauncherRoot(
                    repository = repo,
                    wallpaperZoomController = wpController
                )
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        wallpaperZoomController?.onWindowAttached(window.decorView)
        display144HzManager?.enforce144HzMode()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        wallpaperZoomController?.onWindowDetached()
    }

    override fun onResume() {
        super.onResume()
        // Re-enforce 144Hz mode upon returning to home screen
        display144HzManager?.enforce144HzMode()
    }

    /**
     * Intercepts Home button presses (intent action MAIN) to reset state.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (Intent.ACTION_MAIN == intent.action && intent.hasCategory(Intent.CATEGORY_HOME)) {
            wallpaperZoomController?.updateDrawerProgress(0.0f)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        curvedEdgeGestureController?.detach()
        repository?.onDestroy()
    }
}
