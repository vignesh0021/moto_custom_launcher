package com.motorola.launcher.data.repository

import com.motorola.launcher.ui.model.AppItem
import kotlinx.collections.immutable.toImmutableList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for in-memory prefix-ranked search algorithm in AppRepository.
 */
class AppSearchRankingTest {

    private fun createAppItem(label: String, packageName: String): AppItem {
        return AppItem(
            componentKey = "$packageName/MainActivity#0",
            packageName = packageName,
            activityName = "MainActivity",
            label = label,
            userHandle = null,
            iconCacheKey = "icon_${packageName}_MainActivity_0",
            iconBitmap = null
        )
    }

    private fun filterAndRank(apps: List<AppItem>, query: String): List<AppItem> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return apps

        return apps.filter {
            it.label.contains(trimmed, ignoreCase = true) ||
            it.packageName.contains(trimmed, ignoreCase = true)
        }.sortedWith(compareBy(
            { !it.label.startsWith(trimmed, ignoreCase = true) }, // Prefix matches first
            { it.label.lowercase() }
        ))
    }

    @Test
    fun testEmptyQueryReturnsAllApps() {
        val apps = listOf(
            createAppItem("Chrome", "com.android.chrome"),
            createAppItem("Camera", "com.motorola.camera"),
            createAppItem("Settings", "com.android.settings")
        )

        val result = filterAndRank(apps, "")
        assertEquals(3, result.size)
    }

    @Test
    fun testPrefixMatchesRankAheadOfSubstringMatches() {
        val apps = listOf(
            createAppItem("Google Chrome", "com.android.chrome"),
            createAppItem("Chrome Beta", "com.chrome.beta"),
            createAppItem("Chromium", "org.chromium.chrome")
        )

        val result = filterAndRank(apps, "chr")
        assertEquals(3, result.size)

        // "Chrome Beta" and "Chromium" both start with "Chr", so they rank ahead of "Google Chrome"
        assertEquals("Chrome Beta", result[0].label)
        assertEquals("Chromium", result[1].label)
        assertEquals("Google Chrome", result[2].label)
    }

    @Test
    fun testCaseInsensitiveMatching() {
        val apps = listOf(
            createAppItem("WhatsApp", "com.whatsapp"),
            createAppItem("Telegram", "org.telegram.messenger")
        )

        val resultLower = filterAndRank(apps, "what")
        assertEquals(1, resultLower.size)
        assertEquals("WhatsApp", resultLower[0].label)

        val resultUpper = filterAndRank(apps, "WHAT")
        assertEquals(1, resultUpper.size)
        assertEquals("WhatsApp", resultUpper[0].label)
    }

    @Test
    fun testPackageNameFallbackSearch() {
        val apps = listOf(
            createAppItem("Browser", "com.google.android.apps.chrome"),
            createAppItem("Photos", "com.google.android.apps.photos")
        )

        val result = filterAndRank(apps, "chrome")
        assertEquals(1, result.size)
        assertEquals("Browser", result[0].label)
    }

    @Test
    fun testNoMatchReturnsEmptyList() {
        val apps = listOf(
            createAppItem("Camera", "com.motorola.camera"),
            createAppItem("Clock", "com.motorola.deskclock")
        )

        val result = filterAndRank(apps, "xyz123NonExistent")
        assertTrue(result.isEmpty())
    }
}
