package com.sllauncher.pro.ui

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.sllauncher.pro.common.UiCommand
import com.sllauncher.pro.common.UiCommandBus
import com.sllauncher.pro.navigation.ProductivityDestination
import com.sllauncher.pro.productivity.CalendarEvent
import com.sllauncher.pro.productivity.ProductivityRepository
import com.sllauncher.pro.productivity.daysUntil
import com.sllauncher.core.designsystem.theme.SLTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.foundation.shape.RoundedCornerShape
import com.sllauncher.core.designsystem.component.softRaised
import com.sllauncher.core.designsystem.component.softStyle

data class OverviewState(val lines: List<String> = emptyList())

@HiltViewModel
class DailyOverviewViewModel @Inject constructor(
    private val repo: ProductivityRepository,
    private val bus: UiCommandBus,
) : ViewModel() {
    private val events = MutableStateFlow<List<CalendarEvent>>(emptyList())

    val state: StateFlow<OverviewState> = combine(repo.tasksFlow, repo.countdownsFlow, events) { tasks, countdowns, evts ->
        val now = System.currentTimeMillis()
        buildList {
            val upcoming = evts.firstOrNull { it.end > now }
            if (upcoming != null) add("Next: ${upcoming.title}")
            val dueToday = tasks.count { !it.done && it.dueAt != null && DateUtils.isToday(it.dueAt) }
            if (dueToday > 0) add("$dueToday task${if (dueToday == 1) "" else "s"} due today")
            countdowns.firstOrNull { daysUntil(it.targetAt) >= 0 }?.let {
                val d = daysUntil(it.targetAt)
                add(if (d == 0L) "${it.title} is today" else "${it.title} in $d day${if (d == 1L) "" else "s"}")
            }
        }.let { OverviewState(it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OverviewState())

    fun refresh() = viewModelScope.launch { events.value = repo.todaysEvents() }
    fun open() = bus.send(UiCommand.Navigate(ProductivityDestination))
}

/** Compact "today" block under the home clock. Shows nothing when there is nothing to say. */
@Composable
fun DailyOverview(viewModel: DailyOverviewViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    if (state.lines.isEmpty()) return
    Column(
        Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .softRaised(RoundedCornerShape(SLTheme.shapes.cornerMedium), softStyle(SLTheme.colors.surface.copy(alpha = 0.92f)))
            .clickable(role = Role.Button, onClickLabel = "Open productivity") { viewModel.open() }
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        state.lines.forEach {
            Text(it, style = SLTheme.typography.bodySmall, color = SLTheme.colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
