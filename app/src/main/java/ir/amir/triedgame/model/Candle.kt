package ir.amir.triedgame.model

/**
 * A single OHLC candle. [timestampMillis] marks the candle's open time in real
 * device time (System.currentTimeMillis()), so candles remain meaningful even
 * after the app has been closed and reopened.
 */
data class Candle(
    val timestampMillis: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double
)
