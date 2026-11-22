package com.sllauncher.pro.controlcenter

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.AlarmClock
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.sllauncher.core.designsystem.component.SLBlock
import com.sllauncher.core.designsystem.component.SLButton
import com.sllauncher.core.designsystem.component.SLButtonStyle
import com.sllauncher.core.designsystem.component.SLMediaControls
import com.sllauncher.core.designsystem.component.SLSectionHeader
import com.sllauncher.core.designsystem.component.SLSlider
import com.sllauncher.core.designsystem.component.softInset
import com.sllauncher.core.designsystem.component.softRaised
import com.sllauncher.core.designsystem.component.softStyle
import com.sllauncher.core.designsystem.theme.SLTheme
import com.sllauncher.pro.common.SystemIntents
import com.sllauncher.pro.common.startSafely
import com.sllauncher.pro.data.settings.SettingsRepository
import com.sllauncher.pro.data.settings.ThemeMode
import com.sllauncher.pro.device.TorchController
import com.sllauncher.pro.media.MediaSessionRepository
import com.sllauncher.pro.notifications.AccessCard
import com.sllauncher.pro.notifications.NotificationCard
import com.sllauncher.pro.notifications.NotificationCenterViewModel
import com.sllauncher.pro.productivity.FocusTimer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

/** Which side of the home screen a swipe down started on decides which panel opens (iOS style). */
fun panelForSwipeStart(startX: Float, width: Float): HomePanel =
    if (width > 0f && startX > width / 2f) HomePanel.ControlCenter else HomePanel.Notifications

enum class HomePanel { ControlCenter, Notifications }

data class ControlState(
    val canWriteSettings: Boolean = false,
    val brightness: Float = 0.5f,
    val volume: Float = 0.5f,
    val dndAccess: Boolean = false,
    val dndOn: Boolean = false,
    val autoRotate: Boolean = true,
)

/**
 * SL Control Center. Switches what Android lets an app switch directly
 * (flashlight, brightness, volume, Do Not Disturb, rotation, SL theme, focus)
 * and opens Android's own panels for Wi-Fi, mobile data, Bluetooth and
 * airplane mode, which apps are not allowed to toggle.
 */
@HiltViewModel
class ControlCenterViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    val torch: TorchController,
    private val media: MediaSessionRepository,
    private val focusTimer: FocusTimer,
    private val settings: SettingsRepository,
) : ViewModel() {
    private val audio = context.getSystemService(AudioManager::class.java)
    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    private val _state = MutableStateFlow(read())
    val state: StateFlow<ControlState> = _state.asStateFlow()
    val nowPlaying = media.nowPlaying
    val focus = focusTimer.session
    val darkTheme: StateFlow<Boolean> = settings.settings.map { it.themeMode == ThemeMode.Dark }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun refresh() {
        _state.value = read()
    }

    private fun read(): ControlState {
        val canWrite = Settings.System.canWrite(context)
        val resolver = context.contentResolver
        val maxVolume = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        return ControlState(
            canWriteSettings = canWrite,
            brightness = runCatching { Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS) / 255f }.getOrDefault(0.5f),
            volume = audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume,
            dndAccess = notificationManager.isNotificationPolicyAccessGranted,
            dndOn = notificationManager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL,
            autoRotate = runCatching { Settings.System.getInt(resolver, Settings.System.ACCELEROMETER_ROTATION) == 1 }.getOrDefault(true),
        )
    }

    fun setBrightness(value: Float) {
        if (!Settings.System.canWrite(context)) return
        runCatching { Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, (value * 255).toInt().coerceIn(1, 255)) }
        _state.value = _state.value.copy(brightness = value)
    }

    fun setVolume(value: Float) {
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        runCatching { audio.setStreamVolume(AudioManager.STREAM_MUSIC, (value * max).toInt(), 0) }
        _state.value = _state.value.copy(volume = value)
    }

    /** False when SL has no Do Not Disturb access yet (the caller opens the access screen). */
    fun toggleDoNotDisturb(): Boolean {
        if (!notificationManager.isNotificationPolicyAccessGranted) return false
        val on = notificationManager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
        notificationManager.setInterruptionFilter(
            if (on) NotificationManager.INTERRUPTION_FILTER_ALL else NotificationManager.INTERRUPTION_FILTER_PRIORITY,
        )
        refresh()
        return true
    }

    /** False when SL may not change system settings yet (the caller opens that permission screen). */
    fun toggleRotationLock(): Boolean {
        if (!Settings.System.canWrite(context)) return false
        val auto = _state.value.autoRotate
        runCatching { Settings.System.putInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, if (auto) 0 else 1) }
        refresh()
        return true
    }

    fun toggleTheme() = viewModelScope.launch {
        settings.update { it.copy(themeMode = if (it.themeMode == ThemeMode.Dark) ThemeMode.Light else ThemeMode.Dark) }
    }

    fun toggleFocus() {
        if (focusTimer.session.value != null) focusTimer.stop() else focusTimer.start(25)
    }

    fun playPause() = media.togglePlayPause()
    fun next() = media.next()
    fun previous() = media.previous()
    fun openPlayer() = media.openApp()
}

