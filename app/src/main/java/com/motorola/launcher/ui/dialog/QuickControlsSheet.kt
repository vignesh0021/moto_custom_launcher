package com.motorola.launcher.ui.dialog

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.motorola.launcher.ui.theme.LocalCustomization
import com.motorola.launcher.ui.theme.StarkCyan
import com.motorola.launcher.ui.theme.StarkCyanBright
import com.motorola.launcher.ui.theme.StarkDarkSurface
import com.motorola.launcher.ui.theme.StarkEmerald
import com.motorola.launcher.ui.theme.StarkHudBorder
import com.motorola.launcher.ui.theme.StarkHudBorderBright
import com.motorola.launcher.ui.theme.StarkReactorRed
import com.motorola.launcher.ui.theme.TextDimHud
import com.motorola.launcher.ui.theme.TextPrimary
import com.motorola.launcher.ui.theme.TextSecondary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Screen 4: Quick Controls with Radial HUD Menu and Quick Toggle Grid.
 * Faithfully reproduces the futuristic Iron Man command interface from the reference artwork.
 */
@Composable
fun QuickControlsSheet(
    onDismiss: () -> Unit,
    onTriggerHydrationTest: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val customization = LocalCustomization.current
    val marvelTheme = com.motorola.launcher.ui.theme.LocalMarvelTheme.current
    val themeColor = customization.themeColor
    val scrollState = rememberScrollState()

    var isFlashlightOn by remember { mutableStateOf(false) }
    var isDndOn by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        val sheetShape = RoundedCornerShape(20.dp)

        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(sheetShape)
                .background(marvelTheme.colors.surface)
                .border(1.5.dp, marvelTheme.colors.hudBorderBright, sheetShape)
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
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "QUICK CONTROLS",
                            color = TextPrimary,
                            fontSize = 14.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.2.sp
                        )
                        Text(
                            text = "${marvelTheme.name.uppercase()} // TACTICAL CONTROLS",
                            color = marvelTheme.colors.primary,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.2.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(marvelTheme.colors.subtle)
                            .border(1.dp, marvelTheme.colors.primary, CircleShape)
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
                            color = marvelTheme.colors.bright,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 1. Central Radial HUD Menu (Arc / Hex reticle with 6 surrounding sectors)
                Box(
                    modifier = Modifier
                        .size(260.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Background Radial Arc HUD Canvas
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val c = Offset(size.width / 2f, size.height / 2f)
                        val r = size.minDimension / 2f

                        // Outer dashed perimeter
                        drawCircle(
                            color = marvelTheme.colors.subtle,
                            radius = r * 0.95f,
                            center = c,
                            style = Stroke(1.dp.toPx())
                        )

                        // 6 Segmented Radial Arc Sectors
                        val totalSegments = 6
                        val segmentSweep = 50f
                        val gap = 10f
                        for (i in 0 until totalSegments) {
                            val startAngle = -90f + (i * 60f) + (gap / 2f)
                            drawArc(
                                color = if (i % 2 == 0) marvelTheme.colors.glow else marvelTheme.colors.secondary.copy(alpha = 0.4f),
                                startAngle = startAngle,
                                sweepAngle = segmentSweep,
                                useCenter = false,
                                topLeft = Offset(c.x - r * 0.85f, c.y - r * 0.85f),
                                size = Size(r * 1.7f, r * 1.7f),
                                style = Stroke(12.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        // Inner concentric reticle
                        drawCircle(
                            color = marvelTheme.colors.primary.copy(alpha = 0.5f),
                            radius = r * 0.45f,
                            center = c,
                            style = Stroke(1.5.dp.toPx())
                        )
                    }

                    // Central Mini Superhero Core Reticle
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(marvelTheme.colors.subtle)
                            .border(1.5.dp, marvelTheme.colors.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        MiniThemeInsignia(
                            geometry = marvelTheme.coreGeometry,
                            primary = marvelTheme.colors.primary,
                            secondary = marvelTheme.colors.secondary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // 6 Positioned Radial Buttons
                    // 1. Top (Wi-Fi Settings)
                    RadialSectorItem(
                        icon = "📶",
                        label = "Wi-Fi\nSettings",
                        modifier = Modifier.align(Alignment.TopCenter),
                        onClick = { launchSettings(context, Settings.ACTION_WIFI_SETTINGS) }
                    )

                    // 2. Top-Right (Location Settings)
                    RadialSectorItem(
                        icon = "📍",
                        label = "Location\nSettings",
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 24.dp, end = 6.dp),
                        onClick = { launchSettings(context, Settings.ACTION_LOCATION_SOURCE_SETTINGS) }
                    )

                    // 3. Bottom-Right (Battery Settings)
                    RadialSectorItem(
                        icon = "⚡",
                        label = "Battery\nSettings",
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 24.dp, end = 6.dp),
                        onClick = { launchSettings(context, Settings.ACTION_BATTERY_SAVER_SETTINGS) }
                    )

                    // 4. Bottom (Sound Settings)
                    RadialSectorItem(
                        icon = "🔊",
                        label = "Sound\nSettings",
                        modifier = Modifier.align(Alignment.BottomCenter),
                        onClick = { launchSettings(context, Settings.ACTION_SOUND_SETTINGS) }
                    )

                    // 5. Bottom-Left (Display Settings)
                    RadialSectorItem(
                        icon = "☀",
                        label = "Display\nSettings",
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(bottom = 24.dp, start = 6.dp),
                        onClick = { launchSettings(context, Settings.ACTION_DISPLAY_SETTINGS) }
                    )

                    // 6. Top-Left (Bluetooth Settings)
                    RadialSectorItem(
                        icon = "ᛒ",
                        label = "Bluetooth\nSettings",
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(top = 24.dp, start = 6.dp),
                        onClick = { launchSettings(context, Settings.ACTION_BLUETOOTH_SETTINGS) }
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 2. 2x3 Grid of Functional Quick Toggles
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Row 1: Airplane Mode | Hotspot | Do Not Disturb
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickToggleBox(
                            icon = "✈",
                            label = "Airplane Mode",
                            isActive = false,
                            modifier = Modifier.weight(1f),
                            onClick = { launchSettings(context, Settings.ACTION_AIRPLANE_MODE_SETTINGS) }
                        )
                        QuickToggleBox(
                            icon = "📡",
                            label = "Hotspot",
                            isActive = false,
                            modifier = Modifier.weight(1f),
                            onClick = { launchSettings(context, Settings.ACTION_WIRELESS_SETTINGS) }
                        )
                        QuickToggleBox(
                            icon = "🌙",
                            label = "Do Not Disturb",
                            isActive = isDndOn,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                isDndOn = !isDndOn
                                launchSettings(context, Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                            }
                        )
                    }

                    // Row 2: Flashlight | Auto Rotate | Dark Theme
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickToggleBox(
                            icon = "🔦",
                            label = "Flashlight",
                            isActive = isFlashlightOn,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                isFlashlightOn = toggleFlashlight(context, isFlashlightOn)
                            }
                        )
                        QuickToggleBox(
                            icon = "⟳",
                            label = "Auto Rotate",
                            isActive = true,
                            modifier = Modifier.weight(1f),
                            onClick = { launchSettings(context, Settings.ACTION_DISPLAY_SETTINGS) }
                        )
                        QuickToggleBox(
                            icon = "◑",
                            label = "Dark Theme",
                            isActive = true,
                            modifier = Modifier.weight(1f),
                            onClick = { launchSettings(context, Settings.ACTION_DISPLAY_SETTINGS) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Marvel Hydration Companion Action Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(themeColor.primary.copy(alpha = 0.2f))
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
                        text = "💧 CALL PIXEL FRIEND COMPANION",
                        color = themeColor.bright,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

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
                        text = "CLOSE CONTROLS",
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
private fun RadialSectorItem(
    icon: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val view = LocalView.current
    val customization = LocalCustomization.current
    val themeColor = customization.themeColor

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    if (customization.hapticFeedback) {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    }
                    onClick()
                }
            )
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = icon, fontSize = 16.sp)
        Text(
            text = label,
            color = themeColor.primary,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 11.sp
        )
    }
}

@Composable
private fun QuickToggleBox(
    icon: String,
    label: String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val view = LocalView.current
    val customization = LocalCustomization.current
    val themeColor = customization.themeColor

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "quickToggleScale"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(10.dp))
            .background(if (isActive) themeColor.subtle else Color(0x1F060C16))
            .border(
                width = 1.dp,
                color = if (isActive) themeColor.primary else StarkHudBorder,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    if (customization.hapticFeedback) {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    }
                    onClick()
                }
            )
            .padding(vertical = 12.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = icon, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                color = if (isActive) themeColor.bright else TextSecondary,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun toggleFlashlight(context: Context, currentState: Boolean): Boolean {
    val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    val newState = !currentState
    try {
        val cameraId = cameraManager?.cameraIdList?.firstOrNull()
        if (cameraId != null) {
            cameraManager.setTorchMode(cameraId, newState)
            return newState
        }
    } catch (_: Throwable) {
    }
    return currentState
}

private fun launchSettings(context: Context, action: String) {
    try {
        val intent = Intent(action).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        context.startActivity(intent)
    } catch (_: Throwable) {
        try {
            val fallback = Intent(Settings.ACTION_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(fallback)
        } catch (_: Throwable) {
        }
    }
}
