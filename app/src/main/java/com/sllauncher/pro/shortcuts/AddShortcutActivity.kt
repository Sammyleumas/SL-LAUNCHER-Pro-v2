package com.sllauncher.pro.shortcuts

import android.content.pm.LauncherApps
import android.os.Bundle
import android.os.UserManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.sllauncher.pro.data.settings.SettingsRepository
import com.sllauncher.pro.home.GridSpec
import com.sllauncher.pro.home.HomeLayoutRepository
import com.sllauncher.core.designsystem.component.SLDialog
import com.sllauncher.core.designsystem.theme.SLTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Handles "Add to home screen" requests from other apps (pinned
 * shortcuts). Android delivers them to the default launcher, which must
 * accept them for the shortcut to be created. The user confirms first.
 */
@AndroidEntryPoint
class AddShortcutActivity : ComponentActivity() {

    @Inject lateinit var layout: HomeLayoutRepository
    @Inject lateinit var settings: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val launcherApps = getSystemService(LauncherApps::class.java)
        val request = launcherApps.getPinItemRequest(intent)
        if (request == null || !request.isValid || request.requestType != LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT) {
            finish()
            return
        }
        val info = request.shortcutInfo ?: run { finish(); return }
        val label = info.shortLabel?.toString() ?: info.id
        val serial = getSystemService(UserManager::class.java).getSerialNumberForUser(info.userHandle)

        setContent {
            SLTheme {
                SLDialog(
                    title = "Add to home screen?",
                    text = "\"$label\" from ${info.`package`} will be added to your SL home screen.",
                    confirmLabel = "Add",
                    dismissLabel = "Cancel",
                    onConfirm = {
                        lifecycleScope.launch {
                            if (request.isValid && request.accept()) {
                                val s = settings.current
                                layout.addShortcut(info.`package`, info.id, serial, label, GridSpec(s.homeColumns, s.homeRows), s.homePages)
                            }
                            finish()
                        }
                    },
                    onDismissRequest = { finish() },
                )
            }
        }
    }
}
