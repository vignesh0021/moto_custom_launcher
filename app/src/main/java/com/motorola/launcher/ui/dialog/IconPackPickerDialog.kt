package com.motorola.launcher.ui.dialog

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.motorola.launcher.data.icon.IconPackInfo
import com.motorola.launcher.ui.components.chamferedShape
import com.motorola.launcher.ui.theme.LocalMarvelTheme
import com.motorola.launcher.ui.theme.TextPrimary
import com.motorola.launcher.ui.theme.TextSecondary
import com.motorola.launcher.ui.workspace.HudTextStyle

@Composable
fun IconPackPickerDialog(
    iconPacks: List<IconPackInfo>,
    selectedPackage: String?,
    onSelectIconPack: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val marvelTheme = LocalMarvelTheme.current
    val shape = chamferedShape(cutSize = 16f)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x88000000))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(shape)
                    .background(Color(0xFA0A0E1A))
                    .border(1.5.dp, marvelTheme.colors.primary, shape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Consume inner click
                    )
                    .padding(20.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ICON PACK ENGINE",
                            color = marvelTheme.colors.bright,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            style = HudTextStyle
                        )

                        Text(
                            text = "[CLOSE]",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onDismiss
                                )
                                .padding(4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Apply Nova / Total / Apex compatible icon packs across all screens.",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Icon Pack List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                    ) {
                        items(iconPacks) { pack ->
                            val isSelected = (selectedPackage.isNullOrBlank() && pack.isDefault) ||
                                    (!selectedPackage.isNullOrBlank() && selectedPackage == pack.packageName)

                            val rowShape = RoundedCornerShape(8.dp)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(rowShape)
                                    .background(if (isSelected) marvelTheme.colors.primary.copy(alpha = 0.15f) else Color(0x33000000))
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) marvelTheme.colors.primary else marvelTheme.colors.hudBorder,
                                        shape = rowShape
                                    )
                                    .clickable {
                                        onSelectIconPack(if (pack.isDefault) null else pack.packageName)
                                        onDismiss()
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) marvelTheme.colors.primary else Color(0x44FFFFFF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (pack.isDefault) "⚡" else "🎨",
                                        fontSize = 18.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = pack.label,
                                        color = if (isSelected) marvelTheme.colors.bright else TextPrimary,
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Text(
                                        text = if (pack.isDefault) "Original Stark HUD Cyber insiginas" else pack.packageName,
                                        color = TextSecondary,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "ACTIVE",
                                        color = marvelTheme.colors.primary,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Get More Icon Packs CTA
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x3300E5FF))
                            .border(1.dp, marvelTheme.colors.hudBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                try {
                                    val playStoreIntent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("market://search?q=icon%20pack&c=apps")
                                    ).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(playStoreIntent)
                                } catch (_: Throwable) {
                                    val webIntent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://play.google.com/store/search?q=icon%20pack&c=apps")
                                    ).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(webIntent)
                                }
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "GET MORE ICON PACKS ON PLAY STORE",
                            color = marvelTheme.colors.bright,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}
