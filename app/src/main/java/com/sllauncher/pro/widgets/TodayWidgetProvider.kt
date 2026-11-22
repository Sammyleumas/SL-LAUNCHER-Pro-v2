package com.sllauncher.pro.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
import android.view.View
import android.widget.RemoteViews
import com.sllauncher.pro.MainActivity
import com.sllauncher.pro.R
import com.sllauncher.pro.data.db.TaskDao
import com.sllauncher.pro.data.db.TaskEntity
import com.sllauncher.pro.productivity.FocusTimer
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@EntryPoint
@InstallIn(SingletonComponent::class)
interface TodayWidgetEntryPoint {
    fun taskDao(): TaskDao
    fun focusTimer(): FocusTimer
}

/**
 * "SL Today" home-screen widget. The clock is a TextClock, so Android keeps
 * it current without SL waking up. The next task and the focus timer are
 * refreshed when they change ([refresh]) and every 30 minutes.
 */
class TodayWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        val pending = goAsync()
        scope.launch {
            try {
                render(context.applicationContext, manager, appWidgetIds)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        /** Re-renders every placed SL Today widget. Safe to call when none are placed. */
        fun refresh(context: Context) {
            val app = context.applicationContext
            val manager = AppWidgetManager.getInstance(app)
            val ids = manager.getAppWidgetIds(ComponentName(app, TodayWidgetProvider::class.java))
            if (ids.isEmpty()) return
            scope.launch { render(app, manager, ids) }
        }

        private suspend fun render(context: Context, manager: AppWidgetManager, ids: IntArray) {
            val entry = EntryPointAccessors.fromApplication(context, TodayWidgetEntryPoint::class.java)
            val next = nextTask(entry.taskDao().getAll(), System.currentTimeMillis())
            val focus = entry.focusTimer().session.value
            val open = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val taskText = when {
                next == null -> "No tasks left. Nice."
                next.dueAt != null -> "Next: ${next.title} · " + DateUtils.formatDateTime(context, next.dueAt, DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH)
                else -> "Next: ${next.title}"
            }
            ids.forEach { id ->
                val views = RemoteViews(context.packageName, R.layout.widget_today).apply {
                    setTextViewText(R.id.widget_task, taskText)
                    if (focus != null) {
                        setViewVisibility(R.id.widget_focus, View.VISIBLE)
                        val mins = (focus.remainingMs() / 60_000L).coerceAtLeast(1)
                        setTextViewText(R.id.widget_focus, "${focus.label} · about $mins min left")
                    } else {
                        setViewVisibility(R.id.widget_focus, View.GONE)
                    }
                    setOnClickPendingIntent(R.id.widget_root, open)
                }
                manager.updateAppWidget(id, views)
            }
        }
    }
}

/** The task to show: the earliest due unfinished task, else the newest unfinished one. */
fun nextTask(tasks: List<TaskEntity>, now: Long): TaskEntity? {
    val open = tasks.filter { !it.done }
    return open.filter { it.dueAt != null && it.dueAt >= now - 3_600_000L }.minByOrNull { it.dueAt!! }
        ?: open.filter { it.dueAt == null }.maxByOrNull { it.createdAt }
        ?: open.minByOrNull { it.dueAt ?: Long.MAX_VALUE }
}
