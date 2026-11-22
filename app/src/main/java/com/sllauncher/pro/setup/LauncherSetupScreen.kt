package com.sllauncher.pro.setup

import android.content.ActivityNotFoundException
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sllauncher.pro.R
import com.sllauncher.core.designsystem.component.SLButton
import com.sllauncher.core.designsystem.component.SLButtonStyle
import com.sllauncher.core.designsystem.theme.SLTheme

private const val TAG = "LauncherSetup"

@Composable
fun LauncherSetupRoute(
    onDone: () -> Unit,
    viewModel: LauncherSetupViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var settingsUnavailable by remember { mutableStateOf(false) }

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    val roleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { viewModel.onRoleRequestFinished() }

    val openSettings: () -> Unit = {
        try {
            context.startActivity(viewModel.homeSettingsIntent())
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "Home settings screen not available on this device", e)
            settingsUnavailable = true
        }
    }

    LauncherSetupScreen(
        state = state,
        settingsUnavailable = settingsUnavailable,
        onRequestDefault = {
            val intent = viewModel.roleRequestIntent()
            if (intent != null) roleLauncher.launch(intent) else openSettings()
        },
        onOpenSettings = openSettings,
        onDone = onDone,
    )
}

@Composable
fun LauncherSetupScreen(
    state: LauncherSetupUiState,
    settingsUnavailable: Boolean,
    onRequestDefault: () -> Unit,
    onOpenSettings: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SLTheme.colors
    val spacing = SLTheme.spacing

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .safeDrawingPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(spacing.xl),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            Box(Modifier.size(width = 48.dp, height = 8.dp).background(colors.accentIndicator))
            Text(
                text = stringResource(R.string.setup_title),
                style = SLTheme.typography.headline,
                color = colors.textPrimary,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.setup_body),
                style = SLTheme.typography.body,
                color = colors.textSecondary,
            )

            StatusRow(isDefault = state.isDefaultHome)

            if (state.isDefaultHome) {
                SLButton(
                    text = stringResource(R.string.setup_action_done),
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                SLButton(
                    text = stringResource(R.string.setup_action_request),
                    onClick = onRequestDefault,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (state.showSettingsFallback) {
                    Text(
                        text = stringResource(R.string.setup_settings_hint),
                        style = SLTheme.typography.caption,
                        color = colors.textSecondary,
                    )
                    SLButton(
                        text = stringResource(R.string.setup_action_settings),
                        onClick = onOpenSettings,
                        style = SLButtonStyle.Secondary,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (settingsUnavailable) {
                    Text(
                        text = stringResource(R.string.setup_settings_unavailable),
                        style = SLTheme.typography.caption,
                        color = colors.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusRow(isDefault: Boolean) {
    val colors = SLTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Box(
            Modifier
                .size(12.dp)
                .background(if (isDefault) colors.accentIndicator else colors.borderStrong),
        )
        Text(
            text = stringResource(
                if (isDefault) R.string.setup_status_default else R.string.setup_status_not_default,
            ),
            style = SLTheme.typography.title,
            color = colors.textPrimary,
            modifier = Modifier.padding(start = SLTheme.spacing.md),
        )
    }
}
