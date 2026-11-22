package com.sllauncher.pro.productivity

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class FocusPhase { Focus, Break }

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
 */
@Singleton
class FocusTimer @Inject constructor(@param:ApplicationContext private val context: Context) {
    private val _session = MutableStateFlow<FocusSession?>(null)
    val session: StateFlow<FocusSession?> = _session.asStateFlow()

    fun start(minutes: Int, label: String = "Focus", phase: FocusPhase = FocusPhase.Focus, pomodoroRound: Int? = null) {
        val total = minutes * 60_000L
        val endsAt = SystemClock.elapsedRealtime() + total
        _session.value = FocusSession(phase, label, endsAt, total, pomodoroRound)
        schedule(endsAt, label, phase)
    }

    fun stop() {
        _session.value = null
        context.getSystemService(AlarmManager::class.java).cancel(pendingIntent("", FocusPhase.Focus))
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
