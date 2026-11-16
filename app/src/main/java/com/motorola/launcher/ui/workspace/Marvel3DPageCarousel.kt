package com.motorola.launcher.ui.workspace

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.motorola.launcher.ui.theme.LocalMarvelTheme

/**
 * Optimized 3D page indicator for multi-page home.
 * - Only animates on page change (spring, no infinite loops)
 * - Low camera distance, minimal layers
 * - Battery-safe: zero work when pageCount <= 1
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

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        for (i in 0 until pageCount) {
            val offset = (i - currentPage).toFloat()
            val isActive = i == currentPage
            val scale by animateFloatAsState(
                targetValue = if (isActive) 1f else 0.72f,
                animationSpec = spring(stiffness = 300f),
                label = "s$i"
            )
            val alpha by animateFloatAsState(
                targetValue = if (isActive) 1f else 0.4f,
                animationSpec = spring(stiffness = 300f),
                label = "a$i"
            )
            Box(
                modifier = Modifier
                    .offset(x = (offset * 26).dp)
                    .size(width = 32.dp, height = 18.dp)
                    .graphicsLayer {
                        rotationY = offset * -14f
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                        cameraDistance = 8f * density
                    }
                    .clip(RoundedCornerShape(5.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(primary.copy(alpha = 0.75f), secondary.copy(alpha = 0.5f))
                        )
                    )
            )
        }
    }
}
