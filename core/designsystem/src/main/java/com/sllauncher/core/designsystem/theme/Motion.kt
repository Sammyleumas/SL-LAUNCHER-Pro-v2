package com.sllauncher.core.designsystem.theme

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Motion tokens. SL motion is short and decisive: no bounce, no overshoot.
 *
 * When [reduced] is true (Android "Remove animations", or SL's low-performance
 * mode in a later phase) every spec collapses to an instant change and
 * looping animations render a static frame instead.
 */
@Immutable
data class SLMotion(
    val reduced: Boolean = false,
    val shortMillis: Int = 120,
    val mediumMillis: Int = 220,
) {
    fun <T> short(): FiniteAnimationSpec<T> =
        if (reduced) snap() else tween(shortMillis, easing = SLEasing)

    fun <T> medium(): FiniteAnimationSpec<T> =
        if (reduced) snap() else tween(mediumMillis, easing = SLEasing)

    companion object {
        /** Fast out, firm stop. */
        val SLEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    }
}

/** True when the user turned animations off in Android settings. */
internal fun systemAnimationsDisabled(context: Context): Boolean =
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

internal val LocalSLMotion = staticCompositionLocalOf { SLMotion() }
