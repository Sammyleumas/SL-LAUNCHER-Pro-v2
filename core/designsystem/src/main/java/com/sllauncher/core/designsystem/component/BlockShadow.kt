package com.sllauncher.core.designsystem.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

/**
 * Draws the SL "block shadow": a solid copy of the element's bounds, offset
 * down and right. Apply it before the element's background. Unlike blurred
 * elevation it costs one rectangle draw, which matters on low-end GPUs.
 */
fun Modifier.blockShadow(color: Color, offset: Dp): Modifier = drawBehind {
    val px = offset.toPx()
    drawRect(color = color, topLeft = Offset(px, px), size = size)
}
