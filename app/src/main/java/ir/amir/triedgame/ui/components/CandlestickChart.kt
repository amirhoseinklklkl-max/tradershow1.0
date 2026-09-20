package ir.amir.triedgame.ui.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import ir.amir.triedgame.model.Candle
import ir.amir.triedgame.ui.theme.TsAccent
import ir.amir.triedgame.ui.theme.TsBackground
import ir.amir.triedgame.ui.theme.TsGreen
import ir.amir.triedgame.ui.theme.TsRed
import ir.amir.triedgame.ui.theme.TsTextSecondary
import kotlin.math.abs
import kotlin.math.sqrt

private const val MIN_VISIBLE = 12
private const val DEFAULT_VISIBLE = 60
private const val PRICE_LABEL_COUNT = 8
private const val BOLLINGER_PERIOD = 20
private const val BOLLINGER_STD_MULTIPLIER = 2.0

private data class BollingerPoint(val middle: Double, val upper: Double, val lower: Double)

/** Standard Bollinger Bands: a [BOLLINGER_PERIOD]-candle simple moving
 * average of closes, with upper/lower bands at +/- [BOLLINGER_STD_MULTIPLIER]
 * standard deviations of those same closes. Returns one entry per candle in
 * [candles] (null where there isn't yet enough history). */
private fun computeBollinger(candles: List<Candle>): List<BollingerPoint?> {
    val result = arrayOfNulls<BollingerPoint>(candles.size)
    for (i in candles.indices) {
        if (i + 1 < BOLLINGER_PERIOD) continue
        val window = candles.subList(i + 1 - BOLLINGER_PERIOD, i + 1).map { it.close }
        val mean = window.average()
        val variance = window.sumOf { (it - mean) * (it - mean) } / window.size
        val stdDev = sqrt(variance)
        result[i] = BollingerPoint(mean, mean + BOLLINGER_STD_MULTIPLIER * stdDev, mean - BOLLINGER_STD_MULTIPLIER * stdDev)
    }
    return result.toList()
}

/**
 * Candlestick chart with pinch-to-zoom and horizontal drag-to-pan, similar
 * to a real trading app. Drawn on a plain Compose Canvas -- no external
 * chart library. Reserves a price-axis gutter on the right (matching where
 * the newest candle sits), shows a live current-price line + tag on that
 * axis, and pads the plot area so wicks and the first/last candle never
 * touch the chart's edges, like TradingView. [showBollingerBands] overlays
 * the standard Bollinger Bands indicator (hidden behind a feature flag the
 * caller controls).
 *
 * IMPORTANT: this chart is meant to sit inside a vertically-scrollable
 * screen. A naive gesture handler that consumes every touch would fight the
 * parent's vertical scroll and make the whole screen feel stuck. Instead,
 * this only takes over the gesture for: (a) any pinch (2+ fingers), or
 * (b) a single-finger drag that is clearly more horizontal than vertical.
 * A vertical single-finger drag is left untouched so it passes straight
 * through to the enclosing scroll.
 */
