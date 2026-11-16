package com.motorola.launcher.data.icon

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * Representation of an installed or built-in icon pack.
 */
@Immutable
data class IconPackInfo(
    val packageName: String,
    val label: String,
    val isDefault: Boolean = false,
    val isBuiltIn: Boolean = false,
    val description: String? = null,
    val iconBitmap: Bitmap? = null
)

/**
 * High-performance, zero-allocation icon pack parser and resource resolver.
 * Supports:
 * 1. Official Built-in Marvel Cyber HUD Icon Packs (Stark, J.A.R.V.I.S., Vibranium, Arc Reactor)
 * 2. Custom AI Studio Icon Pack (/sdcard/Download/moto_launcher_iconpack)
 * 3. Third-party icon packs (Nova, Apex, ADW, Go, Total Launcher compatible appfilter.xml)
 */
class IconPackManager(private val context: Context) {

    companion object {
        private const val TAG = "IconPackManager"
        const val CUSTOM_ICONPACK_DIR = "/sdcard/Download/moto_launcher_iconpack"

        val ICON_PACK_INTENT_ACTIONS = listOf(
            "com.novalauncher.THEME",
            "org.adw.launcher.THEMES",
            "com.anddoes.launcher.THEME",
            "com.teslacoilsw.launcher.THEME",
            "com.fede.launcher.THEME_ICONPACK",
            "com.gau.go.launcherex.theme"
        )

        fun isBuiltInPack(pkg: String?): Boolean {
            return pkg != null && (
                pkg == CyberIconGenerator.BUILTIN_STARK_QUANTUM ||
                pkg == CyberIconGenerator.BUILTIN_JARVIS_TACTICAL ||
                pkg == CyberIconGenerator.BUILTIN_VIBRANIUM_STEALTH ||
                pkg == CyberIconGenerator.BUILTIN_ARC_REACTOR ||
                pkg == CyberIconGenerator.BUILTIN_CUSTOM_SDCARD
            )
        }

        /**
         * Normalizes component string:
         * "ComponentInfo{com.pkg/com.pkg.Activity}" -> "com.pkg/com.pkg.Activity"
         */
        fun normalizeComponentString(raw: String): String? {
            val start = raw.indexOf('{')
            val end = raw.indexOf('}')
            val content = if (start != -1 && end != -1 && end > start) {
                raw.substring(start + 1, end).trim()
            } else {
                raw.trim()
            }

            return if (content.contains('/')) content else null
        }
    }

    private val packageManager: PackageManager = context.packageManager
    private val componentToDrawableMap = ConcurrentHashMap<String, String>()
    private var currentPackPackage: String? = null
    private var currentPackResources: Resources? = null

    /**
     * Queries all built-in and installed applications that declare icon pack intent filters.
     */
    fun getInstalledIconPacks(): List<IconPackInfo> {
        val result = mutableListOf<IconPackInfo>()

        // 1. Default Marvel HUD Insignia
        result.add(
            IconPackInfo(
                packageName = "",
                label = "Marvel HUD Insignia (Default)",
                isDefault = true,
                description = "Standard system icons framed with holographic Marvel hero HUD brackets"
            )
        )

        // 2. Built-in Marvel Stark OS Quantum Icon Packs
        result.add(
            IconPackInfo(
                packageName = CyberIconGenerator.BUILTIN_STARK_QUANTUM,
                label = "⚡ Stark Quantum HUD",
                isBuiltIn = true,
                description = "Full vector cyber plates with glowing cyan & arc gold conduits"
            )
        )
        result.add(
            IconPackInfo(
                packageName = CyberIconGenerator.BUILTIN_JARVIS_TACTICAL,
                label = "🔷 J.A.R.V.I.S. Tactical",
                isBuiltIn = true,
                description = "Tactical holographic azure interface with amber radar accents"
            )
        )
        result.add(
            IconPackInfo(
                packageName = CyberIconGenerator.BUILTIN_VIBRANIUM_STEALTH,
                label = "🟣 Vibranium Stealth",
                isBuiltIn = true,
                description = "Dark matte obsidian armor geometry with royal amethyst glow"
            )
        )
        result.add(
            IconPackInfo(
                packageName = CyberIconGenerator.BUILTIN_ARC_REACTOR,
                label = "🔴 Arc Reactor Overcharge",
                isBuiltIn = true,
                description = "Overcharged scarlet arc conduits with solar gold nodes"
            )
        )
        result.add(
            IconPackInfo(
                packageName = CyberIconGenerator.BUILTIN_CUSTOM_SDCARD,
                label = "📁 Custom AI Studio Pack",
                isBuiltIn = true,
                description = "Picks custom generated PNGs from /sdcard/Download/moto_launcher_iconpack"
            )
        )

        // 3. Installed Third-Party Icon Packs (Nova, Apex, Total Launcher compatible)
        val seenPackages = mutableSetOf<String>()

        for (action in ICON_PACK_INTENT_ACTIONS) {
            val intent = Intent(action)
            val activities = packageManager.queryIntentActivities(intent, PackageManager.GET_META_DATA)
            for (resolveInfo in activities) {
                val pkgName = resolveInfo.activityInfo.packageName
                if (pkgName != context.packageName && seenPackages.add(pkgName)) {
                    val label = resolveInfo.loadLabel(packageManager)?.toString() ?: pkgName
                    result.add(
                        IconPackInfo(
                            packageName = pkgName,
                            label = label,
                            isDefault = false,
                            isBuiltIn = false,
                            description = "Installed third-party icon pack ($pkgName)"
                        )
                    )
                }
            }
        }

        return result
    }

