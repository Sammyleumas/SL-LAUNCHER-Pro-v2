package com.sllauncher.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sllauncher.core.designsystem.icon.SLIcons
import com.sllauncher.core.designsystem.theme.SLTheme

/**
 * A settings list row: title, optional subtitle, optional leading and
 * trailing content. Clickable when [onClick] is given. At least 56dp tall.
 */
@Composable
fun SLSettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    RowLayout(
        title = title,
        subtitle = subtitle,
        enabled = enabled,
        leading = leading,
        trailing = trailing,
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick) else Modifier,
        ),
    )
}

/** Settings row that opens another screen; shows a chevron. */
@Composable
fun SLNavigationRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    value: String? = null,
) {
    SLSettingsRow(
        title = title,
        subtitle = subtitle,
        onClick = onClick,
        modifier = modifier,
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (value != null) {
                    Text(value, style = SLTheme.typography.bodySmall, color = SLTheme.colors.textSecondary)
                }
                Icon(SLIcons.ChevronRight, contentDescription = null, tint = SLTheme.colors.textSecondary, modifier = Modifier.size(20.dp))
            }
        },
    )
}

/** Settings row with a switch. The whole row toggles, which gives a large touch target. */
@Composable
fun SLSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
) {
    RowLayout(
        title = title,
        subtitle = subtitle,
        enabled = enabled,
        leading = null,
        trailing = { SLSwitch(checked = checked, onCheckedChange = null, enabled = enabled) },
        modifier = modifier.toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange),
    )
}

@Composable
private fun RowLayout(
    title: String,
    subtitle: String?,
    enabled: Boolean,
    leading: (@Composable () -> Unit)?,
    trailing: (@Composable () -> Unit)?,
    modifier: Modifier,
) {
    val colors = SLTheme.colors
    val spacing = SLTheme.spacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = SLTheme.sizes.rowMinHeight)
            .padding(horizontal = spacing.lg, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        leading?.invoke()
        Column(Modifier.weight(1f)) {
            Text(title, style = SLTheme.typography.body, color = if (enabled) colors.textPrimary else colors.textSecondary)
            if (subtitle != null) {
                Text(subtitle, style = SLTheme.typography.bodySmall, color = colors.textSecondary)
            }
        }
        trailing?.invoke()
    }
}

@Preview(backgroundColor = 0xFF151515, showBackground = true, widthDp = 360)
@Composable
private fun SLSettingsRowPreview() {
    SLTheme {
        Column {
            SLSwitchRow(title = "Show app labels", checked = true, onCheckedChange = {})
            SLSwitchRow(title = "Reduce motion", subtitle = "Turns off panel and island animations", checked = false, onCheckedChange = {})
            SLNavigationRow(title = "App drawer", value = "5 columns", onClick = {})
        }
    }
}
