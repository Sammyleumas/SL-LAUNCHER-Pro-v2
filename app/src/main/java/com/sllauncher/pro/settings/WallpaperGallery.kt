package com.sllauncher.pro.settings

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.sllauncher.core.designsystem.component.SLBottomSheet
import com.sllauncher.core.designsystem.component.SLSwitchRow
import com.sllauncher.core.designsystem.theme.SLTheme
import com.sllauncher.pro.common.UiCommand
import com.sllauncher.pro.common.UiCommandBus
import com.sllauncher.pro.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class WallpaperViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
    private val bus: UiCommandBus,
) : ViewModel() {
    val daily: StateFlow<Boolean> = settings.settings.map { it.dailySoftWallpaper }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun apply(wallpaper: SoftWallpaper) = viewModelScope.launch {
        val ok = applySoftWallpaper(context, wallpaper)
        bus.send(UiCommand.ShowMessage(if (ok) "${wallpaper.name} wallpaper set." else "Couldn't set the wallpaper."))
    }

    fun setDaily(on: Boolean) = viewModelScope.launch {
        settings.update { it.copy(dailySoftWallpaper = on) }
        DailyWallpaperWorker.schedule(context, on)
    }
}

/** A small preview of a built-in wallpaper, drawn off the main thread. */
@Composable
private fun WallpaperThumb(wallpaper: SoftWallpaper, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val preview by produceState<ImageBitmap?>(null, wallpaper) {
        value = withContext(Dispatchers.Default) { renderSoftWallpaper(wallpaper, 180, 320).asImageBitmap() }
    }
    val shape = RoundedCornerShape(SLTheme.shapes.cornerSmall)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val frame = Modifier
            .fillMaxWidth()
            .aspectRatio(9f / 16f)
            .clip(shape)
            .border(1.dp, SLTheme.colors.borderStrong, shape)
            .clickable(role = Role.Button, onClickLabel = "Set ${wallpaper.name} wallpaper", onClick = onClick)
        val bitmap = preview
        if (bitmap != null) {
            Image(bitmap, contentDescription = "${wallpaper.name} wallpaper", contentScale = ContentScale.Crop, modifier = frame)
        } else {
            Spacer(frame)
        }
        Text(wallpaper.name, style = SLTheme.typography.caption, color = SLTheme.colors.textPrimary, maxLines = 1)
    }
}

/** Gallery of SL's default wallpapers. Tap one to set it on the home screen. */
@Composable
fun WallpaperGallerySheet(onDismiss: () -> Unit, viewModel: WallpaperViewModel = hiltViewModel()) {
    val daily by viewModel.daily.collectAsStateWithLifecycle()
    SLBottomSheet(onDismissRequest = onDismiss, title = "SL wallpapers") {
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SoftWallpapers.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { w -> WallpaperThumb(w, { viewModel.apply(w); onDismiss() }, Modifier.weight(1f)) }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        SLSwitchRow(
            title = "Change daily",
            subtitle = "A different SL wallpaper each day",
            checked = daily,
            onCheckedChange = { on -> viewModel.setDaily(on) },
        )
    }
}