    /**
     * Loads and parses icon pack mappings (or prepares built-in packs).
     */
    suspend fun loadIconPack(iconPackPackage: String?): Boolean = withContext(Dispatchers.IO) {
        if (iconPackPackage.isNullOrBlank()) {
            currentPackPackage = null
            currentPackResources = null
            componentToDrawableMap.clear()
            return@withContext true
        }

        // Built-in Marvel Icon Packs
        if (isBuiltInPack(iconPackPackage)) {
            currentPackPackage = iconPackPackage
            currentPackResources = null
            componentToDrawableMap.clear()
            if (iconPackPackage == CyberIconGenerator.BUILTIN_CUSTOM_SDCARD) {
                loadSdcardAppFilter()
            }
            Log.i(TAG, "Activated built-in icon pack: $iconPackPackage")
            return@withContext true
        }

        if (currentPackPackage == iconPackPackage && currentPackResources != null) {
            return@withContext true
        }

        try {
            val resources = packageManager.getResourcesForApplication(iconPackPackage)
            val map = parseAppFilter(resources, iconPackPackage)

            componentToDrawableMap.clear()
            componentToDrawableMap.putAll(map)
            currentPackPackage = iconPackPackage
            currentPackResources = resources

            Log.i(TAG, "Loaded icon pack '$iconPackPackage' with ${map.size} icon mappings")
            true
        } catch (e: Throwable) {
            Log.e(TAG, "Failed loading icon pack '$iconPackPackage': ${e.message}", e)
            currentPackPackage = null
            currentPackResources = null
            componentToDrawableMap.clear()
            false
        }
    }

