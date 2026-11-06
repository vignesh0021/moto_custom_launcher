package com.motorola.launcher.ui.components

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Chamfered (cut-corner) rectangle shape used across STARK OS HUD surfaces.
 * Each corner is sliced diagonally instead of rounded.
 */
fun chamferedShape(cutSize: Dp = 10.dp): Shape = ChamferedShape(cutSize)

private class ChamferedShape(private val cut: Dp) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val c = with(density) { cut.toPx() }.coerceAtMost(size.minDimension / 3f)
        val path = Path().apply {
            moveTo(c, 0f)
            lineTo(size.width - c, 0f)
            lineTo(size.width, c)
            lineTo(size.width, size.height - c)
            lineTo(size.width - c, size.height)
            lineTo(c, size.height)
            lineTo(0f, size.height - c)
            lineTo(0f, c)
            close()
        }
        return Outline.Generic(path)
    }
}

/** CornerBasedShape variant for APIs that require it. */
fun chamferedCornerShape(cutSize: Dp = 10.dp): CornerBasedShape = object : CornerBasedShape(
    topStart = CornerSize(cutSize),
    topEnd = CornerSize(cutSize),
    bottomEnd = CornerSize(cutSize),
    bottomStart = CornerSize(cutSize)
) {
    override fun copy(
        topStart: CornerSize,
        topEnd: CornerSize,
        bottomEnd: CornerSize,
        bottomStart: CornerSize
    ) = this

    override fun createOutline(
        size: Size,
        topStart: Float,
        topEnd: Float,
        bottomEnd: Float,
        bottomStart: Float,
        layoutDirection: LayoutDirection
    ): Outline {
        val c = topStart.coerceAtMost(size.minDimension / 3f)
        val path = Path().apply {
            moveTo(c, 0f)
            lineTo(size.width - c, 0f)
            lineTo(size.width, c)
            lineTo(size.width, size.height - c)
            lineTo(size.width - c, size.height)
            lineTo(c, size.height)
            lineTo(0f, size.height - c)
            lineTo(0f, c)
            close()
        }
        return Outline.Generic(path)
    }
}
