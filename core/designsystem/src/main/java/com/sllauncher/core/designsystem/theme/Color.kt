package com.sllauncher.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Raw brand palette. Never reference these from feature code directly;
 * use [SLColors] through `SLTheme.colors` so themes can be swapped later.
 */
internal object SLPalette {
    val Black = Color(0xFF090909)
    val Charcoal = Color(0xFF151515)
    val Graphite = Color(0xFF252525)
    val Yellow = Color(0xFFFFD400)
    val OffWhite = Color(0xFFF5F1E8)
    val Grey = Color(0xFFA5A5A5)
    val BorderGrey = Color(0xFF383838)

    /**
     * Interactive outlines (WCAG 1.4.11 needs 3:1). Measured: 4.20 on Black,
     * 3.85 on Charcoal, 3.23 on Graphite, 4.21 / 3.77 / 3.34 on the paper surfaces.
     */
    val OutlineGrey = Color(0xFF737373)
    val ErrorRed = Color(0xFFFF5A4E)

    // Light ("paper") variant.
    val Paper = Color(0xFFF5F1E8)
    val PaperDim = Color(0xFFEAE5D9)
    val PaperDeep = Color(0xFFDED8CA)
    val Ink = Color(0xFF090909)
    val InkSoft = Color(0xFF4A4A4A)
    val PaperBorder = Color(0xFFC9C3B5)
    val DarkYellow = Color(0xFF7A6000)
    val ErrorRedDark = Color(0xFFB3261E)
}

/**
 * Semantic colour roles for SL Launcher. Names describe purpose, not hue,
 * so a user theme can change the values without touching screens.
 */
@Immutable
data class SLColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    /** Fill colour for primary actions, selected states and SL identity. */
    val accent: Color,
    /** Text and icons drawn on top of [accent]. */
    val onAccent: Color,
    /** Accent used as a thin line or small mark directly on [background]/[surface]. */
    val accentIndicator: Color,
    /** Outline around an [accent] fill so the shape reads on light surfaces too. */
    val accentOutline: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    /** Decorative dividers and block shadows. Not for control outlines. */
    val border: Color,
    /** Outlines of interactive controls; meets the 3:1 non-text contrast minimum. */
    val borderStrong: Color,
    val error: Color,
    val onError: Color,
    /** Dims the wallpaper behind floating panels, dialogs and sheets. */
    val scrim: Color,
)

val DarkSLColors = SLColors(
    isDark = true,
    background = SLPalette.Black,
    surface = SLPalette.Charcoal,
    surfaceElevated = SLPalette.Graphite,
    accent = SLPalette.Yellow,
    onAccent = SLPalette.Black,
    accentIndicator = SLPalette.Yellow,
    accentOutline = SLPalette.Yellow,
    textPrimary = SLPalette.OffWhite,
    textSecondary = SLPalette.Grey,
    border = SLPalette.BorderGrey,
    borderStrong = SLPalette.OutlineGrey,
    error = SLPalette.ErrorRed,
    onError = SLPalette.Black,
    scrim = Color(0x99000000),
)

/**
 * Paper variant. Yellow stays a fill colour only: yellow text or thin yellow
 * lines on off-white fail contrast, so [accentIndicator] becomes dark ochre.
 */
val LightSLColors = SLColors(
    isDark = false,
    background = SLPalette.Paper,
    surface = SLPalette.PaperDim,
    surfaceElevated = SLPalette.PaperDeep,
    accent = SLPalette.Yellow,
    onAccent = SLPalette.Ink,
    accentIndicator = SLPalette.DarkYellow,
    accentOutline = SLPalette.Ink,
    textPrimary = SLPalette.Ink,
    textSecondary = SLPalette.InkSoft,
    border = SLPalette.PaperBorder,
    borderStrong = SLPalette.OutlineGrey,
    error = SLPalette.ErrorRedDark,
    onError = SLPalette.Paper,
    scrim = Color(0x66000000),
)

/** Kept for Phase 1 call sites; the dark palette is the brand default. */
val DefaultSLColors = DarkSLColors

internal val LocalSLColors = staticCompositionLocalOf { DefaultSLColors }
