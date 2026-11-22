package com.sllauncher.core.designsystem.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sllauncher.core.designsystem.R
import com.sllauncher.core.designsystem.theme.SLTheme

/**
 * Text shown in a badge: nothing for zero or less, the number up to 99,
 * then "99+" so the badge never grows wider than three characters.
 */
fun badgeText(count: Int): String = when {
    count <= 0 -> ""
    count > 99 -> "99+"
    else -> count.toString()
}

/** Unread / notification count badge. Renders nothing when [count] is 0. */
@Composable
fun SLBadge(count: Int, modifier: Modifier = Modifier) {
    val text = badgeText(count)
    if (text.isEmpty()) return
    val colors = SLTheme.colors
    val description = stringResource(R.string.sl_ds_badge_count, count)
    Box(
        modifier = modifier
            .clearAndSetSemantics { contentDescription = description }
            .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
            .background(colors.accent)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = SLTheme.typography.caption.copy(fontSize = 11.sp, lineHeight = 14.sp),
            color = colors.onAccent,
            maxLines = 1,
        )
    }
}

/** Small yellow square marking "something new" where a number is not useful. */
@Composable
fun SLDotBadge(modifier: Modifier = Modifier) {
    Box(modifier.size(8.dp).background(SLTheme.colors.accentIndicator))
}

/**
 * App icon container used on the home screen, dock and app drawer.
 *
 * The icon itself is a slot so callers can pass a real app icon bitmap
 * (Phase 4) or a placeholder. The click target merges the label and the
 * badge ("Messages, 3 new") into one TalkBack item.
 *
 * @param iconSize size of the icon square; the tile is 16dp wider for the label.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SLIconTile(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    iconSize: Dp = SLTheme.sizes.appIcon,
    showLabel: Boolean = true,
    badgeCount: Int = 0,
    icon: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .width(iconSize + 16.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                onLongClickLabel = onLongClickLabel,
                role = Role.Button,
            )
            .padding(vertical = SLTheme.spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SLTheme.spacing.xs),
    ) {
        Box(Modifier.size(iconSize), contentAlignment = Alignment.Center) {
            icon()
            SLBadge(count = badgeCount, modifier = Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-6).dp))
        }
        if (showLabel) {
            Text(
                text = label,
                style = SLTheme.typography.caption,
                color = SLTheme.colors.textPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Placeholder icon for previews and the catalogue: a coloured block with an initial. */
@Composable
fun SLPlaceholderIcon(initial: String, size: Dp = SLTheme.sizes.appIcon) {
    Box(
        Modifier.size(size).background(SLTheme.colors.surfaceElevated),
        contentAlignment = Alignment.Center,
    ) {
        Text(initial.take(1).uppercase(), style = SLTheme.typography.title, color = SLTheme.colors.textPrimary)
    }
}

@Preview(backgroundColor = 0xFF090909, showBackground = true)
@Composable
private fun SLIconTilePreview() {
    SLTheme {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SLIconTile(label = "Messages", onClick = {}, badgeCount = 3) { SLPlaceholderIcon("M") }
            SLIconTile(label = "A very long app name", onClick = {}) { SLPlaceholderIcon("A") }
            SLIconTile(label = "Mail", onClick = {}, badgeCount = 120) { SLPlaceholderIcon("E") }
        }
    }
}
