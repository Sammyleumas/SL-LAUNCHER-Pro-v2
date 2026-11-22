package com.sllauncher.core.designsystem.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sllauncher.core.designsystem.R
import com.sllauncher.core.designsystem.icon.SLIcons
import com.sllauncher.core.designsystem.theme.SLTheme

/** What the SL assistant is doing; drives [SLAssistantButton]'s visuals and label. */
enum class SLAssistantState { Idle, Listening, Processing, Speaking }

/**
 * The SL assistant's tap-to-talk control and its identity mark.
 *
 * - Idle: microphone. Tap to start listening.
 * - Listening / Speaking: animated level bars. Tap to stop or interrupt.
 * - Processing: loading blocks.
 *
 * The state is announced by TalkBack when it changes. With reduced motion
 * the bars are drawn at fixed heights.
 */
@Composable
fun SLAssistantButton(
    state: SLAssistantState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SLTheme.colors
    val label = stringResource(
        when (state) {
            SLAssistantState.Idle -> R.string.sl_ds_assistant_idle
            SLAssistantState.Listening -> R.string.sl_ds_assistant_listening
            SLAssistantState.Processing -> R.string.sl_ds_assistant_processing
            SLAssistantState.Speaking -> R.string.sl_ds_assistant_speaking
        },
    )
    Box(
        modifier = modifier
            .size(SLTheme.sizes.assistantButton)
            .blockShadow(colors.border, SLTheme.shapes.blockShadowOffset)
            .background(colors.accent)
            .border(SLTheme.shapes.borderWidth, colors.accentOutline)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = label
                liveRegion = LiveRegionMode.Polite
            },
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            SLAssistantState.Idle -> Icon(SLIcons.Mic, contentDescription = null, tint = colors.onAccent, modifier = Modifier.size(28.dp))
            SLAssistantState.Processing -> SLLoadingIndicator(blockSize = 8.dp, activeColor = colors.onAccent, idleColor = colors.onAccent.copy(alpha = 0.25f))
            SLAssistantState.Listening -> LevelBars(periodMillis = 420)
            SLAssistantState.Speaking -> LevelBars(periodMillis = 640)
        }
    }
}

private val RestingBarHeights = listOf(0.45f, 0.9f, 0.65f, 0.35f)

@Composable
private fun LevelBars(periodMillis: Int) {
    val color = SLTheme.colors.onAccent
    val reduced = SLTheme.motion.reduced
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(28.dp),
    ) {
        RestingBarHeights.forEachIndexed { index, rest ->
            val fraction = if (reduced) {
                rest
            } else {
                val transition = rememberInfiniteTransition(label = "bar$index")
                val animated by transition.animateFloat(
                    initialValue = 0.25f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(periodMillis, delayMillis = index * periodMillis / 4),
                        repeatMode = RepeatMode.Reverse,
                    ),
                    label = "barHeight$index",
                )
                animated
            }
            Box(Modifier.width(5.dp).height(28.dp * fraction).background(color))
        }
    }
}

@Preview(backgroundColor = 0xFF090909, showBackground = true)
@Composable
private fun SLAssistantButtonPreview() {
    SLTheme(reduceMotion = true) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            SLAssistantState.entries.forEach { SLAssistantButton(state = it, onClick = {}) }
        }
    }
}
