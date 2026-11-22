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

enum class WallpaperStyle { Glow, Bubbles, Waves }

/** One built-in wallpaper: a base colour with soft shapes in three colours. */
data class SoftWallpaper(
    val name: String,
    val base: Int,
    val colors: List<Int>,
    val style: WallpaperStyle = WallpaperStyle.Glow,
    val dark: Boolean = false,
)

private fun c(argb: Long) = argb.toInt()

/** SL Pro's default wallpapers. Drawn in code, so they add nothing to the APK size. */
val SoftWallpapers = listOf(
    SoftWallpaper("Cloud", c(0xFFE6E9EF), listOf(c(0xFFC9D3FF), c(0xFFFFFFFF), c(0xFFD7E7F5))),
    SoftWallpaper("Mint", c(0xFFE3EFEA), listOf(c(0xFFB5E4D2), c(0xFFFFFFFF), c(0xFFCDE7F0))),
    SoftWallpaper("Peach", c(0xFFF3E9E4), listOf(c(0xFFFFD2BF), c(0xFFFFFFFF), c(0xFFF4D7E6))),
    SoftWallpaper("Lavender", c(0xFFECE8F4), listOf(c(0xFFD6C8FF), c(0xFFFFFFFF), c(0xFFE8D5F2)), WallpaperStyle.Bubbles),
    SoftWallpaper("Sky bubbles", c(0xFFE4EDF5), listOf(c(0xFFB9D4F0), c(0xFFFFFFFF), c(0xFFCFE0FF)), WallpaperStyle.Bubbles),
    SoftWallpaper("Sand waves", c(0xFFF1ECE3), listOf(c(0xFFE6D7BF), c(0xFFF8F3EA), c(0xFFDCCAB0)), WallpaperStyle.Waves),
    SoftWallpaper("Sea waves", c(0xFFE2ECEF), listOf(c(0xFFBFDCE3), c(0xFFF2F8FA), c(0xFFA9CCD6)), WallpaperStyle.Waves),
    SoftWallpaper("Dusk", c(0xFF2B2F3A), listOf(c(0xFF4C5BD4), c(0xFF8E5AA8), c(0xFF3A6E8F)), dark = true),
    SoftWallpaper("Night", c(0xFF1D2026), listOf(c(0xFF2E3A5C), c(0xFF23262D), c(0xFF33405A)), dark = true),
    SoftWallpaper("Aurora", c(0xFF1B2229), listOf(c(0xFF1F6F5C), c(0xFF3A4FA0), c(0xFF5C3A80)), dark = true),
    SoftWallpaper("Midnight bubbles", c(0xFF202531), listOf(c(0xFF34406A), c(0xFF2A3350), c(0xFF46406E)), WallpaperStyle.Bubbles, dark = true),
    SoftWallpaper("Deep waves", c(0xFF1E232B), listOf(c(0xFF2C3A52), c(0xFF263042), c(0xFF35466A)), WallpaperStyle.Waves, dark = true),
)

/** The wallpaper SL suggests for a light or dark theme. */
fun defaultSoftWallpaper(dark: Boolean): SoftWallpaper = if (dark) SoftWallpapers.first { it.name == "Night" } else SoftWallpapers.first()

private fun transparent(color: Int) = color and 0x00FFFFFF

/** Draws [wallpaper] at [width] x [height]. Pure drawing, no image files in the APK. */
fun renderSoftWallpaper(wallpaper: SoftWallpaper, width: Int, height: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(wallpaper.base)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val w = width.toFloat()
    val h = height.toFloat()
    when (wallpaper.style) {
        WallpaperStyle.Glow -> {
            val spots = listOf(0.15f to 0.2f, 0.85f to 0.55f, 0.3f to 0.9f)
            wallpaper.colors.zip(spots).forEach { (color, spot) ->
                val radius = w * 0.75f
                paint.shader = RadialGradient(w * spot.first, h * spot.second, radius, color, transparent(color), Shader.TileMode.CLAMP)
                canvas.drawCircle(w * spot.first, h * spot.second, radius, paint)
            }
        }
        WallpaperStyle.Bubbles -> {
            // Soft, slightly lit circles at fixed positions (x, y, radius as fractions of the width/height).
            val bubbles = listOf(
                Triple(0.2f, 0.18f, 0.28f), Triple(0.82f, 0.3f, 0.2f), Triple(0.55f, 0.52f, 0.34f),
                Triple(0.12f, 0.72f, 0.18f), Triple(0.86f, 0.82f, 0.26f), Triple(0.4f, 0.92f, 0.14f),
            )
            bubbles.forEachIndexed { i, (x, y, r) ->
                val color = wallpaper.colors[i % wallpaper.colors.size]
                val radius = w * r
                paint.shader = RadialGradient(w * x - radius * 0.3f, h * y - radius * 0.3f, radius * 1.4f, color, transparent(color), Shader.TileMode.CLAMP)
                canvas.drawCircle(w * x, h * y, radius, paint)
            }
        }
        WallpaperStyle.Waves -> {
            paint.shader = null
            wallpaper.colors.forEachIndexed { i, color ->
                val top = h * (0.45f + i * 0.17f)
                val amplitude = h * 0.05f
                val path = android.graphics.Path().apply {
                    moveTo(0f, top)
                    var x = 0f
                    while (x <= w) {
                        lineTo(x, top + amplitude * kotlin.math.sin((x / w * 2f * Math.PI + i).toFloat()))
                        x += w / 48f
                    }
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                paint.color = (color and 0x00FFFFFF) or (0xB0 shl 24)
                canvas.drawPath(path, paint)
            }
        }
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
