package com.motorola.launcher.ui.dialog

import android.content.Intent
import android.view.HapticFeedbackConstants
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.motorola.launcher.data.repository.LauncherPreferences
import com.motorola.launcher.ui.model.AnimationLevelPreset
import com.motorola.launcher.ui.model.CustomizationSettings
import com.motorola.launcher.ui.model.IconStylePreset
import com.motorola.launcher.ui.model.ThemeColorPreset
import com.motorola.launcher.ui.model.WallpaperPreset
import com.motorola.launcher.ui.theme.LocalCustomization
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
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val context = LocalContext.current
    val themeColor = customization.themeColor
    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = onDismiss) {
        val sheetShape = RoundedCornerShape(20.dp)

        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(sheetShape)
                .background(StarkDarkSurface)
                .border(1.5.dp, StarkHudBorderBright, sheetShape)
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
                    letterSpacing = 2.sp
                )
                Text(
                    text = "MAKE IT YOURS",
                    color = themeColor.primary,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
                )

                // 0. MARVEL HERO PROTOCOL Selection
                CustomizationSection(title = "MARVEL HERO PROTOCOL") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val heroList = com.motorola.launcher.ui.model.MarvelHeroProtocol.entries
                        for (rowHeroes in heroList.chunked(3)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (hero in rowHeroes) {
                                    val isSelected = customization.heroProtocol == hero
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) hero.primary.copy(alpha = 0.25f) else Color(0x1F050A14))
                                            .border(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) hero.primary else StarkHudBorder,
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null,
                                                onClick = {
                                                    if (customization.hapticFeedback) {
                                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                                    }
                                                    preferences.updateHeroProtocol(hero)
                                                }
                                            )
                                            .padding(vertical = 10.dp, horizontal = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(hero.primary)
                                                .border(1.dp, if (isSelected) Color.White else StarkHudBorder, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = hero.heroName,
                                            color = if (isSelected) hero.bright else TextSecondary,
                                            fontSize = 9.5.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 1
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (wallpaper in WallpaperPreset.entries) {
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
                                        .size(30.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Brush.verticalGradient(listOf(wallpaper.top, wallpaper.bottom))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (wallpaper == WallpaperPreset.SYSTEM) {
                                        Text(text = "🖼", fontSize = 14.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = wallpaper.displayName.split(" ").first(),
                                    color = if (isSelected) themeColor.bright else TextSecondary,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (style in IconStylePreset.entries) {
                            val isSelected = customization.iconStyle == style
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
                                            preferences.updateIconStyle(style)
                                        }
                                    )
                                    .padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = style.displayName,
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
                    }
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
