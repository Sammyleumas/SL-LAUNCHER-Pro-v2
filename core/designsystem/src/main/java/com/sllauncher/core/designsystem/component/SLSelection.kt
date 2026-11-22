package com.sllauncher.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sllauncher.core.designsystem.icon.SLIcons
import com.sllauncher.core.designsystem.theme.SLTheme

/**
 * Category selector chip ("All", "Social", "Games"...). Selected chips are
 * pressed into the page and filled with the accent; unselected chips are
 * raised with an outline. 40dp tall with a 48dp touch area.
 */
@Composable
fun SLChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = SLTheme.colors
    Box(
        modifier = modifier
            .heightIn(min = SLTheme.shapes.minTouchTarget)
            .selectable(selected = selected, enabled = enabled, role = Role.Tab, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .height(40.dp)
                .then(
                    if (selected) {
                        Modifier.softInset(CircleShape, softStyle(colors.accent))
                    } else {
                        Modifier.softRaised(CircleShape, softStyle(depth = 3.dp))
                    },
                )
                .border(SLTheme.shapes.borderWidth, if (selected) colors.accentOutline else colors.borderStrong, CircleShape)
                .padding(horizontal = SLTheme.spacing.lg),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = SLTheme.typography.label,
                color = if (selected) colors.onAccent else colors.textPrimary,
                maxLines = 1,
            )
        }
    }
}

/** Horizontally scrolling row of [SLChip]s with single selection. */
@Composable
fun <T> SLChipRow(
    items: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
) {
    LazyRow(
        modifier = modifier.selectableGroup(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(SLTheme.spacing.sm),
    ) {
        itemsIndexed(items, key = { index, item -> "$index:$item" }) { _, item ->
            SLChip(label = label(item), selected = item == selected, onClick = { onSelect(item) })
        }
    }
}

/**
 * Two to four mutually exclusive options in one pressed-in track, such as
 * the app drawer's grid/list toggle. The selected option is a raised pill.
 */
@Composable
fun SLSegmentedControl(
    options: List<SLSegment>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SLTheme.colors
    Row(
        modifier = modifier
            .softInset(CircleShape, softStyle(colors.surfaceElevated))
            .border(SLTheme.shapes.borderWidth, colors.borderStrong, CircleShape)
            .padding(3.dp)
            .selectableGroup(),
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            Row(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .heightIn(min = SLTheme.shapes.minTouchTarget - 6.dp)
                    .then(if (selected) Modifier.softRaised(CircleShape, softStyle(colors.accent, 3.dp)) else Modifier)
                    .selectable(selected = selected, role = Role.RadioButton, onClick = { onSelect(index) })
                    .padding(horizontal = SLTheme.spacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                val tint = if (selected) colors.onAccent else colors.textPrimary
                if (option.icon != null) {
                    Icon(option.icon, contentDescription = if (option.showLabel) null else option.label, tint = tint, modifier = Modifier.size(20.dp))
                }
                if (option.showLabel) {
                    if (option.icon != null) Spacer(Modifier.size(SLTheme.spacing.sm))
                    Text(option.label, style = SLTheme.typography.label, color = tint, maxLines = 1)
                }
            }
        }
    }
}

/** One option of an [SLSegmentedControl]. When [showLabel] is false the label is still read by TalkBack. */
@Immutable
data class SLSegment(val label: String, val icon: ImageVector? = null, val showLabel: Boolean = true)

/** One destination in an [SLNavBar]. */
@Immutable
data class SLNavItem(val label: String, val icon: ImageVector, val badgeCount: Int = 0)

/**
 * Bottom navigation for floating panels and full-screen sections. The active
 * item sits in a pressed-in pill in the accent colour; others are muted.
 */
@Composable
fun SLNavBar(
    items: List<SLNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SLTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = SLTheme.sizes.navBarHeight)
            .background(colors.surface)
            .selectableGroup(),
    ) {
        items.forEachIndexed { index, item ->
            val selected = index == selectedIndex
            val tint = if (selected) colors.accentIndicator else colors.textSecondary
            Column(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = SLTheme.sizes.navBarHeight)
                    .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(index) }),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(SLTheme.spacing.sm))
                Box(
                    Modifier
                        .size(width = 56.dp, height = 30.dp)
                        .then(if (selected) Modifier.softInset(CircleShape, softStyle(colors.surfaceElevated)) else Modifier),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(item.icon, contentDescription = null, tint = tint, modifier = Modifier.size(SLTheme.sizes.icon))
                    if (item.badgeCount > 0) {
                        SLBadge(count = item.badgeCount, modifier = Modifier.align(Alignment.TopEnd).padding(start = 16.dp))
                    }
                }
                Text(
                    text = item.label,
                    style = SLTheme.typography.caption,
                    color = tint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = SLTheme.spacing.xs, bottom = SLTheme.spacing.sm),
                )
            }
        }
    }
}

@Preview(backgroundColor = 0xFFE6E9EF, showBackground = true, widthDp = 360)
@Composable
private fun SLSelectionPreview() {
    SLTheme {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SLChipRow(items = listOf("All", "Social", "Media", "Tools"), selected = "Social", label = { it }, onSelect = {})
            SLSegmentedControl(
                options = listOf(SLSegment("Grid", SLIcons.Grid), SLSegment("List", SLIcons.List)),
                selectedIndex = 0,
                onSelect = {},
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            SLNavBar(
                items = listOf(SLNavItem("Apps", SLIcons.Grid), SLNavItem("Search", SLIcons.Search), SLNavItem("Alerts", SLIcons.Inbox, 3)),
                selectedIndex = 0,
                onSelect = {},
            )
        }
    }
}
