package com.sllauncher.pro.common

import android.content.pm.ShortcutInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.sllauncher.pro.data.apps.AppIconLoader
import com.sllauncher.pro.data.apps.AppKey
import com.sllauncher.core.designsystem.theme.SLTheme
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import androidx.compose.ui.platform.LocalContext

/** Gives composables access to the singleton icon loader without threading it through every screen. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface IconLoaderEntryPoint {
    fun iconLoader(): AppIconLoader
    fun widgetHost(): com.sllauncher.pro.widgets.WidgetHostManager
    fun appRepository(): com.sllauncher.pro.data.apps.AppRepository
}

@Composable
fun rememberAppRepository(): com.sllauncher.pro.data.apps.AppRepository {
    val context = LocalContext.current.applicationContext
    return EntryPointAccessors.fromApplication(context, IconLoaderEntryPoint::class.java).appRepository()
}

@Composable
fun rememberWidgetHost(): com.sllauncher.pro.widgets.WidgetHostManager {
    val context = LocalContext.current.applicationContext
    return EntryPointAccessors.fromApplication(context, IconLoaderEntryPoint::class.java).widgetHost()
}

@Composable
fun rememberIconLoader(): AppIconLoader {
    val context = LocalContext.current.applicationContext
    return EntryPointAccessors.fromApplication(context, IconLoaderEntryPoint::class.java).iconLoader()
}

/** An app's icon at [size]. Shows a plain block with the first letter until the icon loads. */
@Composable
fun AppIcon(key: AppKey, label: String, size: Dp, modifier: Modifier = Modifier) {
    val loader = rememberIconLoader()
    val sizePx = with(LocalDensity.current) { size.roundToPx() }
    val bitmap by produceState(initialValue = loader.cached(key, sizePx), key, sizePx) {
        if (value == null) value = loader.appIcon(key, sizePx)
    }
    IconOrFallback(bitmap, label, size, modifier)
}

@Composable
fun ShortcutIcon(info: ShortcutInfo?, label: String, size: Dp, modifier: Modifier = Modifier) {
    val loader = rememberIconLoader()
    val sizePx = with(LocalDensity.current) { size.roundToPx() }
    val bitmap by produceState<ImageBitmap?>(initialValue = null, info?.id, sizePx) {
        value = info?.let { loader.shortcutIcon(it, sizePx) }
    }
    IconOrFallback(bitmap, label, size, modifier)
}

@Composable
private fun IconOrFallback(bitmap: ImageBitmap?, label: String, size: Dp, modifier: Modifier) {
    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = null, modifier = modifier.size(size))
    } else {
        Box(modifier.size(size).background(SLTheme.colors.surfaceElevated), contentAlignment = Alignment.Center) {
            Text(label.take(1).uppercase(), style = SLTheme.typography.title, color = SLTheme.colors.textSecondary)
        }
    }
}
