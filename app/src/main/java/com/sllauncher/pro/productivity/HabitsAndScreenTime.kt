package com.sllauncher.pro.productivity

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.sllauncher.core.designsystem.component.SLBlock
import com.sllauncher.core.designsystem.component.SLButton
import com.sllauncher.core.designsystem.component.SLButtonStyle
import com.sllauncher.core.designsystem.component.SLProgressBar
import com.sllauncher.core.designsystem.component.SLSearchField
import com.sllauncher.core.designsystem.component.SLSectionHeader
import com.sllauncher.core.designsystem.component.SLSwitch
import com.sllauncher.core.designsystem.theme.SLTheme
import com.sllauncher.pro.data.db.HabitCheckEntity
import com.sllauncher.pro.data.db.HabitDao
import com.sllauncher.pro.data.db.HabitEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

// ---------------------------------------------------------------- Habits

/** Consecutive days ending today (or yesterday, if today isn't ticked yet) on which the habit was done. */
fun habitStreak(days: Set<Long>, today: Long): Int {
    var day = if (today in days) today else today - 1
    var streak = 0
    while (day in days) {
        streak++
        day--
    }
    return streak
}

data class HabitRow(val habit: HabitEntity, val doneToday: Boolean, val streak: Int, val lastSeven: List<Boolean>)

@HiltViewModel
class HabitsViewModel @Inject constructor(private val dao: HabitDao) : ViewModel() {
    private fun today() = LocalDate.now(ZoneId.systemDefault()).toEpochDay()

    val rows: StateFlow<List<HabitRow>> = combine(dao.observeHabits(), dao.observeChecksSince(today() - 400)) { habits, checks ->
        val today = today()
        val byHabit = checks.groupBy({ it.habitId }, { it.epochDay }).mapValues { it.value.toSet() }
        habits.map { h ->
            val days = byHabit[h.id].orEmpty()
            HabitRow(h, today in days, habitStreak(days, today), (6 downTo 0).map { (today - it) in days })
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(name: String) = viewModelScope.launch { dao.insert(HabitEntity(name = name.trim(), createdAt = System.currentTimeMillis())) }

    fun toggleToday(row: HabitRow) = viewModelScope.launch {
        val check = HabitCheckEntity(row.habit.id, today())
        if (row.doneToday) dao.uncheck(check) else dao.check(check)
    }

    fun delete(row: HabitRow) = viewModelScope.launch {
        dao.deleteChecks(row.habit.id)
        dao.deleteHabit(row.habit.id)
    }
}

@Composable
fun HabitsTab(viewModel: HabitsViewModel = hiltViewModel()) {
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf("") }
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SLBlock(Modifier.fillMaxWidth(), shadow = true) {
                Text("New habit", style = SLTheme.typography.title, color = SLTheme.colors.textPrimary)
                SLSearchField(query = name, onQueryChange = { name = it.take(40) }, placeholder = "e.g. Read 10 pages")
                SLButton("Add habit", { viewModel.add(name); name = "" }, enabled = name.isNotBlank())
            }
        }
        if (rows.isEmpty()) {
            item { Text("Tick a habit off each day to build a streak. Everything stays on this phone.", style = SLTheme.typography.body, color = SLTheme.colors.textSecondary) }
        }
        items(rows, key = { it.habit.id }) { row ->
            SLBlock(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(row.habit.name, style = SLTheme.typography.title, color = SLTheme.colors.textPrimary)
                        Text(
                            if (row.streak == 1) "1-day streak" else "${row.streak}-day streak",
                            style = SLTheme.typography.caption,
                            color = if (row.streak > 0) SLTheme.colors.accentIndicator else SLTheme.colors.textSecondary,
                        )
                    }
                    SLSwitch(
                        checked = row.doneToday,
                        onCheckedChange = { viewModel.toggleToday(row) },
                        modifier = Modifier.semantics { contentDescription = "${row.habit.name} done today" },
                    )
                }
                Text(
                    "Last 7 days: " + row.lastSeven.joinToString(" ") { if (it) "●" else "○" },
                    style = SLTheme.typography.bodySmall,
                    color = SLTheme.colors.textSecondary,
                    modifier = Modifier.semantics { contentDescription = "Done on ${row.lastSeven.count { it }} of the last 7 days" },
                )
                SLButton("Delete", { viewModel.delete(row) }, style = SLButtonStyle.Ghost)
            }
        }
    }
}

