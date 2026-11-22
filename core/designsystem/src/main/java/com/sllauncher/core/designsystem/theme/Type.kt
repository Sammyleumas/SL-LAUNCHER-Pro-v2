package com.sllauncher.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.sllauncher.core.designsystem.R

/**
 * Space Grotesk (SIL Open Font License, see assets/licenses) is bundled as a
 * single 133 KB variable font and used for display text, headings and labels,
 * where the brand voice lives. Each weight below selects a point on the
 * font's weight axis (300–700).
 */
@OptIn(ExperimentalTextApi::class)
internal val SLDisplayFontFamily = FontFamily(
    Font(
        R.font.space_grotesk,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500)),
    ),
    Font(
        R.font.space_grotesk,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600)),
    ),
    Font(
        R.font.space_grotesk,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700)),
    ),
)

/**
 * Running text uses the platform sans-serif: zero APK cost and tuned for
 * long reading (news, notifications, settings descriptions).
 */
internal val SLBodyFontFamily: FontFamily = FontFamily.SansSerif

/** All sizes are in `sp`, so they follow the user's system font-size setting. */
@Immutable
data class SLTypography(
    val display: TextStyle,
    val headline: TextStyle,
    val title: TextStyle,
    val body: TextStyle,
    val bodySmall: TextStyle,
    val label: TextStyle,
    val caption: TextStyle,
)

val DefaultSLTypography = SLTypography(
    display = TextStyle(
        fontFamily = SLDisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 64.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.03).em,
    ),
    headline = TextStyle(
        fontFamily = SLDisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.01).em,
    ),
    title = TextStyle(
        fontFamily = SLDisplayFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    body = TextStyle(
        fontFamily = SLBodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = SLBodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    label = TextStyle(
        fontFamily = SLDisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.06.em,
    ),
    caption = TextStyle(
        fontFamily = SLBodyFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
)

internal val LocalSLTypography = staticCompositionLocalOf { DefaultSLTypography }

/** The SL type scale with every style switched to [family] (used for the "system font" option). */
fun SLTypography.withFontFamily(family: FontFamily): SLTypography = copy(
    display = display.copy(fontFamily = family),
    headline = headline.copy(fontFamily = family),
    title = title.copy(fontFamily = family),
    body = body.copy(fontFamily = family),
    bodySmall = bodySmall.copy(fontFamily = family),
    label = label.copy(fontFamily = family),
    caption = caption.copy(fontFamily = family),
)
