package com.sllauncher.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 4dp-based spacing scale. Use these instead of ad-hoc dp values. */
@Immutable
data class SLSpacing(
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val xxxl: Dp = 48.dp,
)

/**
 * Soft-UI shape language: generous rounded corners, surfaces that look
 * raised from or pressed into the background, and a thin outline on
 * interactive controls so they are visible without relying on shadows.
 */
@Immutable
data class SLShapes(
    val cornerSmall: Dp = 14.dp,
    val cornerMedium: Dp = 22.dp,
    val cornerLarge: Dp = 30.dp,
    /** Outline width for interactive controls. */
    val borderWidth: Dp = 1.dp,
    /** Shadow distance for raised elements. */
    val softDepth: Dp = 5.dp,
    /** Shadow distance for floating panels, dialogs and the assistant button. */
    val panelDepth: Dp = 9.dp,
    /** False in low-performance mode: shadows become flat outlines (cheaper to draw). */
    val softShadows: Boolean = true,
    /** Minimum interactive size required by accessibility guidelines. */
    val minTouchTarget: Dp = 48.dp,
)

/** Standard component sizes so tiles, rows and controls line up across screens. */
@Immutable
data class SLSizes(
    val iconSmall: Dp = 16.dp,
    val icon: Dp = 24.dp,
    val appIcon: Dp = 52.dp,
    val controlHeight: Dp = 48.dp,
    val rowMinHeight: Dp = 56.dp,
    val navBarHeight: Dp = 64.dp,
    val assistantButton: Dp = 64.dp,
)

internal val LocalSLSpacing = staticCompositionLocalOf { SLSpacing() }
internal val LocalSLShapes = staticCompositionLocalOf { SLShapes() }
internal val LocalSLSizes = staticCompositionLocalOf { SLSizes() }
