package com.motorola.launcher.ui.dialog

import android.content.Intent
import android.provider.Settings
import android.util.Log
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import com.motorola.launcher.data.companion.CharacterExpression
import com.motorola.launcher.data.companion.CharacterPose
import com.motorola.launcher.data.companion.CharacterSheetEngine
import com.motorola.launcher.R
import com.motorola.launcher.ui.components.VideoCompanionPlayer
import com.motorola.launcher.ui.model.CompanionStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
                // Header with Quick Close
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SETTINGS & THEMES",
                        color = TextPrimary,
                        fontSize = 14.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.8.sp
                    )

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(currentTheme.colors.subtle)
                            .border(1.dp, currentTheme.colors.primary, CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    if (customization.hapticFeedback) {
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    }
                                    onDismiss()
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✕",
                            color = currentTheme.colors.bright,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Quick Action: Open Android System Settings
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(currentTheme.colors.primary.copy(alpha = 0.15f))
                        .border(1.2.dp, currentTheme.colors.primary, RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                if (customization.hapticFeedback) {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                }
                                try {
                                    val intent = Intent(Settings.ACTION_SETTINGS).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                                    }
                                    context.startActivity(intent)
                                    onDismiss()
                                } catch (e: Throwable) {
                                    Log.e("SettingsCustomization", "Failed opening system settings: ${e.message}")
                                }
                            }
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(text = "⚙", fontSize = 20.sp, color = currentTheme.colors.bright)
                        Column {
                            Text(
                                text = "ANDROID SYSTEM SETTINGS",
                                color = TextPrimary,
                                fontSize = 11.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.0.sp
                            )
                            Text(
                                text = "Wi-Fi, Bluetooth, Apps, Display, Battery, Sound",
                                color = TextSecondary,
                                fontSize = 9.sp
                            )
                        }
                    }
                    Text(
                        text = "OPEN ▶",
                        color = currentTheme.colors.bright,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

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
                            text = "WATER COMPANION CHARACTER",
                            color = TextDimHud,
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
                        )
                        // Companion Character Selection (2 rows of 3)
                        val allCompanionStyles = com.motorola.launcher.ui.model.CompanionStyle.entries
                        for (rowStyles in allCompanionStyles.chunked(3)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                for (style in rowStyles) {
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
                                            text = style.displayName.split(" ").first(),
                                            color = if (isSelected) themeColor.bright else TextSecondary,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        // Live Companion Sprite Preview & Dialogue Customizer
                        val selectedStyle = customization.companionStyle
                        val isCustomSheet = selectedStyle == com.motorola.launcher.ui.model.CompanionStyle.MITHRA ||
                                selectedStyle == com.motorola.launcher.ui.model.CompanionStyle.MITHRA_MOM ||
                                selectedStyle == com.motorola.launcher.ui.model.CompanionStyle.CUSTOM_SHEET

                        if (isCustomSheet) {
                            val sheetEngine = remember(context) { com.motorola.launcher.data.companion.CharacterSheetEngine.getInstance(context) }
                            val sprites by produceState<com.motorola.launcher.data.companion.CharacterCompanionSprites?>(
                                initialValue = null,
                                selectedStyle,
                                customization.customSheetUri
                            ) {
                                value = withContext(Dispatchers.IO) {
                                    sheetEngine.getSprites(selectedStyle, customization.customSheetUri)
                                }
                            }
                            val previewBmp = sprites?.poses?.get(com.motorola.launcher.data.companion.CharacterPose.PORTRAIT)
                                ?: sprites?.poses?.get(com.motorola.launcher.data.companion.CharacterPose.FRONT)
                                ?: sprites?.expressions?.get(com.motorola.launcher.data.companion.CharacterExpression.HAPPY)

                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0x22050A14))
                                    .border(1.dp, themeColor.primary.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val externalMithraVideo = remember { java.io.File("/sdcard/Download/video_mithra_lipsync.mp4") }
                                        val externalMomVideo = remember { java.io.File("/sdcard/Download/video_mithra_mom_lipsync.mp4") }
                                        val isMithra = selectedStyle == com.motorola.launcher.ui.model.CompanionStyle.MITHRA
                                        val isMom = selectedStyle == com.motorola.launcher.ui.model.CompanionStyle.MITHRA_MOM
                                        val hasCustomVideo = if (isMithra) externalMithraVideo.exists() && externalMithraVideo.length() > 1000
                                            else if (isMom) externalMomVideo.exists() && externalMomVideo.length() > 1000
                                            else false
                                        val isVideoCompanion = (isMithra || isMom) && (customization.useVideoCompanion || hasCustomVideo)

                                        if (isVideoCompanion) {
                                            val customVideoFile = if (isMithra) externalMithraVideo else externalMomVideo
                                            if (hasCustomVideo && customVideoFile.exists()) {
                                                VideoCompanionPlayer(
                                                    videoUri = android.net.Uri.fromFile(customVideoFile),
                                                    cornerRadius = 8.dp,
                                                    borderColor = themeColor.primary,
                                                    isMuted = true,
                                                    modifier = Modifier.size(width = 46.dp, height = 70.dp)
                                                )
                                            } else {
                                                val rawId = if (isMithra) R.raw.video_mithra else R.raw.video_mithra_mom
                                                VideoCompanionPlayer(
                                                    rawResId = rawId,
                                                    cornerRadius = 8.dp,
                                                    borderColor = themeColor.primary,
                                                    isMuted = true,
                                                    modifier = Modifier.size(width = 46.dp, height = 70.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                        } else if (previewBmp != null) {
                                            Image(
                                                bitmap = previewBmp.asImageBitmap(),
                                                contentDescription = "Companion Preview",
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier
                                                    .size(46.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0x33000000))
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${selectedStyle.displayName} DIALOGUES",
                                                color = themeColor.bright,
                                                fontSize = 9.5.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Editable in Tamil & English. Speaks with character voice.",
                                                color = TextSecondary,
                                                fontSize = 8.sp,
                                                fontFamily = FontFamily.SansSerif
                                            )
                                        }
                                    }

                                    if (isMithra || isMom) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0x18FFFFFF))
                                                .border(1.dp, themeColor.primary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "AI LIP-SYNC VIDEO MODE",
                                                    color = TextPrimary,
                                                    fontSize = 9.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = if (customization.useVideoCompanion || hasCustomVideo) "Using video (from /sdcard/Download or bundled)" else "Using character sheet animated sprites",
                                                    color = TextSecondary,
                                                    fontSize = 7.5.sp
                                                )
                                            }
                                            Switch(
                                                checked = customization.useVideoCompanion || hasCustomVideo,
                                                onCheckedChange = { preferences.setUseVideoCompanion(it) },
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = themeColor.bright,
                                                    checkedTrackColor = themeColor.primary.copy(alpha = 0.5f)
                                                ),
                                                modifier = Modifier.graphicsLayer { scaleX = 0.75f; scaleY = 0.75f }
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    when (selectedStyle) {
                                        com.motorola.launcher.ui.model.CompanionStyle.MITHRA -> {
                                            HudDialogueInput(
                                                label = "PROMPT DIALOGUE (அழைப்பு)",
                                                value = customization.mithraDialoguePrompt,
                                                onValueChange = { preferences.updateMithraDialogues(it, customization.mithraDialogueAccepted, customization.mithraDialogueRefused) },
                                                accentColor = themeColor.bright
                                            )
                                            HudDialogueInput(
                                                label = "ACCEPT DIALOGUE (குடித்தவுடன்)",
                                                value = customization.mithraDialogueAccepted,
                                                onValueChange = { preferences.updateMithraDialogues(customization.mithraDialoguePrompt, it, customization.mithraDialogueRefused) },
                                                accentColor = com.motorola.launcher.ui.theme.StarkEmerald
                                            )
                                            HudDialogueInput(
                                                label = "LATER DIALOGUE (அப்புறம்)",
                                                value = customization.mithraDialogueRefused,
                                                onValueChange = { preferences.updateMithraDialogues(customization.mithraDialoguePrompt, customization.mithraDialogueAccepted, it) },
                                                accentColor = Color(0xFFFFB300)
                                            )

                                            // Quick Preset Buttons
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 4.dp),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(themeColor.subtle)
                                                        .border(1.dp, themeColor.primary, RoundedCornerShape(6.dp))
                                                        .clickable {
                                                            preferences.updateMithraDialogues(
                                                                "அப்பா தண்ணி குடிங்க 💧",
                                                                "அப்பா சூப்பர்! நல்லா தண்ணி குடிச்சிட்டீங்க! 🎉",
                                                                "அப்பா அப்புறம் மறக்காம தண்ணி குடிங்க! 🥺"
                                                            )
                                                        }
                                                        .padding(vertical = 5.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(text = "TAMIL DEFAULT", color = themeColor.bright, fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(Color(0x18FFFFFF))
                                                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(6.dp))
                                                        .clickable {
                                                            preferences.updateMithraDialogues(
                                                                "Daddy, drink water please! 💧",
                                                                "Yay Daddy! You finished your water! 🎉",
                                                                "Okay Daddy, remember to drink later! 🥺"
                                                            )
                                                        }
                                                        .padding(vertical = 5.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(text = "ENGLISH DEFAULT", color = TextSecondary, fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                        com.motorola.launcher.ui.model.CompanionStyle.MITHRA_MOM -> {
                                            HudDialogueInput(
                                                label = "PROMPT DIALOGUE (அழைப்பு)",
                                                value = customization.momDialoguePrompt,
                                                onValueChange = { preferences.updateMomDialogues(it, customization.momDialogueAccepted, customization.momDialogueRefused) },
                                                accentColor = themeColor.bright
                                            )
                                            HudDialogueInput(
                                                label = "ACCEPT DIALOGUE (குடித்தவுடன்)",
                                                value = customization.momDialogueAccepted,
                                                onValueChange = { preferences.updateMomDialogues(customization.momDialoguePrompt, it, customization.momDialogueRefused) },
                                                accentColor = com.motorola.launcher.ui.theme.StarkEmerald
                                            )
                                            HudDialogueInput(
                                                label = "LATER DIALOGUE (அப்புறம்)",
                                                value = customization.momDialogueRefused,
                                                onValueChange = { preferences.updateMomDialogues(customization.momDialoguePrompt, customization.momDialogueAccepted, it) },
                                                accentColor = Color(0xFFFFB300)
                                            )

                                            // Quick Preset Buttons
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 4.dp),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(themeColor.subtle)
                                                        .border(1.dp, themeColor.primary, RoundedCornerShape(6.dp))
                                                        .clickable {
                                                            preferences.updateMomDialogues(
                                                                "ஏங்க தண்ணி குடிங்க 💧",
                                                                "ரொம்ப நல்லதுங்க, உடம்பை நல்லா பார்த்துக்கோங்க! 💛",
                                                                "வேலை முக்கியம் தான், ஆனா தண்ணி குடிக்க மறந்துடாதீங்க! 😤"
                                                            )
                                                        }
                                                        .padding(vertical = 5.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(text = "TAMIL DEFAULT", color = themeColor.bright, fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(Color(0x18FFFFFF))
                                                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(6.dp))
                                                        .clickable {
                                                            preferences.updateMomDialogues(
                                                                "Dear, please drink some water! 💧",
                                                                "Very good, please take care of your health! 💛",
                                                                "Work is important, but don't forget to hydrate! 😤"
                                                            )
                                                        }
                                                        .padding(vertical = 5.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(text = "ENGLISH DEFAULT", color = TextSecondary, fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                        com.motorola.launcher.ui.model.CompanionStyle.CUSTOM_SHEET -> {
                                            HudDialogueInput(
                                                label = "IMAGE FILE PATH / URI",
                                                value = customization.customSheetUri,
                                                onValueChange = { preferences.updateCustomSheet(it, customization.customDialoguePrompt, customization.customDialogueAccepted, customization.customDialogueRefused) },
                                                accentColor = themeColor.bright
                                            )
                                            HudDialogueInput(
                                                label = "PROMPT DIALOGUE",
                                                value = customization.customDialoguePrompt,
                                                onValueChange = { preferences.updateCustomSheet(customization.customSheetUri, it, customization.customDialogueAccepted, customization.customDialogueRefused) },
                                                accentColor = themeColor.bright
                                            )
                                            HudDialogueInput(
                                                label = "ACCEPT DIALOGUE",
                                                value = customization.customDialogueAccepted,
                                                onValueChange = { preferences.updateCustomSheet(customization.customSheetUri, customization.customDialoguePrompt, it, customization.customDialogueRefused) },
                                                accentColor = com.motorola.launcher.ui.theme.StarkEmerald
                                            )
                                            HudDialogueInput(
                                                label = "REFUSED DIALOGUE",
                                                value = customization.customDialogueRefused,
                                                onValueChange = { preferences.updateCustomSheet(customization.customSheetUri, customization.customDialoguePrompt, customization.customDialogueAccepted, it) },
                                                accentColor = Color(0xFFFFB300)
                                            )
                                        }
                                        else -> {}
                                    }
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
                    PreferenceToggleRow(
                        label = "Gesture-Driven World (Pinch / Rotate)",
                        checked = customization.gestureWorldEnabled,
                        onCheckedChange = { preferences.setGestureWorldEnabled(it) }
                    )
                    PreferenceToggleRow(
                        label = "Autonomous Context Adaptation (Auto Theme)",
                        checked = customization.autonomousContextEnabled,
                        onCheckedChange = { preferences.setAutonomousContextEnabled(it) }
                    )
                    PreferenceToggleRow(
                        label = "Daily Family Story Rotation",
                        checked = customization.dailyStoryMode,
                        onCheckedChange = { preferences.setDailyStoryMode(it) }
                    )
                    PreferenceToggleRow(
                        label = "Cinematic Entrance Sequence",
                        checked = customization.cinematicEntranceEnabled,
                        onCheckedChange = { preferences.setCinematicEntranceEnabled(it) }
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

/**
 * Sci-Fi HUD themed text field for editing companion dialogues.
 */
@Composable
private fun HudDialogueInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    accentColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Text(
            text = label,
            color = accentColor,
            fontSize = 8.5.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )
        Spacer(modifier = Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0x22050A14))
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(6.dp))
                .padding(horizontal = 9.dp, vertical = 7.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.SansSerif
                ),
                cursorBrush = SolidColor(accentColor),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}


