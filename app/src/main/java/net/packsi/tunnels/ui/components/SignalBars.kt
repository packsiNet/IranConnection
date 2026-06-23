package net.packsi.tunnels.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import net.packsi.tunnels.ui.theme.AppColors

/** 3-bar signal strength, teal for active bars, grey for the rest. */
@Composable
fun SignalBars(level: Int, modifier: Modifier = Modifier.size(14.dp, 11.dp)) {
    Canvas(modifier) {
        val w = size.width / 14f
        val h = size.height / 11f
        val on = AppColors.Teal
        val off = Color(0xFFD1D9E6)
        fun bar(x: Float, y: Float, bh: Float, active: Boolean) {
            drawRoundRect(
                color = if (active) on else off,
                topLeft = Offset(x * w, y * h),
                size = Size(3 * w, bh * h),
                cornerRadius = CornerRadius(0.6f * w, 0.6f * w),
            )
        }
        bar(0f, 6f, 5f, level >= 1)
        bar(4.5f, 3.5f, 7.5f, level >= 2)
        bar(9f, 0f, 11f, level >= 3)
    }
}
