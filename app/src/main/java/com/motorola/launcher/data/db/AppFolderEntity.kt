package com.motorola.launcher.data.db

/**
 * Represents a customizable application drawer or workspace folder.
 *
 * @property id Unique folder ID.
 * @property name User-visible folder name (e.g. "Productivity", "Media", "Games").
 * @property iconColor Color accent tint (ARGB).
 * @property itemKeys Comma-separated list of componentKeys contained in this folder.
 */
data class AppFolderEntity(
    val id: Long = 0,
    val name: String,
    val iconColor: Int = 0,
    val itemKeys: String = ""
) {
    val keyList: List<String>
        get() = if (itemKeys.isBlank()) emptyList() else itemKeys.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    fun containsKey(componentKey: String): Boolean = keyList.contains(componentKey)

    fun withAddedKey(componentKey: String): AppFolderEntity {
        if (containsKey(componentKey)) return this
        val updated = if (itemKeys.isBlank()) componentKey else "$itemKeys,$componentKey"
        return copy(itemKeys = updated)
    }

    fun withRemovedKey(componentKey: String): AppFolderEntity {
        val updated = keyList.filter { it != componentKey }.joinToString(",")
        return copy(itemKeys = updated)
    }
}