/**
 * A panel that slides down from the top over the home screen. Closes on
 * Back, a tap below it, a swipe up on its handle or the handle's TalkBack
 * "Close" action.
 */
@Composable
fun TopSheet(visible: Boolean, onDismiss: () -> Unit, title: String, content: @Composable ColumnScope.() -> Unit) {
    val motion = SLTheme.motion
    val colors = SLTheme.colors
    val currentDismiss by rememberUpdatedState(onDismiss)
    BackHandler(enabled = visible) { currentDismiss() }

    AnimatedVisibility(visible, enter = fadeIn(motion.short()), exit = fadeOut(motion.short())) {
        Box(
            Modifier
                .fillMaxSize()
                .background(colors.scrim)
                .pointerInput(Unit) { detectTapGestures { currentDismiss() } },
        )
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val maxPanelHeight = maxHeight * 0.9f
        AnimatedVisibility(
            visible,
            enter = slideInVertically(motion.medium()) { -it } + fadeIn(motion.medium()),
            exit = slideOutVertically(motion.short()) { -it } + fadeOut(motion.short()),
        ) {
            val shape = RoundedCornerShape(bottomStart = SLTheme.shapes.cornerLarge, bottomEnd = SLTheme.shapes.cornerLarge)
            var dragTotal by remember { mutableFloatStateOf(0f) }
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxPanelHeight)
                    .softRaised(shape, softStyle(depth = SLTheme.shapes.panelDepth))
                    .clip(shape)
                    .semantics { paneTitle = title },
            ) {
                Column(
                    Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    content = content,
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragStart = { dragTotal = 0f },
                                onDragEnd = { if (dragTotal < -40f) currentDismiss() },
                            ) { _, amount -> dragTotal += amount }
                        }
                        .clickable(onClickLabel = "Close") { currentDismiss() }
                        .semantics {
                            contentDescription = "Close $title"
                            onClick(label = "Close") { currentDismiss(); true }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.size(width = 44.dp, height = 5.dp).background(colors.borderStrong, CircleShape))
                }
            }
        }
    }
}

private data class ControlTile(
    val label: String,
    val detail: String,
    val on: Boolean?,
    val onClick: () -> Unit,
)

