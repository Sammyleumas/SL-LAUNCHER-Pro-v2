package com.sllauncher.pro.data.apps

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ShortcutInfo
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads app and shortcut icons as bitmaps, off the main thread, with a
 * memory cache sized to the device (1/16 of the app's memory class, so
 * low-RAM phones keep a smaller cache). Applies the selected icon pack.
 */
@Singleton
class AppIconLoader @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val appRepository: AppRepository,
    private val iconPacks: IconPackManager,
) {
    private val cache: LruCache<String, ImageBitmap> = run {
        val memoryClassMb = context.getSystemService(ActivityManager::class.java).memoryClass
        val maxBytes = memoryClassMb * 1024 * 1024 / 16
        object : LruCache<String, ImageBitmap>(maxBytes) {
            override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
        }
    }

    private val density get() = context.resources.displayMetrics.densityDpi

    fun cached(key: AppKey, sizePx: Int): ImageBitmap? = cache.get(cacheKey(key.flatten(), sizePx))

    suspend fun appIcon(key: AppKey, sizePx: Int): ImageBitmap? {
        val cacheKey = cacheKey(key.flatten(), sizePx)
        cache.get(cacheKey)?.let { return it }
        return withContext(Dispatchers.IO) {
            val packIcon = iconPacks.iconFor(key.component)
            val drawable: Drawable? = packIcon ?: runCatching {
                appRepository.activityInfo(key)?.getBadgedIcon(density)
            }.getOrNull()
            drawable?.let { render(it, sizePx) }?.also { cache.put(cacheKey, it) }
        }
    }

    suspend fun shortcutIcon(info: ShortcutInfo, sizePx: Int): ImageBitmap? = withContext(Dispatchers.IO) {
        val key = cacheKey("sc:${info.`package`}:${info.id}:${info.userHandle.hashCode()}", sizePx)
        cache.get(key) ?: runCatching {
            appRepository.launcherApps().getShortcutBadgedIconDrawable(info, density)
        }.getOrNull()?.let { render(it, sizePx) }?.also { cache.put(key, it) }
    }

    /** Drops cached icons, e.g. after the icon pack changes. */
    fun clear() = cache.evictAll()

    private fun render(drawable: Drawable, sizePx: Int): ImageBitmap =
        drawable.toBitmap(sizePx, sizePx).asImageBitmap()

    private fun cacheKey(id: String, sizePx: Int) = "$id@$sizePx@${iconPacks.activePackage}"
}
