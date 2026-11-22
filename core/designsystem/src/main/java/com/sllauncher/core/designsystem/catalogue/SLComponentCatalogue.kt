package com.sllauncher.core.designsystem.catalogue

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sllauncher.core.designsystem.component.SLAssistantButton
import com.sllauncher.core.designsystem.component.SLAssistantState
import com.sllauncher.core.designsystem.component.SLBadge
import com.sllauncher.core.designsystem.component.SLBlock
import com.sllauncher.core.designsystem.component.SLBottomSheet
import com.sllauncher.core.designsystem.component.SLButton
import com.sllauncher.core.designsystem.component.SLButtonStyle
import com.sllauncher.core.designsystem.component.SLChipRow
import com.sllauncher.core.designsystem.component.SLDialog
import com.sllauncher.core.designsystem.component.SLDotBadge
import com.sllauncher.core.designsystem.component.SLEmptyState
import com.sllauncher.core.designsystem.component.SLErrorState
import com.sllauncher.core.designsystem.component.SLFloatingPanel
import com.sllauncher.core.designsystem.component.SLIconButton
import com.sllauncher.core.designsystem.component.SLIconButtonStyle
import com.sllauncher.core.designsystem.component.SLIconTile
import com.sllauncher.core.designsystem.component.SLLoadingIndicator
import com.sllauncher.core.designsystem.component.SLMediaControls
import com.sllauncher.core.designsystem.component.SLNavBar
import com.sllauncher.core.designsystem.component.SLNavItem
import com.sllauncher.core.designsystem.component.SLNavigationRow
import com.sllauncher.core.designsystem.component.SLPlaceholderIcon
import com.sllauncher.core.designsystem.component.SLProgressBar
import com.sllauncher.core.designsystem.component.SLSearchField
import com.sllauncher.core.designsystem.component.SLSectionHeader
import com.sllauncher.core.designsystem.component.SLSegment
import com.sllauncher.core.designsystem.component.SLSegmentedControl
import com.sllauncher.core.designsystem.component.SLSlider
import com.sllauncher.core.designsystem.component.SLSwitchRow
import com.sllauncher.core.designsystem.icon.SLIcons
import com.sllauncher.core.designsystem.theme.DarkSLColors
import com.sllauncher.core.designsystem.theme.LightSLColors
import com.sllauncher.core.designsystem.theme.SLTheme
import kotlin.math.roundToInt

/**
 * Live catalogue of every SL design-system component, for design review and
 * manual QA. All data shown here is sample data. Shipped only in debug builds
 * (see the app module's debug source set).
 */
