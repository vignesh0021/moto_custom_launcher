package com.motorola.launcher.data.repository

import com.motorola.launcher.ui.model.AppItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for AppRepository search reactivity and prefix ranking.
 */
class AppSearchReactivityTest {

    private fun createAppItem(label: String, packageName: String, iconKey: String = "icon_$packageName"): AppItem {
        return AppItem(
            componentKey = "$packageName/MainActivity#0",
            packageName = packageName,
            activityName = "MainActivity",
            label = label,
            userHandle = null,
            iconCacheKey = iconKey,
            iconBitmap = null
        )
    }

    @Test
    fun testStaticFilterAndRankPrefixPriority() {
        val apps = listOf(
            createAppItem("Google Chrome", "com.android.chrome"),
            createAppItem("Chrome", "com.chrome"),
            createAppItem("Chromium", "org.chromium")
        )

        val result = AppRepository.filterAndRankApps(apps, "chr")
        assertEquals(3, result.size)
        // Exact prefix matches rank first
        assertEquals("Chrome", result[0].label)
        assertEquals("Chromium", result[1].label)
        assertEquals("Google Chrome", result[2].label)
    }

    @Test
    fun testSearchReactivityOnAppListChange() = runBlocking {
        val appsState = MutableStateFlow<ImmutableList<AppItem>>(
            listOf(
                createAppItem("Camera", "com.motorola.camera"),
                createAppItem("Clock", "com.motorola.deskclock")
            ).toImmutableList()
        )

        val searchFlow = appsState.map { apps ->
            AppRepository.filterAndRankApps(apps, "calc")
        }

        // Initially no calculator
        var currentResults = searchFlow.first()
        assertTrue(currentResults.isEmpty())

        // App installation event occurs in background
        appsState.value = (appsState.value + createAppItem("Calculator", "com.motorola.calculator")).toImmutableList()

        // Search immediately reacts to the new app installation without query modification
        currentResults = searchFlow.first()
        assertEquals(1, currentResults.size)
        assertEquals("Calculator", currentResults[0].label)
    }

    @Test
    fun testSearchReactivityOnIconHydration() = runBlocking {
        val initialApp = createAppItem("Chrome", "com.android.chrome")
        val appsState = MutableStateFlow<ImmutableList<AppItem>>(
            listOf(initialApp).toImmutableList()
        )

        val searchFlow = appsState.map { apps ->
            AppRepository.filterAndRankApps(apps, "chrome")
        }

        var result = searchFlow.first()
        assertEquals(1, result.size)
        assertNull(result[0].iconBitmap)

        // Simulate icon hydration updating _appsState
        val fakeImageBitmap = object : androidx.compose.ui.graphics.ImageBitmap {
            override val width: Int get() = 1
            override val height: Int get() = 1
            override val hasAlpha: Boolean get() = false
            override val colorSpace: androidx.compose.ui.graphics.colorspace.ColorSpace
                get() = androidx.compose.ui.graphics.colorspace.ColorSpaces.Srgb
            override val config: androidx.compose.ui.graphics.ImageBitmapConfig
                get() = androidx.compose.ui.graphics.ImageBitmapConfig.Argb8888
            override fun readPixels(
                buffer: IntArray,
                startX: Int,
                startY: Int,
                width: Int,
                height: Int,
                bufferOffset: Int,
                stride: Int
            ) {}
            override fun prepareToDraw() {}
        }
        val hydratedApp = initialApp.copy(iconBitmap = fakeImageBitmap)
        appsState.value = listOf(hydratedApp).toImmutableList()

        result = searchFlow.first()
        assertEquals(1, result.size)
        assertNotNull(result[0].iconBitmap)
    }
}
