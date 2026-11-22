package com.sllauncher.core.designsystem.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sllauncher.core.designsystem.R
import com.sllauncher.core.designsystem.theme.SLTheme

private val SliderThumbSize = 20.dp
private val SliderTrackHeight = 6.dp

/**
 * Brutalist slider: square thumb on a hard-edged track, filled in yellow up
 * to the current value. Tap or drag anywhere on the 48dp-tall control.
 * TalkBack users can adjust it with volume keys / swipe (setProgress).
 *
 * @param contentDescription what the slider controls, e.g. "Icon size".
 */
@Composable
fun SLSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val colors = SLTheme.colors
    val currentOnChange by rememberUpdatedState(onValueChange)
    val currentOnFinished by rememberUpdatedState(onValueChangeFinished)
    val fraction = sliderFractionForValue(value, valueRange)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(SLTheme.shapes.minTouchTarget)
            .alpha(if (enabled) 1f else 0.4f)
            .semantics {
                this.contentDescription = contentDescription
                progressBarRangeInfo = ProgressBarRangeInfo(value, valueRange, steps)
                if (enabled) {
                    setProgress { target ->
                        currentOnChange(sliderValueForFraction(sliderFractionForValue(target, valueRange), valueRange, steps))
                        currentOnFinished?.invoke()
                        true
                    }
                } else {
                    disabled()
                }
            }
            .then(
                if (enabled) {
                    Modifier
                        .pointerInput(valueRange, steps) {
                            detectTapGestures { offset ->
                                currentOnChange(sliderValueForFraction(offset.x / size.width, valueRange, steps))
                                currentOnFinished?.invoke()
                            }
                        }
                        .pointerInput(valueRange, steps) {
                            detectHorizontalDragGestures(
                                onDragEnd = { currentOnFinished?.invoke() },
                            ) { change, _ ->
                                change.consume()
                                currentOnChange(sliderValueForFraction(change.position.x / size.width, valueRange, steps))
                            }
                        }
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        val trackWidth = maxWidth
        Box(
            Modifier
                .fillMaxWidth()
                .height(SliderTrackHeight)
                .background(colors.surfaceElevated)
                .border(1.dp, colors.borderStrong),
        )
        Box(
            Modifier
                .fillMaxWidth(fraction)
                .height(SliderTrackHeight)
                .background(colors.accent),
        )
        Box(
            Modifier
                .offset(x = (trackWidth - SliderThumbSize) * fraction)
                .size(SliderThumbSize)
                .background(colors.accent)
                .border(SLTheme.shapes.borderWidth, colors.accentOutline),
        )
    }
}

/**
 * Square on/off switch. Pass `onCheckedChange = null` when the switch sits
 * inside a row that handles the toggle itself (see [SLSwitchRow]).
 */
@Composable
fun SLSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = SLTheme.colors
    val knobOffset by animateDpAsState(
        targetValue = if (checked) 24.dp else 0.dp,
        animationSpec = SLTheme.motion.short(),
        label = "switchKnob",
    )
    val onLabel = stringResource(R.string.sl_ds_switch_on)
    val offLabel = stringResource(R.string.sl_ds_switch_off)

    Box(
        modifier = modifier
            .then(
                if (onCheckedChange != null) {
                    Modifier
                        .minimumInteractiveComponentSize()
                        .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
                        .semantics { stateDescription = if (checked) onLabel else offLabel }
                } else {
                    Modifier
                },
            )
            .alpha(if (enabled) 1f else 0.4f),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(width = 52.dp, height = 28.dp)
                .background(if (checked) colors.accent else colors.surface)
                .border(SLTheme.shapes.borderWidth, if (checked) colors.accentOutline else colors.borderStrong)
                .padding(4.dp),
        ) {
            Box(
                Modifier
                    .offset(x = knobOffset)
                    .size(20.dp)
                    .background(if (checked) colors.onAccent else colors.textSecondary),
            )
        }
    }
}

@Preview(backgroundColor = 0xFF090909, showBackground = true, widthDp = 320)
@Composable
private fun SLControlsPreview() {
    SLTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SLSlider(value = 0.4f, onValueChange = {}, contentDescription = "Icon size")
            SLSlider(value = 5f, onValueChange = {}, contentDescription = "Columns", valueRange = 3f..7f, steps = 3)
            SLSwitch(checked = true, onCheckedChange = {})
            SLSwitch(checked = false, onCheckedChange = {})
        }
    }
}
