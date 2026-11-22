package com.sllauncher.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Root theme for every SL Launcher screen.
 *
 * SL tokens are exposed through [SLTheme]. A Material 3 theme is also
 * provided, mapped onto the same tokens, so any Material component we use
 * (text selection, ripples, sheets) inherits the SL identity instead of
 * Material's default purple.
 *
 * @param reduceMotion force reduced motion on or off; `null` follows the
 *   Android "Remove animations" setting.
 */
@Composable
fun SLTheme(
    colors: SLColors = DarkSLColors,
    typography: SLTypography = DefaultSLTypography,
    reduceMotion: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val systemReduced = remember(context) { systemAnimationsDisabled(context) }
    val motion = SLMotion(reduced = reduceMotion ?: systemReduced)
    val shapes = SLShapes()

    CompositionLocalProvider(
        LocalSLColors provides colors,
        LocalSLTypography provides typography,
        LocalSLSpacing provides SLSpacing(),
        LocalSLShapes provides shapes,
        LocalSLSizes provides SLSizes(),
        LocalSLMotion provides motion,
    ) {
        MaterialTheme(
            colorScheme = materialColorsFor(colors),
            shapes = Shapes(
                extraSmall = RoundedCornerShape(shapes.cornerSmall),
                small = RoundedCornerShape(shapes.cornerSmall),
                medium = RoundedCornerShape(shapes.cornerMedium),
                large = RoundedCornerShape(shapes.cornerMedium),
                extraLarge = RoundedCornerShape(shapes.cornerMedium),
            ),
            content = content,
        )
    }
}

private fun materialColorsFor(colors: SLColors) = if (colors.isDark) {
    darkColorScheme(
        primary = colors.accent,
        onPrimary = colors.onAccent,
        secondary = colors.textPrimary,
        onSecondary = colors.background,
        background = colors.background,
        onBackground = colors.textPrimary,
        surface = colors.surface,
        onSurface = colors.textPrimary,
        surfaceVariant = colors.surfaceElevated,
        onSurfaceVariant = colors.textSecondary,
        surfaceContainerLow = colors.surface,
        surfaceContainer = colors.surface,
        surfaceContainerHigh = colors.surfaceElevated,
        outline = colors.borderStrong,
        outlineVariant = colors.border,
        error = colors.error,
        onError = colors.onError,
        scrim = colors.scrim,
    )
} else {
    lightColorScheme(
        primary = colors.accent,
        onPrimary = colors.onAccent,
        secondary = colors.textPrimary,
        onSecondary = colors.background,
        background = colors.background,
        onBackground = colors.textPrimary,
        surface = colors.surface,
        onSurface = colors.textPrimary,
        surfaceVariant = colors.surfaceElevated,
        onSurfaceVariant = colors.textSecondary,
        surfaceContainerLow = colors.surface,
        surfaceContainer = colors.surface,
        surfaceContainerHigh = colors.surfaceElevated,
        outline = colors.borderStrong,
        outlineVariant = colors.border,
        error = colors.error,
        onError = colors.onError,
        scrim = colors.scrim,
    )
}

/** Accessors for the current SL design tokens. */
object SLTheme {
    val colors: SLColors
        @Composable @ReadOnlyComposable get() = LocalSLColors.current

    val typography: SLTypography
        @Composable @ReadOnlyComposable get() = LocalSLTypography.current

    val spacing: SLSpacing
        @Composable @ReadOnlyComposable get() = LocalSLSpacing.current

    val shapes: SLShapes
        @Composable @ReadOnlyComposable get() = LocalSLShapes.current

    val sizes: SLSizes
        @Composable @ReadOnlyComposable get() = LocalSLSizes.current

    val motion: SLMotion
        @Composable @ReadOnlyComposable get() = LocalSLMotion.current
}
