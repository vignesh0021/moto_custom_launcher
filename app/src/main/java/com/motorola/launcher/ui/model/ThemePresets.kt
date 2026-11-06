package com.motorola.launcher.ui.model

import androidx.compose.ui.graphics.Color

enum class ThemeColorPreset(val id: String, val color: Color) {
    CYAN("cyan", Color(0xFF00E5FF)),
    PURPLE("purple", Color(0xFFB388FF)),
    AMBER("amber", Color(0xFFFFD700)),
    GREEN("green", Color(0xFF00E676)),
    RED("red", Color(0xFFFF1744));

    companion object {
        fun fromId(id: String?): ThemeColorPreset = entries.find { it.id == id } ?: CYAN
    }
}

enum class WallpaperPreset(val id: String, val label: String) {
    MARVEL_HERO("marvel_hero", "Marvel Hero"),
    ARC_REACTOR("arc_reactor", "Arc Reactor"),
    ABSTRACT("abstract", "Abstract"),
    NATURE("nature", "Nature"),
    MITHRA_FAMILY("mithra_family", "Mithra Family");

    companion object {
        fun fromId(id: String?): WallpaperPreset = entries.find { it.id == id } ?: MARVEL_HERO
    }
}
