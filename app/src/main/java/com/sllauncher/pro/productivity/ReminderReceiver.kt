package com.sllauncher.pro.productivity

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.sllauncher.pro.MainActivity
import com.sllauncher.pro.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val CHANNEL_REMINDERS = "reminders"

/** Posts task reminders and focus-timer completions. */
@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    @Inject lateinit var focusTimer: FocusTimer

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_FOCUS_DONE -> {
                val finished = focusTimer.onFinished()
                val next = focusTimer.session.value
                val title = if (finished?.phase == FocusPhase.Break) "Break's over" else "Focus session complete"
                val text = next?.let { "${it.label} started: ${it.totalMs / 60_000} minutes." } ?: "Nice work."
                notify(context, 7001, title, text)
            }
            ACTION_TASK_REMINDER -> {
                val id = intent.getLongExtra(EXTRA_TASK_ID, 0)
                notify(context, (8000 + id % 100_000).toInt(), "Reminder", intent.getStringExtra(EXTRA_TITLE) ?: "Task due")
            }
        }
    }

    companion object {
        const val ACTION_FOCUS_DONE = "com.sllauncher.pro.FOCUS_DONE"
        const val ACTION_TASK_REMINDER = "com.sllauncher.pro.TASK_REMINDER"
        const val EXTRA_TITLE = "title"
        const val EXTRA_PHASE = "phase"
        const val EXTRA_TASK_ID = "taskId"

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(CHANNEL_REMINDERS, "Reminders and timers", NotificationManager.IMPORTANCE_HIGH)
                channel.description = "Task reminders and focus-timer alerts you create in SL"
                context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
            }
        }

        fun canNotify(context: Context): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

        /** Schedules a task reminder (inexact window, no special permission). */
        fun scheduleTask(context: Context, taskId: Long, title: String, atMillis: Long) {
            context.getSystemService(AlarmManager::class.java)
                .setWindow(AlarmManager.RTC_WAKEUP, atMillis, 60_000L, taskIntent(context, taskId, title))
        }

        fun cancelTask(context: Context, taskId: Long) {
            context.getSystemService(AlarmManager::class.java).cancel(taskIntent(context, taskId, ""))
        }

        private fun taskIntent(context: Context, taskId: Long, title: String) = PendingIntent.getBroadcast(
            context,
            (8000 + taskId % 100_000).toInt(),
            Intent(context, ReminderReceiver::class.java).setAction(ACTION_TASK_REMINDER).putExtra(EXTRA_TASK_ID, taskId).putExtra(EXTRA_TITLE, title),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        private fun notify(context: Context, id: Int, title: String, text: String) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            ensureChannel(context)
            val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
            val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setContentIntent(open)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
            try {
                NotificationManagerCompat.from(context).notify(id, notification)
            } catch (e: SecurityException) {
                android.util.Log.w("ReminderReceiver", "Notification permission revoked", e)
            }
        }
    }
}
