package com.sllauncher.pro.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.sllauncher.pro.common.SystemIntents
import com.sllauncher.pro.common.UiCommand
import com.sllauncher.pro.common.UiCommandBus
import com.sllauncher.pro.common.startSafely
import com.sllauncher.pro.data.apps.AppEntry
import com.sllauncher.pro.data.apps.AppRepository
import com.sllauncher.pro.data.db.NotificationHistoryDao
import com.sllauncher.pro.data.db.SearchHistoryDao
import com.sllauncher.pro.data.settings.LauncherSettings
import com.sllauncher.pro.data.settings.SettingsRepository
import com.sllauncher.pro.launcher.HomeRoleRepository
import com.sllauncher.pro.notifications.NotificationRepository
import com.sllauncher.pro.ui.ScreenScaffold
import com.sllauncher.core.designsystem.component.SLBlock
import com.sllauncher.core.designsystem.component.SLButton
import com.sllauncher.core.designsystem.component.SLButtonStyle
import com.sllauncher.core.designsystem.component.SLDialog
import com.sllauncher.core.designsystem.component.SLNavigationRow
import com.sllauncher.core.designsystem.component.SLSectionHeader
import com.sllauncher.core.designsystem.component.SLSwitchRow
import com.sllauncher.core.designsystem.theme.SLTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class PrivacyViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
    private val apps: AppRepository,
    private val notifications: NotificationRepository,
    private val homeRole: HomeRoleRepository,
    private val searchHistory: SearchHistoryDao,
    private val notificationHistory: NotificationHistoryDao,
    private val backup: BackupManager,
    private val bus: UiCommandBus,
) : ViewModel() {
    val state: StateFlow<LauncherSettings> = settings.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), settings.current)
    val hiddenApps = apps.entries

    fun update(transform: (LauncherSettings) -> LauncherSettings) = viewModelScope.launch { settings.update(transform) }
    fun unhide(entry: AppEntry) = viewModelScope.launch { apps.updateMeta(entry.app.key) { it.copy(hidden = false) } }
    fun clearSuggestions() = viewModelScope.launch { apps.clearLaunchHistory(); bus.send(UiCommand.ShowMessage("App suggestions reset.")) }

    /** Android's lock-screen notification settings (where content can be hidden on the lock screen). */
    fun openLockScreenSettings() {
        val intents = listOf(
            android.content.Intent("android.settings.LOCK_SCREEN_SETTINGS"),
            android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_SETTINGS),
            android.content.Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS),
        )
        for (intent in intents) {
            val ok = runCatching { context.startActivity(intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
            if (ok) return
        }
    }

    fun clearSearchHistory() = viewModelScope.launch { searchHistory.clear(); bus.send(UiCommand.ShowMessage("Search history cleared.")) }
    fun clearNotificationHistory() = viewModelScope.launch { notificationHistory.clear(); bus.send(UiCommand.ShowMessage("Notification history cleared.")) }

    fun isDefaultHome(): Boolean { homeRole.refresh(); return homeRole.isDefaultHome.value }
    fun hasNotificationAccess() = notifications.hasAccess()
    fun granted(permission: String) = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    fun canWriteSettings() = Settings.System.canWrite(context)

    fun exportTo(uri: android.net.Uri) = viewModelScope.launch {
        val ok = runCatching {
            val text = backup.export()
            withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) } ?: error("no stream") }
        }.isSuccess
        bus.send(UiCommand.ShowMessage(if (ok) "Backup saved." else "Couldn't save the backup there."))
    }

    fun importFrom(uri: android.net.Uri) = viewModelScope.launch {
        val message = try {
            val text = withContext(Dispatchers.IO) { context.contentResolver.openInputStream(uri)?.use { String(it.readBytes()) } } ?: error("Couldn't open that file.")
            val restored = backup.import(text)
            "Restored backup from ${android.text.format.DateFormat.getDateFormat(context).format(java.util.Date(restored.createdAt))}. Re-add widgets from the home menu."
        } catch (e: InvalidBackupException) {
            e.message ?: "That backup couldn't be read."
        } catch (e: IllegalStateException) {
            e.message ?: "That backup couldn't be read."
        }
        bus.send(UiCommand.ShowMessage(message))
    }
}

private data class PermissionRow(val title: String, val purpose: String, val granted: Boolean, val manage: () -> Unit)

