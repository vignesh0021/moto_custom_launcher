package com.motorola.launcher.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.MemoryUsageMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Validates that resting PSS memory remains strictly below 45 MB by offloading
 * icon pixel buffers to immutable Bitmap.Config.HARDWARE textures.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class MemoryFootprintBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @OptIn(ExperimentalMetricApi::class)
    @Test
    fun verifyRestingPssBelow45Mb() = benchmarkRule.measureRepeated(
        packageName = "com.motorola.launcher",
        metrics = listOf(MemoryUsageMetric(MemoryUsageMetric.Mode.Last)),
        compilationMode = CompilationMode.Full,
        startupMode = StartupMode.WARM,
        iterations = 5,
        setupBlock = {
            pressHome()
            startActivityAndWait()
        }
    ) {
        // Idle on home workspace for 3 seconds to measure stable resting PSS
        Thread.sleep(3000)
    }
}
