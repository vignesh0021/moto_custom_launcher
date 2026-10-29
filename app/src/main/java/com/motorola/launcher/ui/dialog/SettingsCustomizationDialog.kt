package com.motorola.launcher.ui.dialog

import android.content.Intent
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.motorola.launcher.data.repository.LauncherPreferences
import com.motorola.launcher.theme.MarvelTheme
import com.motorola.launcher.theme.MarvelThemeRegistry
import com.motorola.launcher.ui.model.AnimationLevelPreset
import com.motorola.launcher.ui.model.CoreGeometry
import com.motorola.launcher.ui.model.CustomizationSettings
import com.motorola.launcher.ui.model.IconStylePreset
import com.motorola.launcher.ui.model.ThemeColorPreset
import com.motorola.launcher.ui.model.WallpaperPreset
import com.motorola.launcher.ui.theme.LocalCustomization
import com.motorola.launcher.ui.theme.LocalMarvelTheme
import com.motorola.launcher.ui.theme.StarkCyan
import com.motorola.launcher.ui.theme.StarkDarkSurface
import com.motorola.launcher.ui.theme.StarkHudBorder
import com.motorola.launcher.ui.theme.StarkHudBorderBright
import com.motorola.launcher.ui.theme.TextDimHud
import com.motorola.launcher.ui.theme.TextPrimary
import com.motorola.launcher.ui.theme.TextSecondary

/**
 * Screen 5: Settings & Customization modal ("MAKE IT YOURS").
 * Offers live selection of Theme Colors, Wallpapers, Icon Styles, Animation Levels,
 * and core feature switches with instant persistent updates.
 */
