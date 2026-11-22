package com.sllauncher.pro.launcher

import java.time.ZonedDateTime
import javax.inject.Inject

/**
 * Wraps "now" so the clock can be tested. Reads the system default zone on
 * every call, so time-zone changes (travel, manual change) are picked up.
 */
fun interface TimeSource {
    fun now(): ZonedDateTime
}

class SystemTimeSource @Inject constructor() : TimeSource {
    override fun now(): ZonedDateTime = ZonedDateTime.now()
}
