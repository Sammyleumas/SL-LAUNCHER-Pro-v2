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
 * Neo-brutalist shape language: hard edges, visible borders, and an offset
 * "block shadow" instead of soft blurred elevation.
 */
@Immutable
data class SLShapes(
    val cornerSmall: Dp = 2.dp,
    val cornerMedium: Dp = 4.dp,
    val borderWidth: Dp = 2.dp,
    val blockShadowOffset: Dp = 4.dp,
    val panelShadowOffset: Dp = 6.dp,
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
