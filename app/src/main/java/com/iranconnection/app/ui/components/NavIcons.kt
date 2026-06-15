package com.iranconnection.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

// Custom 24x24 nav glyphs recreated from the design's inline SVGs.

@Composable
fun HomeIcon(color: Color, filled: Boolean, modifier: Modifier = Modifier.size24()) {
    Canvas(modifier) {
        val u = size.width / 24f
        // M3 10.5 L12 3 L21 10.5 V21 H15 V15 H9 V21 H3 Z
        val path = Path().apply {
            moveTo(3 * u, 10.5f * u)
            lineTo(12 * u, 3 * u)
            lineTo(21 * u, 10.5f * u)
            lineTo(21 * u, 21 * u)
            lineTo(15 * u, 21 * u)
            lineTo(15 * u, 15 * u)
            lineTo(9 * u, 15 * u)
            lineTo(9 * u, 21 * u)
            lineTo(3 * u, 21 * u)
            close()
        }
        if (filled) drawPath(path, color)
        else drawPath(path, color, style = Stroke(width = 1.5f * u))
    }
}

@Composable
fun GlobeIcon(color: Color, modifier: Modifier = Modifier.size24()) {
    Canvas(modifier) {
        val u = size.width / 24f
        val c = Offset(12 * u, 12 * u)
        drawCircle(color, radius = 9 * u, center = c, style = Stroke(width = 1.5f * u))
        // Meridians
        val left = Path().apply {
            moveTo(12 * u, 3 * u)
            cubicTo(10 * u, 6.5f * u, 8 * u, 9 * u, 8 * u, 12 * u)
            cubicTo(8 * u, 15 * u, 10 * u, 17.5f * u, 12 * u, 21 * u)
        }
        val right = Path().apply {
            moveTo(12 * u, 3 * u)
            cubicTo(14 * u, 6.5f * u, 16 * u, 9 * u, 16 * u, 12 * u)
            cubicTo(16 * u, 15 * u, 14 * u, 17.5f * u, 12 * u, 21 * u)
        }
        drawPath(left, color, style = Stroke(width = 1.5f * u, cap = StrokeCap.Round))
        drawPath(right, color, style = Stroke(width = 1.5f * u, cap = StrokeCap.Round))
        // Equator
        drawLine(color, Offset(3 * u, 12 * u), Offset(21 * u, 12 * u), strokeWidth = 1.5f * u, cap = StrokeCap.Round)
    }
}

@Composable
fun AppsIcon(color: Color, modifier: Modifier = Modifier.size24()) {
    Canvas(modifier) {
        val u = size.width / 24f
        val s = 8 * u
        val r = 2 * u
        fun cell(x: Float, y: Float, alpha: Float) {
            drawRoundRect(
                color = color.copy(alpha = alpha),
                topLeft = Offset(x * u, y * u),
                size = Size(s, s),
                cornerRadius = CornerRadius(r, r),
            )
        }
        cell(3f, 3f, 1f)
        cell(13f, 3f, 0.6f)
        cell(3f, 13f, 0.6f)
        cell(13f, 13f, 0.3f)
    }
}

private fun Modifier.size24() = this.then(Modifier.size(24.dp))
