package com.sllauncher.core.designsystem.component

import kotlin.math.roundToInt

/**
 * Converts a touch position (0 = left edge, 1 = right edge) into a slider
 * value. With [steps] > 0 the value snaps to `steps` evenly spaced stops
 * between the range ends (Material's convention: 3 steps on 0..4 gives
 * 0, 1, 2, 3, 4).
 */
fun sliderValueForFraction(
    fraction: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
): Float {
    val f = fraction.coerceIn(0f, 1f)
    val snapped = if (steps > 0) {
        val segments = steps + 1
        (f * segments).roundToInt().toFloat() / segments
    } else {
        f
    }
    return range.start + snapped * (range.endInclusive - range.start)
}

/** Inverse of [sliderValueForFraction]: where on the track a value sits (0..1). */
fun sliderFractionForValue(value: Float, range: ClosedFloatingPointRange<Float>): Float {
    val span = range.endInclusive - range.start
    if (span == 0f) return 0f
    return ((value - range.start) / span).coerceIn(0f, 1f)
}