// ---------------------------------------------------------------- Screen time

data class AppUsage(val packageName: String, val label: String, val foregroundMs: Long)

data class ScreenTimeState(val hasAccess: Boolean = false, val loading: Boolean = true, val totalMs: Long = 0, val apps: List<AppUsage> = emptyList())

/** Today's per-app foreground time from Android's usage statistics. Read on the phone only. */
@HiltViewModel
class ScreenTimeViewModel @Inject constructor(@param:ApplicationContext private val context: Context) : ViewModel() {
    private val _state = MutableStateFlow(ScreenTimeState())
    val state: StateFlow<ScreenTimeState> = _state.asStateFlow()

    fun hasAccess(): Boolean {
        val ops = context.getSystemService(AppOpsManager::class.java)
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun refresh() = viewModelScope.launch {
        if (!hasAccess()) {
            _state.value = ScreenTimeState(hasAccess = false, loading = false)
            return@launch
        }
        val apps = withContext(Dispatchers.IO) {
            val zone = ZoneId.systemDefault()
            val start = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
            val usm = context.getSystemService(UsageStatsManager::class.java)
            val pm = context.packageManager
            usm.queryAndAggregateUsageStats(start, System.currentTimeMillis())
                .values
                .filter { it.totalTimeInForeground >= 60_000L && it.packageName != context.packageName }
                .map { stats ->
                    val label = runCatching { pm.getApplicationLabel(pm.getApplicationInfo(stats.packageName, 0)).toString() }.getOrDefault(stats.packageName)
                    AppUsage(stats.packageName, label, stats.totalTimeInForeground)
                }
                .sortedByDescending { it.foregroundMs }
        }
        _state.value = ScreenTimeState(hasAccess = true, loading = false, totalMs = apps.sumOf { it.foregroundMs }, apps = apps.take(15))
    }
}

/** "1 h 25 min" / "12 min". */
fun formatDuration(ms: Long): String {
    val minutes = ms / 60_000L
    val h = minutes / 60
    val m = minutes % 60
    return if (h > 0) "$h h $m min" else "$m min"
}

@Composable
fun ScreenTimeTab(viewModel: ScreenTimeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    if (!state.loading && !state.hasAccess) {
        SLBlock(Modifier.padding(16.dp).fillMaxWidth(), shadow = true) {
            Text("See your screen time", style = SLTheme.typography.title, color = SLTheme.colors.textPrimary)
            Text(
                "SL needs Android's \"Usage access\" to show how long you used each app today. The numbers stay on this phone.",
                style = SLTheme.typography.bodySmall,
                color = SLTheme.colors.textSecondary,
            )
            SLButton("Allow usage access", {
                runCatching { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            })
        }
        return
    }
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            SLBlock(Modifier.fillMaxWidth(), shadow = true) {
                Text("Today", style = SLTheme.typography.caption, color = SLTheme.colors.textSecondary)
                Text(formatDuration(state.totalMs), style = SLTheme.typography.headline, color = SLTheme.colors.textPrimary)
                Text("Apps used for at least a minute. Android's figures are approximate.", style = SLTheme.typography.caption, color = SLTheme.colors.textSecondary)
            }
        }
        item { SLSectionHeader("Most used today") }
        val top = state.apps.firstOrNull()?.foregroundMs ?: 1L
        items(state.apps, key = { it.packageName }) { app ->
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row {
                    Text(app.label, style = SLTheme.typography.body, color = SLTheme.colors.textPrimary, modifier = Modifier.weight(1f))
                    Text(formatDuration(app.foregroundMs), style = SLTheme.typography.body, color = SLTheme.colors.textSecondary)
                }
                SLProgressBar(progress = app.foregroundMs.toFloat() / top, contentDescription = "${app.label}, ${formatDuration(app.foregroundMs)}")
            }
        }
    }
}
