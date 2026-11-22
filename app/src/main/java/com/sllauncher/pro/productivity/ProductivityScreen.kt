package com.sllauncher.pro.productivity

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.CalendarContract
import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.sllauncher.pro.common.SystemIntents
import com.sllauncher.pro.common.startSafely
import com.sllauncher.pro.data.db.CountdownEntity
import com.sllauncher.pro.data.db.NoteEntity
import com.sllauncher.pro.data.db.TaskEntity
import com.sllauncher.pro.ui.ScreenScaffold
import com.sllauncher.core.designsystem.component.SLBlock
import com.sllauncher.core.designsystem.component.SLBottomSheet
import com.sllauncher.core.designsystem.component.SLButton
import com.sllauncher.core.designsystem.component.SLButtonStyle
import com.sllauncher.core.designsystem.component.SLChip
import com.sllauncher.core.designsystem.component.SLEmptyState
import com.sllauncher.core.designsystem.component.SLProgressBar
import com.sllauncher.core.designsystem.component.SLSearchField
import com.sllauncher.core.designsystem.component.SLSectionHeader
import com.sllauncher.core.designsystem.component.SLSwitch
import com.sllauncher.core.designsystem.component.SLSwitchRow
import com.sllauncher.core.designsystem.theme.SLTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

enum class ProductivityTab(val label: String) { Today("Today"), Tasks("Tasks"), Habits("Habits"), Notes("Notes"), Focus("Focus"), ScreenTime("Screen time"), Countdowns("Countdowns"), Contacts("Contacts") }

data class ProductivityState(
    val notes: List<NoteEntity> = emptyList(),
    val tasks: List<TaskEntity> = emptyList(),
    val countdowns: List<CountdownEntity> = emptyList(),
    val events: List<CalendarEvent> = emptyList(),
    val contacts: List<QuickContact> = emptyList(),
    val focus: FocusSession? = null,
    val calendarPermission: Boolean = false,
    val contactsPermission: Boolean = false,
)

@HiltViewModel
class ProductivityViewModel @Inject constructor(
    private val repo: ProductivityRepository,
    private val focusTimer: FocusTimer,
) : ViewModel() {
    private val events = MutableStateFlow<List<CalendarEvent>>(emptyList())
    private val contacts = MutableStateFlow<List<QuickContact>>(emptyList())
    private val perms = MutableStateFlow(repo.hasCalendarPermission() to repo.hasContactsPermission())

    val state: StateFlow<ProductivityState> = combine(
        combine(repo.notesFlow, repo.tasksFlow, repo.countdownsFlow) { n, t, c -> Triple(n, t, c) },
        combine(events, contacts) { e, c -> e to c },
        focusTimer.session,
        perms,
    ) { (n, t, c), (e, qc), f, (cal, con) ->
        ProductivityState(n, t, c, e, qc, f, cal, con)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProductivityState())

    fun refresh() = viewModelScope.launch {
        perms.value = repo.hasCalendarPermission() to repo.hasContactsPermission()
        events.value = repo.todaysEvents()
        contacts.value = repo.quickContacts()
    }

    fun saveNote(note: NoteEntity) = viewModelScope.launch { repo.saveNote(note) }
    fun deleteNote(note: NoteEntity) = viewModelScope.launch { repo.deleteNote(note) }
    fun saveTask(task: TaskEntity) = viewModelScope.launch { repo.saveTask(task) }
    fun toggleTask(task: TaskEntity) = viewModelScope.launch { repo.setDone(task, !task.done) }
    fun deleteTask(task: TaskEntity) = viewModelScope.launch { repo.deleteTask(task) }
    fun saveCountdown(c: CountdownEntity) = viewModelScope.launch { repo.saveCountdown(c) }
    fun deleteCountdown(c: CountdownEntity) = viewModelScope.launch { repo.deleteCountdown(c) }
    fun startFocus(minutes: Int, pomodoro: Boolean) = focusTimer.start(minutes, if (pomodoro) "Pomodoro" else "Focus", pomodoroRound = if (pomodoro) 1 else null)
    fun stopFocus() = focusTimer.stop()
}

