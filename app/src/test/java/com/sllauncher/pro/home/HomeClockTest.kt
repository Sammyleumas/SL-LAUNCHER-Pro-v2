package com.sllauncher.pro.home

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneOffset
import java.time.ZonedDateTime

class HomeClockTest {

    private val fixedTime = ZonedDateTime.of(2026, 11, 16, 9, 41, 15, 500_000_000, ZoneOffset.UTC)

    @Test
    fun `ticker waits exactly until the next minute boundary`() {
        // 15.5 s into the minute -> 44.5 s until the next minute.
        assertEquals(44_500L, HomeClock.millisUntilNextMinute(fixedTime))
    }

    @Test
    fun `ticker at an exact minute waits a full minute`() {
        assertEquals(60_000L, HomeClock.millisUntilNextMinute(fixedTime.withSecond(0).withNano(0)))
    }

    @Test
    fun `first emission is the current time`() = runTest {
        val clock = HomeClock { fixedTime }
        assertEquals(fixedTime, clock.minutes.first())
    }
}