@Composable
fun SettingsCustomizationDialog(
    preferences: LauncherPreferences,
    customization: CustomizationSettings,
    onDismiss: () -> Unit,
    onTriggerHydrationTest: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val context = LocalContext.current
    val currentTheme = LocalMarvelTheme.current
    val themeColor = customization.themeColor
    val scrollState = rememberScrollState()

    var previewThemeId by remember { mutableStateOf(customization.activeThemeId.ifBlank { customization.heroProtocol.id }) }
    val previewTheme = remember(previewThemeId) { MarvelThemeRegistry.getTheme(previewThemeId) }
    val allThemes = remember { MarvelThemeRegistry.getAllThemes() }

    Dialog(onDismissRequest = onDismiss) {
        val sheetShape = RoundedCornerShape(20.dp)

        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(sheetShape)
                .background(currentTheme.colors.surface)
                .border(1.5.dp, currentTheme.colors.hudBorderBright, sheetShape)
                .padding(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Text(
                    text = "SETTINGS & CUSTOMIZATION",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(bottom = 14.dp)
                )

                // 0. MARVEL THEME GALLERY (Full Extensible Superhero Themes)
                CustomizationSection(title = "MARVEL UNIVERSE THEME GALLERY") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Select a superhero theme to preview signature geometry, HUD tokens, wallpapers, and colors.",
                            color = TextSecondary,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.SansSerif,
                            lineHeight = 13.sp
                        )

                        // Theme Selection Grid (3 per row)
                        for (rowThemes in allThemes.chunked(3)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (theme in rowThemes) {
                                    val isCurrent = customization.activeThemeId.equals(theme.id, ignoreCase = true)
                                    val isSelectedForPreview = previewThemeId.equals(theme.id, ignoreCase = true)

                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                when {
                                                    isSelectedForPreview -> theme.colors.subtle
                                                    isCurrent -> theme.colors.primary.copy(alpha = 0.15f)
                                                    else -> Color(0x1F050A14)
                                                }
                                            )
                                            .border(
                                                width = if (isSelectedForPreview) 2.dp else if (isCurrent) 1.5.dp else 1.dp,
                                                color = when {
                                                    isSelectedForPreview -> theme.colors.bright
                                                    isCurrent -> theme.colors.primary
                                                    else -> StarkHudBorder
                                                },
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null,
                                                onClick = {
                                                    if (customization.hapticFeedback) {
                                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                                    }
                                                    previewThemeId = theme.id
                                                }
                                            )
                                            .padding(vertical = 8.dp, horizontal = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        // Mini Insignia Preview
                                        MiniThemeInsignia(
                                            geometry = theme.coreGeometry,
                                            primary = theme.colors.primary,
                                            secondary = theme.colors.secondary,
                                            modifier = Modifier.size(28.dp)
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = theme.name,
                                            color = if (isSelectedForPreview || isCurrent) theme.colors.bright else TextSecondary,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = if (isSelectedForPreview || isCurrent) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 1
                                        )

                                        // Status Pill
                                        if (isCurrent) {
                                            Text(
                                                text = "ACTIVE",
                                                color = theme.colors.primary,
                                                fontSize = 7.5.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.5.sp
                                            )
                                        } else if (isSelectedForPreview) {
                                            Text(
                                                text = "PREVIEW",
                                                color = theme.colors.bright,
                                                fontSize = 7.5.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.5.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // PREVIEW SHOWCASE & APPLY/RESET CONTROLS
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(previewTheme.colors.surfaceCard)
                                .border(1.dp, previewTheme.colors.primary, RoundedCornerShape(12.dp))
                                .padding(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    MiniThemeInsignia(
                                        geometry = previewTheme.coreGeometry,
                                        primary = previewTheme.colors.primary,
                                        secondary = previewTheme.colors.secondary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = previewTheme.name.uppercase(),
                                            color = TextPrimary,
                                            fontSize = 11.5.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = previewTheme.protocolTitle,
                                            color = previewTheme.colors.primary,
                                            fontSize = 8.5.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = "HUD: ${previewTheme.hudConfig.statusBadge}",
                                            color = previewTheme.colors.bright,
                                            fontSize = 8.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                // Apply and Reset Controls
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Apply Button
                                    Box(
                                        modifier = Modifier
                                            .weight(1.4f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(previewTheme.colors.primary)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null,
                                                onClick = {
                                                    if (customization.hapticFeedback) {
                                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                                    }
                                                    preferences.applyTheme(previewTheme)
                                                }
                                            )
                                            .padding(vertical = 9.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (customization.activeThemeId.equals(previewTheme.id, ignoreCase = true)) "✓ CURRENT THEME" else "APPLY THEME",
                                            color = Color.Black,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )
                                    }

                                    // Reset Default Button
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0x1F000000))
                                            .border(1.dp, StarkHudBorder, RoundedCornerShape(8.dp))
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null,
                                                onClick = {
                                                    if (customization.hapticFeedback) {
                                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                                    }
                                                    preferences.resetThemeToDefault()
                                                    previewThemeId = MarvelThemeRegistry.defaultTheme.id
                                                }
                                            )
                                            .padding(vertical = 9.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "RESET DEFAULT",
                                            color = TextDimHud,
                                            fontSize = 9.5.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 1. THEME COLOR Selection
                CustomizationSection(title = "THEME COLOR ACCENT") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (preset in ThemeColorPreset.entries) {
                            val isSelected = customization.themeColor == preset
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = {
                                            if (customization.hapticFeedback) {
                                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                            }
                                            preferences.updateThemeColor(preset)
                                        }
                                    )
                                    .padding(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(preset.primary)
                                        .border(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) Color.White else StarkHudBorder,
                                            shape = CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = preset.displayName,
                                    color = if (isSelected) preset.primary else TextSecondary,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2. WALLPAPER Selection
                CustomizationSection(title = "WALLPAPER") {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (rowWallpapers in WallpaperPreset.entries.chunked(5)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                for (wallpaper in rowWallpapers) {
                                    val isSelected = customization.wallpaper == wallpaper
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) themeColor.subtle else Color(0x1F00E5FF))
                                            .border(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) themeColor.primary else StarkHudBorder,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null,
                                                onClick = {
                                                    if (customization.hapticFeedback) {
                                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                                    }
                                                    preferences.updateWallpaper(wallpaper)
                                                }
                                            )
                                            .padding(vertical = 8.dp, horizontal = 2.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Brush.verticalGradient(listOf(wallpaper.top, wallpaper.bottom))),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (wallpaper == WallpaperPreset.SYSTEM) {
                                                Text(text = "🖼", fontSize = 12.sp)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = wallpaper.displayName.split(" ").first(),
                                            color = if (isSelected) themeColor.bright else TextSecondary,
                                            fontSize = 8.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (customization.wallpaper == WallpaperPreset.SYSTEM) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, themeColor.primary, RoundedCornerShape(8.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    try {
                                        val chooser = Intent.createChooser(
                                            Intent(Intent.ACTION_SET_WALLPAPER),
                                            "Choose wallpaper"
                                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(chooser)
                                    } catch (_: Exception) {
                                        // No wallpaper picker available on this device
                                    }
                                }
                            )
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "CHANGE MY PHOTO…",
                            color = themeColor.primary,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. ICON STYLE Selection
            CustomizationSection(title = "ICON STYLE") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (rowStyles in IconStylePreset.entries.chunked(3)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            for (style in rowStyles) {
                                val isSelected = customization.iconStyle == style
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (isSelected) themeColor.primary else Color(0x1F00E5FF))
                                        .border(1.dp, if (isSelected) themeColor.bright else StarkHudBorder, RoundedCornerShape(14.dp))
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = {
                                                if (customization.hapticFeedback) {
                                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                                }
                                                preferences.updateIconStyle(style)
                                            }
                                        )
                                        .padding(vertical = 7.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = style.displayName,
                                        color = if (isSelected) Color.Black else TextSecondary,
                                        fontSize = 9.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. ANIMATION LEVEL Selection
            CustomizationSection(title = "ANIMATION LEVEL") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (level in AnimationLevelPreset.entries) {
                        val isSelected = customization.animationLevel == level
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) themeColor.primary else Color(0x1F00E5FF))
                                .border(1.dp, if (isSelected) themeColor.bright else StarkHudBorder, RoundedCornerShape(16.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        if (customization.hapticFeedback) {
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        }
                                        preferences.updateAnimationLevel(level)
                                    }
                                )
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = level.displayName,
                                color = if (isSelected) Color.Black else TextSecondary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 5. Tactical Feature Switches
            CustomizationSection(title = "PREFERENCES & TOGGLES") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    PreferenceToggleRow(
                        label = "Water Reminder (Marvel Companion)",
                        checked = customization.hydrationReminderEnabled,
                        onCheckedChange = { preferences.toggleHydrationReminder(it) }
                    )
                    if (customization.hydrationReminderEnabled) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "REMINDER INTERVAL",
                            color = TextDimHud,
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                15 to "15M",
                                30 to "30M",
                                45 to "45M",
                                60 to "1 HR",
                                120 to "2 HR"
                            ).forEach { (minutes, label) ->
                                val isSelected = customization.hydrationIntervalMinutes == minutes
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) themeColor.subtle else Color(0x10FFFFFF))
                                        .border(
                                            1.dp,
                                            if (isSelected) themeColor.primary else Color(0x28FFFFFF),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .clickable {
                                            if (customization.hapticFeedback) {
                                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                            }
                                            preferences.updateHydrationInterval(minutes)
                                        }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) themeColor.bright else TextSecondary,
                                        fontSize = 9.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "COMPANION OUTFIT",
                            color = TextDimHud,
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            com.motorola.launcher.ui.model.CompanionStyle.entries.forEach { style ->
                                val isSelected = customization.companionStyle == style
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) themeColor.subtle else Color(0x10FFFFFF))
                                        .border(
                                            1.dp,
                                            if (isSelected) themeColor.primary else Color(0x28FFFFFF),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .clickable {
                                            if (customization.hapticFeedback) {
                                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                            }
                                            preferences.updateCompanionStyle(style)
                                        }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = style.displayName,
                                        color = if (isSelected) themeColor.bright else TextSecondary,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        PreferenceToggleRow(
                            label = "Companion Voice Notifications",
                            checked = customization.companionVoiceEnabled,
                            onCheckedChange = { preferences.toggleCompanionVoice(it) }
                        )
                    }
                    PreferenceToggleRow(
                        label = "Show System Info",
                        checked = customization.showSystemInfo,
                        onCheckedChange = { preferences.toggleShowSystemInfo(it) }
                    )
                    PreferenceToggleRow(
                        label = "Enable Quick Access Shelf",
                        checked = customization.enableQuickAccessShelf,
                        onCheckedChange = { preferences.toggleQuickAccessShelf(it) }
                    )
                    PreferenceToggleRow(
                        label = "Dynamic Wallpaper",
                        checked = customization.dynamicWallpaper,
                        onCheckedChange = { preferences.toggleDynamicWallpaper(it) }
                    )
                    PreferenceToggleRow(
                        label = "Haptic Feedback",
                        checked = customization.hapticFeedback,
                        onCheckedChange = { preferences.toggleHapticFeedback(it) }
                    )
                    PreferenceToggleRow(
                        label = "Theme Sound Effects",
                        checked = customization.soundEffectsEnabled,
                        onCheckedChange = { preferences.toggleSoundEffects(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Companion Test Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x2200E5FF))
                    .border(1.dp, themeColor.primary, RoundedCornerShape(8.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            if (customization.hapticFeedback) {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            }
                            onDismiss()
                            onTriggerHydrationTest()
                        }
                    )
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "💧 MEET PIXEL FRIEND (TEST COMPANION)",
                    color = themeColor.bright,
                    fontSize = 10.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

                Spacer(modifier = Modifier.height(16.dp))

                // Close Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(themeColor.subtle)
                        .border(1.dp, themeColor.primary, RoundedCornerShape(8.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                if (customization.hapticFeedback) {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                }
                                onDismiss()
                            }
                        )
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "APPLY & CLOSE",
                        color = themeColor.primary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.6.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomizationSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x14050B16))
            .border(1.dp, StarkHudBorder, RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Text(
            text = title,
            color = TextDimHud,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        content()
    }
}

@Composable
private fun PreferenceToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val customization = LocalCustomization.current
    val themeColor = customization.themeColor

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = TextPrimary,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = themeColor.primary,
                uncheckedThumbColor = TextDimHud,
                uncheckedTrackColor = Color(0x3300E5FF)
            )
        )
    }
}

