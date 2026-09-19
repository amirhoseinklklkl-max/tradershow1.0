package ir.amir.triedgame.ui.components

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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import ir.amir.triedgame.model.Candle
import ir.amir.triedgame.ui.theme.TsBackground
import ir.amir.triedgame.ui.theme.TsGreen
import ir.amir.triedgame.ui.theme.TsRed
import kotlin.math.abs

private const val MIN_VISIBLE = 12
private const val DEFAULT_VISIBLE = 60

/**
 * Candlestick chart with pinch-to-zoom and horizontal drag-to-pan, similar
 * to a real trading app. Drawn on a plain Compose Canvas -- no external
 * chart library.
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
    modifier: Modifier = Modifier
) {
    var visibleCount by remember { mutableIntStateOf(DEFAULT_VISIBLE) }
    // Fractional scroll offset from the right edge (0 = pinned to latest candle).
    var scrollFromEnd by remember { mutableFloatStateOf(0f) }

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
                            scrollFromEnd = (scrollFromEnd - deltaCandles).coerceIn(0f, maxScroll)
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
            val visible = candles.subList(startIndex, (startIndex + count).coerceAtMost(candles.size))
            if (visible.isEmpty()) return@Canvas

            val maxPrice = visible.maxOf { it.high }
            val minPrice = visible.minOf { it.low }
            val priceRange = (maxPrice - minPrice).let { if (it <= 0.0) 1.0 else it }

            val candleSlotWidth = size.width / visible.size
            val bodyWidth = candleSlotWidth * 0.62f
            val wickWidth = (candleSlotWidth * 0.1f).coerceAtLeast(2f)

            fun yFor(price: Double): Float {
                val fraction = (price - minPrice) / priceRange
                return (size.height * (1.0 - fraction)).toFloat()
            }

            visible.forEachIndexed { index, candle ->
                val centerX = candleSlotWidth * index + candleSlotWidth / 2f
                val isBullish = candle.close >= candle.open
                val color = if (isBullish) TsGreen else TsRed

                val highY = yFor(candle.high)
                val lowY = yFor(candle.low)
                val openY = yFor(candle.open)
                val closeY = yFor(candle.close)

                drawLine(
                    color = color,
                    start = Offset(centerX, highY),
                    end = Offset(centerX, lowY),
                    strokeWidth = wickWidth
                )

                val bodyTop = minOf(openY, closeY)
                val bodyBottom = maxOf(openY, closeY).coerceAtLeast(bodyTop + 2f)
                drawRect(
                    color = color,
                    topLeft = Offset(centerX - bodyWidth / 2f, bodyTop),
                    size = androidx.compose.ui.geometry.Size(bodyWidth, bodyBottom - bodyTop)
                )
            }
        }
    }
}
