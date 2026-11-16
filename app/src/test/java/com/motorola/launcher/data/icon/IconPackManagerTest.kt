package com.motorola.launcher.data.icon

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

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
    fun testParseXmlPullParserExtractsDrawablesCorrectly() {
        val xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
                <item component="ComponentInfo{com.whatsapp/com.whatsapp.Main}" drawable="ic_whatsapp" />
                <item component="ComponentInfo{com.google.android.youtube/com.google.android.apps.youtube.app.WatchWhileActivity}" drawable="ic_youtube" />
                <item component="ComponentInfo{com.android.settings/com.android.settings.Settings}" drawable="ic_settings" />
                <item component="invalid_entry" drawable="ic_none" />
            </resources>
        """.trimIndent()

        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xml))

        // Create a dummy IconPackManager without calling Android Context APIs
        val mapping = mutableMapOf<String, String>()
        var eventType = parser.eventType
        while (eventType != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
            if (eventType == org.xmlpull.v1.XmlPullParser.START_TAG && parser.name.equals("item", ignoreCase = true)) {
                val component = parser.getAttributeValue(null, "component")
                val drawable = parser.getAttributeValue(null, "drawable")
                if (!component.isNullOrBlank() && !drawable.isNullOrBlank()) {
                    val normalizedKey = IconPackManager.normalizeComponentString(component)
                    if (normalizedKey != null) {
                        mapping[normalizedKey] = drawable
                    }
                }
            }
            eventType = parser.next()
        }

        assertEquals(3, mapping.size)
        assertEquals("ic_whatsapp", mapping["com.whatsapp/com.whatsapp.Main"])
        assertEquals("ic_youtube", mapping["com.google.android.youtube/com.google.android.apps.youtube.app.WatchWhileActivity"])
        assertEquals("ic_settings", mapping["com.android.settings/com.android.settings.Settings"])
        assertNull(mapping["invalid_entry"])
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
