package com.sllauncher.core.designsystem.component

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import com.sllauncher.core.designsystem.theme.SLTheme

/** Everything a soft surface needs to draw itself, read once from the theme. */
@Immutable
data class SoftStyle(
    val fill: Color,
    val dark: Color,
    val light: Color,
    val depth: Dp,
    val enabled: Boolean,
)

/** The theme's soft style for ordinary raised elements, optionally with another [fill]. */
@Composable
@ReadOnlyComposable
fun softStyle(fill: Color = SLTheme.colors.surface, depth: Dp = SLTheme.shapes.softDepth): SoftStyle = SoftStyle(
    fill = fill,
    dark = SLTheme.colors.shadowDark,
    light = SLTheme.colors.shadowLight,
    depth = depth,
    enabled = SLTheme.shapes.softShadows,
)

/**
 * Draws [shape] filled with the style's colour, lifted off the background by
 * two soft shadows: a highlight towards the top-left and a shade towards the
 * bottom-right. Replaces `background()` for the element.
 *
 * Blurred shadows on paths need hardware support from Android 9 (API 28).
 * Older phones, and low-performance mode, get a cheap flat offset shade.
 */
fun Modifier.softRaised(shape: Shape, style: SoftStyle): Modifier = drawWithCache {
    val d = style.depth.toPx()
    val path = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawWithCache)) }.asAndroidPath()
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    val fill = style.fill.toArgb()
    val dark = style.dark.toArgb()
    val light = style.light.toArgb()
    val blurred = style.enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
    onDrawBehind {
        drawIntoCanvas { canvas ->
            val c = canvas.nativeCanvas
            paint.color = fill
            if (blurred) {
                paint.setShadowLayer(d * 1.6f, -d, -d, light)
                c.drawPath(path, paint)
                paint.setShadowLayer(d * 1.6f, d, d, dark)
                c.drawPath(path, paint)
                paint.clearShadowLayer()
            } else if (d > 0f) {
                val offset = d / 2f
                c.save()
                c.translate(offset, offset)
                paint.color = dark
                c.drawPath(path, paint)
                c.restore()
                paint.color = fill
            }
            c.drawPath(path, paint)
        }
    }
}

/**
 * Draws [shape] as a well pressed into the background: the fill with soft
 * shadows falling inside its top-left edge and highlights inside the
 * bottom-right. Used for search fields, slider tracks and pressed buttons.
 */
fun Modifier.softInset(shape: Shape, style: SoftStyle): Modifier = drawWithCache {
    val d = style.depth.toPx() * 0.6f
    val path = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawWithCache)) }.asAndroidPath()
    val margin = d * 4f
    val ring = android.graphics.Path().apply {
        addRect(-margin, -margin, size.width + margin, size.height + margin, android.graphics.Path.Direction.CW)
        addPath(path)
        fillType = android.graphics.Path.FillType.EVEN_ODD
    }
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    val fill = style.fill.toArgb()
    val dark = style.dark.toArgb()
    val light = style.light.toArgb()
    val blurred = style.enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
    onDrawBehind {
        drawIntoCanvas { canvas ->
            val c = canvas.nativeCanvas
            paint.color = fill
            c.drawPath(path, paint)
            if (blurred) {
                c.save()
                c.clipPath(path)
                paint.setShadowLayer(d * 1.6f, d, d, dark)
                c.drawPath(ring, paint)
                paint.setShadowLayer(d * 1.6f, -d, -d, light)
                c.drawPath(ring, paint)
                paint.clearShadowLayer()
                c.restore()
            }
        }
    }
}
