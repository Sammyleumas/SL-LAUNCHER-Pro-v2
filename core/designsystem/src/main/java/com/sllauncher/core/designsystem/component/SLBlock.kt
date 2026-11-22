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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
 * A content block (SL's soft "card"). Use for content that is a distinct
 * object, such as a widget or a now-playing panel.
 *
 * @param elevated draws the block pressed into the page instead of raised.
 * @param shadow lifts the block off the page with soft shadows; without it
 *   the block is a flat rounded surface with a hairline edge.
 */
@Composable
fun SLBlock(
    modifier: Modifier = Modifier,
    elevated: Boolean = false,
    shadow: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = SLTheme.colors
    val shape = RoundedCornerShape(SLTheme.shapes.cornerMedium)
    val surface = when {
        elevated -> Modifier.softInset(shape, softStyle(colors.surfaceElevated))
        shadow -> Modifier.softRaised(shape, softStyle())
        else -> Modifier.background(colors.surface, shape).border(SLTheme.shapes.borderWidth, colors.border, shape)
    }
    Column(
        modifier = modifier
            .then(surface)
            .padding(SLTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(SLTheme.spacing.sm),
        content = content,
    )
}

/** Section heading with a small accent dot. Marked as a heading for TalkBack navigation. */
@Composable
fun SLSectionHeader(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.semantics { heading() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).background(SLTheme.colors.accentIndicator, CircleShape))
        Text(
            text = text,
            style = SLTheme.typography.label,
            color = SLTheme.colors.textSecondary,
            modifier = Modifier.padding(start = SLTheme.spacing.sm),
        )
    }
}

@Preview(backgroundColor = 0xFFE6E9EF, showBackground = true, widthDp = 320)
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
