package com.sllauncher.pro.island

import com.sllauncher.pro.device.BatteryState
import com.sllauncher.pro.device.PowerSource
import com.sllauncher.pro.media.NowPlaying
import com.sllauncher.pro.notifications.SLNotification
import com.sllauncher.pro.productivity.FocusSession

/** One live activity in the Dynamic Island. */
sealed interface IslandActivity {
    /** Stable id: survives updates so pinning and dismissing keep working. */
    val id: String
    val priority: Int

    data class Call(val notification: SLNotification) : IslandActivity {
        override val id = "call:" + notification.key
        override val priority = 100
    }

    /** Timer, stopwatch or alarm from the clock app. */
    data class Timer(val notification: SLNotification) : IslandActivity {
        override val id = "timer:" + notification.key
        override val priority = 90
    }

    data class Focus(val session: FocusSession) : IslandActivity {
        override val id = "focus"
        override val priority = 85
    }

    data class Navigation(val notification: SLNotification) : IslandActivity {
        override val id = "nav:" + notification.key
        override val priority = 80
    }

    data class Music(val nowPlaying: NowPlaying) : IslandActivity {
        override val id = "music:" + nowPlaying.packageName
        override val priority = if (nowPlaying.isPlaying) 70 else 40
    }

    /** Latest unread conversation; [unread] counts all message notifications. */
    data class Message(val notification: SLNotification, val unread: Int) : IslandActivity {
        override val id = "msg:" + notification.key
        override val priority = 60
    }

    /** Downloads, uploads, installs and other progress notifications. */
    data class Progress(val notification: SLNotification) : IslandActivity {
        override val id = "progress:" + notification.key
        override val priority = 50
    }

    data class Charging(val battery: BatteryState) : IslandActivity {
        override val id = "charging"
        override val priority = if (battery.low) 65 else 30
    }
}

/** User choices that shape the island. */
data class IslandPreferences(
    val showMusic: Boolean = true,
    val showMessages: Boolean = true,
    val showCharging: Boolean = true,
    val mutedPackages: Set<String> = emptySet(),
)

/**
 * Builds the island's activities, highest priority first. [pinnedId] is
 * kept first when present; [dismissedIds] are hidden.
 */
fun buildIslandActivities(
    notifications: List<SLNotification>,
    nowPlaying: NowPlaying?,
    battery: BatteryState?,
    focus: FocusSession?,
    prefs: IslandPreferences,
    pinnedId: String?,
    dismissedIds: Set<String>,
    ownPackage: String,
): List<IslandActivity> {
    val visible = notifications.filter { it.packageName !in prefs.mutedPackages && it.packageName != ownPackage && !it.isGroupSummary }
    val list = buildList {
        visible.filter { it.isCall }.forEach { add(IslandActivity.Call(it)) }
        visible.filter { it.category == android.app.Notification.CATEGORY_ALARM || it.category == android.app.Notification.CATEGORY_STOPWATCH || (it.chronometerBase != null && !it.isCall) }
            .forEach { add(IslandActivity.Timer(it)) }
        focus?.let { add(IslandActivity.Focus(it)) }
        visible.filter { it.category == android.app.Notification.CATEGORY_NAVIGATION }.forEach { add(IslandActivity.Navigation(it)) }
        if (prefs.showMusic && nowPlaying != null) add(IslandActivity.Music(nowPlaying))
        if (prefs.showMessages) {
            val messages = visible.filter { it.isMessage && !it.isOngoing }
            messages.maxByOrNull { it.postTime }?.let { add(IslandActivity.Message(it, messages.sumOf { m -> maxOf(1, m.count) })) }
        }
        visible.filter { it.isProgress && !it.isCall && it.category != android.app.Notification.CATEGORY_TRANSPORT }.take(2).forEach { add(IslandActivity.Progress(it)) }
        if (prefs.showCharging && battery != null && (battery.source != PowerSource.None || battery.low)) add(IslandActivity.Charging(battery))
    }
    return list
        .filter { it.id !in dismissedIds }
        .distinctBy { it.id }
        .sortedWith(compareByDescending<IslandActivity> { it.id == pinnedId }.thenByDescending { it.priority })
}
