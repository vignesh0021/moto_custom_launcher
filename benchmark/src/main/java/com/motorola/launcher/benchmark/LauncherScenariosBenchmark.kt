package com.motorola.launcher.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Comprehensive Macrobenchmark suite covering:
 * - Cold and Warm Launcher Startup Latency.
 * - App Drawer Open / Close Gesture Frame Pacing.
 * - Catalog Scrolling and Rapid Repeated Gestures.
 * - Return from Background behavior.
 */
@RunWith(AndroidJUnit4::class)
class LauncherScenariosBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    private val targetPackage = "com.motorola.launcher"

    /**
     * Cold Startup Benchmark (Process creation + initial Compose render)
     */
    @Test
    fun benchmarkColdStartup() = benchmarkRule.measureRepeated(
        packageName = targetPackage,
        metrics = listOf(StartupTimingMetric()),
        compilationMode = CompilationMode.Partial(),
        startupMode = StartupMode.COLD,
        iterations = 5
    ) {
        pressHome()
        startActivityAndWait()
    }

    /**
     * Warm Startup Benchmark (Process alive, Activity recreation/resumption)
     */
    @Test
    fun benchmarkWarmStartup() = benchmarkRule.measureRepeated(
        packageName = targetPackage,
        metrics = listOf(StartupTimingMetric()),
        compilationMode = CompilationMode.Partial(),
        startupMode = StartupMode.WARM,
        iterations = 5
    ) {
        pressHome()
        startActivityAndWait()
    }

    /**
     * Drawer Open and Close Frame Timing Benchmark
     */
    @OptIn(ExperimentalMetricApi::class)
    @Test
    fun benchmarkDrawerOpenClosePacing() = benchmarkRule.measureRepeated(
        packageName = targetPackage,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(),
        startupMode = StartupMode.WARM,
        iterations = 8,
        setupBlock = {
            pressHome()
            startActivityAndWait()
        }
    ) {
        val width = device.displayWidth
        val height = device.displayHeight

        // Swipe up to open drawer
        device.swipe(width / 2, (height * 0.85f).toInt(), width / 2, (height * 0.20f).toInt(), 8)
        device.waitForIdle()

        // Swipe down to close drawer
        device.swipe(width / 2, (height * 0.25f).toInt(), width / 2, (height * 0.80f).toInt(), 8)
        device.waitForIdle()
    }

    /**
     * App List Fling and Rapid Gestures Benchmark
     */
    @OptIn(ExperimentalMetricApi::class)
    @Test
    fun benchmarkAppListFlingPacing() = benchmarkRule.measureRepeated(
        packageName = targetPackage,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(),
        startupMode = StartupMode.WARM,
        iterations = 8,
        setupBlock = {
            pressHome()
            startActivityAndWait()
            val width = device.displayWidth
            val height = device.displayHeight
            device.swipe(width / 2, (height * 0.85f).toInt(), width / 2, (height * 0.20f).toInt(), 8)
            device.waitForIdle()
        }
    ) {
        val composeRoot = device.findObject(By.pkg(targetPackage))
        repeat(3) {
            composeRoot?.fling(Direction.DOWN)
            composeRoot?.fling(Direction.UP)
        }
        device.waitForIdle()
    }

    /**
     * Return from Background Benchmark
     */
    @Test
    fun benchmarkReturnFromBackground() = benchmarkRule.measureRepeated(
        packageName = targetPackage,
        metrics = listOf(StartupTimingMetric()),
        compilationMode = CompilationMode.Partial(),
        startupMode = StartupMode.HOT,
        iterations = 5,
        setupBlock = {
            pressHome()
            startActivityAndWait()
        }
    ) {
        // Send to background by opening recent apps or pressing home
        pressHome()
        startActivityAndWait()
    }
}
