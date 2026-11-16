package com.motorola.launcher.ui.workspace

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.motorola.launcher.ui.theme.LocalMarvelTheme

/**
 * 3D carousel visualization for multiple home "shops"/pages.
 * Each page is rendered with perspective tilt colored by the active Marvel theme.
 * Battery-safe: only animates when the current page changes.
 */
@Composable
fun Marvel3DPageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    if (pageCount <= 1) return
    val theme = LocalMarvelTheme.current
    val primary = theme.colors.primary
    val secondary = theme.colors.secondary

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        pageCount.let { count ->
            val mid = (count - 1) / 2f
            for (i in 0 until count) {
                val offset = (i - currentPage).toFloat()
                val scale by animateFloatAsState(
                    targetValue = if (i == currentPage) 1f else 0.75f,
                    animationSpec = spring(),
                    label = "pageScale"
                )
                val alpha by animateFloatAsState(
                    targetValue = if (i == currentPage) 1f else 0.45f,
                    animationSpec = spring(),
                    label = "pageAlpha"
                )
                Box(
                    modifier = Modifier
                        .offset(x = (offset * 28).dp)
                        .size(width = 36.dp, height = 22.dp)
                        .graphicsLayer {
                            rotationY = offset * -18f
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                            cameraDistance = 12f * density
                        }
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(primary.copy(alpha = 0.8f), secondary.copy(alpha = 0.6f))
                            )
                        )
                )
            }
        }
    }
}
