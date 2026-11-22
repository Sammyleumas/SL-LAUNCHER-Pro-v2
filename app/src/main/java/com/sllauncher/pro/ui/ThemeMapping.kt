package com.sllauncher.pro.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import com.sllauncher.pro.data.settings.DisplayFont
import com.sllauncher.pro.data.settings.LauncherSettings
import com.sllauncher.pro.data.settings.ThemeMode
import com.sllauncher.core.designsystem.theme.DarkSLColors
import com.sllauncher.core.designsystem.theme.DefaultSLTypography
import com.sllauncher.core.designsystem.theme.LightSLColors
import com.sllauncher.core.designsystem.theme.SLColors
import com.sllauncher.core.designsystem.theme.SLTypography
import com.sllauncher.core.designsystem.theme.withFontFamily

/**
 * Applies the user's theme choices to the design-system tokens.
 * [systemDark] is the phone's dark-mode setting, used by [ThemeMode.Auto].
 */
fun colorsFor(settings: LauncherSettings, systemDark: Boolean): SLColors {
    val dark = when (settings.themeMode) {
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
        ThemeMode.Auto -> systemDark
    }
    val base = if (dark) DarkSLColors else LightSLColors
    val accent = Color(settings.accent.argbFor(dark))
    return base.copy(accent = accent, accentIndicator = accent, accentOutline = accent)
}

fun typographyFor(settings: LauncherSettings): SLTypography = when (settings.displayFont) {
    DisplayFont.Brand -> DefaultSLTypography
    DisplayFont.System -> DefaultSLTypography.withFontFamily(FontFamily.SansSerif)
}