@Composable
private fun ControlTileView(tile: ControlTile, modifier: Modifier = Modifier) {
    val colors = SLTheme.colors
    val shape = RoundedCornerShape(SLTheme.shapes.cornerMedium)
    val on = tile.on == true
    Box(
        modifier
            .aspectRatio(1f)
            .then(if (on) Modifier.softInset(shape, softStyle(colors.accent)) else Modifier.softRaised(shape, softStyle(depth = 4.dp)))
            .border(SLTheme.shapes.borderWidth, if (on) colors.accentOutline else colors.borderStrong, shape)
            .clip(shape)
            .clickable(role = if (tile.on != null) Role.Switch else Role.Button, onClick = tile.onClick)
            .semantics { if (tile.on != null) stateDescription = if (on) "On" else "Off" }
            .padding(6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                tile.label,
                style = SLTheme.typography.label,
                color = if (on) colors.onAccent else colors.textPrimary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                tile.detail,
                style = SLTheme.typography.caption,
                color = if (on) colors.onAccent else colors.textSecondary,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

/** iOS-style Control Center, opened by swiping down on the right half of the home screen. */
@Composable
fun ControlCenterPanel(
    visible: Boolean,
    onDismiss: () -> Unit,
    onOpenSlSettings: () -> Unit,
    viewModel: ControlCenterViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val torchOn by viewModel.torch.on.collectAsStateWithLifecycle()
    val nowPlaying by viewModel.nowPlaying.collectAsStateWithLifecycle()
    val focus by viewModel.focus.collectAsStateWithLifecycle()
    val dark by viewModel.darkTheme.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(visible) { if (visible) viewModel.refresh() }

    fun open(intent: Intent) = context.startSafely(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

    val tiles = listOfNotNull(
        ControlTile("Wi-Fi", "Panel ›", null) { open(SystemIntents.wifi()) },
        ControlTile("Mobile data", "Panel ›", null) { open(SystemIntents.internet()) },
        ControlTile("Bluetooth", "Settings ›", null) { open(SystemIntents.bluetooth()) },
        ControlTile("Airplane", "Settings ›", null) { open(SystemIntents.airplane()) },
        ControlTile("Flashlight", if (torchOn) "On" else "Off", torchOn) { viewModel.torch.set(!torchOn) }.takeIf { viewModel.torch.available },
        ControlTile("Do Not Disturb", if (!state.dndAccess) "Allow ›" else if (state.dndOn) "On" else "Off", state.dndOn.takeIf { state.dndAccess }) {
            if (!viewModel.toggleDoNotDisturb()) open(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        },
        ControlTile("Rotation lock", if (!state.canWriteSettings) "Allow ›" else if (!state.autoRotate) "On" else "Off", (!state.autoRotate).takeIf { state.canWriteSettings }) {
            if (!viewModel.toggleRotationLock()) open(SystemIntents.writeSettings(context))
        },
        ControlTile("Dark theme", if (dark) "On" else "Off", dark) { viewModel.toggleTheme() },
        ControlTile("Focus", if (focus != null) "On" else "25 min", focus != null) { viewModel.toggleFocus() },
        ControlTile("Hotspot", "Settings ›", null) { open(SystemIntents.hotspot()) },
        ControlTile("Battery", "Saver ›", null) { open(SystemIntents.battery()) },
        ControlTile("Sound", "Panel ›", null) { open(SystemIntents.volumePanel()) },
    )

    TopSheet(visible = visible, onDismiss = onDismiss, title = "Control Center") {
        Text("Control Center", style = SLTheme.typography.headline, color = SLTheme.colors.textPrimary)
        tiles.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { ControlTileView(it, Modifier.weight(1f)) }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }

        SLSectionHeader("Brightness")
        if (state.canWriteSettings) {
            SLSlider(value = state.brightness, onValueChange = viewModel::setBrightness, contentDescription = "Screen brightness")
        } else {
            SLButton("Allow brightness control", { open(SystemIntents.writeSettings(context)) }, style = SLButtonStyle.Secondary)
        }
        SLSectionHeader("Media volume")
        SLSlider(value = state.volume, onValueChange = viewModel::setVolume, contentDescription = "Media volume")

        nowPlaying?.let { np ->
            SLBlock(Modifier.fillMaxWidth(), shadow = true) {
                Text(np.appLabel, style = SLTheme.typography.caption, color = SLTheme.colors.accentIndicator)
                Text(
                    np.title,
                    style = SLTheme.typography.title,
                    color = SLTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable(onClickLabel = "Open player") { viewModel.openPlayer() },
                )
                np.artist?.let { Text(it, style = SLTheme.typography.bodySmall, color = SLTheme.colors.textSecondary, maxLines = 1) }
                SLMediaControls(
                    isPlaying = np.isPlaying,
                    onPlayPause = { viewModel.playPause() },
                    onPrevious = { viewModel.previous() },
                    onNext = { viewModel.next() },
                    canSkipPrevious = np.canSkipPrevious,
                    canSkipNext = np.canSkipNext,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SLButton("Camera", { onDismiss(); open(SystemIntents.camera()) }, style = SLButtonStyle.Secondary)
            SLButton("Timers", { onDismiss(); open(Intent(AlarmClock.ACTION_SHOW_TIMERS)) }, style = SLButtonStyle.Secondary)
            SLButton("Settings", { onDismiss(); onOpenSlSettings() }, style = SLButtonStyle.Secondary)
        }
        Text(
            "Android lets apps switch Wi-Fi, mobile data, Bluetooth and airplane mode only through its own panels, so those tiles open them.",
            style = SLTheme.typography.caption,
            color = SLTheme.colors.textSecondary,
        )
    }
}

/** iOS-style notifications panel, opened by swiping down on the left half of the home screen. */
@Composable
fun NotificationsPanel(
    visible: Boolean,
    onDismiss: () -> Unit,
    onOpenAll: () -> Unit,
    viewModel: NotificationCenterViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(visible) { if (visible) viewModel.refreshAccess() }
    val now = remember(visible) { Date() }

    TopSheet(visible = visible, onDismiss = onDismiss, title = "Notifications") {
        Text(DateFormat.getTimeFormat(context).format(now), style = SLTheme.typography.display, color = SLTheme.colors.textPrimary)
        Text(
            DateFormat.format("EEEE d MMMM", now).toString(),
            style = SLTheme.typography.title,
            color = SLTheme.colors.textSecondary,
        )
        if (!state.connected) {
            AccessCard(onOpenSettings = { context.startSafely(SystemIntents.notificationListenerSettings()) })
            return@TopSheet
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Notifications", style = SLTheme.typography.headline, color = SLTheme.colors.textPrimary, modifier = Modifier.weight(1f))
            if (state.groups.isNotEmpty()) SLButton("Clear all", { viewModel.dismissAll() }, style = SLButtonStyle.Ghost)
        }
        if (state.groups.isEmpty()) {
            Text("You're all caught up.", style = SLTheme.typography.body, color = SLTheme.colors.textSecondary)
        }
        state.groups.take(12).forEach { (app, items) ->
            SLSectionHeader("$app · ${items.size}")
            items.take(3).forEach { n -> NotificationCard(n, state.settings.hideNotificationContent, viewModel) }
            if (items.size > 3) {
                Text("+${items.size - 3} more", style = SLTheme.typography.caption, color = SLTheme.colors.textSecondary)
            }
        }
        SLButton("Open notification center", { onDismiss(); onOpenAll() }, style = SLButtonStyle.Secondary)
    }
}
