package com.sllauncher.pro.productivity

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class FocusPhase { Focus, Break }

private const val CHANNEL_LIVE = "focus_live"
private const val LIVE_ID = 7100

/** A running focus or Pomodoro session. Times use elapsedRealtime so clock changes do not affect them. */
data class FocusSession(
    val phase: FocusPhase,
    val label: String,
    val endsAtElapsed: Long,
    val totalMs: Long,
    val pomodoroRound: Int?,
) {
    fun remainingMs(now: Long = SystemClock.elapsedRealtime()) = (endsAtElapsed - now).coerceAtLeast(0)
}

/**
 * Focus and Pomodoro timer. The end is scheduled with AlarmManager (an
 * inexact window, no special permission) so the "time's up" notification
 * arrives even if SL is closed.
 *
 * While a session runs, a silent ongoing notification shows a live
 * countdown in the shade and on the lock screen, and Do Not Disturb
 * (priority only) is turned on if the user chose that and granted access.
 */
@Singleton
class FocusTimer @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settings: com.sllauncher.pro.data.settings.SettingsRepository,
) {
    private val _session = MutableStateFlow<FocusSession?>(null)
    val session: StateFlow<FocusSession?> = _session.asStateFlow()

    /** Interruption filter to restore when focus ends; null when SL did not change it. */
    private var savedFilter: Int? = null

    fun start(minutes: Int, label: String = "Focus", phase: FocusPhase = FocusPhase.Focus, pomodoroRound: Int? = null) {
        val total = minutes * 60_000L
        val endsAt = SystemClock.elapsedRealtime() + total
        val session = FocusSession(phase, label, endsAt, total, pomodoroRound)
        _session.value = session
        schedule(endsAt, label, phase)
        if (phase == FocusPhase.Focus) applyDoNotDisturb() else restoreDoNotDisturb()
        showLive(session)
        com.sllauncher.pro.widgets.TodayWidgetProvider.refresh(context)
    }

    fun stop() {
        _session.value = null
        context.getSystemService(AlarmManager::class.java).cancel(pendingIntent("", FocusPhase.Focus))
        ended()
    }

    private fun ended() {
        restoreDoNotDisturb()
        NotificationManagerCompat.from(context).cancel(LIVE_ID)
        com.sllauncher.pro.widgets.TodayWidgetProvider.refresh(context)
    }

    /** True when SL may change Do Not Disturb (the user granted access in Settings). */
    fun hasDoNotDisturbAccess(): Boolean =
        context.getSystemService(NotificationManager::class.java).isNotificationPolicyAccessGranted

    private fun applyDoNotDisturb() {
        if (!settings.current.focusDoNotDisturb || !hasDoNotDisturbAccess()) return
        val nm = context.getSystemService(NotificationManager::class.java)
        if (savedFilter == null) savedFilter = nm.currentInterruptionFilter
        nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
    }

    private fun restoreDoNotDisturb() {
        val previous = savedFilter ?: return
        savedFilter = null
        if (hasDoNotDisturbAccess()) {
            context.getSystemService(NotificationManager::class.java).setInterruptionFilter(previous)
        }
    }

    /** Silent ongoing notification with a live countdown, visible on the lock screen. */
    private fun showLive(session: FocusSession) {
        if (!ReminderReceiver.canNotify(context)) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_LIVE, "Focus timer", NotificationManager.IMPORTANCE_LOW)
            channel.description = "Live countdown while a focus session runs"
            channel.lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val open = PendingIntent.getActivity(
            context,
            7002,
            Intent(context, com.sllauncher.pro.MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val endWall = System.currentTimeMillis() + session.remainingMs()
        val notification = NotificationCompat.Builder(context, CHANNEL_LIVE)
            .setSmallIcon(com.sllauncher.pro.R.drawable.ic_stat_sl)
            .setContentTitle(if (session.phase == FocusPhase.Focus) "${session.label} in progress" else "Break")
            .setContentText("Ends in ${session.totalMs / 60_000} minutes or less")
            .setWhen(endWall)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setContentIntent(open)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(LIVE_ID, notification)
        } catch (e: SecurityException) {
            android.util.Log.w("FocusTimer", "Notification permission revoked", e)
        }
    }

    /** Called when the alarm fires: starts the Pomodoro break or finishes. */
    fun onFinished(): FocusSession? {
        val finished = _session.value ?: return null
        val round = finished.pomodoroRound
        if (round != null && finished.phase == FocusPhase.Focus) {
            start(if (round % 4 == 0) 15 else 5, "Break", FocusPhase.Break, round)
        } else if (round != null && finished.phase == FocusPhase.Break) {
            start(25, "Focus", FocusPhase.Focus, round + 1)
        } else {
            _session.value = null
            ended()
        }
        return finished
    }

    private fun schedule(endsAt: Long, label: String, phase: FocusPhase) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        // Inexact window: no exact-alarm permission needed, fires within about a minute.
        alarms.setWindow(AlarmManager.ELAPSED_REALTIME_WAKEUP, endsAt, 60_000L, pendingIntent(label, phase))
    }

    private fun pendingIntent(label: String, phase: FocusPhase): PendingIntent = PendingIntent.getBroadcast(
        context,
        7001,
        Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_FOCUS_DONE)
            .putExtra(ReminderReceiver.EXTRA_TITLE, label)
            .putExtra(ReminderReceiver.EXTRA_PHASE, phase.name),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
