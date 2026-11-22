package com.sllauncher.core.designsystem.component

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min

/** Resolved on-screen size of an [SLFloatingPanel]. */
data class FloatingPanelSize(val width: Dp, val height: Dp)

/** Space always left between the panel and the window edge, so wallpaper stays visible. */
val FloatingPanelEdgeGutter = 16.dp

/**
 * Sizes the floating panel for the available window.
 *
 * - Normal phones: [widthFraction] of the width (80% by default) and
 *   [heightFraction] of the height (70%), the "medium floating panel" spec.
 * - Very narrow phones: grows to at least 280dp so a 4-column app grid
 *   still fits, but never past the 16dp edge gutter.
 * - Tablets and unfolded foldables: capped at 560dp x 720dp so the panel
 *   stays a panel instead of stretching edge to edge.
 * - Short windows (landscape phones, split screen): uses 90% of the height.
 */
fun floatingPanelSize(
    maxWidth: Dp,
    maxHeight: Dp,
    widthFraction: Float = 0.8f,
    heightFraction: Float = 0.7f,
): FloatingPanelSize {
    val usableWidth = (maxWidth - FloatingPanelEdgeGutter * 2).coerceAtLeast(0.dp)
    val usableHeight = (maxHeight - FloatingPanelEdgeGutter * 2).coerceAtLeast(0.dp)

    val width = (maxWidth * widthFraction).coerceIn(
        minimumValue = min(MinPanelWidth, usableWidth),
        maximumValue = min(MaxPanelWidth, usableWidth),
    )

    val effectiveHeightFraction = if (maxHeight < ShortWindowHeight) 0.9f else heightFraction
    val height = (maxHeight * effectiveHeightFraction).coerceIn(
        minimumValue = min(MinPanelHeight, usableHeight),
        maximumValue = min(MaxPanelHeight, usableHeight),
    )
    return FloatingPanelSize(width, height)
}

private val MinPanelWidth = 280.dp
private val MaxPanelWidth = 560.dp
private val MinPanelHeight = 360.dp
private val MaxPanelHeight = 720.dp
private val ShortWindowHeight = 480.dp
