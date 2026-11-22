package com.sllauncher.pro.settings

import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sllauncher.pro.common.SystemIntents
import com.sllauncher.pro.common.startSafely
import com.sllauncher.pro.device.TorchController
import com.sllauncher.pro.navigation.DeviceInfoDestination
import com.sllauncher.pro.ui.ScreenScaffold
import com.sllauncher.core.designsystem.component.SLButton
import com.sllauncher.core.designsystem.component.SLButtonStyle
import com.sllauncher.core.designsystem.component.SLSectionHeader
import com.sllauncher.core.designsystem.component.SLSlider
import com.sllauncher.core.designsystem.theme.SLTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import androidx.compose.foundation.shape.RoundedCornerShape
import com.sllauncher.core.designsystem.component.softRaised
import com.sllauncher.core.designsystem.component.softStyle
import com.sllauncher.core.designsystem.component.softInset

@HiltViewModel
class QuickSettingsViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    val torch: TorchController,
) : ViewModel() {
    private val audio = context.getSystemService(AudioManager::class.java)

    fun canWriteSettings(): Boolean = Settings.System.canWrite(context)

    /** Screen brightness 0..1. Turning off auto-brightness is the system's choice, so SL only adjusts the level. */
    fun brightness(): Float = runCatching {
        Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f
    }.getOrDefault(0.5f)

    fun setBrightness(value: Float) {
        if (!canWriteSettings()) return
        runCatching { Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, (value * 255).toInt().coerceIn(1, 255)) }
    }

    fun mediaVolume(): Float = audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)

    fun setMediaVolume(value: Float) {
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        runCatching { audio.setStreamVolume(AudioManager.STREAM_MUSIC, (value * max).toInt(), AudioManager.FLAG_SHOW_UI) }
    }
}

private data class Tile(val label: String, val detail: String, val onClick: () -> Unit, val on: Boolean? = null)

/** Quick settings: direct controls where Android allows them, system panels everywhere else. */
@Composable
fun QuickSettingsRoute(onBack: () -> Unit, onNavigate: (Any) -> Unit, viewModel: QuickSettingsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val torchOn by viewModel.torch.on.collectAsStateWithLifecycle()
    var canWrite by remember { mutableStateOf(viewModel.canWriteSettings()) }
    var brightness by remember { mutableFloatStateOf(viewModel.brightness()) }
    var volume by remember { mutableFloatStateOf(viewModel.mediaVolume()) }

    LifecycleResumeEffect(Unit) {
        canWrite = viewModel.canWriteSettings()
        brightness = viewModel.brightness()
        volume = viewModel.mediaVolume()
        onPauseOrDispose { }
    }

    val tiles = listOf(
        Tile("Wi-Fi", "Android panel", { context.startSafely(SystemIntents.wifi()) }),
        Tile("Internet", "Data and Wi-Fi", { context.startSafely(SystemIntents.internet()) }),
        Tile("Bluetooth", "Android settings", { context.startSafely(SystemIntents.bluetooth()) }),
        Tile("Flashlight", if (torchOn) "On" else "Off", { viewModel.torch.set(!torchOn) }, on = torchOn).takeIf { viewModel.torch.available },
        Tile("Airplane", "Android settings", { context.startSafely(SystemIntents.airplane()) }),
        Tile("Hotspot", "Android settings", { context.startSafely(SystemIntents.hotspot()) }),
        Tile("Dark mode", "Display settings", { context.startSafely(SystemIntents.display()) }),
        Tile("Battery", "Usage and saver", { context.startSafely(SystemIntents.battery()) }),
        Tile("Sound", "Volume panel", { context.startSafely(SystemIntents.volumePanel()) }),
        Tile("Display", "Android settings", { context.startSafely(SystemIntents.display()) }),
        Tile("All settings", "Android settings", { context.startSafely(SystemIntents.deviceSettings()) }),
        Tile("Device info", "This phone", { onNavigate(DeviceInfoDestination) }),
    ).filterNotNull()

    ScreenScaffold(title = "Quick settings", onBack = onBack) {
        LazyVerticalGrid(columns = GridCells.Adaptive(150.dp), contentPadding = PaddingValues(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SLSectionHeader("Brightness")
                    if (canWrite) {
                        SLSlider(value = brightness, onValueChange = { brightness = it; viewModel.setBrightness(it) }, contentDescription = "Screen brightness")
                    } else {
                        Text("To change brightness here, allow SL to modify system settings. Only brightness is changed.", style = SLTheme.typography.bodySmall, color = SLTheme.colors.textSecondary)
                        SLButton("Allow", { context.startSafely(SystemIntents.writeSettings(context)) }, style = SLButtonStyle.Secondary)
                    }
                    SLSectionHeader("Media volume")
                    SLSlider(value = volume, onValueChange = { volume = it; viewModel.setMediaVolume(it) }, contentDescription = "Media volume")
                    Text("Wi-Fi, Bluetooth, airplane mode and hotspot can't be switched by apps on modern Android. These tiles open the system panel instead.", style = SLTheme.typography.caption, color = SLTheme.colors.textSecondary)
                }
            }
            items(tiles, key = { it.label }) { tile ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 76.dp)
                        .then(
                            if (tile.on == true) {
                                Modifier.softInset(RoundedCornerShape(SLTheme.shapes.cornerMedium), softStyle(SLTheme.colors.accent))
                            } else {
                                Modifier.softRaised(RoundedCornerShape(SLTheme.shapes.cornerMedium), softStyle())
                            },
                        )
                        .border(SLTheme.shapes.borderWidth, if (tile.on == true) SLTheme.colors.accentOutline else SLTheme.colors.borderStrong, RoundedCornerShape(SLTheme.shapes.cornerMedium))
                        .clickable(role = if (tile.on != null) Role.Switch else Role.Button, onClick = tile.onClick)
                        .semantics { if (tile.on != null) stateDescription = if (tile.on) "On" else "Off" }
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val c = if (tile.on == true) SLTheme.colors.onAccent else SLTheme.colors.textPrimary
                    Text(tile.label, style = SLTheme.typography.label, color = c)
                    Text(tile.detail, style = SLTheme.typography.caption, color = if (tile.on == true) c else SLTheme.colors.textSecondary)
                }
            }
        }
    }
}
