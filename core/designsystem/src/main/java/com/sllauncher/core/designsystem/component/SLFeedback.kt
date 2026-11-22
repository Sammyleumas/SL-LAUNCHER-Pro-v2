package com.sllauncher.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sllauncher.core.designsystem.R
import com.sllauncher.core.designsystem.icon.SLIcons
import com.sllauncher.core.designsystem.theme.SLTheme

/**
 * Indeterminate loading indicator: three blocks lit one after another.
 * With reduced motion it shows a static frame. TalkBack announces "Loading".
 */
@Composable
fun SLLoadingIndicator(
    modifier: Modifier = Modifier,
    blockSize: Dp = 10.dp,
    activeColor: Color = SLTheme.colors.accent,
    idleColor: Color = SLTheme.colors.surfaceElevated,
) {
    val label = stringResource(R.string.sl_ds_loading)
    val activeIndex = if (SLTheme.motion.reduced) {
        0
    } else {
        val transition = rememberInfiniteTransition(label = "loading")
        val phase by transition.animateFloat(
            initialValue = 0f,
            targetValue = 3f,
            animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
            label = "loadingPhase",
        )
        phase.toInt().coerceIn(0, 2)
    }
    Row(
        modifier = modifier.semantics {
            contentDescription = label
            progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
        },
        horizontalArrangement = Arrangement.spacedBy(blockSize / 2),
    ) {
        repeat(3) { index ->
            Box(Modifier.size(blockSize).background(if (index == activeIndex) activeColor else idleColor))
        }
    }
}

/** Determinate progress bar (downloads, playback position, setup steps). [progress] is 0..1. */
@Composable
fun SLProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val colors = SLTheme.colors
    val clamped = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .background(colors.surfaceElevated)
            .semantics {
                if (contentDescription != null) this.contentDescription = contentDescription
                progressBarRangeInfo = ProgressBarRangeInfo(clamped, 0f..1f)
            },
    ) {
        Box(Modifier.fillMaxWidth(clamped).height(6.dp).background(colors.accent))
    }
}

enum class SLMessageTone { Neutral, Error }

/**
 * Full-area message used for empty, offline and error states. Always says
 * what happened and, when possible, offers one action to fix it.
 */
@Composable
fun SLMessageState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = SLIcons.Inbox,
    tone: SLMessageTone = SLMessageTone.Neutral,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = SLTheme.colors
    val accent = if (tone == SLMessageTone.Error) colors.error else colors.accentIndicator
    Column(
        modifier = modifier
            .widthIn(max = 360.dp)
            .padding(SLTheme.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SLTheme.spacing.md),
    ) {
        Box(
            Modifier
                .size(56.dp)
                .border(SLTheme.shapes.borderWidth, accent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(28.dp))
        }
        Text(
            title,
            style = SLTheme.typography.title,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(body, style = SLTheme.typography.bodySmall, color = colors.textSecondary, textAlign = TextAlign.Center)
        if (actionLabel != null && onAction != null) {
            SLButton(
                text = actionLabel,
                onClick = onAction,
                style = if (tone == SLMessageTone.Error) SLButtonStyle.Primary else SLButtonStyle.Secondary,
                modifier = Modifier.padding(top = SLTheme.spacing.sm),
            )
        }
    }
}

/** Empty state, e.g. "No hidden apps". */
@Composable
fun SLEmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) = SLMessageState(title, body, modifier, SLIcons.Inbox, SLMessageTone.Neutral, actionLabel, onAction)

/** Error state with a retry action, e.g. "Couldn't load news". */
@Composable
fun SLErrorState(
    title: String,
    body: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    retryLabel: String = stringResource(R.string.sl_ds_retry),
) = SLMessageState(title, body, modifier, SLIcons.Warning, SLMessageTone.Error, retryLabel, onRetry)

@Preview(backgroundColor = 0xFF090909, showBackground = true, widthDp = 360)
@Composable
private fun SLFeedbackPreview() {
    SLTheme(reduceMotion = true) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SLLoadingIndicator()
            SLProgressBar(progress = 0.6f)
            SLErrorState(title = "Couldn't load news", body = "Check your connection and try again.", onRetry = {})
        }
    }
}
