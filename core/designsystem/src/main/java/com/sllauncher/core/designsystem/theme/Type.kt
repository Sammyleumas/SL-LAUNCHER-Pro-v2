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
 * Nunito (SIL Open Font License, see assets/licenses) is bundled as one
 * variable font. Its rounded terminals match the soft UI. Each weight below
 * selects a point on the font's weight axis.
 */
@OptIn(ExperimentalTextApi::class)
private fun nunito(weight: Int) = Font(
    R.font.nunito,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

internal val SLDisplayFontFamily = FontFamily(nunito(400), nunito(500), nunito(600), nunito(700), nunito(800))

/** Body text uses Nunito too, so the whole interface has one soft voice. */
internal val SLBodyFontFamily: FontFamily = SLDisplayFontFamily

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
        fontWeight = FontWeight.ExtraBold,
        fontSize = 64.sp,
        lineHeight = 68.sp,
        letterSpacing = (-0.02).em,
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
        letterSpacing = 0.02.em,
    ),
    caption = TextStyle(
        fontFamily = SLBodyFontFamily,
        fontWeight = FontWeight.SemiBold,
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
