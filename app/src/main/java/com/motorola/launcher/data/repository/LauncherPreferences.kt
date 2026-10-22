package com.motorola.launcher.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages persistent user preferences for Quick Access and Dock apps.
 */
class LauncherPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("moto_launcher_prefs", Context.MODE_PRIVATE)

    private val _quickAccessKeys = MutableStateFlow<Set<String>>(loadQuickAccessKeys())
    val quickAccessKeys: StateFlow<Set<String>> = _quickAccessKeys.asStateFlow()

    private val _dockKeys = MutableStateFlow<List<String>>(loadDockKeys())
    val dockKeys: StateFlow<List<String>> = _dockKeys.asStateFlow()

    private fun loadQuickAccessKeys(): Set<String> {
        return prefs.getStringSet(KEY_QUICK_ACCESS, null) ?: emptySet()
    }

    private fun loadDockKeys(): List<String> {
        val raw = prefs.getString(KEY_DOCK, null) ?: return emptyList()
        return raw.split(",").filter { it.isNotBlank() }
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

    companion object {
        private const val KEY_QUICK_ACCESS = "quick_access_keys"
        private const val KEY_DOCK = "dock_keys"
    }
}