@Composable
fun ProductivityRoute(onBack: () -> Unit, viewModel: ProductivityViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var tab by rememberSaveable { mutableStateOf(ProductivityTab.Today) }
    var editingNote by remember { mutableStateOf<NoteEntity?>(null) }
    var editingTask by remember { mutableStateOf<TaskEntity?>(null) }
    var addingCountdown by remember { mutableStateOf(false) }

    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val calendarPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { viewModel.refresh() }
    val contactsPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { viewModel.refresh() }
    fun ensureNotifications() {
        if (!ReminderReceiver.canNotify(context) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    ScreenScaffold(title = "Productivity", onBack = onBack) {
        LazyRow(contentPadding = PaddingValues(12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(ProductivityTab.entries) { t -> SLChip(t.label, tab == t, { tab = t }) }
        }
        when (tab) {
            ProductivityTab.Today -> LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { SLSectionHeader("Calendar") }
                if (!state.calendarPermission) {
                    item {
                        SLBlock(Modifier.fillMaxWidth()) {
                            Text("Show today's events from your calendars. SL only reads them on this phone.", style = SLTheme.typography.bodySmall, color = SLTheme.colors.textSecondary)
                            SLButton("Allow calendar access", { calendarPermission.launch(Manifest.permission.READ_CALENDAR) }, style = SLButtonStyle.Secondary)
                        }
                    }
                } else if (state.events.isEmpty()) {
                    item { Text("No events today.", style = SLTheme.typography.body, color = SLTheme.colors.textSecondary) }
                } else {
                    items(state.events, key = { "e${it.id}${it.begin}" }) { e ->
                        Column(Modifier.fillMaxWidth()) {
                            Text(e.title, style = SLTheme.typography.body, color = SLTheme.colors.textPrimary)
                            Text(if (e.allDay) "All day" else "${timeOf(context, e.begin)} – ${timeOf(context, e.end)}${e.location?.let { " · $it" } ?: ""}", style = SLTheme.typography.caption, color = SLTheme.colors.textSecondary)
                        }
                    }
                }
                item {
                    SLButton("Add event", {
                        context.startSafely(Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI))
                    }, style = SLButtonStyle.Secondary)
                }
                item { SLSectionHeader("Due today") }
                val today = state.tasks.filter { !it.done && it.dueAt != null && DateUtils.isToday(it.dueAt) }
                if (today.isEmpty()) item { Text("Nothing due today.", style = SLTheme.typography.body, color = SLTheme.colors.textSecondary) }
                items(today, key = { "t${it.id}" }) { t -> TaskRow(t, context, viewModel, onEdit = { editingTask = t }) }
                state.focus?.let { f -> item { FocusCard(f, viewModel) } }
            }
            ProductivityTab.Tasks -> {
                SLButton("New task", { editingTask = TaskEntity(title = "", createdAt = System.currentTimeMillis()) }, modifier = Modifier.padding(12.dp))
                if (state.tasks.isEmpty()) SLEmptyState("No tasks", "Add one here, or ask SL: \"remind me to call mum\".")
                LazyColumn(contentPadding = PaddingValues(12.dp)) {
                    items(state.tasks, key = { it.id }) { t -> TaskRow(t, context, viewModel, onEdit = { editingTask = t }) }
                }
            }
            ProductivityTab.Notes -> {
                SLButton("New note", { editingNote = NoteEntity(title = "", body = "", updatedAt = System.currentTimeMillis()) }, modifier = Modifier.padding(12.dp))
                if (state.notes.isEmpty()) SLEmptyState("No notes", "Write one here, or ask SL: \"note: buy bread\".")
                LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.notes, key = { it.id }) { n ->
                        SLBlock(Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text((if (n.pinned) "★ " else "") + n.title.ifEmpty { "Untitled" }, style = SLTheme.typography.title, color = SLTheme.colors.textPrimary, modifier = Modifier.weight(1f))
                                SLButton("Edit", { editingNote = n }, style = SLButtonStyle.Ghost)
                            }
                            Text(n.body, style = SLTheme.typography.bodySmall, color = SLTheme.colors.textSecondary, maxLines = 4, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            ProductivityTab.Focus -> Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val session = state.focus
                if (session != null) {
                    FocusCard(session, viewModel)
                } else {
                    Text("Start a focus session. It shows in the Dynamic Island, and you'll get a notification when time is up.", style = SLTheme.typography.body, color = SLTheme.colors.textSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(15, 25, 50).forEach { m -> SLButton("$m min", { ensureNotifications(); viewModel.startFocus(m, pomodoro = false) }, style = SLButtonStyle.Secondary) }
                    }
                    SLButton("Pomodoro (25 + 5)", { ensureNotifications(); viewModel.startFocus(25, pomodoro = true) })
                }
            }
            ProductivityTab.Habits -> HabitsTab()
            ProductivityTab.ScreenTime -> ScreenTimeTab()
            ProductivityTab.Countdowns -> {
                SLButton("New countdown", { addingCountdown = true }, modifier = Modifier.padding(12.dp))
                if (state.countdowns.isEmpty()) SLEmptyState("No countdowns", "Count down to exams, birthdays or trips.")
                LazyColumn(contentPadding = PaddingValues(12.dp)) {
                    items(state.countdowns, key = { it.id }) { c ->
                        val days = daysUntil(c.targetAt)
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(c.title, style = SLTheme.typography.body, color = SLTheme.colors.textPrimary)
                                Text(DateUtils.formatDateTime(context, c.targetAt, DateUtils.FORMAT_SHOW_DATE), style = SLTheme.typography.caption, color = SLTheme.colors.textSecondary)
                            }
                            Text(when { days > 1 -> "$days days"; days == 1L -> "Tomorrow"; days == 0L -> "Today"; else -> "Passed" }, style = SLTheme.typography.title, color = SLTheme.colors.accentIndicator)
                            SLButton("Delete", { viewModel.deleteCountdown(c) }, style = SLButtonStyle.Ghost)
                        }
                    }
                }
            }
            ProductivityTab.Contacts -> {
                if (!state.contactsPermission) {
                    SLBlock(Modifier.padding(16.dp).fillMaxWidth()) {
                        Text("Show your starred contacts for one-tap calls and messages.", style = SLTheme.typography.bodySmall, color = SLTheme.colors.textSecondary)
                        SLButton("Allow contacts access", { contactsPermission.launch(Manifest.permission.READ_CONTACTS) }, style = SLButtonStyle.Secondary)
                    }
                } else if (state.contacts.isEmpty()) {
                    SLEmptyState("No starred contacts", "Star contacts in your Contacts app to see them here.")
                } else {
                    LazyColumn(contentPadding = PaddingValues(12.dp)) {
                        items(state.contacts, key = { it.id }) { c ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(c.name, style = SLTheme.typography.body, color = SLTheme.colors.textPrimary, modifier = Modifier.weight(1f))
                                if (c.number != null) {
                                    SLButton("Call", { context.startSafely(SystemIntents.dial(c.number)) }, style = SLButtonStyle.Secondary)
                                    SLButton("Text", { context.startSafely(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${c.number}"))) }, style = SLButtonStyle.Ghost)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    editingNote?.let { note -> NoteEditor(note, onSave = { viewModel.saveNote(it); editingNote = null }, onDelete = { viewModel.deleteNote(note); editingNote = null }, onDismiss = { editingNote = null }) }
    editingTask?.let { task -> TaskEditor(task, context, onSave = { if (it.remind) ensureNotifications(); viewModel.saveTask(it); editingTask = null }, onDelete = { viewModel.deleteTask(task); editingTask = null }, onDismiss = { editingTask = null }) }
    if (addingCountdown) CountdownEditor(context, onSave = { viewModel.saveCountdown(it); addingCountdown = false }, onDismiss = { addingCountdown = false })
}

private fun timeOf(context: Context, millis: Long) = DateFormat.getTimeFormat(context).format(java.util.Date(millis))

@Composable
private fun TaskRow(t: TaskEntity, context: Context, viewModel: ProductivityViewModel, onEdit: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SLSwitch(checked = t.done, onCheckedChange = { viewModel.toggleTask(t) })
        Column(Modifier.weight(1f)) {
            Text(t.title, style = SLTheme.typography.body, color = if (t.done) SLTheme.colors.textSecondary else SLTheme.colors.textPrimary, textDecoration = if (t.done) TextDecoration.LineThrough else null)
            t.dueAt?.let { Text(DateUtils.formatDateTime(context, it, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME) + (if (t.remind) " · reminder" else "") + (if (t.repeat != 0) " · repeats" else ""), style = SLTheme.typography.caption, color = SLTheme.colors.textSecondary) }
        }
        SLButton("Edit", onEdit, style = SLButtonStyle.Ghost)
    }
}

@Composable
private fun FocusCard(f: FocusSession, viewModel: ProductivityViewModel) {
    var now by remember { mutableLongStateOf(android.os.SystemClock.elapsedRealtime()) }
    LaunchedEffect(f) { while (true) { now = android.os.SystemClock.elapsedRealtime(); delay(1000) } }
    SLBlock(Modifier.fillMaxWidth(), shadow = true) {
        val remaining = f.remainingMs(now) / 1000
        Text("%d:%02d".format(remaining / 60, remaining % 60), style = SLTheme.typography.display, color = SLTheme.colors.textPrimary)
        Text(f.label + (f.pomodoroRound?.let { " · round $it" } ?: ""), style = SLTheme.typography.body, color = SLTheme.colors.textSecondary)
        SLProgressBar(1f - f.remainingMs(now).toFloat() / f.totalMs, contentDescription = "Focus progress")
        SLButton("Stop", { viewModel.stopFocus() }, style = SLButtonStyle.Secondary)
    }
}

@Composable
private fun NoteEditor(note: NoteEntity, onSave: (NoteEntity) -> Unit, onDelete: () -> Unit, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf(note.title) }
    var body by remember { mutableStateOf(note.body) }
    var pinned by remember { mutableStateOf(note.pinned) }
    SLBottomSheet(onDismissRequest = onDismiss, title = if (note.id == 0L) "New note" else "Edit note") {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SLSearchField(query = title, onQueryChange = { title = it }, placeholder = "Title")
            SLSearchField(query = body, onQueryChange = { body = it }, placeholder = "Note")
            SLSwitchRow(title = "Pin to top", checked = pinned, onCheckedChange = { pinned = it })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SLButton("Save", { onSave(note.copy(title = title.trim(), body = body, pinned = pinned)) }, enabled = title.isNotBlank() || body.isNotBlank())
                if (note.id != 0L) SLButton("Delete", onDelete, style = SLButtonStyle.Destructive)
            }
        }
    }
}

@Composable
private fun TaskEditor(task: TaskEntity, context: Context, onSave: (TaskEntity) -> Unit, onDelete: () -> Unit, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf(task.title) }
    var due by remember { mutableStateOf(task.dueAt) }
    var remind by remember { mutableStateOf(task.remind) }
    var repeat by remember { mutableStateOf(task.repeat) }
    SLBottomSheet(onDismissRequest = onDismiss, title = if (task.id == 0L) "New task" else "Edit task") {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SLSearchField(query = title, onQueryChange = { title = it }, placeholder = "What needs doing?")
            Text(due?.let { "Due " + DateUtils.formatDateTime(context, it, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME) } ?: "No due date", style = SLTheme.typography.body, color = SLTheme.colors.textSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SLButton("Set due time", { pickDateTime(context, due) { due = it } }, style = SLButtonStyle.Secondary)
                if (due != null) SLButton("Clear", { due = null; remind = false }, style = SLButtonStyle.Ghost)
            }
            SLSwitchRow(title = "Remind me", subtitle = "Notification at the due time (within about a minute)", checked = remind, enabled = due != null, onCheckedChange = { remind = it })
            if (due != null) {
                Text("Repeat", style = SLTheme.typography.label, color = SLTheme.colors.textSecondary)
                com.sllauncher.core.designsystem.component.SLSegmentedControl(
                    options = listOf("Never", "Daily", "Weekly", "Weekdays").map { com.sllauncher.core.designsystem.component.SLSegment(it) },
                    selectedIndex = repeat,
                    onSelect = { repeat = it },
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SLButton("Save", { onSave(task.copy(title = title.trim(), dueAt = due, remind = remind && due != null, repeat = if (due != null) repeat else 0)) }, enabled = title.isNotBlank())
                if (task.id != 0L) SLButton("Delete", onDelete, style = SLButtonStyle.Destructive)
            }
        }
    }
}

@Composable
private fun CountdownEditor(context: Context, onSave: (CountdownEntity) -> Unit, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var target by remember { mutableStateOf<Long?>(null) }
    SLBottomSheet(onDismissRequest = onDismiss, title = "New countdown") {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SLSearchField(query = title, onQueryChange = { title = it }, placeholder = "e.g. WAEC exams")
            Text(target?.let { DateUtils.formatDateTime(context, it, DateUtils.FORMAT_SHOW_DATE) } ?: "No date", style = SLTheme.typography.body, color = SLTheme.colors.textSecondary)
            SLButton("Pick date", { pickDate(context, target) { target = it } }, style = SLButtonStyle.Secondary)
            SLButton("Save", { target?.let { onSave(CountdownEntity(title = title.trim(), targetAt = it)) } }, enabled = title.isNotBlank() && target != null)
        }
    }
}

private fun pickDate(context: Context, initial: Long?, onPicked: (Long) -> Unit) {
    val cal = Calendar.getInstance().apply { initial?.let { timeInMillis = it } }
    DatePickerDialog(context, { _, y, m, d ->
        onPicked(Calendar.getInstance().apply { set(y, m, d, 9, 0, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis)
    }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
}

private fun pickDateTime(context: Context, initial: Long?, onPicked: (Long) -> Unit) {
    pickDate(context, initial) { day ->
        val cal = Calendar.getInstance().apply { timeInMillis = initial ?: (System.currentTimeMillis() + 3_600_000) }
        TimePickerDialog(context, { _, h, min ->
            onPicked(Calendar.getInstance().apply { timeInMillis = day; set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, min) }.timeInMillis)
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), DateFormat.is24HourFormat(context)).show()
    }
}