    /**
     * Loads custom appfilter.xml from SD card if present.
     */
    private fun loadSdcardAppFilter() {
        val dir = File(CUSTOM_ICONPACK_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val appFilterFile = File(dir, "appfilter.xml")
        if (!appFilterFile.exists() || !appFilterFile.canRead()) return
        var inputStream: InputStream? = null
        try {
            inputStream = FileInputStream(appFilterFile)
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setInput(inputStream, "UTF-8")
            val map = parseFromXmlPullParser(parser)
            componentToDrawableMap.putAll(map)
            Log.i(TAG, "Loaded ${map.size} custom icon mappings from $CUSTOM_ICONPACK_DIR/appfilter.xml")
        } catch (e: Throwable) {
            Log.w(TAG, "Failed loading custom appfilter.xml: ${e.message}")
        } finally {
            try { inputStream?.close() } catch (_: Throwable) {}
        }
    }

    /**
     * Parses `appfilter.xml` from assets or XML resource folder.
     */
    internal fun parseAppFilter(resources: Resources, iconPackPackage: String): Map<String, String> {
        val mapping = mutableMapOf<String, String>()

        var inputStream: InputStream? = null
        try {
            // 1. Try reading from APK assets
            try {
                inputStream = resources.assets.open("appfilter.xml")
            } catch (_: Throwable) {
                // 2. Try XML resource identifier
                val xmlId = resources.getIdentifier("appfilter", "xml", iconPackPackage)
                if (xmlId != 0) {
                    val parser = resources.getXml(xmlId)
                    return parseFromXmlPullParser(parser)
                }
            }

            if (inputStream != null) {
                val factory = XmlPullParserFactory.newInstance()
                factory.isNamespaceAware = false
                val parser = factory.newPullParser()
                parser.setInput(inputStream, "UTF-8")
                return parseFromXmlPullParser(parser)
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Could not open appfilter.xml from $iconPackPackage: ${e.message}")
        } finally {
            try {
                inputStream?.close()
            } catch (_: Throwable) {}
        }

        return mapping
    }

    /**
     * Extracts component -> drawable mappings from an XmlPullParser.
     */
    internal fun parseFromXmlPullParser(parser: XmlPullParser): Map<String, String> {
        val mapping = mutableMapOf<String, String>()
        var eventType = parser.eventType

        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name.equals("item", ignoreCase = true)) {
                val component = parser.getAttributeValue(null, "component")
                val drawable = parser.getAttributeValue(null, "drawable")

                if (!component.isNullOrBlank() && !drawable.isNullOrBlank()) {
                    val normalizedKey = normalizeComponentString(component)
                    if (normalizedKey != null) {
                        mapping[normalizedKey] = drawable
                    }
                }
            }
            eventType = parser.next()
        }

        return mapping
    }

    /**
     * Resolves an app's icon Drawable from the active icon pack.
     * Returns null if default insignia is active, allowing standard icon loader fallback.
     */
    fun getAppIcon(packageName: String, activityName: String): Drawable? {
        val packPkg = currentPackPackage ?: return null

        // 1. Custom AI Studio Pack from SD Card
        if (packPkg == CyberIconGenerator.BUILTIN_CUSTOM_SDCARD) {
            val key = "$packageName/$activityName"
            val drawableName = componentToDrawableMap[key] ?: componentToDrawableMap[packageName]
            if (drawableName != null) {
                val customFile = File(CUSTOM_ICONPACK_DIR, "$drawableName.png")
                if (customFile.exists() && customFile.canRead()) {
                    val bmp = BitmapFactory.decodeFile(customFile.absolutePath)
                    if (bmp != null) return BitmapDrawable(context.resources, bmp)
                }
            }

            val pkgFile = File(CUSTOM_ICONPACK_DIR, "$packageName.png")
            if (pkgFile.exists() && pkgFile.canRead()) {
                val bmp = BitmapFactory.decodeFile(pkgFile.absolutePath)
                if (bmp != null) return BitmapDrawable(context.resources, bmp)
            }

            val actFile = File(CUSTOM_ICONPACK_DIR, "$activityName.png")
            if (actFile.exists() && actFile.canRead()) {
                val bmp = BitmapFactory.decodeFile(actFile.absolutePath)
                if (bmp != null) return BitmapDrawable(context.resources, bmp)
            }

            // Fallback to procedural custom studio icon
            return CyberIconGenerator.generateIcon(context, packageName, activityName, packPkg)
        }

        // 2. Built-in Marvel Cyber HUD Icon Packs
        if (isBuiltInPack(packPkg)) {
            return CyberIconGenerator.generateIcon(context, packageName, activityName, packPkg)
        }

        // 3. Third-party APK Icon Pack
        val packRes = currentPackResources ?: return null
        val key = "$packageName/$activityName"
        val drawableName = componentToDrawableMap[key] ?: componentToDrawableMap[packageName] ?: return null

        return try {
            val resId = packRes.getIdentifier(drawableName, "drawable", packPkg)
            if (resId != 0) {
                packRes.getDrawable(resId, null)
            } else {
                null
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Failed resolving drawable '$drawableName' in pack '$packPkg': ${e.message}")
            null
        }
    }

    /**
     * Currently active icon pack package (or null if default).
     */
    val activePackPackage: String?
        get() = currentPackPackage
}
