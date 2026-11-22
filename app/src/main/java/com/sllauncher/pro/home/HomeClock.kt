package com.sllauncher.pro.home

import com.sllauncher.pro.launcher.TimeSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.ZonedDateTime
import javax.inject.Inject

/**
 * Emits the current time once per minute, aligned to the start of each
 * minute, so the clock flips exactly when the minute changes without
 * waking the CPU every second.
 */
class HomeClock @Inject constructor(private val timeSource: TimeSource) {

    fun now(): ZonedDateTime = timeSource.now()

    val minutes: Flow<ZonedDateTime> = flow {
        while (true) {
            val now = timeSource.now()
            emit(now)
            delay(millisUntilNextMinute(now))
        }
    }

    companion object {
        fun millisUntilNextMinute(now: ZonedDateTime): Long {
            val elapsedInMinute = now.second * 1_000L + now.nano / 1_000_000L
            return 60_000L - elapsedInMinute
        }
    }
}
