package com.motorola.launcher.data.icon

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * Representation of an installed third-party icon pack (Nova, ADW, Apex, Go, Total Launcher compatible).
 */
@Immutable
data class IconPackInfo(
    val packageName: String,
    val label: String,
    val isDefault: Boolean = false,
    val iconBitmap: Bitmap? = null
)

/**
 * High-performance, zero-allocation icon pack parser and resource resolver.
 * Parses standard `appfilter.xml` from target icon pack APK and extracts drawable mappings.
 */
class IconPackManager(private val context: Context) {

    companion object {
        private const val TAG = "IconPackManager"

        val ICON_PACK_INTENT_ACTIONS = listOf(
            "com.novalauncher.THEME",
            "org.adw.launcher.THEMES",
            "com.anddoes.launcher.THEME",
            "com.teslacoilsw.launcher.THEME",
            "com.fede.launcher.THEME_ICONPACK",
            "com.gau.go.launcherex.theme"
        )

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
     * Queries all installed applications that declare icon pack intent filters.
     */
    fun getInstalledIconPacks(): List<IconPackInfo> {
        val result = mutableListOf<IconPackInfo>()

        // Default Marvel HUD insignia entry
        result.add(
            IconPackInfo(
                packageName = "",
                label = "Marvel HUD Insignia (Default)",
                isDefault = true
            )
        )

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
                            isDefault = false
                        )
                    )
                }
            }
        }

        return result
    }

    /**
     * Loads and parses `appfilter.xml` for the given icon pack package.
     */
    suspend fun loadIconPack(iconPackPackage: String?): Boolean = withContext(Dispatchers.IO) {
        if (iconPackPackage.isNullOrBlank()) {
            currentPackPackage = null
            currentPackResources = null
            componentToDrawableMap.clear()
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
                    // Extract ComponentInfo{com.example/com.example.MainActivity}
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
     * Returns null if no custom icon pack is active or no mapping exists.
     */
    fun getAppIcon(packageName: String, activityName: String): Drawable? {
        val packRes = currentPackResources ?: return null
        val packPkg = currentPackPackage ?: return null

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
