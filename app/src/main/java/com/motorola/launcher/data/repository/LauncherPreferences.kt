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
        val hero = com.motorola.launcher.ui.model.MarvelHeroProtocol.fromId(prefs.getString(KEY_HERO_PROTOCOL, "iron_man"))
        return CustomizationSettings(
            heroProtocol = hero,
            themeColor = ThemeColorPreset.fromId(prefs.getString(KEY_THEME_COLOR, "cyan")),
            wallpaper = WallpaperPreset.fromId(prefs.getString(KEY_WALLPAPER, "system")),
            iconStyle = IconStylePreset.fromId(prefs.getString(KEY_ICON_STYLE, "clean")),
            animationLevel = AnimationLevelPreset.fromId(prefs.getString(KEY_ANIMATION_LEVEL, "smooth")),
            showSystemInfo = prefs.getBoolean(KEY_SHOW_SYSTEM_INFO, true),
            enableQuickAccessShelf = prefs.getBoolean(KEY_ENABLE_QUICK_SHELF, true),
            dynamicWallpaper = prefs.getBoolean(KEY_DYNAMIC_WALLPAPER, true),
            hapticFeedback = prefs.getBoolean(KEY_HAPTIC_FEEDBACK, true)
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
            if (current.size >= 5) {
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

    fun updateHeroProtocol(protocol: com.motorola.launcher.ui.model.MarvelHeroProtocol) {
        prefs.edit().putString(KEY_HERO_PROTOCOL, protocol.id).apply()
        val matchingTheme = when (protocol) {
            com.motorola.launcher.ui.model.MarvelHeroProtocol.IRON_MAN -> ThemeColorPreset.CYAN
            com.motorola.launcher.ui.model.MarvelHeroProtocol.BLACK_PANTHER -> ThemeColorPreset.PURPLE
            com.motorola.launcher.ui.model.MarvelHeroProtocol.CAPTAIN_AMERICA -> ThemeColorPreset.CYAN
            com.motorola.launcher.ui.model.MarvelHeroProtocol.THOR -> ThemeColorPreset.AMBER
            com.motorola.launcher.ui.model.MarvelHeroProtocol.DOCTOR_STRANGE -> ThemeColorPreset.AMBER
            com.motorola.launcher.ui.model.MarvelHeroProtocol.SPIDER_MAN -> ThemeColorPreset.RED
        }
        _customization.value = _customization.value.copy(heroProtocol = protocol, themeColor = matchingTheme)
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

    companion object {
        private const val KEY_QUICK_ACCESS = "quick_access_keys"
        private const val KEY_DOCK = "dock_keys"
        private const val KEY_HERO_PROTOCOL = "hero_protocol"
        private const val KEY_THEME_COLOR = "theme_color"
        private const val KEY_WALLPAPER = "wallpaper_preset"
        private const val KEY_ICON_STYLE = "icon_style"
        private const val KEY_ANIMATION_LEVEL = "animation_level"
        private const val KEY_SHOW_SYSTEM_INFO = "show_system_info"
        private const val KEY_ENABLE_QUICK_SHELF = "enable_quick_shelf"
        private const val KEY_DYNAMIC_WALLPAPER = "dynamic_wallpaper"
        private const val KEY_HAPTIC_FEEDBACK = "haptic_feedback"
    }
}
