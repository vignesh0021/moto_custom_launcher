package com.motorola.launcher.data.icon

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for IconPackManager component normalization, XML parsing, and data models.
 */
class IconPackManagerTest {

    @Test
    fun testNormalizeComponentStringWithStandardComponentInfo() {
        val raw = "ComponentInfo{com.whatsapp/com.whatsapp.Main}"
        val normalized = IconPackManager.normalizeComponentString(raw)
        assertEquals("com.whatsapp/com.whatsapp.Main", normalized)
    }

    @Test
    fun testNormalizeComponentStringWithDirectPair() {
        val raw = "com.google.android.youtube/com.google.android.apps.youtube.app.WatchWhileActivity"
        val normalized = IconPackManager.normalizeComponentString(raw)
        assertEquals("com.google.android.youtube/com.google.android.apps.youtube.app.WatchWhileActivity", normalized)
    }

    @Test
    fun testNormalizeComponentStringInvalid() {
        assertNull(IconPackManager.normalizeComponentString(""))
        assertNull(IconPackManager.normalizeComponentString("   "))
        assertNull(IconPackManager.normalizeComponentString("com.example.invalidNoSlash"))
        assertNull(IconPackManager.normalizeComponentString("ComponentInfo{}"))
    }

    @Test
    fun testComponentMappingExtraction() {
        val testInputs = listOf(
            "ComponentInfo{com.whatsapp/com.whatsapp.Main}" to "ic_whatsapp",
            "ComponentInfo{com.google.android.youtube/com.google.android.apps.youtube.app.WatchWhileActivity}" to "ic_youtube",
            "ComponentInfo{com.android.settings/com.android.settings.Settings}" to "ic_settings",
            "invalid_component_without_slash" to "ic_none"
        )

        val mapping = mutableMapOf<String, String>()
        for ((rawComponent, drawable) in testInputs) {
            val normalized = IconPackManager.normalizeComponentString(rawComponent)
            if (normalized != null) {
                mapping[normalized] = drawable
            }
        }

        assertEquals(3, mapping.size)
        assertEquals("ic_whatsapp", mapping["com.whatsapp/com.whatsapp.Main"])
        assertEquals("ic_youtube", mapping["com.google.android.youtube/com.google.android.apps.youtube.app.WatchWhileActivity"])
        assertEquals("ic_settings", mapping["com.android.settings/com.android.settings.Settings"])
        assertNull(mapping["invalid_component_without_slash"])
    }

    @Test
    fun testIconPackInfoDefaults() {
        val info = IconPackInfo(
            packageName = "com.theme.neon",
            label = "Neon Cyber Icons",
            isDefault = false
        )
        assertEquals("com.theme.neon", info.packageName)
        assertEquals("Neon Cyber Icons", info.label)
        assertEquals(false, info.isDefault)
        assertNull(info.iconBitmap)
    }
}
