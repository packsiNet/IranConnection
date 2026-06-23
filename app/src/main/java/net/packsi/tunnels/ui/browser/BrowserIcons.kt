package net.packsi.tunnels.ui.browser

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Hand-drawn 24x24 glyphs so the browser UI needs no material-icons dependency,
// matching the project's existing Canvas icon style (see NavIcons.kt).

@Composable
fun BackArrowIcon(color: Color, size: Dp = 20.dp) {
    Canvas(Modifier.size(size)) {
        val u = this.size.width / 24f
        val w = 1.8f * u
        // Arrow shaft
        drawLine(color, Offset(20 * u, 12 * u), Offset(5 * u, 12 * u), strokeWidth = w, cap = StrokeCap.Round)
        // Arrow head
        val head = Path().apply {
            moveTo(11 * u, 6 * u)
            lineTo(5 * u, 12 * u)
            lineTo(11 * u, 18 * u)
        }
        drawPath(head, color, style = Stroke(width = w, cap = StrokeCap.Round))
    }
}

@Composable
fun ForwardArrowIcon(color: Color, size: Dp = 20.dp) {
    Canvas(Modifier.size(size)) {
        val u = this.size.width / 24f
        val w = 1.8f * u
        drawLine(color, Offset(4 * u, 12 * u), Offset(19 * u, 12 * u), strokeWidth = w, cap = StrokeCap.Round)
        val head = Path().apply {
            moveTo(13 * u, 6 * u)
            lineTo(19 * u, 12 * u)
            lineTo(13 * u, 18 * u)
        }
        drawPath(head, color, style = Stroke(width = w, cap = StrokeCap.Round))
    }
}

@Composable
fun CloseIcon(color: Color, size: Dp = 20.dp) {
    Canvas(Modifier.size(size)) {
        val u = this.size.width / 24f
        val w = 1.8f * u
        drawLine(color, Offset(6 * u, 6 * u), Offset(18 * u, 18 * u), strokeWidth = w, cap = StrokeCap.Round)
        drawLine(color, Offset(18 * u, 6 * u), Offset(6 * u, 18 * u), strokeWidth = w, cap = StrokeCap.Round)
    }
}

@Composable
fun AddIcon(color: Color, size: Dp = 20.dp) {
    Canvas(Modifier.size(size)) {
        val u = this.size.width / 24f
        val w = 1.8f * u
        drawLine(color, Offset(12 * u, 5 * u), Offset(12 * u, 19 * u), strokeWidth = w, cap = StrokeCap.Round)
        drawLine(color, Offset(5 * u, 12 * u), Offset(19 * u, 12 * u), strokeWidth = w, cap = StrokeCap.Round)
    }
}

@Composable
fun RefreshIcon(color: Color, size: Dp = 20.dp) {
    Canvas(Modifier.size(size)) {
        val u = this.size.width / 24f
        val w = 1.8f * u
        // ~270° arc, leaving a gap at the top-right for the arrowhead.
        drawArc(
            color = color,
            startAngle = -60f,
            sweepAngle = 300f,
            useCenter = false,
            topLeft = Offset(5 * u, 5 * u),
            size = androidx.compose.ui.geometry.Size(14 * u, 14 * u),
            style = Stroke(width = w, cap = StrokeCap.Round),
        )
        // Arrowhead at the arc's open end.
        val head = Path().apply {
            moveTo(15.5f * u, 4 * u)
            lineTo(18.5f * u, 7 * u)
            lineTo(14.5f * u, 8.5f * u)
        }
        drawPath(head, color, style = Stroke(width = w, cap = StrokeCap.Round))
    }
}
