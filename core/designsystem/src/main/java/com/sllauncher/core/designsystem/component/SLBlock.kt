package com.sllauncher.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sllauncher.core.designsystem.theme.SLTheme

/**
 * A content block (SL's "card"). Use sparingly: only for content that is a
 * distinct object, such as a widget or a now-playing panel, never as a
 * default wrapper for every section.
 *
 * @param shadow adds the offset block shadow to lift the block off the page.
 */
@Composable
fun SLBlock(
    modifier: Modifier = Modifier,
    elevated: Boolean = false,
    shadow: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = SLTheme.colors
    Column(
        modifier = modifier
            .then(if (shadow) Modifier.blockShadow(colors.border, SLTheme.shapes.blockShadowOffset) else Modifier)
            .background(if (elevated) colors.surfaceElevated else colors.surface)
            .border(SLTheme.shapes.borderWidth, colors.border)
            .padding(SLTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(SLTheme.spacing.sm),
        content = content,
    )
}

/** Section heading with the yellow SL tick. Marked as a heading for TalkBack navigation. */
@Composable
fun SLSectionHeader(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.semantics { heading() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(width = 16.dp, height = 4.dp).background(SLTheme.colors.accentIndicator))
        Text(
            text = text.uppercase(),
            style = SLTheme.typography.label,
            color = SLTheme.colors.textSecondary,
            modifier = Modifier.padding(start = SLTheme.spacing.sm),
        )
    }
}

@Preview(backgroundColor = 0xFF090909, showBackground = true, widthDp = 320)
@Composable
private fun SLBlockPreview() {
    SLTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SLSectionHeader("Now playing")
            SLBlock(shadow = true) {
                Text("Block content", style = SLTheme.typography.body, color = SLTheme.colors.textPrimary)
            }
        }
    }
}
