package com.sllauncher.core.designsystem.component

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.sllauncher.core.designsystem.R
import com.sllauncher.core.designsystem.theme.SLTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * SL's signature floating panel: a dark block centred over a dimmed
 * wallpaper, sized by [floatingPanelSize]. Used by the app drawer and
 * other launcher overlays.
 *
 * Closes on Back, on a tap outside the panel ([dismissOnScrimTap]) and by
 * dragging the top handle down ([dragToDismiss]). The handle is also a
 * TalkBack "Close" action. With reduced motion the panel appears and
 * disappears instantly.
 *
 * Render it as the last child of a full-screen Box so it draws on top.
 *
 * @param title announced by TalkBack when the panel opens.
 * @param surfaceAlpha opacity of the panel surface; restrained transparency
 *   lets a hint of wallpaper through without hurting legibility.
 * @param widthFraction preferred share of the window width (user setting).
 * @param heightFraction preferred share of the window height (user setting).
 */
@Composable
fun SLFloatingPanel(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    dismissOnScrimTap: Boolean = true,
    dragToDismiss: Boolean = true,
    surfaceAlpha: Float = 0.94f,
    widthFraction: Float = 0.8f,
    heightFraction: Float = 0.7f,
    content: @Composable ColumnScope.() -> Unit,
) {
    val visibleState = remember { MutableTransitionState(false) }
    visibleState.targetState = visible
    if (!visibleState.currentState && !visibleState.targetState && visibleState.isIdle) return

    val colors = SLTheme.colors
    val motion = SLTheme.motion
    val shapes = SLTheme.shapes
    val currentOnDismiss by rememberUpdatedState(onDismissRequest)

    BackHandler(enabled = visible) { currentOnDismiss() }

    // Starts at 0 each time the panel is (re)composed, so the scrim fades in.
    val scrimAlpha = remember { Animatable(0f) }
    LaunchedEffect(visible) {
        scrimAlpha.animateTo(if (visible) 1f else 0f, if (visible) motion.medium() else motion.short())
    }

    Box(modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = scrimAlpha.value }
                .background(colors.scrim)
                .then(
                    if (dismissOnScrimTap && visible) {
                        Modifier.pointerInput(Unit) { detectTapGestures { currentOnDismiss() } }
                    } else {
                        Modifier
                    },
                ),
        )

        // Size from the area inside the status and navigation bars, so the
        // panel never slides under them on short or landscape windows.
        BoxWithConstraints(
            Modifier.fillMaxSize().safeDrawingPadding(),
            contentAlignment = Alignment.Center,
        ) {
            val panelSize = floatingPanelSize(maxWidth, maxHeight, widthFraction, heightFraction)
            AnimatedVisibility(
                visibleState = visibleState,
                enter = fadeIn(motion.medium()) + scaleIn(motion.medium(), initialScale = 0.94f),
                exit = fadeOut(motion.short()) + scaleOut(motion.short(), targetScale = 0.96f),
            ) {
                var dragOffset by remember { mutableFloatStateOf(0f) }
                val scope = rememberCoroutineScope()
                LaunchedEffect(visible) { if (visible) dragOffset = 0f }
                val dismissThreshold = with(LocalDensity.current) { 120.dp.toPx() }

                Column(
                    Modifier
                        .size(panelSize.width, panelSize.height)
                        .offset { IntOffset(0, dragOffset.roundToInt()) }
                        .blockShadow(colors.border, shapes.panelShadowOffset)
                        .background(colors.surface.copy(alpha = surfaceAlpha))
                        .border(shapes.borderWidth, colors.border)
                        .semantics { paneTitle = title },
                ) {
                    if (dragToDismiss) {
                        val handleLabel = stringResource(R.string.sl_ds_panel_handle)
                        val closeLabel = stringResource(R.string.sl_ds_panel_close)
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(28.dp)
                                .draggable(
                                    orientation = Orientation.Vertical,
                                    state = rememberDraggableState { delta ->
                                        dragOffset = (dragOffset + delta).coerceAtLeast(0f)
                                    },
                                    onDragStopped = { velocity ->
                                        if (dragOffset > dismissThreshold || velocity > 1_500f) {
                                            currentOnDismiss()
                                        } else if (motion.reduced) {
                                            dragOffset = 0f
                                        } else {
                                            scope.launch { animate(dragOffset, 0f) { value, _ -> dragOffset = value } }
                                        }
                                    },
                                )
                                .semantics {
                                    contentDescription = handleLabel
                                    onClick(label = closeLabel) { currentOnDismiss(); true }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(Modifier.size(width = 40.dp, height = 4.dp).background(colors.borderStrong))
                        }
                    }
                    content()
                }
            }
        }
    }
}