@Composable
fun SLComponentCatalogue() {
    var dark by rememberSaveable { mutableStateOf(true) }
    var reduceMotion by rememberSaveable { mutableStateOf(false) }

    SLTheme(colors = if (dark) DarkSLColors else LightSLColors, reduceMotion = reduceMotion) {
        var panelOpen by remember { mutableStateOf(false) }
        Box(Modifier.fillMaxSize().background(SLTheme.colors.background)) {
            Column(
                Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(SLTheme.spacing.lg),
                verticalArrangement = Arrangement.spacedBy(SLTheme.spacing.xl),
            ) {
                Text("SL Components", style = SLTheme.typography.headline, color = SLTheme.colors.textPrimary)
                Section("Catalogue settings") {
                    SLSwitchRow(title = "Dark palette", checked = dark, onCheckedChange = { dark = it })
                    SLSwitchRow(
                        title = "Reduce motion",
                        subtitle = "Off follows Android's Remove animations setting",
                        checked = reduceMotion,
                        onCheckedChange = { reduceMotion = it },
                    )
                }
                TypeSection()
                ColourSection()
                ButtonSection(onOpenPanel = { panelOpen = true })
                InputSection()
                SelectionSection()
                TileSection()
                FeedbackSection()
                MediaAndAssistantSection()
                OverlaySection()
            }
            CataloguePanel(open = panelOpen, onClose = { panelOpen = false })
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(SLTheme.spacing.md)) {
        SLSectionHeader(title)
        content()
    }
}

@Composable
private fun TypeSection() = Section("Typography") {
    val t = SLTheme.typography
    val c = SLTheme.colors.textPrimary
    Text("09:41", style = t.display, color = c)
    Text("Headline", style = t.headline, color = c)
    Text("Title", style = t.title, color = c)
    Text("Body text for settings, notifications and news.", style = t.body, color = c)
    Text("Body small", style = t.bodySmall, color = SLTheme.colors.textSecondary)
    Text("LABEL", style = t.label, color = c)
    Text("Caption", style = t.caption, color = SLTheme.colors.textSecondary)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColourSection() = Section("Colour tokens") {
    val c = SLTheme.colors
    val swatches = listOf(
        "background" to c.background, "surface" to c.surface, "elevated" to c.surfaceElevated,
        "accent" to c.accent, "indicator" to c.accentIndicator, "text" to c.textPrimary,
        "text 2" to c.textSecondary, "border" to c.border, "outline" to c.borderStrong, "error" to c.error,
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        swatches.forEach { (name, colour) -> Swatch(name, colour) }
    }
}

@Composable
private fun Swatch(name: String, colour: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(56.dp).background(SLTheme.colors.borderStrong).padding(2.dp).background(colour))
        Text(name, style = SLTheme.typography.caption, color = SLTheme.colors.textSecondary)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ButtonSection(onOpenPanel: () -> Unit) = Section("Buttons") {
    SLButton(text = "Open floating panel", onClick = onOpenPanel, modifier = Modifier.fillMaxWidth())
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SLButton(text = "Secondary", onClick = {}, style = SLButtonStyle.Secondary, leadingIcon = SLIcons.Plus)
        SLButton(text = "Ghost", onClick = {}, style = SLButtonStyle.Ghost)
        SLButton(text = "Delete", onClick = {}, style = SLButtonStyle.Destructive)
        SLButton(text = "Disabled", onClick = {}, enabled = false)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SLIconButton(SLIcons.Search, "Search", {}, style = SLIconButtonStyle.Filled)
        SLIconButton(SLIcons.Grid, "Grid", {})
        SLIconButton(SLIcons.Close, "Close", {}, style = SLIconButtonStyle.Plain)
    }
}

@Composable
private fun InputSection() = Section("Inputs") {
    var query by rememberSaveable { mutableStateOf("") }
    var size by remember { mutableFloatStateOf(0.5f) }
    var columns by remember { mutableFloatStateOf(5f) }
    var labels by remember { mutableStateOf(true) }
    SLSearchField(
        query = query,
        onQueryChange = { query = it },
        placeholder = "Search apps, contacts, settings",
        trailing = { SLIconButton(SLIcons.Mic, "Voice search", {}, style = SLIconButtonStyle.Plain) },
    )
    Text("Icon size ${(size * 100).roundToInt()}%", style = SLTheme.typography.bodySmall, color = SLTheme.colors.textSecondary)
    SLSlider(value = size, onValueChange = { size = it }, contentDescription = "Icon size")
    Text("Grid columns ${columns.roundToInt()}", style = SLTheme.typography.bodySmall, color = SLTheme.colors.textSecondary)
    SLSlider(value = columns, onValueChange = { columns = it }, contentDescription = "Grid columns", valueRange = 3f..7f, steps = 3)
    SLBlock(modifier = Modifier.fillMaxWidth()) {
        SLSwitchRow(title = "Show app labels", checked = labels, onCheckedChange = { labels = it })
        SLNavigationRow(title = "App drawer", value = "${columns.roundToInt()} columns", onClick = {})
    }
}

@Composable
private fun SelectionSection() = Section("Selection") {
    val categories = listOf("All", "Social", "Media", "Tools", "Games", "Productivity")
    var category by rememberSaveable { mutableStateOf("All") }
    var mode by rememberSaveable { mutableIntStateOf(0) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    SLChipRow(items = categories, selected = category, label = { it }, onSelect = { category = it })
    SLSegmentedControl(
        options = listOf(SLSegment("Grid", SLIcons.Grid), SLSegment("List", SLIcons.List)),
        selectedIndex = mode,
        onSelect = { mode = it },
    )
    SLNavBar(
        items = listOf(SLNavItem("Apps", SLIcons.Grid), SLNavItem("Search", SLIcons.Search), SLNavItem("Alerts", SLIcons.Inbox, badgeCount = 4)),
        selectedIndex = tab,
        onSelect = { tab = it },
    )
}

@Composable
private fun TileSection() = Section("App tiles and badges") {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SLIconTile(label = "Messages", onClick = {}, badgeCount = 3) { SLPlaceholderIcon("M") }
        SLIconTile(label = "Calendar planner", onClick = {}) { SLPlaceholderIcon("C") }
        SLIconTile(label = "Mail", onClick = {}, badgeCount = 128) { SLPlaceholderIcon("E") }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        SLBadge(count = 7)
        SLBadge(count = 100)
        SLDotBadge()
    }
}

@Composable
private fun FeedbackSection() = Section("Loading, empty and error") {
    SLLoadingIndicator()
    SLProgressBar(progress = 0.62f, contentDescription = "Download progress")
    SLBlock(modifier = Modifier.fillMaxWidth()) {
        SLEmptyState(title = "No hidden apps", body = "Apps you hide from the drawer will appear here.")
    }
    SLBlock(modifier = Modifier.fillMaxWidth()) {
        SLErrorState(title = "Couldn't load news", body = "You're offline. Connect to Wi-Fi or mobile data and try again.", onRetry = {})
    }
}

@Composable
private fun MediaAndAssistantSection() = Section("Media and assistant") {
    var playing by remember { mutableStateOf(false) }
    var assistant by remember { mutableStateOf(SLAssistantState.Idle) }
    SLBlock(modifier = Modifier.fillMaxWidth(), shadow = true) {
        Text("Sample track", style = SLTheme.typography.title, color = SLTheme.colors.textPrimary)
        Text("Sample artist", style = SLTheme.typography.bodySmall, color = SLTheme.colors.textSecondary)
        SLProgressBar(progress = 0.35f)
        SLMediaControls(isPlaying = playing, onPlayPause = { playing = !playing }, onPrevious = {}, onNext = {})
    }
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        SLAssistantButton(
            state = assistant,
            onClick = { assistant = SLAssistantState.entries[(assistant.ordinal + 1) % SLAssistantState.entries.size] },
        )
        Text("Tap to cycle: ${assistant.name}", style = SLTheme.typography.bodySmall, color = SLTheme.colors.textSecondary)
    }
}

