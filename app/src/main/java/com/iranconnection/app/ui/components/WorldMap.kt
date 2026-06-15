package com.iranconnection.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import com.iranconnection.app.ui.theme.AppColors

/**
 * Dotted world map, ported from the design's inline SVG (viewBox 0 0 393 310).
 * Continents are clipped, then filled with a 7px dot grid (r 1.35, #B8C2CF).
 */
@Composable
fun WorldMap(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val sx = size.width / 393f
        val sy = size.height / 310f
        val s = minOf(sx, sy)

        val continents = continentPaths().map { spec ->
            Path().apply {
                spec(this) { x, y -> Offset(x * sx, y * sy) }
            }
        }

        val combined = Path()
        continents.forEach { combined.addPath(it) }

        clipPath(combined, clipOp = ClipOp.Intersect) {
            val step = 7f * s
            val r = 1.35f * s
            var y = step / 2f
            while (y < size.height) {
                var x = step / 2f
                while (x < size.width) {
                    drawCircle(AppColors.MapDot, radius = r, center = Offset(x, y))
                    x += step
                }
                y += step
            }
        }
    }
}

// Each lambda builds one continent path using a point mapper (vx, vy) -> Offset.
private fun continentPaths(): List<(Path, (Float, Float) -> Offset) -> Unit> = listOf(
    // North America
    { p, m ->
        p.moveTo(m(18f, 40f)); p.cubic(m, 36f, 22f, 72f, 16f, 110f, 11f)
        p.cubic(m, 134f, 9f, 150f, 20f, 137f, 47f)
        p.cubic(m, 127f, 64f, 132f, 108f, 102f, 115f)
        p.cubic(m, 82f, 121f, 66f, 100f, 49f, 90f)
        p.cubic(m, 30f, 80f, 12f, 57f, 18f, 40f); p.close()
    },
    // Greenland
    { p, m ->
        p.moveTo(m(140f, 9f)); p.cubic(m, 157f, 4f, 170f, 6f, 170f, 20f)
        p.cubic(m, 170f, 34f, 160f, 40f, 143f, 40f)
        p.cubic(m, 129f, 40f, 123f, 29f, 140f, 9f); p.close()
    },
    // South America
    { p, m ->
        p.moveTo(m(102f, 115f)); p.cubic(m, 127f, 110f, 157f, 114f, 160f, 133f)
        p.cubic(m, 164f, 151f, 160f, 174f, 149f, 193f)
        p.cubic(m, 136f, 215f, 116f, 220f, 101f, 210f)
        p.cubic(m, 88f, 202f, 82f, 186f, 85f, 165f)
        p.cubic(m, 88f, 145f, 88f, 121f, 102f, 115f); p.close()
    },
    // Europe
    { p, m ->
        p.moveTo(m(184f, 23f)); p.cubic(m, 197f, 19f, 228f, 21f, 251f, 34f)
        p.cubic(m, 261f, 42f, 258f, 57f, 244f, 67f)
        p.cubic(m, 230f, 77f, 212f, 80f, 199f, 74f)
        p.cubic(m, 186f, 70f, 179f, 57f, 184f, 23f); p.close()
    },
    // Africa
    { p, m ->
        p.moveTo(m(179f, 74f)); p.cubic(m, 202f, 70f, 250f, 74f, 264f, 91f)
        p.cubic(m, 274f, 108f, 270f, 148f, 257f, 179f)
        p.cubic(m, 242f, 200f, 221f, 205f, 200f, 197f)
        p.cubic(m, 180f, 190f, 168f, 162f, 168f, 135f)
        p.cubic(m, 168f, 108f, 165f, 80f, 179f, 74f); p.close()
    },
    // Asia main
    { p, m ->
        p.moveTo(m(250f, 27f)); p.cubic(m, 288f, 20f, 343f, 17f, 385f, 27f)
        p.cubic(m, 397f, 43f, 390f, 73f, 374f, 95f)
        p.cubic(m, 357f, 118f, 322f, 125f, 287f, 121f)
        p.cubic(m, 260f, 118f, 244f, 98f, 250f, 74f)
        p.cubic(m, 254f, 57f, 244f, 34f, 250f, 27f); p.close()
    },
    // Indian subcontinent
    { p, m ->
        p.moveTo(m(284f, 95f)); p.cubic(m, 300f, 91f, 317f, 93f, 322f, 108f)
        p.cubic(m, 327f, 121f, 320f, 134f, 307f, 142f)
        p.cubic(m, 292f, 152f, 277f, 149f, 271f, 139f)
        p.cubic(m, 264f, 128f, 269f, 110f, 284f, 95f); p.close()
    },
    // Southeast Asia
    { p, m ->
        p.moveTo(m(310f, 118f)); p.cubic(m, 330f, 113f, 354f, 121f, 367f, 136f)
        p.cubic(m, 374f, 149f, 364f, 162f, 347f, 166f)
        p.cubic(m, 330f, 172f, 310f, 159f, 304f, 146f)
        p.cubic(m, 297f, 132f, 300f, 124f, 310f, 118f); p.close()
    },
    // Japan
    { p, m ->
        p.moveTo(m(348f, 60f)); p.cubic(m, 358f, 53f, 370f, 57f, 373f, 70f)
        p.cubic(m, 376f, 83f, 366f, 92f, 355f, 93f)
        p.cubic(m, 343f, 95f, 337f, 86f, 340f, 74f)
        p.cubic(m, 343f, 64f, 345f, 64f, 348f, 60f); p.close()
    },
    // Australia
    { p, m ->
        p.moveTo(m(320f, 152f)); p.cubic(m, 344f, 142f, 370f, 144f, 380f, 162f)
        p.cubic(m, 390f, 180f, 384f, 202f, 366f, 210f)
        p.cubic(m, 348f, 218f, 320f, 213f, 308f, 196f)
        p.cubic(m, 297f, 180f, 302f, 162f, 320f, 152f); p.close()
    },
)

// Helper: cubic Bézier through three control/anchor points in viewBox coords.
private fun Path.cubic(
    m: (Float, Float) -> Offset,
    x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float,
) {
    val a = m(x1, y1); val b = m(x2, y2); val c = m(x3, y3)
    cubicTo(a.x, a.y, b.x, b.y, c.x, c.y)
}

private fun Path.moveTo(o: Offset) = moveTo(o.x, o.y)
