package ir.amir.triedgame.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import ir.amir.triedgame.model.NetWorthPoint
import ir.amir.triedgame.ui.theme.TsAccent
import ir.amir.triedgame.ui.theme.TsTextSecondary

/** Plain non-interactive line chart -- no gestures, safe to embed inside a
 * scrollable screen (unlike the candlestick chart, which owns pan/zoom). */
@Composable
fun NetWorthChart(points: List<NetWorthPoint>, modifier: Modifier = Modifier) {
    if (points.size < 2) {
        Text(
            "با معامله کردن، روند دارایی‌ت اینجا شکل می‌گیره",
            style = MaterialTheme.typography.bodySmall,
            color = TsTextSecondary,
            modifier = modifier
        )
        return
    }

    Canvas(modifier = modifier) {
        val minV = points.minOf { it.totalUsd }
        val maxV = points.maxOf { it.totalUsd }
        val range = (maxV - minV).let { if (it <= 0.0) 1.0 else it }
        val stepX = size.width / (points.size - 1).toFloat()

        val path = androidx.compose.ui.graphics.Path()
        points.forEachIndexed { index, point ->
            val x = stepX * index
            val fraction = (point.totalUsd - minV) / range
            val y = size.height * (1f - fraction.toFloat())
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = TsAccent, style = Stroke(width = 4f))

        // Endpoint dot
        val lastFraction = (points.last().totalUsd - minV) / range
        drawCircle(
            color = TsAccent,
            radius = 6f,
            center = Offset(size.width, size.height * (1f - lastFraction.toFloat()))
        )
    }
}
