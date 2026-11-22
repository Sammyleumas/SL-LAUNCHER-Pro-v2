package com.sllauncher.pro.productivity

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.sllauncher.pro.data.db.CountdownDao
import com.sllauncher.pro.data.db.CountdownEntity
import com.sllauncher.pro.data.db.NoteDao
import com.sllauncher.pro.data.db.NoteEntity
import com.sllauncher.pro.data.db.TaskDao
import com.sllauncher.pro.data.db.TaskEntity
import com.sllauncher.pro.data.db.TaskRepeat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

data class CalendarEvent(val id: Long, val title: String, val begin: Long, val end: Long, val allDay: Boolean, val location: String?)
data class QuickContact(val id: Long, val name: String, val number: String?)

/** Notes, tasks and countdowns live in SL's local database. Calendar and contacts are read from the phone only with permission. */
@Singleton
class ProductivityRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val notes: NoteDao,
    private val tasks: TaskDao,
    private val countdowns: CountdownDao,
) {
    val notesFlow = notes.observeAll()
    val tasksFlow = tasks.observeAll()
    val countdownsFlow = countdowns.observeAll()

    suspend fun saveNote(note: NoteEntity) = notes.upsert(note.copy(updatedAt = System.currentTimeMillis()))
    suspend fun deleteNote(note: NoteEntity) = notes.delete(note)

    suspend fun saveTask(task: TaskEntity): Long {
        val id = if (task.id == 0L) tasks.upsert(task) else { tasks.upsert(task); task.id }
        val due = task.dueAt
        if (task.remind && due != null && !task.done && due > System.currentTimeMillis()) {
            ReminderReceiver.scheduleTask(context, id, task.title, due)
        } else {
            ReminderReceiver.cancelTask(context, id)
        }
        com.sllauncher.pro.widgets.TodayWidgetProvider.refresh(context)
        return id
    }

    /** Ticking off a repeating task moves it to its next date instead of finishing it. */
    suspend fun setDone(task: TaskEntity, done: Boolean): Long {
        val due = task.dueAt
        if (done && task.repeat != TaskRepeat.NONE && due != null) {
            return saveTask(task.copy(dueAt = nextOccurrence(due, task.repeat, System.currentTimeMillis())))
        }
        return saveTask(task.copy(done = done))
    }

    suspend fun deleteTask(task: TaskEntity) {
        ReminderReceiver.cancelTask(context, task.id)
        tasks.delete(task)
        com.sllauncher.pro.widgets.TodayWidgetProvider.refresh(context)
    }

    suspend fun saveCountdown(countdown: CountdownEntity) = countdowns.upsert(countdown)
    suspend fun deleteCountdown(countdown: CountdownEntity) = countdowns.delete(countdown)

    fun hasCalendarPermission() = granted(Manifest.permission.READ_CALENDAR)
    fun hasContactsPermission() = granted(Manifest.permission.READ_CONTACTS)
    private fun granted(p: String) = ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED

    /** Today's events from every calendar on the phone (Google, Samsung, Outlook...). */
    suspend fun todaysEvents(): List<CalendarEvent> = withContext(Dispatchers.IO) {
        if (!hasCalendarPermission()) return@withContext emptyList()
        val zone = ZoneId.systemDefault()
        val start = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = start + 24L * 3600 * 1000
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, start)
            ContentUris.appendId(it, end)
        }.build()
        val out = mutableListOf<CalendarEvent>()
        runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(CalendarContract.Instances.EVENT_ID, CalendarContract.Instances.TITLE, CalendarContract.Instances.BEGIN, CalendarContract.Instances.END, CalendarContract.Instances.ALL_DAY, CalendarContract.Instances.EVENT_LOCATION),
                null, null, "${CalendarContract.Instances.BEGIN} ASC",
            )?.use { c ->
                while (c.moveToNext()) {
                    out += CalendarEvent(c.getLong(0), c.getString(1) ?: "Event", c.getLong(2), c.getLong(3), c.getInt(4) == 1, c.getString(5))
                }
            }
        }
        out
    }

    /** Starred contacts, for one-tap call or message. */
    suspend fun quickContacts(): List<QuickContact> = withContext(Dispatchers.IO) {
        if (!hasContactsPermission()) return@withContext emptyList()
        val out = mutableListOf<QuickContact>()
        runCatching {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.CONTACT_ID, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER),
                "${ContactsContract.CommonDataKinds.Phone.STARRED} = 1",
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC",
            )?.use { c ->
                while (c.moveToNext()) out += QuickContact(c.getLong(0), c.getString(1) ?: "Contact", c.getString(2))
            }
        }
        out.distinctBy { it.id }.take(12)
    }
}

/**
 * The next due time of a repeating task after it is ticked off: the same
 * time of day, moved forward by the repeat rule until it is after [now].
 */
fun nextOccurrence(dueAt: Long, repeat: Int, now: Long, zone: ZoneId = ZoneId.systemDefault()): Long {
    var next = java.time.Instant.ofEpochMilli(dueAt).atZone(zone)
    do {
        next = when (repeat) {
            TaskRepeat.DAILY -> next.plusDays(1)
            TaskRepeat.WEEKLY -> next.plusWeeks(1)
            TaskRepeat.WEEKDAYS -> {
                var d = next.plusDays(1)
                while (d.dayOfWeek == java.time.DayOfWeek.SATURDAY || d.dayOfWeek == java.time.DayOfWeek.SUNDAY) d = d.plusDays(1)
                d
            }
            else -> return dueAt
        }
    } while (next.toInstant().toEpochMilli() <= now)
    return next.toInstant().toEpochMilli()
}

/** Whole days from today until [targetAt] (negative once passed). */
fun daysUntil(targetAt: Long, zone: ZoneId = ZoneId.systemDefault(), today: LocalDate = LocalDate.now(zone)): Long {
    val target = java.time.Instant.ofEpochMilli(targetAt).atZone(zone).toLocalDate()
    return java.time.temporal.ChronoUnit.DAYS.between(today, target)
}
