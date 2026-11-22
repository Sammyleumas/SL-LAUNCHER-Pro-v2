package com.sllauncher.core.designsystem.layout

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Width buckets matching Android's window size class breakpoints.
 * Layouts switch on these, never on a specific phone model.
 */
enum class WindowWidthClass { Compact, Medium, Expanded }

fun windowWidthClassOf(width: Dp): WindowWidthClass = when {
    width < 600.dp -> WindowWidthClass.Compact
    width < 840.dp -> WindowWidthClass.Medium
    else -> WindowWidthClass.Expanded
}

/** Width class of the current window (split-screen and foldables included). */
@OptIn(ExperimentalComposeUiApi::class) // containerSize was introduced as experimental
@Composable
fun currentWindowWidthClass(): WindowWidthClass {
    val widthPx = LocalWindowInfo.current.containerSize.width
    val width = with(LocalDensity.current) { widthPx.toDp() }
    return windowWidthClassOf(width)
}
