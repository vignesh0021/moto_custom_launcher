package com.motorola.launcher.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Generates production baseline profiles capturing startup, workspace idle,
 * and 144Hz app-drawer flings on Motorola Edge 50.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generateBaselineProfile() = baselineProfileRule.collect(
        packageName = "com.motorola.launcher",
        includeInStartupProfile = true
    ) {
        // 1. Cold start to Workspace
        pressHome()
        startActivityAndWait()

        // 2. Perform 360Hz Swipe-up to open App Drawer
        val deviceWidth = device.displayWidth
        val deviceHeight = device.displayHeight

        // Swipe up from bottom 85% to top 20%
        val startX = deviceWidth / 2
        val startY = (deviceHeight * 0.85f).toInt()
        val endY = (deviceHeight * 0.20f).toInt()

        device.swipe(startX, startY, startX, endY, 10) // 10 steps = fast fling
        device.waitForIdle()

        // 3. Fling down and up inside app drawer
        val drawerArea = device.findObject(By.clazz("androidx.compose.ui.platform.ComposeView"))
        drawerArea?.let {
            it.fling(Direction.DOWN)
            device.waitForIdle()
            it.fling(Direction.UP)
            device.waitForIdle()
        }

        // 4. Return to Workspace
        pressHome()
        device.waitForIdle()
    }
}
