package com.sllauncher.pro.assistant

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sllauncher.pro.common.SystemIntents
import com.sllauncher.pro.common.startSafely
import com.sllauncher.core.designsystem.component.SLAssistantButton
import com.sllauncher.core.designsystem.component.SLAssistantState
import com.sllauncher.core.designsystem.component.SLButton
import com.sllauncher.core.designsystem.component.SLButtonStyle
import com.sllauncher.core.designsystem.component.SLFloatingPanel
import com.sllauncher.core.designsystem.component.SLSearchField
import com.sllauncher.core.designsystem.theme.SLTheme

/** The SL assistant panel: conversation, voice button and text input. */
@Composable
fun AssistantOverlay(visible: Boolean, onDismiss: () -> Unit, viewModel: AssistantViewModel = hiltViewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var draft by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.startListening() else viewModel.onButton(hasMicPermission = false)
    }

    LaunchedEffect(ui.messages.size) { if (ui.messages.isNotEmpty()) listState.animateScrollToItem(ui.messages.size - 1) }
    LaunchedEffect(visible) { if (!visible) viewModel.interrupt() }

    SLFloatingPanel(visible = visible, onDismissRequest = { viewModel.interrupt(); onDismiss() }, title = "SL assistant") {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("SL", style = SLTheme.typography.headline, color = SLTheme.colors.accentIndicator)
            Text(
                when (ui.state) {
                    SLAssistantState.Listening -> "  Listening…"
                    SLAssistantState.Processing -> "  Thinking…"
                    SLAssistantState.Speaking -> "  Speaking. Tap to interrupt."
                    SLAssistantState.Idle -> "  Ready"
                },
                style = SLTheme.typography.bodySmall,
                color = SLTheme.colors.textSecondary,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listState, contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ui.messages, key = { it.id }) { m -> Bubble(m, viewModel, onOpenSettings = { context.startSafely(SystemIntents.notificationListenerSettings()) }) }
            if (ui.partial.isNotEmpty()) item { Text(ui.partial, style = SLTheme.typography.body, color = SLTheme.colors.textSecondary, modifier = Modifier.padding(8.dp)) }
        }
        Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
            SLAssistantButton(state = ui.state, onClick = {
                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                if (ui.state == SLAssistantState.Idle && !granted) micPermission.launch(Manifest.permission.RECORD_AUDIO) else viewModel.onButton(granted)
            })
        }
        if (!ui.onDeviceVoice && ui.voiceAvailable) {
            Text(
                "Voice is processed by your phone's speech service, which may use the internet.",
                style = SLTheme.typography.caption,
                color = SLTheme.colors.textSecondary,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        SLSearchField(
            query = draft,
            onQueryChange = { draft = it },
            placeholder = "Type a request",
            onSearch = { viewModel.submit(draft); draft = "" },
            modifier = Modifier.padding(12.dp),
        )
    }
}

@Composable
private fun Bubble(m: ChatMessage, viewModel: AssistantViewModel, onOpenSettings: () -> Unit) {
    val mine = m.from == Sender.User
    Box(Modifier.fillMaxWidth(), contentAlignment = if (mine) Alignment.CenterEnd else Alignment.CenterStart) {
        Column(
            Modifier
                .widthIn(max = 300.dp)
                .background(if (mine) SLTheme.colors.surfaceElevated else SLTheme.colors.background)
                .border(SLTheme.shapes.borderWidth, if (mine) SLTheme.colors.border else SLTheme.colors.accentOutline)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(m.text, style = SLTheme.typography.body, color = SLTheme.colors.textPrimary)
            m.confirm?.let { command ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SLButton("Yes", { viewModel.confirm(command) })
                    SLButton("No", { viewModel.decline() }, style = SLButtonStyle.Ghost)
                }
            }
            m.settingsHint?.let {
                SLButton("Open settings", onOpenSettings, style = SLButtonStyle.Secondary)
            }
        }
    }
}
