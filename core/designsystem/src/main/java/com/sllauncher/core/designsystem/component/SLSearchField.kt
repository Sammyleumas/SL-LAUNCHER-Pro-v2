package com.sllauncher.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sllauncher.core.designsystem.R
import com.sllauncher.core.designsystem.icon.SLIcons
import com.sllauncher.core.designsystem.theme.SLTheme

/**
 * Single-line search input. The outline turns yellow while focused, a clear
 * button appears once there is text, and the keyboard shows a Search key.
 *
 * @param trailing optional extra control, e.g. a voice-input button.
 */
@Composable
fun SLSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onSearch: (String) -> Unit = {},
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = SLTheme.colors
    val shapes = SLTheme.shapes
    val spacing = SLTheme.spacing
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()

    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = true,
        textStyle = SLTheme.typography.body.copy(color = colors.textPrimary),
        cursorBrush = SolidColor(colors.accentIndicator),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
        interactionSource = interaction,
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .heightIn(min = SLTheme.sizes.controlHeight)
                    .background(colors.surface)
                    .border(shapes.borderWidth, if (focused) colors.accentOutline else colors.borderStrong)
                    .padding(start = spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    SLIcons.Search,
                    contentDescription = null,
                    tint = if (focused) colors.accentIndicator else colors.textSecondary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(spacing.md))
                Box(Modifier.weight(1f).padding(vertical = spacing.md)) {
                    if (query.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = SLTheme.typography.body,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    innerTextField()
                }
                if (query.isNotEmpty()) {
                    SLIconButton(
                        icon = SLIcons.Close,
                        contentDescription = stringResource(R.string.sl_ds_search_clear),
                        onClick = { onQueryChange("") },
                        style = SLIconButtonStyle.Plain,
                    )
                }
                trailing?.invoke()
            }
        },
    )
}

@Preview(backgroundColor = 0xFF090909, showBackground = true)
@Composable
private fun SLSearchFieldPreview() {
    SLTheme {
        Box(Modifier.padding(16.dp)) {
            SLSearchField(query = "", onQueryChange = {}, placeholder = "Search apps")
        }
    }
}
