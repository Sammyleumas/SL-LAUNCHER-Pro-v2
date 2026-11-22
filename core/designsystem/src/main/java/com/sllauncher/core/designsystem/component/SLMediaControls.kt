package com.sllauncher.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sllauncher.core.designsystem.R
import com.sllauncher.core.designsystem.icon.SLIcons
import com.sllauncher.core.designsystem.theme.SLTheme

/**
 * Previous / play-pause / next controls. Stateless: the caller passes the
 * playback state from a media session (Phase 7/9) and handles the clicks.
 * A control is disabled when the session does not support that action.
 */
@Composable
fun SLMediaControls(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    canSkipPrevious: Boolean = true,
    canSkipNext: Boolean = true,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(SLTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SLIconButton(
            icon = SLIcons.SkipPrevious,
            contentDescription = stringResource(R.string.sl_ds_media_previous),
            onClick = onPrevious,
            style = SLIconButtonStyle.Plain,
            enabled = canSkipPrevious,
        )
        SLIconButton(
            icon = if (isPlaying) SLIcons.Pause else SLIcons.Play,
            contentDescription = stringResource(if (isPlaying) R.string.sl_ds_media_pause else R.string.sl_ds_media_play),
            onClick = onPlayPause,
            style = SLIconButtonStyle.Filled,
        )
        SLIconButton(
            icon = SLIcons.SkipNext,
            contentDescription = stringResource(R.string.sl_ds_media_next),
            onClick = onNext,
            style = SLIconButtonStyle.Plain,
            enabled = canSkipNext,
        )
    }
}

@Preview(backgroundColor = 0xFF151515, showBackground = true)
@Composable
private fun SLMediaControlsPreview() {
    SLTheme {
        Box(Modifier.padding(16.dp)) {
            SLMediaControls(isPlaying = true, onPlayPause = {}, onPrevious = {}, onNext = {})
        }
    }
}
