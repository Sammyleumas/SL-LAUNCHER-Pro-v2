package com.sllauncher.pro.productivity

import com.sllauncher.pro.data.db.TaskRepeat
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class HabitsAndRepeatTest {
    @Test fun `streak counts back from today`() {
        assertEquals(3, habitStreak(setOf(8L, 9L, 10L), today = 10L))
    }

    @Test fun `streak still counts when today is not ticked yet`() {
        assertEquals(2, habitStreak(setOf(8L, 9L), today = 10L))
    }

    @Test fun `gap ends the streak`() {
        assertEquals(1, habitStreak(setOf(7L, 10L), today = 10L))
        assertEquals(0, habitStreak(setOf(5L), today = 10L))
    }

    private val zone = ZoneOffset.UTC
    private fun t(y: Int, m: Int, d: Int, h: Int) = LocalDateTime.of(y, m, d, h, 0).toInstant(zone).toEpochMilli()

    @Test fun `daily task moves to the next day at the same time`() {
        val due = t(2026, 11, 20, 9)
        assertEquals(t(2026, 11, 21, 9), nextOccurrence(due, TaskRepeat.DAILY, now = t(2026, 11, 20, 10), zone = zone))
    }

    @Test fun `overdue daily task skips to the first future day`() {
        val due = t(2026, 11, 10, 9)
        assertEquals(t(2026, 11, 21, 9), nextOccurrence(due, TaskRepeat.DAILY, now = t(2026, 11, 20, 12), zone = zone))
    }

    @Test fun `weekly task moves seven days`() {
        val due = t(2026, 11, 20, 18)
        assertEquals(t(2026, 11, 27, 18), nextOccurrence(due, TaskRepeat.WEEKLY, now = t(2026, 11, 20, 19), zone = zone))
    }

    @Test fun `weekday task skips the weekend`() {
        val friday = t(2026, 11, 20, 8)
        assertEquals(DayOfWeek.FRIDAY, Instant.ofEpochMilli(friday).atZone(zone).dayOfWeek)
        val next = nextOccurrence(friday, TaskRepeat.WEEKDAYS, now = t(2026, 11, 20, 9), zone = zone)
        assertEquals(t(2026, 11, 23, 8), next)
    }

    @Test fun `non-repeating task is unchanged`() {
        val due = t(2026, 11, 20, 8)
        assertEquals(due, nextOccurrence(due, TaskRepeat.NONE, now = t(2026, 11, 21, 8), zone = zone))
    }

    @Test fun `durations read naturally`() {
        assertEquals("12 min", formatDuration(12 * 60_000L))
        assertEquals("1 h 5 min", formatDuration(65 * 60_000L))
    }
}
