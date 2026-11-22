package com.sllauncher.core.designsystem.component

import org.junit.Assert.assertEquals
import org.junit.Test

class SliderMathTest {

    @Test
    fun `continuous slider maps fraction linearly`() {
        assertEquals(25f, sliderValueForFraction(0.25f, 0f..100f), 0.001f)
    }

    @Test
    fun `out of range touches clamp to the ends`() {
        assertEquals(0f, sliderValueForFraction(-1f, 0f..100f), 0.001f)
        assertEquals(100f, sliderValueForFraction(2f, 0f..100f), 0.001f)
    }

    @Test
    fun `stepped slider snaps to the nearest stop`() {
        // 3..7 with 3 steps -> stops at 3, 4, 5, 6, 7 (grid column count).
        assertEquals(5f, sliderValueForFraction(0.6f, 3f..7f, steps = 3), 0.001f)
        assertEquals(5f, sliderValueForFraction(0.62f, 3f..7f, steps = 3), 0.001f)
        assertEquals(6f, sliderValueForFraction(0.7f, 3f..7f, steps = 3), 0.001f)
        assertEquals(3f, sliderValueForFraction(0.1f, 3f..7f, steps = 3), 0.001f)
    }

    @Test
    fun `fraction for value is the inverse and handles an empty range`() {
        assertEquals(0.5f, sliderFractionForValue(5f, 3f..7f), 0.001f)
        assertEquals(1f, sliderFractionForValue(10f, 3f..7f), 0.001f)
        assertEquals(0f, sliderFractionForValue(4f, 4f..4f), 0.001f)
    }
}
