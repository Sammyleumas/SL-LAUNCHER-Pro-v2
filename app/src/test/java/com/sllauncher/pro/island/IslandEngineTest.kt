package com.sllauncher.pro.island

import android.app.Notification
import com.sllauncher.pro.device.BatteryState
import com.sllauncher.pro.device.PowerSource
import com.sllauncher.pro.media.NowPlaying
import com.sllauncher.pro.notifications.SLNotification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IslandEngineTest {

    private fun note(key: String, pkg: String = "com.chat", category: String? = null, ongoing: Boolean = false, time: Long = 1, progressMax: Int = 0) = SLNotification(
        key = key, packageName = pkg, appLabel = pkg, title = "T", text = "x", postTime = time, category = category,
        isOngoing = ongoing, isClearable = !ongoing, groupKey = key, isGroupSummary = false, contentIntent = null,
        actions = emptyList(), progressMax = progressMax, progress = 0, progressIndeterminate = false,
        messages = emptyList(), conversationTitle = null, count = 0, chronometerBase = null,
    )

    private val playing = NowPlaying("com.music", "Music", "Song", "Artist", null, null, 1000, 0, 0, 1f, true, true, true, true)

    private fun build(
        notifications: List<SLNotification> = emptyList(),
        np: NowPlaying? = null,
        battery: BatteryState? = null,
        prefs: IslandPreferences = IslandPreferences(),
        pinned: String? = null,
        dismissed: Set<String> = emptySet(),
    ) = buildIslandActivities(notifications, np, battery, null, prefs, pinned, dismissed, ownPackage = "com.sllauncher.pro")

    @Test
    fun `calls outrank music, which outranks messages`() {
        val list = build(
            notifications = listOf(note("m", category = Notification.CATEGORY_MESSAGE), note("c", pkg = "com.dialer", category = Notification.CATEGORY_CALL, ongoing = true)),
            np = playing,
        )
        assertEquals(listOf("call:c", "music:com.music", "msg:m"), list.map { it.id })
    }

    @Test
    fun `pinned activity stays first`() {
        val list = build(notifications = listOf(note("c", category = Notification.CATEGORY_CALL, ongoing = true)), np = playing, pinned = "music:com.music")
        assertEquals("music:com.music", list.first().id)
    }

    @Test
    fun `muted apps and dismissed activities are hidden`() {
        val list = build(
            notifications = listOf(note("m", pkg = "com.noisy", category = Notification.CATEGORY_MESSAGE)),
            np = playing,
            prefs = IslandPreferences(mutedPackages = setOf("com.noisy")),
            dismissed = setOf("music:com.music"),
        )
        assertTrue(list.isEmpty())
    }

    @Test
    fun `messages collapse into one activity with an unread count`() {
        val list = build(notifications = listOf(note("a", category = Notification.CATEGORY_MESSAGE, time = 1), note("b", category = Notification.CATEGORY_MESSAGE, time = 5)))
        val message = list.single() as IslandActivity.Message
        assertEquals("b", message.notification.key)
        assertEquals(2, message.unread)
    }

    @Test
    fun `charging shows only when plugged in or low`() {
        assertTrue(build(battery = BatteryState(80, charging = false, full = false, source = PowerSource.None)).isEmpty())
        assertEquals("charging", build(battery = BatteryState(50, true, false, PowerSource.Wired)).single().id)
        assertEquals("charging", build(battery = BatteryState(10, false, false, PowerSource.None)).single().id)
    }

    @Test
    fun `progress notifications become progress activities`() {
        val list = build(notifications = listOf(note("d", pkg = "com.downloads", progressMax = 100)))
        assertTrue(list.single() is IslandActivity.Progress)
    }
}
