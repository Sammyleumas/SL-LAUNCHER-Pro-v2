package com.sllauncher.core.designsystem.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.unit.dp

/**
 * SL icon set: square-capped strokes on a 24-unit grid, matching the
 * neo-brutalist shape language. Drawn in code, so no icon library is added
 * to the APK. Tint them with `Icon(..., tint = ...)`.
 */
object SLIcons {
    val Search: ImageVector by lazy {
        strokeIcon("Search") {
            moveTo(4f, 4f); lineTo(16f, 4f); lineTo(16f, 16f); lineTo(4f, 16f); close()
            moveTo(16f, 16f); lineTo(20f, 20f)
        }
    }
    val Close: ImageVector by lazy {
        strokeIcon("Close") {
            moveTo(5f, 5f); lineTo(19f, 19f)
            moveTo(19f, 5f); lineTo(5f, 19f)
        }
    }
    val Mic: ImageVector by lazy {
        strokeIcon("Mic") {
            moveTo(9f, 3f); lineTo(15f, 3f); lineTo(15f, 13f); lineTo(9f, 13f); close()
            moveTo(5f, 11f); lineTo(5f, 16f); lineTo(19f, 16f); lineTo(19f, 11f)
            moveTo(12f, 16f); lineTo(12f, 20f)
            moveTo(8f, 20f); lineTo(16f, 20f)
        }
    }
    val Grid: ImageVector by lazy {
        strokeIcon("Grid") {
            square(4f, 4f); square(14f, 4f); square(4f, 14f); square(14f, 14f)
        }
    }
    val List: ImageVector by lazy {
        strokeIcon("List") {
            moveTo(4f, 6f); lineTo(20f, 6f)
            moveTo(4f, 12f); lineTo(20f, 12f)
            moveTo(4f, 18f); lineTo(20f, 18f)
        }
    }
    val Check: ImageVector by lazy {
        strokeIcon("Check") { moveTo(4f, 12f); lineTo(9f, 17f); lineTo(20f, 6f) }
    }
    val ChevronRight: ImageVector by lazy {
        strokeIcon("ChevronRight") { moveTo(9f, 5f); lineTo(16f, 12f); lineTo(9f, 19f) }
    }
    val ArrowBack: ImageVector by lazy {
        strokeIcon("ArrowBack") {
            moveTo(20f, 12f); lineTo(5f, 12f)
            moveTo(11f, 5f); lineTo(4f, 12f); lineTo(11f, 19f)
        }
    }
    val Plus: ImageVector by lazy {
        strokeIcon("Plus") {
            moveTo(12f, 5f); lineTo(12f, 19f)
            moveTo(5f, 12f); lineTo(19f, 12f)
        }
    }
    val Warning: ImageVector by lazy {
        strokeIcon("Warning") {
            moveTo(12f, 3f); lineTo(22f, 20f); lineTo(2f, 20f); close()
            moveTo(12f, 9f); lineTo(12f, 13f)
            moveTo(12f, 16.5f); lineTo(12f, 17f)
        }
    }
    val Inbox: ImageVector by lazy {
        strokeIcon("Inbox") {
            moveTo(3f, 13f); lineTo(6f, 4f); lineTo(18f, 4f); lineTo(21f, 13f)
            lineTo(21f, 20f); lineTo(3f, 20f); close()
            moveTo(3f, 13f); lineTo(8f, 13f); lineTo(9f, 16f); lineTo(15f, 16f); lineTo(16f, 13f); lineTo(21f, 13f)
        }
    }
    val Play: ImageVector by lazy {
        fillIcon("Play") { moveTo(7f, 4f); lineTo(20f, 12f); lineTo(7f, 20f); close() }
    }
    val Pause: ImageVector by lazy {
        fillIcon("Pause") { rect(6f, 4f, 4f, 16f); rect(14f, 4f, 4f, 16f) }
    }
    val SkipNext: ImageVector by lazy {
        fillIcon("SkipNext") {
            moveTo(5f, 4f); lineTo(15f, 12f); lineTo(5f, 20f); close()
            rect(17f, 4f, 3f, 16f)
        }
    }
    val SkipPrevious: ImageVector by lazy {
        fillIcon("SkipPrevious") {
            moveTo(19f, 4f); lineTo(9f, 12f); lineTo(19f, 20f); close()
            rect(4f, 4f, 3f, 16f)
        }
    }
}

private fun PathBuilder.square(x: Float, y: Float, size: Float = 6f) {
    moveTo(x, y); lineTo(x + size, y); lineTo(x + size, y + size); lineTo(x, y + size); close()
}

private fun PathBuilder.rect(x: Float, y: Float, w: Float, h: Float) {
    moveTo(x, y); lineTo(x + w, y); lineTo(x + w, y + h); lineTo(x, y + h); close()
}

private fun strokeIcon(name: String, block: PathBuilder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = "SL.$name",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(
        pathData = PathBuilder().apply(block).nodes,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 2.5f,
        strokeLineCap = StrokeCap.Square,
        strokeLineJoin = StrokeJoin.Miter,
    ).build()

private fun fillIcon(name: String, block: PathBuilder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = "SL.$name",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(
        pathData = PathBuilder().apply(block).nodes,
        fill = SolidColor(Color.Black),
    ).build()
