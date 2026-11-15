package com.motorola.launcher.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.motorola.launcher.ui.model.AnimationLevelPreset
import com.motorola.launcher.ui.model.CustomizationSettings
import com.motorola.launcher.ui.model.IconStylePreset
import com.motorola.launcher.ui.model.ThemeColorPreset
import com.motorola.launcher.ui.model.WallpaperPreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages persistent user preferences for Quick Access, Dock apps,
 * and STARK OS JARVIS Edition theme and UI customization.
 */
class LauncherPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("moto_launcher_prefs", Context.MODE_PRIVATE)

    private val _quickAccessKeys = MutableStateFlow<Set<String>>(loadQuickAccessKeys())
    val quickAccessKeys: StateFlow<Set<String>> = _quickAccessKeys.asStateFlow()

    private val _dockKeys = MutableStateFlow<List<String>>(loadDockKeys())
    val dockKeys: StateFlow<List<String>> = _dockKeys.asStateFlow()

    private val _customization = MutableStateFlow(loadCustomization())
    val customization: StateFlow<CustomizationSettings> = _customization.asStateFlow()

    private fun loadQuickAccessKeys(): Set<String> {
        return prefs.getStringSet(KEY_QUICK_ACCESS, null) ?: emptySet()
    }

    private fun loadDockKeys(): List<String> {
        val raw = prefs.getString(KEY_DOCK, null) ?: return emptyList()
        return raw.split(",").filter { it.isNotBlank() }
    }

    private fun loadCustomization(): CustomizationSettings {
        val themeId = prefs.getString(KEY_THEME_ID, "iron_man") ?: "iron_man"
        val hero = com.motorola.launcher.ui.model.MarvelHeroProtocol.fromId(prefs.getString(KEY_HERO_PROTOCOL, themeId))
        return CustomizationSettings(
            activeThemeId = themeId,
            heroProtocol = hero,
            themeColor = ThemeColorPreset.fromId(prefs.getString(KEY_THEME_COLOR, "cyan")),
            wallpaper = WallpaperPreset.fromId(prefs.getString(KEY_WALLPAPER, "marvel_hero")),
            iconStyle = IconStylePreset.fromId(prefs.getString(KEY_ICON_STYLE, "hero_themed")),
            animationLevel = AnimationLevelPreset.fromId(prefs.getString(KEY_ANIMATION_LEVEL, "smooth")),
            companionStyle = com.motorola.launcher.ui.model.CompanionStyle.fromId(prefs.getString(KEY_COMPANION_STYLE, "hero_suit")),
            showSystemInfo = prefs.getBoolean(KEY_SHOW_SYSTEM_INFO, true),
            enableQuickAccessShelf = prefs.getBoolean(KEY_ENABLE_QUICK_SHELF, true),
            dynamicWallpaper = prefs.getBoolean(KEY_DYNAMIC_WALLPAPER, true),
            hapticFeedback = prefs.getBoolean(KEY_HAPTIC_FEEDBACK, true),
            soundEffectsEnabled = prefs.getBoolean(KEY_SOUND_EFFECTS, false),
            hydrationReminderEnabled = prefs.getBoolean(KEY_HYDRATION_ENABLED, true),
            hydrationIntervalMinutes = prefs.getInt(KEY_HYDRATION_INTERVAL, 60),
            companionVoiceEnabled = prefs.getBoolean(KEY_COMPANION_VOICE, true),
            waterGlassesCount = prefs.getInt(KEY_WATER_GLASSES, 0),
            lastHydrationHour = prefs.getLong(KEY_LAST_HYDRATION_HOUR, 0L)
        )
    }

    fun isQuickAccess(componentKey: String): Boolean {
        return _quickAccessKeys.value.contains(componentKey)
    }

    fun toggleQuickAccess(componentKey: String) {
        val current = _quickAccessKeys.value.toMutableSet()
        if (current.contains(componentKey)) {
            current.remove(componentKey)
        } else {
            current.add(componentKey)
        }
        prefs.edit().putStringSet(KEY_QUICK_ACCESS, current).apply()
        _quickAccessKeys.value = current
    }

    fun setQuickAccessKeys(keys: Set<String>) {
        prefs.edit().putStringSet(KEY_QUICK_ACCESS, keys).apply()
        _quickAccessKeys.value = keys
    }

    fun isDockPinned(componentKey: String): Boolean {
        return _dockKeys.value.contains(componentKey)
    }

    fun toggleDock(componentKey: String) {
        val current = _dockKeys.value.toMutableList()
        if (current.contains(componentKey)) {
            current.remove(componentKey)
        } else {
            if (current.size >= 6) {
                current.removeAt(current.size - 1)
            }
            current.add(componentKey)
        }
        prefs.edit().putString(KEY_DOCK, current.joinToString(",")).apply()
        _dockKeys.value = current
    }

    fun setDockKeys(keys: List<String>) {
        prefs.edit().putString(KEY_DOCK, keys.joinToString(",")).apply()
        _dockKeys.value = keys
    }

    fun applyTheme(theme: com.motorola.launcher.theme.MarvelTheme) {
        prefs.edit()
            .putString(KEY_THEME_ID, theme.id)
            .putString(KEY_HERO_PROTOCOL, theme.heroProtocol.id)
            .apply()
        val matchingTheme = when (theme.heroProtocol) {
            com.motorola.launcher.ui.model.MarvelHeroProtocol.IRON_MAN -> ThemeColorPreset.CYAN
            com.motorola.launcher.ui.model.MarvelHeroProtocol.BLACK_PANTHER -> ThemeColorPreset.PURPLE
            com.motorola.launcher.ui.model.MarvelHeroProtocol.CAPTAIN_AMERICA -> ThemeColorPreset.CYAN
            com.motorola.launcher.ui.model.MarvelHeroProtocol.THOR -> ThemeColorPreset.AMBER
            com.motorola.launcher.ui.model.MarvelHeroProtocol.HULK -> ThemeColorPreset.GREEN
            com.motorola.launcher.ui.model.MarvelHeroProtocol.DOCTOR_STRANGE -> ThemeColorPreset.AMBER
            com.motorola.launcher.ui.model.MarvelHeroProtocol.SPIDER_MAN -> ThemeColorPreset.RED
            com.motorola.launcher.ui.model.MarvelHeroProtocol.BLACK_WIDOW -> ThemeColorPreset.RED
            com.motorola.launcher.ui.model.MarvelHeroProtocol.AVENGERS_UNIFIED -> ThemeColorPreset.CYAN
        }
        _customization.value = _customization.value.copy(
            activeThemeId = theme.id,
            heroProtocol = theme.heroProtocol,
            themeColor = matchingTheme
        )
    }

    fun resetThemeToDefault() {
        applyTheme(com.motorola.launcher.theme.MarvelThemeRegistry.defaultTheme)
    }

    fun updateHeroProtocol(protocol: com.motorola.launcher.ui.model.MarvelHeroProtocol) {
        val theme = com.motorola.launcher.theme.MarvelThemeRegistry.getTheme(protocol.id)
        applyTheme(theme)
    }

    fun updateThemeColor(preset: ThemeColorPreset) {
        prefs.edit().putString(KEY_THEME_COLOR, preset.id).apply()
        _customization.value = _customization.value.copy(themeColor = preset)
    }

    fun updateWallpaper(preset: WallpaperPreset) {
        prefs.edit().putString(KEY_WALLPAPER, preset.id).apply()
        _customization.value = _customization.value.copy(wallpaper = preset)
    }

    fun updateIconStyle(preset: IconStylePreset) {
        prefs.edit().putString(KEY_ICON_STYLE, preset.id).apply()
        _customization.value = _customization.value.copy(iconStyle = preset)
    }

    fun updateAnimationLevel(preset: AnimationLevelPreset) {
        prefs.edit().putString(KEY_ANIMATION_LEVEL, preset.id).apply()
        _customization.value = _customization.value.copy(animationLevel = preset)
    }

    fun toggleShowSystemInfo(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_SYSTEM_INFO, enabled).apply()
        _customization.value = _customization.value.copy(showSystemInfo = enabled)
    }

    fun toggleQuickAccessShelf(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLE_QUICK_SHELF, enabled).apply()
        _customization.value = _customization.value.copy(enableQuickAccessShelf = enabled)
    }

    fun toggleDynamicWallpaper(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DYNAMIC_WALLPAPER, enabled).apply()
        _customization.value = _customization.value.copy(dynamicWallpaper = enabled)
    }

    fun toggleHapticFeedback(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HAPTIC_FEEDBACK, enabled).apply()
        _customization.value = _customization.value.copy(hapticFeedback = enabled)
    }

    fun recordWaterAccepted() {
        val count = _customization.value.waterGlassesCount + 1
        val now = System.currentTimeMillis()
        prefs.edit()
            .putInt(KEY_WATER_GLASSES, count)
            .putLong(KEY_LAST_HYDRATION_HOUR, now)
            .apply()
        _customization.value = _customization.value.copy(
            waterGlassesCount = count,
            lastHydrationHour = now
        )
    }

    fun recordWaterRefused() {
        val now = System.currentTimeMillis()
        prefs.edit()
            .putLong(KEY_LAST_HYDRATION_HOUR, now)
            .apply()
        _customization.value = _customization.value.copy(
            lastHydrationHour = now
        )
    }

    fun toggleHydrationReminder(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HYDRATION_ENABLED, enabled).apply()
        _customization.value = _customization.value.copy(hydrationReminderEnabled = enabled)
    }

    fun updateHydrationInterval(minutes: Int) {
        val validMinutes = minutes.coerceIn(5, 360)
        prefs.edit().putInt(KEY_HYDRATION_INTERVAL, validMinutes).apply()
        _customization.value = _customization.value.copy(hydrationIntervalMinutes = validMinutes)
    }

    fun updateCompanionStyle(style: com.motorola.launcher.ui.model.CompanionStyle) {
        prefs.edit().putString(KEY_COMPANION_STYLE, style.id).apply()
        _customization.value = _customization.value.copy(companionStyle = style)
    }

    fun toggleCompanionVoice(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_COMPANION_VOICE, enabled).apply()
        _customization.value = _customization.value.copy(companionVoiceEnabled = enabled)
    }

    fun toggleSoundEffects(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SOUND_EFFECTS, enabled).apply()
        _customization.value = _customization.value.copy(soundEffectsEnabled = enabled)
    }

    companion object {
        private const val KEY_THEME_ID = "theme_id"
        private const val KEY_SOUND_EFFECTS = "sound_effects_enabled"
        private const val KEY_QUICK_ACCESS = "quick_access_keys"
        private const val KEY_DOCK = "dock_keys"
        private const val KEY_HERO_PROTOCOL = "hero_protocol"
        private const val KEY_THEME_COLOR = "theme_color"
        private const val KEY_WALLPAPER = "wallpaper_preset"
        private const val KEY_ICON_STYLE = "icon_style"
        private const val KEY_ANIMATION_LEVEL = "animation_level"
        private const val KEY_COMPANION_STYLE = "companion_style"
        private const val KEY_SHOW_SYSTEM_INFO = "show_system_info"
        private const val KEY_ENABLE_QUICK_SHELF = "enable_quick_shelf"
        private const val KEY_DYNAMIC_WALLPAPER = "dynamic_wallpaper"
        private const val KEY_HAPTIC_FEEDBACK = "haptic_feedback"
        private const val KEY_HYDRATION_ENABLED = "hydration_enabled"
        private const val KEY_HYDRATION_INTERVAL = "hydration_interval_minutes"
        private const val KEY_COMPANION_VOICE = "companion_voice_enabled"
        private const val KEY_WATER_GLASSES = "water_glasses"
        private const val KEY_LAST_HYDRATION_HOUR = "last_hydration_hour"
    }

    // Home pages (component keys per page)
    private val _homePages = MutableStateFlow<List<List<String>>>(loadHomePages())
    val homePages: StateFlow<List<List<String>>> = _homePages.asStateFlow()

    // Drawer folders: folderName -> set of componentKeys
    private val _folders = MutableStateFlow<Map<String, Set<String>>>(loadFolders())
    val folders: StateFlow<Map<String, Set<String>>> = _folders.asStateFlow()

    // Hidden apps
    private val _hiddenApps = MutableStateFlow<Set<String>>(loadHiddenApps())
    val hiddenApps: StateFlow<Set<String>> = _hiddenApps.asStateFlow()

    // Notification badge counts (package -> count)
    private val _badgeCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val badgeCounts: StateFlow<Map<String, Int>> = _badgeCounts.asStateFlow()

    private fun loadHomePages(): List<List<String>> {
        val raw = prefs.getString("home_pages", null) ?: return listOf(emptyList())
        return raw.split("||").map { it.split("|").filter { k -> k.isNotBlank() } }
    }

    private fun loadFolders(): Map<String, Set<String>> {
        val raw = prefs.getString("folders", null) ?: return emptyMap()
        return raw.split("||").mapNotNull { entry ->
            val parts = entry.split(":")
            if (parts.size == 2) parts[0] to parts[1].split("|").toSet() else null
        }.toMap()
    }

    private fun loadHiddenApps(): Set<String> {
        return prefs.getStringSet("hidden_apps", emptySet()) ?: emptySet()
    }

    fun setHomePage(pageIndex: Int, keys: List<String>) {
        val current = _homePages.value.toMutableList()
        while (current.size <= pageIndex) current.add(emptyList())
        current[pageIndex] = keys
        _homePages.value = current
        prefs.edit().putString("home_pages", current.joinToString("||") { it.joinToString("|") }).apply()
    }

    fun addFolder(name: String, keys: Set<String>) {
        val updated = _folders.value.toMutableMap()
        updated[name] = keys
        _folders.value = updated
        saveFolders(updated)
    }

    fun toggleHidden(componentKey: String) {
        val current = _hiddenApps.value.toMutableSet()
        if (componentKey in current) current.remove(componentKey) else current.add(componentKey)
        _hiddenApps.value = current
        prefs.edit().putStringSet("hidden_apps", current).apply()
    }

    fun updateBadge(packageName: String, count: Int) {
        val updated = _badgeCounts.value.toMutableMap()
        if (count <= 0) updated.remove(packageName) else updated[packageName] = count
        _badgeCounts.value = updated
    }

    private fun saveFolders(map: Map<String, Set<String>>) {
        val raw = map.entries.joinToString("||") { "${it.key}:${it.value.joinToString("|")}" }
        prefs.edit().putString("folders", raw).apply()
    }

    fun exportBackup(): String {
        return prefs.all.entries.joinToString("
") { "${it.key}=${it.value}" }
    }

    fun importBackup(data: String) {
        val editor = prefs.edit()
        data.lines().forEach { line ->
            val idx = line.indexOf('=')
            if (idx > 0) {
                val k = line.substring(0, idx)
                val v = line.substring(idx + 1)
                editor.putString(k, v)
            }
        }
        editor.apply()
        // Reload states (simplified)
        _homePages.value = loadHomePages()
        _folders.value = loadFolders()
        _hiddenApps.value = loadHiddenApps()
    }

}
