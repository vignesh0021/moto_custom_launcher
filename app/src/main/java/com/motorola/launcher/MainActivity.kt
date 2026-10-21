package com.motorola.launcher

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
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

    private lateinit var display144HzManager: Display144HzManager
    private lateinit var wallpaperZoomController: WallpaperZoomController
    private var curvedEdgeGestureController: CurvedEdgeGestureController? = null
    private lateinit var repository: AppRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        // Enforce edge-to-edge transparent system bars
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        super.onCreate(savedInstanceState)

        // 1. Hardware Wallpaper Window Plumbing
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // 2. Lock 144Hz Display Mode (6.944ms VSYNC cadence)
        display144HzManager = Display144HzManager(this)
        display144HzManager.enforce144HzMode()

        // 3. Hardware Wallpaper Zoom & Parallax Controller
        wallpaperZoomController = WallpaperZoomController(this)

        // 4. Curved Edge Palm Exclusion Controller
        curvedEdgeGestureController = CurvedEdgeGestureController(window.decorView)

        // 5. Reactive Repository & Hardware Icon Hydration
        repository = AppRepository(applicationContext)

        setContent {
            MotoLauncherTheme {
                LauncherRoot(
                    repository = repository,
                    wallpaperZoomController = wallpaperZoomController
                )
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        wallpaperZoomController.onWindowAttached(window.decorView)
        display144HzManager.enforce144HzMode()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        wallpaperZoomController.onWindowDetached()
    }

    override fun onResume() {
        super.onResume()
        // Re-enforce 144Hz mode upon returning to home screen
        display144HzManager.enforce144HzMode()
    }

    /**
     * Intercepts Home button presses (intent action MAIN) to reset state.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (Intent.ACTION_MAIN == intent.action && intent.hasCategory(Intent.CATEGORY_HOME)) {
            wallpaperZoomController.updateDrawerProgress(0.0f)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        curvedEdgeGestureController?.detach()
        repository.onDestroy()
    }
}
