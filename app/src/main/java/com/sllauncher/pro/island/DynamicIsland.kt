package com.sllauncher.pro.island

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.SystemClock
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sllauncher.pro.common.AppIcon
import com.sllauncher.pro.common.SystemIntents
import com.sllauncher.pro.common.startSafely
import com.sllauncher.pro.data.settings.IslandPosition
import com.sllauncher.pro.device.PowerSource
import com.sllauncher.pro.notifications.SLNotification
import com.sllauncher.core.designsystem.component.SLBottomSheet
import com.sllauncher.core.designsystem.component.SLButton
import com.sllauncher.core.designsystem.component.SLButtonStyle
import com.sllauncher.core.designsystem.component.SLChip
import com.sllauncher.core.designsystem.component.SLIconButton
import com.sllauncher.core.designsystem.component.SLIconButtonStyle
import com.sllauncher.core.designsystem.component.SLMediaControls
import com.sllauncher.core.designsystem.component.SLNavigationRow
import com.sllauncher.core.designsystem.component.SLProgressBar
import com.sllauncher.core.designsystem.component.SLSearchField
import com.sllauncher.core.designsystem.icon.SLIcons
import com.sllauncher.core.designsystem.theme.SLTheme
import kotlinx.coroutines.delay
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape

/** The island is always black, like the hardware cutout it echoes, in every theme. */
private val IslandBlack = Color(0xFF090909)
private val IslandText = Color(0xFFF5F1E8)
private val IslandMuted = Color(0xFFA5A5A5)

private val SuggestedReplies = listOf("OK", "On my way", "Thanks!", "Can't talk now, I'll call you later")
private val QuickEmoji = listOf("👍", "😂", "❤️", "🙏", "🔥", "👌")

/**
 * SL's Dynamic Island: live activities at the top of the home screen.
 * It lives inside the launcher (Android restricts drawing over other apps).
 */
@Composable
fun DynamicIsland(onOpenSettings: () -> Unit, viewModel: IslandViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    if (!state.enabled) return

    var expanded by rememberSaveable { mutableStateOf(false) }
    var index by rememberSaveable { mutableIntStateOf(0) }
    var controlsFor by remember { mutableStateOf<IslandActivity?>(null) }
    val activities = state.activities
    val current = activities.getOrNull(index.coerceIn(0, (activities.size - 1).coerceAtLeast(0)))

    // One-second ticker, only while something on screen shows a running time.
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    val needsTicker = current is IslandActivity.Focus || current is IslandActivity.Timer || current is IslandActivity.Call ||
        (expanded && current is IslandActivity.Music && current.nowPlaying.isPlaying)
    LaunchedEffect(needsTicker) {
        while (needsTicker) { now = SystemClock.elapsedRealtime(); delay(1_000) }
    }

    Column(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = if (state.position == IslandPosition.Center) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        when {
            current == null && !state.hasNotificationAccess -> SetupPill(onClick = { context.startSafely(SystemIntents.notificationListenerSettings()) })
            current == null -> Unit
            else -> CompactPill(
                activity = current,
                count = activities.size,
                position = index,
                now = now,
                hideContent = state.hideMessageContent,
                appKeyFor = viewModel::appKeyFor,
                onTap = { expanded = !expanded },
                onLongPress = { controlsFor = current },
                onSwipe = { dir ->
                    if (activities.isNotEmpty()) index = (index + dir + activities.size) % activities.size
                },
            )
        }
        AnimatedVisibility(
            visible = expanded && current != null,
            enter = fadeIn(SLTheme.motion.medium()) + expandVertically(SLTheme.motion.medium()),
            exit = fadeOut(SLTheme.motion.short()) + shrinkVertically(SLTheme.motion.short()),
        ) {
            current?.let {
                Expanded(it, now, state, viewModel, onCollapse = { expanded = false; viewModel.resetReply() })
            }
        }
    }

    controlsFor?.let { activity ->
        SLBottomSheet(onDismissRequest = { controlsFor = null }, title = "Dynamic Island") {
            SLNavigationRow(title = if (state.pinnedId == activity.id) "Unpin" else "Pin to the front", onClick = { viewModel.togglePin(activity); controlsFor = null })
            SLNavigationRow(title = "Dismiss", subtitle = "Clears the notification when the app allows it", onClick = { viewModel.dismiss(activity); controlsFor = null })
            SLNavigationRow(title = "Island settings", onClick = { controlsFor = null; onOpenSettings() })
        }
    }
}