/** Privacy dashboard, data controls and backup. */
@Composable
fun PrivacyRoute(onBack: () -> Unit, viewModel: PrivacyViewModel = hiltViewModel()) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val entries by viewModel.hiddenApps.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    var pendingImport by remember { mutableStateOf<android.net.Uri?>(null) }

    LifecycleResumeEffect(Unit) {
        refresh++
        onPauseOrDispose { }
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uri?.let(viewModel::exportTo) }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> pendingImport = uri }

    val appSettings: () -> Unit = { context.startSafely(SystemIntents.appDetails(context)) }
    // Re-read permission states every time the screen resumes (the user may have just changed one).
    val rows = remember(refresh) { listOf(
        PermissionRow("Default home app", "Lets SL be your home screen and show app shortcuts.", viewModel.isDefaultHome()) { context.startSafely(SystemIntents.homeSettings()) },
        PermissionRow("Notification access", "Dynamic Island, notification center, quick replies, music controls, badges.", viewModel.hasNotificationAccess()) { context.startSafely(SystemIntents.notificationListenerSettings()) },
        PermissionRow("Microphone", "Talking to SL. Only while you hold a conversation.", viewModel.granted(Manifest.permission.RECORD_AUDIO), appSettings),
        PermissionRow("Contacts", "Contact search and quick contacts.", viewModel.granted(Manifest.permission.READ_CONTACTS), appSettings),
        PermissionRow(
            "Music and audio",
            "Playing songs stored on this phone.",
            viewModel.granted(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE),
            appSettings,
        ),
        PermissionRow("Calendar", "Today's events in your overview.", viewModel.granted(Manifest.permission.READ_CALENDAR), appSettings),
        PermissionRow(
            "Notifications",
            "Reminders and focus-timer alerts you create.",
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || viewModel.granted(Manifest.permission.POST_NOTIFICATIONS),
        ) { context.startSafely(SystemIntents.appNotificationSettings(context)) },
        PermissionRow("Modify system settings", "Brightness slider in Quick settings. Nothing else.", viewModel.canWriteSettings()) { context.startSafely(SystemIntents.writeSettings(context)) },
    ) }
    val hidden = entries.filter { it.hidden }

    ScreenScaffold(title = "Privacy and backup", onBack = onBack, scrollable = true) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SLBlock(Modifier.fillMaxWidth()) {
                Text("What SL keeps", style = SLTheme.typography.title, color = SLTheme.colors.textPrimary)
                Text(
                    "Your layout, notes, tasks and settings are stored on this phone only. SL has no account and no servers. " +
                        "The internet is used only for news headlines and images, and by your phone's speech service if it isn't on-device.",
                    style = SLTheme.typography.bodySmall,
                    color = SLTheme.colors.textSecondary,
                )
            }
            SLSectionHeader("Permissions")
            rows.forEach { r ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(r.title, style = SLTheme.typography.body, color = SLTheme.colors.textPrimary)
                        Text(r.purpose, style = SLTheme.typography.caption, color = SLTheme.colors.textSecondary)
                        Text(if (r.granted) "Allowed" else "Not allowed", style = SLTheme.typography.caption, color = if (r.granted) SLTheme.colors.accentIndicator else SLTheme.colors.textSecondary)
                    }
                    SLButton("Manage", r.manage, style = SLButtonStyle.Ghost)
                }
            }

            SLSectionHeader("Notifications")
            SLSwitchRow("Hide notification content", s.hideNotificationContent, { on -> viewModel.update { it.copy(hideNotificationContent = on) } }, subtitle = "Island and notification center show only app names")
            SLSwitchRow("Keep notification history", s.notificationHistoryEnabled, { on -> viewModel.update { it.copy(notificationHistoryEnabled = on) } }, subtitle = "Last 7 days, on this phone only")
            SLButton("Clear notification history", { viewModel.clearNotificationHistory() }, style = SLButtonStyle.Secondary)
            SLNavigationRow(
                "Lock screen notifications",
                { viewModel.openLockScreenSettings() },
                subtitle = "Hide message content on the lock screen (Android setting)",
            )

            SLSectionHeader("Search")
            SLSwitchRow("Remember searches", s.searchHistoryEnabled, { on -> viewModel.update { it.copy(searchHistoryEnabled = on) } })
            SLButton("Clear search history", { viewModel.clearSearchHistory() }, style = SLButtonStyle.Secondary)
            SLButton("Reset app suggestions", { viewModel.clearSuggestions() }, style = SLButtonStyle.Secondary)

            SLSectionHeader("Hidden apps")
            Text(
                "Hiding removes an app from SL's drawer and search only. It is not a security feature: the app stays installed, runs normally and is visible in Android Settings and other launchers.",
                style = SLTheme.typography.caption,
                color = SLTheme.colors.textSecondary,
            )
            if (hidden.isEmpty()) Text("No hidden apps.", style = SLTheme.typography.body, color = SLTheme.colors.textSecondary)
            hidden.forEach { e ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(e.app.label, style = SLTheme.typography.body, color = SLTheme.colors.textPrimary, modifier = Modifier.weight(1f))
                    SLButton("Show", { viewModel.unhide(e) }, style = SLButtonStyle.Ghost)
                }
            }

            SLSectionHeader("Backup and restore")
            Text("Saves your layout, categories, notes, tasks, countdowns, profiles and settings to a file you choose. Widgets must be re-added after restoring.", style = SLTheme.typography.caption, color = SLTheme.colors.textSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SLButton("Export", { exportLauncher.launch("sl-launcher-backup.json") })
                SLButton("Import", { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }, style = SLButtonStyle.Secondary)
            }
            Text("Encrypted cloud sync needs an account service, which SL doesn't run. Keep your exported file somewhere safe, such as your own cloud drive.", style = SLTheme.typography.caption, color = SLTheme.colors.textSecondary)

            SLSectionHeader("Your data")
            SLButton("Delete all SL data", appSettings, style = SLButtonStyle.Destructive)
            Text("Opens Android's app info, where Storage › Clear storage removes everything SL has saved.", style = SLTheme.typography.caption, color = SLTheme.colors.textSecondary)
        }
    }

    pendingImport?.let { uri ->
        SLDialog(
            title = "Restore this backup?",
            text = "Your current home layout, categories and settings will be replaced. Notes and tasks from the backup are added to yours.",
            confirmLabel = "Restore",
            dismissLabel = "Cancel",
            onConfirm = { viewModel.importFrom(uri); pendingImport = null },
            onDismissRequest = { pendingImport = null },
            destructive = true,
        )
    }
}
