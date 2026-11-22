package com.sllauncher.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sllauncher.core.designsystem.icon.SLIcons
import com.sllauncher.core.designsystem.theme.SLColors
import com.sllauncher.core.designsystem.theme.SLTheme

enum class SLButtonStyle {
    /** The one main action on a screen. Accent fill. */
    Primary,

    /** Supporting actions. Raised soft surface with an outline. */
    Secondary,

    /** Low-emphasis actions such as "Cancel". No fill, no shadow. */
    Ghost,

    /** Irreversible actions such as "Uninstall". */
    Destructive,
}

private data class ButtonColors(val container: Color, val content: Color, val outline: Color, val hasShadow: Boolean)

private fun SLColors.forStyle(style: SLButtonStyle) = when (style) {
    SLButtonStyle.Primary -> ButtonColors(accent, onAccent, accentOutline, hasShadow = true)
    SLButtonStyle.Secondary -> ButtonColors(surface, textPrimary, borderStrong, hasShadow = true)
    SLButtonStyle.Ghost -> ButtonColors(Color.Transparent, textPrimary, Color.Transparent, hasShadow = false)
    SLButtonStyle.Destructive -> ButtonColors(error, onError, error, hasShadow = true)
}

/**
 * Soft-UI button: a rounded surface raised off the page by soft shadows,
 * with a thin outline so it reads as tappable without relying on shadows.
 * Pressing it sinks the surface into the page. Always at least 48dp tall.
 */
@Composable
fun SLButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: SLButtonStyle = SLButtonStyle.Primary,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
) {
    val colors = SLTheme.colors.forStyle(style)
    val spacing = SLTheme.spacing
    PressableBlock(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        container = colors.container,
        outline = colors.outline,
        raised = colors.hasShadow,
        role = Role.Button,
        contentPadding = Modifier.padding(horizontal = spacing.xl, vertical = spacing.md),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) {
                Icon(leadingIcon, contentDescription = null, tint = colors.content, modifier = Modifier.size(SLTheme.sizes.iconSmall))
            }
            Text(
                text = text,
                style = SLTheme.typography.label,
                color = colors.content,
                textAlign = TextAlign.Center,
            )
        }
    }
}

enum class SLIconButtonStyle { Filled, Outlined, Plain }

/** Square 48dp icon-only button. [contentDescription] is required for TalkBack. */
@Composable
fun SLIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: SLIconButtonStyle = SLIconButtonStyle.Outlined,
    enabled: Boolean = true,
) {
    val c = SLTheme.colors
    val (container, content, outline) = when (style) {
        SLIconButtonStyle.Filled -> Triple(c.accent, c.onAccent, c.accentOutline)
        SLIconButtonStyle.Outlined -> Triple(c.surface, c.textPrimary, c.borderStrong)
        SLIconButtonStyle.Plain -> Triple(Color.Transparent, c.textPrimary, Color.Transparent)
    }
    PressableBlock(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        container = container,
        outline = outline,
        raised = style != SLIconButtonStyle.Plain,
        role = Role.Button,
        contentPadding = Modifier.size(SLTheme.shapes.minTouchTarget),
        onClickLabel = contentDescription,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = content, modifier = Modifier.size(SLTheme.sizes.icon))
    }
}

/**
 * Shared press mechanics for SL buttons: raised while idle, pressed into the
 * page while touched. The change is instant, so reduced motion needs no
 * special case.
 */
@Composable
private fun PressableBlock(
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    container: Color,
    outline: Color,
    raised: Boolean,
    role: Role,
    contentPadding: Modifier,
    onClickLabel: String? = null,
    content: @Composable () -> Unit,
) {
    val shapes = SLTheme.shapes
    val shape = RoundedCornerShape(shapes.cornerSmall)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val style = softStyle(container)
    val surface = when {
        !raised -> Modifier.background(container, shape)
        pressed && enabled -> Modifier.softInset(shape, style)
        else -> Modifier.softRaised(shape, style)
    }

    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.4f)
            .defaultMinSize(minWidth = shapes.minTouchTarget, minHeight = shapes.minTouchTarget)
            .then(surface)
            .border(shapes.borderWidth, outline, shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClickLabel = onClickLabel,
                role = role,
                onClick = onClick,
            )
            .then(contentPadding),
        contentAlignment = Alignment.Center,
        propagateMinConstraints = false,
    ) {
        content()
    }
}

@Preview(backgroundColor = 0xFFE6E9EF, showBackground = true)
@Composable
private fun SLButtonPreview() {
    SLTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SLButton(text = "Set as default", onClick = {})
            SLButton(text = "Open settings", onClick = {}, style = SLButtonStyle.Secondary, leadingIcon = SLIcons.ChevronRight)
            SLButton(text = "Cancel", onClick = {}, style = SLButtonStyle.Ghost)
            SLButton(text = "Uninstall", onClick = {}, style = SLButtonStyle.Destructive)
            SLIconButton(icon = SLIcons.Search, contentDescription = "Search", onClick = {})
        }
    }
}