@Composable
private fun SetupPill(onClick: () -> Unit) {
    Row(
        Modifier
            .background(IslandBlack, CircleShape)
            .border(1.5.dp, SLTheme.colors.accent, CircleShape)
            .pointerInput(Unit) { detectTapGestures { onClick() } }
            .semantics { onClick(label = "Turn on notification access") { onClick(); true } }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(8.dp).background(SLTheme.colors.accent))
        Text("Turn on Dynamic Island", style = SLTheme.typography.label, color = IslandText)
    }
}

@Composable
private fun CompactPill(
    activity: IslandActivity,
    count: Int,
    position: Int,
    now: Long,
    hideContent: Boolean,
    appKeyFor: (String) -> com.sllauncher.pro.data.apps.AppKey?,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onSwipe: (Int) -> Unit,
) {
    var drag by remember { mutableStateOf(0f) }
    val summary = compactSummary(activity, now, hideContent)
    Row(
        Modifier
            .widthIn(min = 160.dp, max = 360.dp)
            .heightIn(min = 40.dp)
            .background(IslandBlack, CircleShape)
            .pointerInput(activity.id) { detectTapGestures(onTap = { onTap() }, onLongPress = { onLongPress() }) }
            .pointerInput(count) {
                detectHorizontalDragGestures(
                    onDragStart = { drag = 0f },
                    onDragEnd = { if (drag > 60f) onSwipe(-1) else if (drag < -60f) onSwipe(1) },
                ) { change, amount -> change.consume(); drag += amount }
            }
            .semantics {
                contentDescription = "Dynamic Island: $summary" + if (count > 1) ", ${position + 1} of $count" else ""
                liveRegion = LiveRegionMode.Polite
                onClick(label = "Expand") { onTap(); true }
                customActions = listOfNotNull(
                    CustomAccessibilityAction("Options") { onLongPress(); true },
                    if (count > 1) CustomAccessibilityAction("Next activity") { onSwipe(1); true } else null,
                )
            }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LeadingMark(activity, appKeyFor)
        Text(summary, style = SLTheme.typography.caption, color = IslandText, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        TrailingMark(activity, now)
        if (count > 1) Text("${position + 1}/$count", style = SLTheme.typography.caption, color = IslandMuted)
    }
}

@Composable
private fun LeadingMark(activity: IslandActivity, appKeyFor: (String) -> com.sllauncher.pro.data.apps.AppKey?) {
    val pkg = when (activity) {
        is IslandActivity.Call -> activity.notification.packageName
        is IslandActivity.Timer -> activity.notification.packageName
        is IslandActivity.Navigation -> activity.notification.packageName
        is IslandActivity.Message -> activity.notification.packageName
        is IslandActivity.Progress -> activity.notification.packageName
        is IslandActivity.Music -> activity.nowPlaying.packageName
        else -> null
    }
    val key = pkg?.let(appKeyFor)
    when {
        activity is IslandActivity.Music && activity.nowPlaying.art != null ->
            Image(activity.nowPlaying.art.asImageBitmap(), contentDescription = null, modifier = Modifier.size(24.dp))
        key != null -> AppIcon(key, pkg.orEmpty(), 24.dp)
        else -> Box(Modifier.size(10.dp).background(SLTheme.colors.accent))
    }
}

@Composable
private fun TrailingMark(activity: IslandActivity, now: Long) {
    when (activity) {
        is IslandActivity.Charging -> Text("${activity.battery.percent}%", style = SLTheme.typography.label, color = if (activity.battery.low) SLTheme.colors.error else SLTheme.colors.accent)
        is IslandActivity.Focus -> Text(mmss(activity.session.remainingMs(now)), style = SLTheme.typography.label, color = SLTheme.colors.accent)
        is IslandActivity.Message -> if (activity.unread > 1) Text("${activity.unread}", style = SLTheme.typography.label, color = SLTheme.colors.accent)
        is IslandActivity.Progress -> if (!activity.notification.progressIndeterminate && activity.notification.progressMax > 0) {
            Text("${activity.notification.progress * 100 / activity.notification.progressMax}%", style = SLTheme.typography.label, color = SLTheme.colors.accent)
        }
        is IslandActivity.Music -> Box(Modifier.size(width = 4.dp, height = if (activity.nowPlaying.isPlaying) 14.dp else 6.dp).background(SLTheme.colors.accent))
        else -> Box(Modifier.size(8.dp).background(SLTheme.colors.accent))
    }
}

private fun compactSummary(activity: IslandActivity, now: Long, hideContent: Boolean): String = when (activity) {
    is IslandActivity.Call -> activity.notification.title.ifEmpty { "Call" }
    is IslandActivity.Timer -> activity.notification.title.ifEmpty { activity.notification.text }
    is IslandActivity.Focus -> activity.session.label
    is IslandActivity.Navigation -> activity.notification.title.ifEmpty { activity.notification.text }
    is IslandActivity.Music -> listOfNotNull(activity.nowPlaying.title, activity.nowPlaying.artist).joinToString(" · ")
    is IslandActivity.Message -> if (hideContent) "${activity.notification.appLabel}: new message" else "${activity.notification.title}: ${activity.notification.text}"
    is IslandActivity.Progress -> activity.notification.title.ifEmpty { activity.notification.appLabel }
    is IslandActivity.Charging -> when {
        activity.battery.full -> "Fully charged"
        activity.battery.charging && activity.battery.source == PowerSource.Wireless -> "Charging wirelessly"
        activity.battery.charging -> "Charging"
        activity.battery.low -> "Battery low"
        else -> "Battery"
    }
}

private fun mmss(ms: Long): String {
    val total = ms / 1000
    return "%d:%02d".format(total / 60, total % 60)
}

@Composable
private fun Expanded(activity: IslandActivity, now: Long, state: IslandUiState, viewModel: IslandViewModel, onCollapse: () -> Unit) {
    Column(
        Modifier
            .padding(top = 6.dp)
            .fillMaxWidth()
            .widthIn(max = 480.dp)
            .background(IslandBlack, RoundedCornerShape(28.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        when (activity) {
            is IslandActivity.Music -> {
                val np = activity.nowPlaying
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    np.art?.let { Image(it.asImageBitmap(), contentDescription = null, modifier = Modifier.size(56.dp)) }
                    Column(Modifier.weight(1f)) {
                        Text(np.title, style = SLTheme.typography.title, color = IslandText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(listOfNotNull(np.artist, np.appLabel).joinToString(" · "), style = SLTheme.typography.caption, color = IslandMuted, maxLines = 1)
                    }
                }
                if (np.durationMs > 0) {
                    SLProgressBar(np.currentPosition(now).toFloat() / np.durationMs, contentDescription = "Playback position")
                    Text("${mmss(np.currentPosition(now))} / ${mmss(np.durationMs)}", style = SLTheme.typography.caption, color = IslandMuted)
                }
                SLMediaControls(
                    isPlaying = np.isPlaying,
                    onPlayPause = { viewModel.media.togglePlayPause() },
                    onPrevious = { viewModel.media.previous() },
                    onNext = { viewModel.media.next() },
                    canSkipPrevious = np.canSkipPrevious,
                    canSkipNext = np.canSkipNext,
                )
                SLButton("Open ${np.appLabel}", { viewModel.media.openApp() }, style = SLButtonStyle.Ghost)
            }
            is IslandActivity.Message -> MessageExpanded(activity, state, viewModel, onCollapse)
            is IslandActivity.Call -> NotificationExpanded(activity.notification, viewModel, extra = activity.notification.chronometerBase?.let { "Duration ${mmss(System.currentTimeMillis() - it)}" })
            is IslandActivity.Timer -> NotificationExpanded(activity.notification, viewModel, extra = null)
            is IslandActivity.Navigation -> NotificationExpanded(activity.notification, viewModel, extra = null)
            is IslandActivity.Progress -> {
                NotificationExpanded(activity.notification, viewModel, extra = null)
                val n = activity.notification
                if (n.progressMax > 0 && !n.progressIndeterminate) SLProgressBar(n.progress.toFloat() / n.progressMax, contentDescription = "Progress")
            }
            is IslandActivity.Charging -> {
                val b = activity.battery
                Text("${b.percent}%", style = SLTheme.typography.headline, color = IslandText)
                Text(
                    when {
                        b.full -> "Fully charged. You can unplug."
                        b.charging -> if (b.source == PowerSource.Wireless) "Charging wirelessly" else "Charging with a cable"
                        b.low -> "Battery low. Plug in soon or turn on battery saver."
                        else -> "On battery"
                    },
                    style = SLTheme.typography.bodySmall,
                    color = IslandMuted,
                )
                SLProgressBar(b.percent / 100f, contentDescription = "Battery level")
            }
            is IslandActivity.Focus -> {
                Text(mmss(activity.session.remainingMs(now)), style = SLTheme.typography.headline, color = IslandText)
                Text(activity.session.label + (activity.session.pomodoroRound?.let { " · round $it" } ?: ""), style = SLTheme.typography.bodySmall, color = IslandMuted)
                SLProgressBar(1f - activity.session.remainingMs(now).toFloat() / activity.session.totalMs, contentDescription = "Focus progress")
                SLButton("Stop", { viewModel.stopFocus() }, style = SLButtonStyle.Secondary)
            }
        }
    }
}

@Composable
private fun NotificationExpanded(n: SLNotification, viewModel: IslandViewModel, extra: String?) {
    Text(n.title.ifEmpty { n.appLabel }, style = SLTheme.typography.title, color = IslandText)
    if (n.text.isNotEmpty()) Text(n.text, style = SLTheme.typography.bodySmall, color = IslandMuted, maxLines = 3, overflow = TextOverflow.Ellipsis)
    extra?.let { Text(it, style = SLTheme.typography.caption, color = IslandMuted) }
    // The app's own actions (e.g. Answer/Decline on a call), exactly as in the notification shade.
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        n.actions.filter { it.remoteInput == null }.take(3).forEach { action ->
            SLButton(action.title, { viewModel.runAction(action) }, style = SLButtonStyle.Secondary)
        }
    }
    SLButton("Open ${n.appLabel}", { viewModel.open(n) }, style = SLButtonStyle.Ghost)
}

@Composable
private fun MessageExpanded(activity: IslandActivity.Message, state: IslandUiState, viewModel: IslandViewModel, onCollapse: () -> Unit) {
    val n = activity.notification
    var draft by remember(n.key) { mutableStateOf("") }
    val voice = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { draft = it }
        }
    }
    Text(n.conversationTitle ?: n.title, style = SLTheme.typography.title, color = IslandText)
    Text("${n.appLabel}${if (activity.unread > 1) " · ${activity.unread} unread" else ""}", style = SLTheme.typography.caption, color = IslandMuted)
    if (state.hideMessageContent) {
        Text("Message content hidden by your privacy settings.", style = SLTheme.typography.bodySmall, color = IslandMuted)
    } else if (n.messages.isNotEmpty()) {
        n.messages.takeLast(3).forEach { m ->
            Text("${m.sender?.let { "$it: " } ?: ""}${m.text}", style = SLTheme.typography.bodySmall, color = IslandText, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    } else {
        Text(n.text, style = SLTheme.typography.bodySmall, color = IslandText, maxLines = 3, overflow = TextOverflow.Ellipsis)
    }

    if (n.replyAction == null) {
        Text("${n.appLabel} doesn't allow replies from outside the app.", style = SLTheme.typography.caption, color = IslandMuted)
        SLButton("Open ${n.appLabel}", { viewModel.open(n) }, style = SLButtonStyle.Secondary)
        return
    }
    when (state.replyStatus) {
        ReplyStatus.Sent -> { Text("Reply sent.", style = SLTheme.typography.bodySmall, color = SLTheme.colors.accent); SLButton("Done", onCollapse, style = SLButtonStyle.Ghost); return }
        ReplyStatus.Failed -> Text("Couldn't send. Open the app to reply.", style = SLTheme.typography.bodySmall, color = SLTheme.colors.error)
        ReplyStatus.NotSupported -> Text("This conversation doesn't accept quick replies.", style = SLTheme.typography.bodySmall, color = SLTheme.colors.error)
        ReplyStatus.Sending -> Text("Sending…", style = SLTheme.typography.bodySmall, color = IslandMuted)
        ReplyStatus.Idle -> Unit
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(SuggestedReplies) { s -> SLChip(s, false, { draft = s }) }
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        items(QuickEmoji) { e ->
            Text(e, modifier = Modifier.pointerInput(e) { detectTapGestures { draft += e } }.padding(8.dp).semantics { contentDescription = "Add $e" })
        }
    }
    SLSearchField(
        query = draft,
        onQueryChange = { draft = it },
        placeholder = "Reply to ${n.title}",
        onSearch = { viewModel.reply(n, draft) },
        trailing = {
            SLIconButton(SLIcons.Mic, "Dictate reply", {
                try {
                    voice.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM))
                } catch (_: ActivityNotFoundException) { }
            }, style = SLIconButtonStyle.Plain)
        },
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SLButton("Send", { viewModel.reply(n, draft) }, enabled = draft.isNotBlank())
        SLButton("Cancel", { draft = ""; onCollapse() }, style = SLButtonStyle.Ghost)
    }
}
