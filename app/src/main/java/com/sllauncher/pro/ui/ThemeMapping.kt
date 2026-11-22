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
 * Applies the user's theme choices to the design-system tokens. Accents
 * are fills; on the light palette, thin accent marks stay dark ochre and
 * fills keep an ink outline so they remain visible (see DESIGN-SYSTEM.md).
 */
fun colorsFor(settings: LauncherSettings): SLColors {
    val base = if (settings.themeMode == ThemeMode.Dark) DarkSLColors else LightSLColors
    val accent = Color(settings.accent.argb)
    return if (base.isDark) {
        base.copy(accent = accent, accentIndicator = accent, accentOutline = accent)
    } else {
        base.copy(accent = accent)
    }
}

fun typographyFor(settings: LauncherSettings): SLTypography = when (settings.displayFont) {
    DisplayFont.Brand -> DefaultSLTypography
    DisplayFont.System -> DefaultSLTypography.withFontFamily(FontFamily.SansSerif)
}
