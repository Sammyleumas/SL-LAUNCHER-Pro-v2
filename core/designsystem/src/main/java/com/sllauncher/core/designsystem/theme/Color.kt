package com.sllauncher.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Raw soft-UI palette. Never reference these from feature code directly;
 * use [SLColors] through `SLTheme.colors` so themes can be swapped.
 *
 * Contrast (WCAG relative luminance), measured against the base surface:
 * - Light: text 11.0:1, secondary text 6.4:1, control outline 3.3:1.
 * - Dark: text 12.6:1, secondary text 6.6:1, control outline 3.6:1.
 * Classic neumorphism relies on shadows alone and fails these; SL keeps the
 * soft shadows but adds real outlines and strong text so it stays readable.
 */
internal object SLPalette {
    // Light soft
    val Cloud = Color(0xFFE6E9EF)
    val CloudDeep = Color(0xFFDCE0E8)
    val Slate = Color(0xFF2D3142)
    val SlateSoft = Color(0xFF4A5068)
    val CloudLine = Color(0xFFC3C7CF)
    val Outline = Color(0xFF7A8092)
    val ShadowDarkOnLight = Color(0xFFB8BEC9)
    val ShadowLightOnLight = Color(0xFFFFFFFF)
    val ErrorOnLight = Color(0xFFB3261E)

    // Dark soft
    val Night = Color(0xFF23262D)
    val NightRaised = Color(0xFF2B2F37)
    val Mist = Color(0xFFE8EAF0)
    val MistSoft = Color(0xFFA9AFBF)
    val NightLine = Color(0xFF3A3F49)
    val ShadowDarkOnDark = Color(0xFF16181D)
    val ShadowLightOnDark = Color(0xFF30343D)
    val ErrorOnDark = Color(0xFFFF8A80)

    // Default accent (blue): a deep tone on light, a pale tone on dark.
    val BlueDeep = Color(0xFF3A55D9)
    val BluePale = Color(0xFF8FA2FF)
    val InkOnPale = Color(0xFF101320)
}

/**
 * Semantic colour roles for SL Launcher Pro. Names describe purpose, not hue,
 * so a user theme can change the values without touching screens.
 */
@Immutable
data class SLColors(
    val isDark: Boolean,
    val background: Color,
    /** Raised soft surfaces. Equal to [background] in neumorphism: shape comes from shadows. */
    val surface: Color,
    /** Pressed-in wells and secondary fills. */
    val surfaceElevated: Color,
    /** Fill colour for primary actions, selected states and SL identity. */
    val accent: Color,
    /** Text and icons drawn on top of [accent]. */
    val onAccent: Color,
    /** Accent used as text, a thin line or a small mark directly on [background]. */
    val accentIndicator: Color,
    /** Outline around an [accent] fill. */
    val accentOutline: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    /** Decorative dividers. Not for control outlines. */
    val border: Color,
    /** Outlines of interactive controls; meets the 3:1 non-text contrast minimum. */
    val borderStrong: Color,
    val error: Color,
    val onError: Color,
    /** Dims the wallpaper behind floating panels, dialogs and sheets. */
    val scrim: Color,
    /** Soft shadow cast away from the light (bottom-right). */
    val shadowDark: Color,
    /** Soft highlight towards the light (top-left). */
    val shadowLight: Color,
)

val LightSLColors = SLColors(
    isDark = false,
    background = SLPalette.Cloud,
    surface = SLPalette.Cloud,
    surfaceElevated = SLPalette.CloudDeep,
    accent = SLPalette.BlueDeep,
    onAccent = Color.White,
    accentIndicator = SLPalette.BlueDeep,
    accentOutline = SLPalette.BlueDeep,
    textPrimary = SLPalette.Slate,
    textSecondary = SLPalette.SlateSoft,
    border = SLPalette.CloudLine,
    borderStrong = SLPalette.Outline,
    error = SLPalette.ErrorOnLight,
    onError = Color.White,
    scrim = Color(0x662D3142),
    shadowDark = SLPalette.ShadowDarkOnLight,
    shadowLight = SLPalette.ShadowLightOnLight,
)

val DarkSLColors = SLColors(
    isDark = true,
    background = SLPalette.Night,
    surface = SLPalette.Night,
    surfaceElevated = SLPalette.NightRaised,
    accent = SLPalette.BluePale,
    onAccent = SLPalette.InkOnPale,
    accentIndicator = SLPalette.BluePale,
    accentOutline = SLPalette.BluePale,
    textPrimary = SLPalette.Mist,
    textSecondary = SLPalette.MistSoft,
    border = SLPalette.NightLine,
    borderStrong = SLPalette.Outline,
    error = SLPalette.ErrorOnDark,
    onError = SLPalette.InkOnPale,
    scrim = Color(0x99000000),
    shadowDark = SLPalette.ShadowDarkOnDark,
    shadowLight = SLPalette.ShadowLightOnDark,
)

/** The light soft palette is the brand default. */
val DefaultSLColors = LightSLColors

internal val LocalSLColors = staticCompositionLocalOf { DefaultSLColors }
