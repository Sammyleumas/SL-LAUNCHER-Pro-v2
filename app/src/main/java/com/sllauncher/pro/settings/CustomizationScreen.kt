package com.sllauncher.pro.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.sllauncher.pro.common.SystemIntents
import com.sllauncher.pro.common.UiCommand
import com.sllauncher.pro.common.UiCommandBus
import com.sllauncher.pro.common.startSafely
import com.sllauncher.pro.data.apps.AppCategory
import com.sllauncher.pro.data.apps.AppIconLoader
import com.sllauncher.pro.data.apps.IconPack
import com.sllauncher.pro.data.apps.IconPackManager
import com.sllauncher.pro.data.db.ProfileEntity
import com.sllauncher.pro.data.settings.AccentPreset
import com.sllauncher.pro.data.settings.DisplayFont
import com.sllauncher.pro.data.settings.DrawerSort
import com.sllauncher.pro.data.settings.IslandPosition
import com.sllauncher.pro.data.settings.LauncherSettings
import com.sllauncher.pro.data.settings.SettingsRepository
import com.sllauncher.pro.data.settings.ThemeMode
import com.sllauncher.pro.drawer.categoryLabel
import com.sllauncher.pro.ui.ScreenScaffold
import com.sllauncher.pro.ui.colorsFor
import com.sllauncher.core.designsystem.component.SLBottomSheet
import com.sllauncher.core.designsystem.component.SLButton
import com.sllauncher.core.designsystem.component.SLButtonStyle
import com.sllauncher.core.designsystem.component.SLChip
import com.sllauncher.core.designsystem.component.SLNavigationRow
import com.sllauncher.core.designsystem.component.SLSearchField
import com.sllauncher.core.designsystem.component.SLSectionHeader
import com.sllauncher.core.designsystem.component.SLSegment
import com.sllauncher.core.designsystem.component.SLSegmentedControl
import com.sllauncher.core.designsystem.component.SLSlider
import com.sllauncher.core.designsystem.component.SLSwitchRow
import com.sllauncher.core.designsystem.theme.SLTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import javax.inject.Inject

@HiltViewModel
class CustomizationViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val profiles: ProfileManager,
    private val iconPacks: IconPackManager,
    private val icons: AppIconLoader,
    val entitlements: EntitlementRepository,
    private val bus: UiCommandBus,
) : ViewModel() {
    val state: StateFlow<LauncherSettings> = settings.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), settings.current)
    val savedProfiles = profiles.saved.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun update(transform: (LauncherSettings) -> LauncherSettings) = viewModelScope.launch { settings.update(transform) }

    fun packs(): List<IconPack> = iconPacks.installedPacks()

    fun selectIconPack(pkg: String?) = viewModelScope.launch {
        if (iconPacks.select(pkg)) {
            icons.clear()
            settings.update { it.copy(iconPackPackage = pkg) }
        } else {
            bus.send(UiCommand.ShowMessage("That icon pack couldn't be loaded."))
        }
    }

    fun applyProfile(name: String) = viewModelScope.launch {
        if (profiles.apply(name)) bus.send(UiCommand.ShowMessage("$name profile applied."))
    }

    fun undoProfile() = viewModelScope.launch {
        bus.send(UiCommand.ShowMessage(if (profiles.undo()) "Restored your previous setup." else "Nothing to undo."))
    }

    fun saveProfile(name: String) = viewModelScope.launch {
        val count = savedProfiles.value.count { !it.name.startsWith("__") }
        if (count >= 3 && !entitlements.has(PremiumFeature.CustomProfiles)) {
            bus.send(UiCommand.ShowMessage("Free includes 3 saved profiles. Delete one to save another."))
            return@launch
        }
        profiles.saveCurrentAs(name)
        bus.send(UiCommand.ShowMessage("Saved \"$name\"."))
    }

    fun deleteProfile(p: ProfileEntity) = viewModelScope.launch { profiles.delete(p) }
    fun builtInProfiles() = profiles.builtInNames()
}

