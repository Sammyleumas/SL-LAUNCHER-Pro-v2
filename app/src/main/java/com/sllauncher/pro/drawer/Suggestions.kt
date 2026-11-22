package com.sllauncher.pro.drawer

import java.time.Instant
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.min

/**
 * Suggests apps for the current time of day from the user's own launch
 * history, on the phone only. Each past launch votes for its app with a
 * weight that is highest when it happened at the same time of day as now
 * (Gaussian over the minutes between the two times, sigma = 75 minutes)
 * and fades with age (half-life of about 10 days).
 *
 * @param launches pairs of (app key, launch time in epoch millis).
 * @return app keys, best first, at most [limit]; empty when there is no history.
 */
fun suggestApps(
    launches: List<Pair<String, Long>>,
    now: Long,
    zone: ZoneId = ZoneId.systemDefault(),
    limit: Int = 4,
): List<String> {
    if (launches.isEmpty()) return emptyList()
    val nowMinute = minuteOfDay(now, zone)
    val scores = HashMap<String, Double>()
    for ((key, at) in launches) {
        if (at > now) continue
        val diff = circularMinutes(minuteOfDay(at, zone), nowMinute).toDouble()
        val timeWeight = exp(-(diff * diff) / (2 * 75.0 * 75.0))
        val ageDays = (now - at) / 86_400_000.0
        val ageWeight = exp(-ageDays / 14.0)
        scores[key] = (scores[key] ?: 0.0) + timeWeight * ageWeight
    }
    return scores.entries
        .filter { it.value >= 0.15 }
        .sortedByDescending { it.value }
        .take(limit)
        .map { it.key }
}

private fun minuteOfDay(millis: Long, zone: ZoneId): Int {
    val t = Instant.ofEpochMilli(millis).atZone(zone)
    return t.hour * 60 + t.minute
}

/** Minutes between two times of day, going the short way round midnight. */
internal fun circularMinutes(a: Int, b: Int): Int {
    val d = abs(a - b)
    return min(d, 24 * 60 - d)
}
