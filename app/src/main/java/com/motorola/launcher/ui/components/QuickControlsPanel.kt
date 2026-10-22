package com.motorola.launcher.ui.components

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motorola.launcher.ui.theme.StarkCyan
import com.motorola.launcher.ui.theme.StarkCyanBright
import com.motorola.launcher.ui.theme.StarkEmerald
import com.motorola.launcher.ui.theme.StarkGold
import com.motorola.launcher.ui.theme.StarkHudBorder
import com.motorola.launcher.ui.theme.StarkHudBorderBright
import com.motorola.launcher.ui.theme.StarkIceBlue
import com.motorola.launcher.ui.theme.StarkReactorRed
import com.motorola.launcher.ui.theme.TextPrimary

/**
 * Tactical Quick Controls Bar for STARK OS.
 * Directs users to real system settings pages without fake reflection toggles.
 */
@Composable
fun QuickControlsPanel(
    onOpenDiagnostics: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        QuickControlItem(
            label = "WI-FI",
            accentColor = StarkCyan,
            onClick = { launchSettings(context, Settings.ACTION_WIFI_SETTINGS) }
        )
        QuickControlItem(
            label = "BLUETOOTH",
            accentColor = StarkIceBlue,
            onClick = { launchSettings(context, Settings.ACTION_BLUETOOTH_SETTINGS) }
        )
        QuickControlItem(
            label = "DISPLAY",
            accentColor = StarkCyanBright,
            onClick = { launchSettings(context, Settings.ACTION_DISPLAY_SETTINGS) }
        )
        QuickControlItem(
            label = "POWER",
            accentColor = StarkGold,
            onClick = { launchSettings(context, Settings.ACTION_BATTERY_SAVER_SETTINGS) }
        )
        QuickControlItem(
            label = "APPS",
            accentColor = StarkEmerald,
            onClick = { launchSettings(context, Settings.ACTION_APPLICATION_SETTINGS) }
        )
        QuickControlItem(
            label = "SOUND",
            accentColor = StarkIceBlue,
            onClick = { launchSettings(context, Settings.ACTION_SOUND_SETTINGS) }
        )
        QuickControlItem(
            label = "DIAGNOSTICS",
            accentColor = StarkReactorRed,
            onClick = onOpenDiagnostics
        )
    }
}

@Composable
private fun QuickControlItem(
    label: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "quickControlScale"
    )

    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isPressed) accentColor.copy(alpha = 0.25f) else Color(0x1F060C18)
            )
            .border(
                width = 1.dp,
                color = if (isPressed) accentColor else StarkHudBorder,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onClick()
                }
            )
            .padding(horizontal = 11.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .background(accentColor, RoundedCornerShape(1.dp))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = if (isPressed) TextPrimary else accentColor,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
        }
    }
}

private fun launchSettings(context: Context, action: String) {
    try {
        val intent = Intent(action).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Throwable) {
        // Fallback to primary Android settings
        try {
            val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
        } catch (_: Throwable) {
        }
    }
}