/** Customization studio: every visual setting, with a live preview. */
@Composable
fun CustomizationRoute(onBack: () -> Unit, viewModel: CustomizationViewModel = hiltViewModel()) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val saved by viewModel.savedProfiles.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var section by rememberSaveable { mutableStateOf("Home") }
    var showPacks by remember { mutableStateOf(false) }
    var saveName by remember { mutableStateOf<String?>(null) }

    ScreenScaffold(title = "Customize", onBack = onBack, scrollable = true) {
        Preview(s, Modifier.padding(16.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(listOf("Home", "Drawer", "Island", "Theme", "Profiles")) { name -> SLChip(name, section == name, { section = name }) }
        }
        Column(Modifier.padding(vertical = 8.dp)) {
            when (section) {
                "Home" -> {
                    StepSlider("Grid columns", s.homeColumns, 3..6) { v -> viewModel.update { it.copy(homeColumns = v) } }
                    StepSlider("Grid rows", s.homeRows, 4..7) { v -> viewModel.update { it.copy(homeRows = v) } }
                    PercentSlider("Icon size", s.homeIconScale, 0.7f..1.3f) { v -> viewModel.update { it.copy(homeIconScale = v) } }
                    SLSwitchRow("App names", s.homeShowLabels, { on -> viewModel.update { it.copy(homeShowLabels = on) } })
                    SLSwitchRow("Dock", s.dockEnabled, { on -> viewModel.update { it.copy(dockEnabled = on) } })
                    SLSwitchRow("Clock", s.homeShowClock, { on -> viewModel.update { it.copy(homeShowClock = on) } })
                    SLSwitchRow("Search bar", s.homeShowSearchBar, { on -> viewModel.update { it.copy(homeShowSearchBar = on) } })
                    SLSwitchRow("Daily overview", s.homeShowOverview, { on -> viewModel.update { it.copy(homeShowOverview = on) } })
                    SLNavigationRow("Wallpaper", { context.startSafely(SystemIntents.wallpaperPicker()) }, subtitle = "Opens Android's wallpaper picker")
                }
                "Drawer" -> {
                    StepSlider("Columns", s.drawerColumns, 3..7) { v -> viewModel.update { it.copy(drawerColumns = v) } }
                    PercentSlider("Icon size", s.drawerIconScale, 0.7f..1.3f) { v -> viewModel.update { it.copy(drawerIconScale = v) } }
                    PercentSlider("Panel width", s.drawerWidthFraction, 0.7f..0.95f) { v -> viewModel.update { it.copy(drawerWidthFraction = v) } }
                    PercentSlider("Panel height", s.drawerHeightFraction, 0.6f..0.9f) { v -> viewModel.update { it.copy(drawerHeightFraction = v) } }
                    PercentSlider("Panel opacity", s.drawerPanelAlpha, 0.8f..1f) { v -> viewModel.update { it.copy(drawerPanelAlpha = v) } }
                    SLSwitchRow("App names", s.drawerShowLabels, { on -> viewModel.update { it.copy(drawerShowLabels = on) } })
                    SLSwitchRow("List view", s.drawerListMode, { on -> viewModel.update { it.copy(drawerListMode = on) } })
                    SLSwitchRow("Alphabet index", s.drawerShowAlphabetIndex, { on -> viewModel.update { it.copy(drawerShowAlphabetIndex = on) } })
                    SLSwitchRow("Recent apps row", s.drawerShowRecents, { on -> viewModel.update { it.copy(drawerShowRecents = on) } })
                    SLSectionHeader("Sort", Modifier.padding(16.dp, 8.dp))
                    SLSegmentedControl(
                        options = listOf(SLSegment("A–Z"), SLSegment("Most used"), SLSegment("Newest")),
                        selectedIndex = s.drawerSort.ordinal,
                        onSelect = { i -> viewModel.update { it.copy(drawerSort = DrawerSort.entries[i]) } },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    SLSectionHeader("Visible categories", Modifier.padding(16.dp, 8.dp))
                    AppCategory.entries.filter { it != AppCategory.Other }.forEach { c ->
                        SLSwitchRow(categoryLabel(c), c.name !in s.drawerHiddenCategories, { on ->
                            viewModel.update { it.copy(drawerHiddenCategories = if (on) it.drawerHiddenCategories - c.name else it.drawerHiddenCategories + c.name) }
                        })
                    }
                }
                "Island" -> {
                    SLSwitchRow("Dynamic Island", s.islandEnabled, { on -> viewModel.update { it.copy(islandEnabled = on) } })
                    SLSectionHeader("Position", Modifier.padding(16.dp, 8.dp))
                    SLSegmentedControl(
                        options = listOf(SLSegment("Centre"), SLSegment("Left")),
                        selectedIndex = s.islandPosition.ordinal,
                        onSelect = { i -> viewModel.update { it.copy(islandPosition = IslandPosition.entries[i]) } },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    SLSwitchRow("Music", s.islandShowMusic, { on -> viewModel.update { it.copy(islandShowMusic = on) } })
                    SLSwitchRow("Messages", s.islandShowMessages, { on -> viewModel.update { it.copy(islandShowMessages = on) } })
                    SLSwitchRow("Charging and battery", s.islandShowCharging, { on -> viewModel.update { it.copy(islandShowCharging = on) } })
                    SLSwitchRow("Hide message content", s.islandHideMessageContent, { on -> viewModel.update { it.copy(islandHideMessageContent = on) } }, subtitle = "Shows only the app name")
                }
                "Theme" -> {
                    SLSectionHeader("Mode", Modifier.padding(16.dp, 8.dp))
                    SLSegmentedControl(
                        options = listOf(SLSegment("Dark"), SLSegment("Light")),
                        selectedIndex = s.themeMode.ordinal,
                        onSelect = { i -> viewModel.update { it.copy(themeMode = ThemeMode.entries[i]) } },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    SLSectionHeader("Accent", Modifier.padding(16.dp, 8.dp))
                    Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AccentPreset.entries.forEach { a ->
                            val locked = a.premium && !viewModel.entitlements.has(PremiumFeature.ExtraAccents)
                            Box(
                                Modifier
                                    .size(48.dp)
                                    .background(Color(a.argb))
                                    .border(if (s.accent == a) 4.dp else 2.dp, if (s.accent == a) SLTheme.colors.textPrimary else SLTheme.colors.borderStrong)
                                    .clickable(role = Role.RadioButton, enabled = !locked) { viewModel.update { it.copy(accent = a) } }
                                    .semantics { contentDescription = "${a.name} accent" + if (locked) ", premium" else "" },
                                contentAlignment = Alignment.Center,
                            ) { if (locked) Text("P", style = SLTheme.typography.label, color = Color(0xFF090909)) }
                        }
                    }
                    Text("Accents marked P are part of SL Premium, which isn't on sale yet.", style = SLTheme.typography.caption, color = SLTheme.colors.textSecondary, modifier = Modifier.padding(16.dp, 4.dp))
                    SLSectionHeader("Font", Modifier.padding(16.dp, 8.dp))
                    SLSegmentedControl(
                        options = listOf(SLSegment("SL (Space Grotesk)"), SLSegment("System")),
                        selectedIndex = s.displayFont.ordinal,
                        onSelect = { i -> viewModel.update { it.copy(displayFont = DisplayFont.entries[i]) } },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    SLNavigationRow("Icon pack", { showPacks = true }, value = s.iconPackPackage ?: "System icons")
                    SLSwitchRow("Low-performance mode", s.lowPerformanceMode, { on -> viewModel.update { it.copy(lowPerformanceMode = on) } }, subtitle = "No animations, no transparency. Best for older phones.")
                }
                "Profiles" -> {
                    Text("A profile saves your layout and look. Switching never changes privacy settings.", style = SLTheme.typography.bodySmall, color = SLTheme.colors.textSecondary, modifier = Modifier.padding(16.dp, 4.dp))
                    s.activeProfile?.let { Text("Active: $it", style = SLTheme.typography.title, color = SLTheme.colors.accentIndicator, modifier = Modifier.padding(16.dp, 4.dp)) }
                    viewModel.builtInProfiles().forEach { name -> SLNavigationRow(name, { viewModel.applyProfile(name) }, subtitle = "Built-in") }
                    saved.filter { !it.name.startsWith("__") }.forEach { p ->
                        SLNavigationRow(p.name, { viewModel.applyProfile(p.name) }, subtitle = "Saved")
                        SLButton("Delete ${p.name}", { viewModel.deleteProfile(p) }, style = SLButtonStyle.Ghost, modifier = Modifier.padding(start = 8.dp))
                    }
                    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SLButton("Save current", { saveName = "" })
                        SLButton("Undo last switch", { viewModel.undoProfile() }, style = SLButtonStyle.Secondary)
                    }
                }
            }
        }
    }

    if (showPacks) {
        val packs = remember { viewModel.packs() }
        SLBottomSheet(onDismissRequest = { showPacks = false }, title = "Icon pack") {
            SLNavigationRow("System icons", { viewModel.selectIconPack(null); showPacks = false })
            packs.forEach { p -> SLNavigationRow(p.label, { viewModel.selectIconPack(p.packageName); showPacks = false }) }
            if (packs.isEmpty()) Text("No icon packs installed. Packs for Nova or ADW launchers work with SL.", style = SLTheme.typography.bodySmall, color = SLTheme.colors.textSecondary, modifier = Modifier.padding(16.dp))
        }
    }
    saveName?.let { name ->
        SLBottomSheet(onDismissRequest = { saveName = null }, title = "Save profile") {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SLSearchField(query = name, onQueryChange = { saveName = it.take(24) }, placeholder = "e.g. Weekend")
                SLButton("Save", { viewModel.saveProfile(name); saveName = null }, enabled = name.isNotBlank())
            }
        }
    }
}

@Composable
private fun StepSlider(title: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Text("$title: $value", style = SLTheme.typography.body, color = SLTheme.colors.textPrimary)
        SLSlider(
            value = value.toFloat(),
            onValueChange = { onChange(it.roundToInt()) },
            contentDescription = title,
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = range.last - range.first - 1,
        )
    }
}