@Composable
fun MiniThemeInsignia(
    geometry: CoreGeometry,
    primary: Color,
    secondary: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val radius = minOf(w, h) * 0.42f

        when (geometry) {
            CoreGeometry.ARC_REACTOR -> {
                drawCircle(color = primary.copy(alpha = 0.4f), radius = radius, style = Stroke(width = 1.5.dp.toPx()))
                val path = Path().apply {
                    moveTo(cx, cy + radius * 0.75f)
                    lineTo(cx - radius * 0.7f, cy - radius * 0.55f)
                    lineTo(cx + radius * 0.7f, cy - radius * 0.55f)
                    close()
                }
                drawPath(path, color = primary)
                drawCircle(color = secondary, radius = radius * 0.25f)
            }
            CoreGeometry.VIBRANIUM_WEAVE -> {
                drawCircle(color = primary, radius = radius, style = Stroke(width = 1.5.dp.toPx()))
                drawCircle(color = secondary.copy(alpha = 0.6f), radius = radius * 0.65f, style = Stroke(width = 1.2.dp.toPx()))
                drawCircle(color = primary, radius = radius * 0.3f)
            }
            CoreGeometry.STAR_SHIELD -> {
                drawCircle(color = primary, radius = radius, style = Stroke(width = 1.5.dp.toPx()))
                drawCircle(color = secondary, radius = radius * 0.7f, style = Stroke(width = 1.2.dp.toPx()))
                val starPath = Path()
                val innerR = radius * 0.2f
                val outerR = radius * 0.48f
                for (i in 0 until 10) {
                    val angle = Math.toRadians((i * 36 - 90).toDouble())
                    val r = if (i % 2 == 0) outerR else innerR
                    val px = cx + (r * Math.cos(angle)).toFloat()
                    val py = cy + (r * Math.sin(angle)).toFloat()
                    if (i == 0) starPath.moveTo(px, py) else starPath.lineTo(px, py)
                }
                starPath.close()
                drawPath(starPath, color = primary)
            }
            CoreGeometry.MJOLNIR_RUNIC -> {
                drawCircle(color = secondary.copy(alpha = 0.4f), radius = radius, style = Stroke(width = 1.2.dp.toPx()))
                val boltPath = Path().apply {
                    moveTo(cx + 2.dp.toPx(), cy - radius * 0.7f)
                    lineTo(cx - 3.dp.toPx(), cy)
                    lineTo(cx + 1.dp.toPx(), cy)
                    lineTo(cx - 2.dp.toPx(), cy + radius * 0.7f)
                    lineTo(cx + 4.dp.toPx(), cy - 2.dp.toPx())
                    lineTo(cx, cy - 2.dp.toPx())
                    close()
                }
                drawPath(boltPath, color = primary)
            }
            CoreGeometry.GAMMA_FIST -> {
                val octPath = Path()
                for (i in 0 until 8) {
                    val angle = Math.toRadians((i * 45 - 22.5).toDouble())
                    val px = cx + (radius * Math.cos(angle)).toFloat()
                    val py = cy + (radius * Math.sin(angle)).toFloat()
                    if (i == 0) octPath.moveTo(px, py) else octPath.lineTo(px, py)
                }
                octPath.close()
                drawPath(octPath, color = primary, style = Stroke(width = 1.5.dp.toPx()))
                drawCircle(color = secondary, radius = radius * 0.4f)
            }
            CoreGeometry.EYE_OF_AGAMOTTO -> {
                drawCircle(color = primary, radius = radius, style = Stroke(width = 1.2.dp.toPx()))
                drawCircle(color = secondary, radius = radius * 0.7f, style = Stroke(width = 1.dp.toPx()))
                val s = radius * 0.85f
                val squarePath = Path().apply {
                    moveTo(cx, cy - s)
                    lineTo(cx + s, cy)
                    lineTo(cx, cy + s)
                    lineTo(cx - s, cy)
                    close()
                }
                drawPath(squarePath, color = primary.copy(alpha = 0.7f), style = Stroke(width = 1.dp.toPx()))
                drawCircle(color = primary, radius = radius * 0.25f)
            }
            CoreGeometry.SPIDER_MATRIX -> {
                val polyPath = Path()
                for (i in 0 until 6) {
                    val angle = Math.toRadians((i * 60 - 90).toDouble())
                    val px = cx + (radius * Math.cos(angle)).toFloat()
                    val py = cy + (radius * Math.sin(angle)).toFloat()
                    if (i == 0) polyPath.moveTo(px, py) else polyPath.lineTo(px, py)
                    drawLine(color = secondary.copy(alpha = 0.5f), start = Offset(cx, cy), end = Offset(px, py), strokeWidth = 1.dp.toPx())
                }
                polyPath.close()
                drawPath(polyPath, color = primary, style = Stroke(width = 1.5.dp.toPx()))
                drawCircle(color = primary, radius = radius * 0.3f)
            }
            CoreGeometry.BLACK_WIDOW_HOURGLASS -> {
                val wHalf = radius * 0.65f
                val hHalf = radius * 0.85f
                val hourglassPath = Path().apply {
                    moveTo(cx - wHalf, cy - hHalf)
                    lineTo(cx + wHalf, cy - hHalf)
                    lineTo(cx + 1.dp.toPx(), cy)
                    lineTo(cx + wHalf, cy + hHalf)
                    lineTo(cx - wHalf, cy + hHalf)
                    lineTo(cx - 1.dp.toPx(), cy)
                    close()
                }
                drawPath(hourglassPath, color = primary)
                drawCircle(color = secondary, radius = radius * 0.15f)
            }
            CoreGeometry.AVENGERS_CHEVRON -> {
                drawCircle(color = primary, radius = radius, style = Stroke(width = 1.5.dp.toPx()))
                val chevronPath = Path().apply {
                    moveTo(cx - radius * 0.45f, cy + radius * 0.5f)
                    lineTo(cx, cy - radius * 0.6f)
                    lineTo(cx + radius * 0.45f, cy + radius * 0.5f)
                    lineTo(cx + radius * 0.2f, cy + radius * 0.5f)
                    lineTo(cx, cy - radius * 0.15f)
                    lineTo(cx - radius * 0.2f, cy + radius * 0.5f)
                    close()
                }
                drawPath(chevronPath, color = secondary)
            }
        }
    }
}