@Composable
private fun OverlaySection() = Section("Dialogs and sheets") {
    var dialog by remember { mutableStateOf(false) }
    var sheet by remember { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SLButton(text = "Dialog", onClick = { dialog = true }, style = SLButtonStyle.Secondary)
        SLButton(text = "Sheet", onClick = { sheet = true }, style = SLButtonStyle.Secondary)
    }
    if (dialog) {
        SLDialog(
            title = "Apply minimal theme?",
            text = "SL will hide the dock labels and switch to a 4-column grid. You can undo this in Customization.",
            confirmLabel = "Apply",
            dismissLabel = "Cancel",
            onConfirm = { dialog = false },
            onDismissRequest = { dialog = false },
        )
    }
    if (sheet) {
        SLBottomSheet(onDismissRequest = { sheet = false }, title = "Messages") {
            SLNavigationRow(title = "App info", onClick = { sheet = false })
            SLNavigationRow(title = "Add to favourites", onClick = { sheet = false })
            SLNavigationRow(title = "Hide from drawer", onClick = { sheet = false })
        }
    }
}

@Composable
private fun CataloguePanel(open: Boolean, onClose: () -> Unit) {
    val names = listOf("Messages", "Camera", "Maps", "Music", "Notes", "Clock", "Files", "Photos", "Mail", "Weather", "Wallet", "Settings")
    SLFloatingPanel(visible = open, onDismissRequest = onClose, title = "Sample app drawer") {
        Column(Modifier.padding(horizontal = SLTheme.spacing.lg), verticalArrangement = Arrangement.spacedBy(SLTheme.spacing.md)) {
            var query by remember { mutableStateOf("") }
            SLSearchField(query = query, onQueryChange = { query = it }, placeholder = "Search apps")
            Text("${names.size} SAMPLE APPS", style = SLTheme.typography.label, color = SLTheme.colors.textSecondary)
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 68.dp),
            modifier = Modifier.weight(1f).widthIn(max = 600.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            items(names) { name ->
                Box(contentAlignment = Alignment.Center) {
                    SLIconTile(label = name, onClick = {}, iconSize = 44.dp) { SLPlaceholderIcon(name, size = 44.dp) }
                }
            }
        }
        SLNavBar(
            items = listOf(SLNavItem("Apps", SLIcons.Grid), SLNavItem("Search", SLIcons.Search), SLNavItem("Close", SLIcons.Close)),
            selectedIndex = 0,
            onSelect = { if (it == 2) onClose() },
        )
    }
}
