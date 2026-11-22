package com.sllauncher.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sllauncher.core.designsystem.theme.SLTheme

/**
 * Confirmation dialog. Use for decisions that need an explicit answer, such
 * as confirming a theme change the SL assistant proposed.
 *
 * @param destructive styles the confirm button for irreversible actions.
 */
@Composable
fun SLDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
    dismissLabel: String? = null,
    icon: ImageVector? = null,
    destructive: Boolean = false,
) {
    val colors = SLTheme.colors
    val spacing = SLTheme.spacing
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            Modifier
                .padding(spacing.xl)
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .blockShadow(colors.border, SLTheme.shapes.panelShadowOffset)
                .background(colors.surface)
                .border(SLTheme.shapes.borderWidth, colors.borderStrong)
                .padding(spacing.xl),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = if (destructive) colors.error else colors.accentIndicator, modifier = Modifier.size(28.dp))
            }
            Text(title, style = SLTheme.typography.headline, color = colors.textPrimary, modifier = Modifier.semantics { heading() })
            Text(text, style = SLTheme.typography.body, color = colors.textSecondary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (dismissLabel != null) {
                    SLButton(text = dismissLabel, onClick = onDismissRequest, style = SLButtonStyle.Ghost)
                }
                SLButton(
                    text = confirmLabel,
                    onClick = onConfirm,
                    style = if (destructive) SLButtonStyle.Destructive else SLButtonStyle.Primary,
                )
            }
        }
    }
}

/**
 * Modal bottom sheet in SL styling: square top, hard border line, SL drag
 * handle. Opens fully expanded (no half-open state) so content is never
 * cut off on short phones.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SLBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = SLTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RectangleShape,
        containerColor = colors.surface,
        contentColor = colors.textPrimary,
        tonalElevation = 0.dp,
        scrimColor = colors.scrim,
        dragHandle = {
            Column(Modifier.fillMaxWidth()) {
                Box(Modifier.fillMaxWidth().height(SLTheme.shapes.borderWidth).background(colors.borderStrong))
                Box(Modifier.fillMaxWidth().padding(vertical = SLTheme.spacing.md), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(width = 40.dp, height = 4.dp).background(colors.borderStrong))
                }
            }
        },
    ) {
        Column(Modifier.navigationBarsPadding().padding(bottom = SLTheme.spacing.lg)) {
            if (title != null) {
                Text(
                    title,
                    style = SLTheme.typography.title,
                    color = colors.textPrimary,
                    modifier = Modifier
                        .padding(horizontal = SLTheme.spacing.lg, vertical = SLTheme.spacing.sm)
                        .semantics { heading() },
                )
            }
            content()
        }
    }
}
