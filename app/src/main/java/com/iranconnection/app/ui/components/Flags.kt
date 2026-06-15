package com.iranconnection.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

/** Circular flag avatar. Country code drives the design; falls back to a grey disc. */
@Composable
fun CountryFlag(code: String, size: Dp = 38.dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(size)
            .shadow(3.dp, CircleShape)
            .clip(CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        when (code) {
            "ca" -> CanadaFlag(size)
            else -> Canvas(Modifier.size(size)) { drawFlag(code) }
        }
    }
}

private fun DrawScope.drawFlag(code: String) {
    val w = size.width
    val h = size.height
    // viewBox was 0..46; scale factor
    fun sx(v: Float) = v / 46f * w
    fun sy(v: Float) = v / 46f * h
    fun rect(x: Float, y: Float, rw: Float, rh: Float, c: Color) =
        drawRect(c, Offset(sx(x), sy(y)), Size(sx(rw), sy(rh)))

    when (code) {
        "ir" -> {
            // Iran: green / white / red with simple central emblem
            rect(0f, 0f, 46f, 15.33f, Color(0xFF239F40))
            rect(0f, 15.33f, 46f, 15.34f, Color.White)
            rect(0f, 30.67f, 46f, 15.33f, Color(0xFFDA0000))
            val c = Offset(w / 2f, h / 2f)
            drawCircle(Color(0xFFDA0000), radius = sx(5f), center = c, style = Stroke(width = sx(1.3f)))
            drawLine(Color(0xFFDA0000), Offset(c.x, c.y - sy(5f)), Offset(c.x, c.y + sy(5f)), strokeWidth = sx(1.3f), cap = StrokeCap.Round)
            drawLine(Color(0xFFDA0000), Offset(c.x - sx(5f), c.y), Offset(c.x + sx(5f), c.y), strokeWidth = sx(1.3f), cap = StrokeCap.Round)
        }
        "us" -> {
            rect(0f, 0f, 46f, 46f, Color(0xFFB22234))
            listOf(3.54f, 10.62f, 17.69f, 24.77f, 31.85f, 38.92f).forEach {
                rect(0f, it, 46f, 3.54f, Color.White)
            }
            rect(0f, 0f, 23f, 24.77f, Color(0xFF3C3B6E))
        }
        "ar" -> {
            rect(0f, 0f, 46f, 46f, Color(0xFF74ACDF))
            rect(0f, 15.33f, 46f, 15.33f, Color.White)
            val c = Offset(w / 2f, h / 2f)
            drawCircle(Color(0xFFF6B40E), radius = sx(5.5f), center = c)
            for (i in 0 until 8) {
                val a = i * 45.0 * Math.PI / 180.0
                drawLine(
                    Color(0xFFF6B40E),
                    Offset(c.x + sx(7.5f) * cos(a).toFloat(), c.y + sy(7.5f) * sin(a).toFloat()),
                    Offset(c.x + sx(10.5f) * cos(a).toFloat(), c.y + sy(10.5f) * sin(a).toFloat()),
                    strokeWidth = sx(1.5f),
                )
            }
        }
        "fi" -> {
            rect(0f, 0f, 46f, 46f, Color.White)
            rect(12f, 0f, 9f, 46f, Color(0xFF003580))
            rect(0f, 18f, 46f, 9f, Color(0xFF003580))
        }
        "au", "uk" -> {
            val navy = if (code == "au") Color(0xFF00008B) else Color(0xFF012169)
            val red = if (code == "au") Color(0xFFCC0001) else Color(0xFFC8102E)
            rect(0f, 0f, 46f, 46f, navy)
            val diag = Path().apply { moveTo(0f, 0f); lineTo(w, h) }
            val diag2 = Path().apply { moveTo(w, 0f); lineTo(0f, h) }
            drawPath(diag, Color.White, style = Stroke(width = sx(11f)))
            drawPath(diag2, Color.White, style = Stroke(width = sx(11f)))
            drawPath(diag, red, style = Stroke(width = sx(6f)))
            drawPath(diag2, red, style = Stroke(width = sx(6f)))
            rect(18f, 0f, 10f, 46f, Color.White)
            rect(0f, 18f, 46f, 10f, Color.White)
            rect(20f, 0f, 6f, 46f, red)
            rect(0f, 20f, 46f, 6f, red)
        }
        "nl" -> {
            rect(0f, 0f, 46f, 15.33f, Color(0xFFAE1C28))
            rect(0f, 15.33f, 46f, 15.34f, Color.White)
            rect(0f, 30.67f, 46f, 15.33f, Color(0xFF21468B))
        }
        else -> drawRect(Color(0xFFC0C8D4))
    }
}

@Composable
private fun CanadaFlag(size: Dp) {
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            fun sx(v: Float) = v / 46f * w
            drawRect(Color(0xFFD80621))
            drawRect(Color.White, Offset(sx(11.5f), 0f), Size(sx(23f), h))
        }
        Text("🍁", fontSize = 14.sp)
    }
}