@Composable
fun CandlestickChart(
    candles: List<Candle>,
    currentPrice: Double? = null,
    showBollingerBands: Boolean = false,
    modifier: Modifier = Modifier
) {
    var visibleCount by remember { mutableIntStateOf(DEFAULT_VISIBLE) }
    // Fractional scroll offset from the right edge (0 = pinned to latest candle).
    var scrollFromEnd by remember { mutableFloatStateOf(0f) }
    val labelColor = TsTextSecondary.toArgb()
    val gridColor = TsTextSecondary.copy(alpha = 0.15f)
    val accentColor = TsAccent
    val bandColor = TsTextSecondary.copy(alpha = 0.7f)

    Box(
        modifier = modifier
            .background(TsBackground)
            .pointerInput(candles.size) {
                awaitEachGesture {
                    var decided = false
                    var chartOwnsGesture = false
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val zoom = event.calculateZoom()
                        val pan = event.calculatePan()
                        val isPinch = event.changes.size > 1

                        if (!decided) {
                            val movedEnough = abs(pan.x) > 4f || abs(pan.y) > 4f || isPinch
                            if (movedEnough) {
                                decided = true
                                chartOwnsGesture = isPinch || zoom != 1f || abs(pan.x) > abs(pan.y) * 1.3f
                            }
                        }

                        if (chartOwnsGesture) {
                            val maxVisible = candles.size.coerceAtLeast(MIN_VISIBLE)
                            if (zoom != 1f) {
                                visibleCount = (visibleCount / zoom).toInt().coerceIn(MIN_VISIBLE, maxVisible)
                            }
                            val candleWidthPx = (size.width / visibleCount.toFloat()).coerceAtLeast(1f)
                            val deltaCandles = pan.x / candleWidthPx
                            val maxScroll = (candles.size - visibleCount).coerceAtLeast(0).toFloat()
                            // Dragging right reveals older candles (content follows the
                            // finger, like a normal scroll) -- dragging left goes back
                            // toward the latest candle.
                            scrollFromEnd = (scrollFromEnd + deltaCandles).coerceIn(0f, maxScroll)
                            event.changes.forEach { it.consume() }
                        }
                        // else: leave changes unconsumed so the parent scroll container handles them.

                        if (event.changes.all { !it.pressed }) break
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (candles.isEmpty()) return@Canvas

            val count = visibleCount.coerceAtMost(candles.size)
            val maxScroll = (candles.size - count).coerceAtLeast(0)
            val startIndex = (candles.size - count - scrollFromEnd.toInt()).coerceIn(0, maxScroll)
            val endIndexExclusive = (startIndex + count).coerceAtMost(candles.size)
            val visible = candles.subList(startIndex, endIndexExclusive)
            if (visible.isEmpty()) return@Canvas

            val bollinger = if (showBollingerBands) computeBollinger(candles) else emptyList()

            var rawMax = visible.maxOf { it.high }
            var rawMin = visible.minOf { it.low }
            if (currentPrice != null) {
                rawMax = maxOf(rawMax, currentPrice)
                rawMin = minOf(rawMin, currentPrice)
            }
            val rawRange = (rawMax - rawMin).let { if (it <= 0.0) rawMax.coerceAtLeast(1.0) * 0.01 else it }
            // Pad the price range top/bottom by 10% each so the highest wick
            // and lowest wick never touch the very edge of the chart.
            val minPrice = rawMin - rawRange * 0.1
            val maxPrice = rawMax + rawRange * 0.1
            val priceRange = (maxPrice - minPrice).let { if (it <= 0.0) 1.0 else it }

            val axisWidthPx = 68.dp.toPx()
            val plotWidth = (size.width - axisWidthPx).coerceAtLeast(1f)
            val horizontalInset = (plotWidth / visible.size.toFloat()) * 0.5f
            val usableWidth = (plotWidth - horizontalInset * 2f).coerceAtLeast(1f)

            val candleSlotWidth = usableWidth / visible.size
            val bodyWidth = candleSlotWidth * 0.62f
            val wickWidth = (candleSlotWidth * 0.12f).coerceAtLeast(2f)

            fun yFor(price: Double): Float {
                val fraction = (price - minPrice) / priceRange
                return (size.height * (1.0 - fraction)).toFloat()
            }

            fun centerXFor(indexInVisible: Int): Float =
                horizontalInset + candleSlotWidth * indexInVisible + candleSlotWidth / 2f

            // Price axis: gridlines across the plot + labels in the right gutter.
            val labelPaint = Paint().apply {
                color = labelColor
                textSize = 11.dp.toPx()
                isAntiAlias = true
            }
            for (i in 0 until PRICE_LABEL_COUNT) {
                val price = minPrice + priceRange * (i.toDouble() / (PRICE_LABEL_COUNT - 1))
                val y = yFor(price)
                drawLine(color = gridColor, start = Offset(0f, y), end = Offset(plotWidth, y), strokeWidth = 1f)
                val decimals = if (price < 1) 4 else 2
                drawContext.canvas.nativeCanvas.drawText(
                    String.format("%.${decimals}f", price),
                    plotWidth + 6.dp.toPx(),
                    y + 4.dp.toPx(),
                    labelPaint
                )
            }

            // Bollinger Bands overlay (hidden feature).
            if (showBollingerBands) {
                val middlePath = androidx.compose.ui.graphics.Path()
                val upperPath = androidx.compose.ui.graphics.Path()
                val lowerPath = androidx.compose.ui.graphics.Path()
                var started = false
                visible.forEachIndexed { i, _ ->
                    val point = bollinger.getOrNull(startIndex + i) ?: return@forEachIndexed
                    val x = centerXFor(i)
                    if (!started) {
                        middlePath.moveTo(x, yFor(point.middle))
                        upperPath.moveTo(x, yFor(point.upper))
                        lowerPath.moveTo(x, yFor(point.lower))
                        started = true
                    } else {
                        middlePath.lineTo(x, yFor(point.middle))
                        upperPath.lineTo(x, yFor(point.upper))
                        lowerPath.lineTo(x, yFor(point.lower))
                    }
                }
                if (started) {
                    drawPath(upperPath, color = bandColor, style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))))
                    drawPath(lowerPath, color = bandColor, style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))))
                    drawPath(middlePath, color = accentColor.copy(alpha = 0.8f), style = Stroke(width = 2f))
                }
            }

            visible.forEachIndexed { index, candle ->
                val centerX = centerXFor(index)
                val isBullish = candle.close >= candle.open
                val color = if (isBullish) TsGreen else TsRed

                val highY = yFor(candle.high)
                val lowY = yFor(candle.low)
                val openY = yFor(candle.open)
                val closeY = yFor(candle.close)

                drawLine(color = color, start = Offset(centerX, highY), end = Offset(centerX, lowY), strokeWidth = wickWidth)

                val bodyTop = minOf(openY, closeY)
                val bodyBottom = maxOf(openY, closeY).coerceAtLeast(bodyTop + 2f)
                drawRect(
                    color = color,
                    topLeft = Offset(centerX - bodyWidth / 2f, bodyTop),
                    size = androidx.compose.ui.geometry.Size(bodyWidth, bodyBottom - bodyTop)
                )
            }

            // Live current-price line + tag on the right axis, TradingView-style.
            if (currentPrice != null) {
                val y = yFor(currentPrice)
                drawLine(
                    color = accentColor.copy(alpha = 0.7f),
                    start = Offset(0f, y),
                    end = Offset(plotWidth, y),
                    strokeWidth = 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                )
                val decimals = if (currentPrice < 1) 4 else 2
                val tagText = String.format("%.${decimals}f", currentPrice)
                val tagPaint = Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 11.dp.toPx()
                    isAntiAlias = true
                }
                val tagWidth = tagPaint.measureText(tagText) + 12.dp.toPx()
                drawRect(
                    color = accentColor,
                    topLeft = Offset(plotWidth, y - 10.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(tagWidth.coerceAtMost(axisWidthPx), 20.dp.toPx())
                )
                drawContext.canvas.nativeCanvas.drawText(tagText, plotWidth + 6.dp.toPx(), y + 4.dp.toPx(), tagPaint)
            }
        }
    }
}