@Composable
private fun PercentSlider(title: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Text("$title: ${(value * 100).roundToInt()}%", style = SLTheme.typography.body, color = SLTheme.colors.textPrimary)
        SLSlider(value = value, onValueChange = onChange, contentDescription = title, valueRange = range)
    }
}

/** Small live preview of the home grid with the current theme, accent, columns and labels. */
@Composable
private fun Preview(s: LauncherSettings, modifier: Modifier) {
    val colors = colorsFor(s)
    Column(
        modifier
            .fillMaxWidth()
            .background(colors.background)
            .border(2.dp, colors.borderStrong)
            .padding(12.dp)
            .semantics { contentDescription = "Preview: ${s.homeColumns} columns, ${s.themeMode.name.lowercase()} theme" },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("09:41", style = SLTheme.typography.headline, color = colors.textPrimary)
        repeat(2) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(s.homeColumns) { i ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size((28 * s.homeIconScale).dp).background(if (i == 0) colors.accent else colors.surfaceElevated))
                        if (s.homeShowLabels) Box(Modifier.padding(top = 3.dp).size(width = 22.dp, height = 3.dp).background(colors.textSecondary))
                    }
                }
            }
        }
        if (s.dockEnabled) Box(Modifier.fillMaxWidth().height(20.dp).background(colors.surface).border(1.dp, colors.border))
    }
}
