package com.sllauncher.pro.settings

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.sllauncher.pro.data.settings.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/** One built-in wallpaper: a base colour with three large, soft colour glows. */
data class SoftWallpaper(val name: String, val base: Int, val glows: List<Int>)

val SoftWallpapers = listOf(
    SoftWallpaper("Cloud", 0xFFE6E9EF.toInt(), listOf(0xFFC9D3FF.toInt(), 0xFFFFFFFF.toInt(), 0xFFD7E7F5.toInt())),
    SoftWallpaper("Dusk", 0xFF2B2F3A.toInt(), listOf(0xFF4C5BD4.toInt(), 0xFF8E5AA8.toInt(), 0xFF3A6E8F.toInt())),
    SoftWallpaper("Mint", 0xFFE3EFEA.toInt(), listOf(0xFFB5E4D2.toInt(), 0xFFFFFFFF.toInt(), 0xFFCDE7F0.toInt())),
    SoftWallpaper("Peach", 0xFFF3E9E4.toInt(), listOf(0xFFFFD2BF.toInt(), 0xFFFFFFFF.toInt(), 0xFFF4D7E6.toInt())),
    SoftWallpaper("Night", 0xFF1D2026.toInt(), listOf(0xFF2E3A5C.toInt(), 0xFF23262D.toInt(), 0xFF33405A.toInt())),
)

/** Draws [wallpaper] at [width] x [height]. Pure drawing, no image files in the APK. */
fun renderSoftWallpaper(wallpaper: SoftWallpaper, width: Int, height: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(wallpaper.base)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val spots = listOf(0.15f to 0.2f, 0.85f to 0.55f, 0.3f to 0.9f)
    wallpaper.glows.zip(spots).forEach { (color, spot) ->
        val cx = width * spot.first
        val cy = height * spot.second
        val radius = width * 0.75f
        paint.shader = RadialGradient(cx, cy, radius, color, color and 0x00FFFFFF, Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, cy, radius, paint)
    }
    return bitmap
}

/** Sets a soft wallpaper on the home screen (and the lock screen when Android allows it). */
suspend fun applySoftWallpaper(context: Context, wallpaper: SoftWallpaper): Boolean = withContext(Dispatchers.Default) {
    runCatching {
        val metrics = context.resources.displayMetrics
        val bitmap = renderSoftWallpaper(wallpaper, metrics.widthPixels, metrics.heightPixels)
        WallpaperManager.getInstance(context).setBitmap(bitmap)
        bitmap.recycle()
    }.isSuccess
}

/** Changes to the next built-in wallpaper once a day while "Daily wallpaper" is on. */
@HiltWorker
class DailyWallpaperWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val settings: SettingsRepository,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (!settings.settings.first().dailySoftWallpaper) return Result.success()
        val pick = SoftWallpapers[(LocalDate.now().toEpochDay() % SoftWallpapers.size).toInt()]
        applySoftWallpaper(applicationContext, pick)
        return Result.success()
    }

    companion object {
        private const val NAME = "daily-wallpaper"

        fun schedule(context: Context, enabled: Boolean) {
            val wm = WorkManager.getInstance(context)
            if (!enabled) {
                wm.cancelUniqueWork(NAME)
                return
            }
            wm.enqueueUniquePeriodicWork(
                NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<DailyWallpaperWorker>(24, TimeUnit.HOURS).build(),
            )
        }
    }
}
