package com.sllauncher.pro.widgets

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.sllauncher.pro.common.UiCommand
import com.sllauncher.pro.common.UiCommandBus
import com.sllauncher.pro.data.settings.SettingsRepository
import com.sllauncher.pro.home.GridSpec
import com.sllauncher.pro.home.HomeLayoutRepository
import com.sllauncher.pro.home.widgetSpan
import com.sllauncher.pro.ui.ScreenScaffold
import com.sllauncher.core.designsystem.component.SLEmptyState
import com.sllauncher.core.designsystem.component.SLLoadingIndicator
import com.sllauncher.core.designsystem.component.SLSearchField
import com.sllauncher.core.designsystem.component.SLSectionHeader
import com.sllauncher.core.designsystem.theme.SLTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class WidgetPickerViewModel @Inject constructor(
    val host: WidgetHostManager,
    private val layout: HomeLayoutRepository,
    private val settings: SettingsRepository,
    private val bus: UiCommandBus,
) : ViewModel() {
    private val _options = MutableStateFlow<List<WidgetOption>?>(null)
    val options: StateFlow<List<WidgetOption>?> = _options.asStateFlow()

    init {
        viewModelScope.launch { _options.value = withContext(Dispatchers.IO) { host.availableWidgets() } }
    }

    /** Places a bound and configured widget using its minimum size. */
    fun place(widgetId: Int, cellWidthDp: Float, cellHeightDp: Float, onDone: () -> Unit) = viewModelScope.launch {
        val info = host.info(widgetId)
        if (info == null) {
            host.deleteId(widgetId)
            bus.send(UiCommand.ShowMessage("That widget couldn't be added."))
            onDone(); return@launch
        }
        val s = settings.current
        val grid = GridSpec(s.homeColumns, s.homeRows)
        val density = android.content.res.Resources.getSystem().displayMetrics.density
        val (spanX, spanY) = widgetSpan((info.minWidth / density).toInt(), (info.minHeight / density).toInt(), cellWidthDp, cellHeightDp, grid)
        val page = layout.addWidget(widgetId, spanX, spanY, grid, s.homePages, preferredPage = 0)
        if (page + 1 > s.homePages) settings.update { it.copy(homePages = page + 1) }
        bus.send(UiCommand.ShowMessage("Widget added to page ${page + 1}."))
        onDone()
    }

    fun cancel(widgetId: Int) = host.deleteId(widgetId)
}

/**
 * Lists every widget installed apps offer. Adding one asks Android for
 * permission the first time (system dialog), then runs the widget's own
 * setup screen if it has one.
 */
@Composable
fun WidgetPickerRoute(onBack: () -> Unit, viewModel: WidgetPickerViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val options by viewModel.options.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var pendingId by remember { mutableIntStateOf(-1) }
    var pendingOption by remember { mutableStateOf<WidgetOption?>(null) }

    val metrics = context.resources.displayMetrics
    val host = viewModel.host
    val columns = 4
    val cellWidthDp = metrics.widthPixels / metrics.density / columns
    val cellHeightDp = metrics.heightPixels / metrics.density * 0.62f / 5

    val configureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.place(pendingId, cellWidthDp, cellHeightDp, onBack)
        } else {
            viewModel.cancel(pendingId)
        }
    }

    fun configureOrPlace(widgetId: Int, option: WidgetOption) {
        val configure = option.info.configure
        if (configure == null) {
            viewModel.place(widgetId, cellWidthDp, cellHeightDp, onBack)
            return
        }
        try {
            configureLauncher.launch(
                Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
                    .setComponent(configure)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId),
            )
        } catch (e: ActivityNotFoundException) {
            viewModel.place(widgetId, cellWidthDp, cellHeightDp, onBack)
        } catch (e: SecurityException) {
            // Setup screen not reachable from a launcher (some work-profile widgets): place with defaults.
            viewModel.place(widgetId, cellWidthDp, cellHeightDp, onBack)
        }
    }

    val bindLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val option = pendingOption
        if (result.resultCode == Activity.RESULT_OK && option != null) configureOrPlace(pendingId, option) else viewModel.cancel(pendingId)
    }

    ScreenScaffold(title = "Widgets", onBack = onBack) {
        SLSearchField(query = query, onQueryChange = { query = it }, placeholder = "Search widgets", modifier = Modifier.padding(16.dp))
        val list = options
        when {
            list == null -> SLLoadingIndicator(Modifier.padding(24.dp))
            list.isEmpty() -> SLEmptyState(title = "No widgets available", body = "None of your installed apps offer widgets.")
            else -> {
                val filtered = list.filter { query.isBlank() || it.label.contains(query, true) || it.appLabel.contains(query, true) }
                LazyColumn(Modifier.fillMaxSize()) {
                    filtered.groupBy { it.appLabel }.forEach { (app, widgets) ->
                        item(key = "h:$app") { SLSectionHeader(app, Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)) }
                        items(widgets, key = { "${it.info.provider.flattenToString()}#${it.user.hashCode()}" }) { option ->
                            WidgetRow(option, host) {
                                val id = viewModel.host.allocateId()
                                pendingId = id
                                pendingOption = option
                                if (viewModel.host.bindIfAllowed(id, option)) {
                                    configureOrPlace(id, option)
                                } else {
                                    bindLauncher.launch(
                                        Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                                            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                                            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, option.info.provider)
                                            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, option.user),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetRow(option: WidgetOption, host: WidgetHostManager, onAdd: () -> Unit) {
    val density = LocalDensity.current
    val sizePx = with(density) { 72.dp.roundToPx() }
    val preview by produceState<android.graphics.Bitmap?>(null, option) {
        value = withContext(Dispatchers.IO) { host.previewOf(option)?.let { runCatching { it.toBitmap(sizePx, sizePx) }.getOrNull() } }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 88.dp)
            .clickable(role = Role.Button, onClickLabel = "Add to home screen", onClick = onAdd)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        val bmp = preview
        if (bmp != null) {
            Image(bmp.asImageBitmap(), contentDescription = null, modifier = Modifier.size(72.dp))
        } else {
            Column(Modifier.size(72.dp).background(SLTheme.colors.surfaceElevated)) {}
        }
        Column(Modifier.weight(1f)) {
            Text(option.label, style = SLTheme.typography.body, color = SLTheme.colors.textPrimary)
            val d = android.content.res.Resources.getSystem().displayMetrics.density
            Text(
                "Minimum ${(option.info.minWidth / d).toInt()} × ${(option.info.minHeight / d).toInt()} dp",
                style = SLTheme.typography.caption,
                color = SLTheme.colors.textSecondary,
            )
        }
    }
}
