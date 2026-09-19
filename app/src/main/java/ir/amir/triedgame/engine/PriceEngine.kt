package ir.amir.triedgame.engine

import ir.amir.triedgame.model.Asset
import ir.amir.triedgame.model.Candle
import java.util.Random
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sqrt

/**
 * Deterministic, seed-based price simulator.
 *
 * The core idea: price at any timestamp is computed directly from a fixed
 * reference point (the moment the user's save/account was created) using a
 * scaled-Brownian-motion style formula, driven by a hash-based pseudo-random
 * generator keyed on (asset symbol, user seed, time bucket). Because it's a
 * pure function of time rather than an iterative step-by-step walk, it can
 * answer "what was/is the price at time T" instantly for any T -- including
 * while the app was closed -- without replaying every tick in between. This
 * also means re-querying the same moment (e.g. scrolling the chart back)
 * always returns the exact same value.
 */
object PriceEngine {

    const val MINUTE_MS = 60_000L
    const val HOUR_MS = 60 * MINUTE_MS
    const val DAY_MS = 24 * HOUR_MS

    private fun mix(vararg parts: Long): Long {
        var h = -0x61c8864680b583ebL // golden-ratio constant, as a Long
        for (p in parts) {
            h = h xor p
            h *= -0x395b586ca42e166bL
            h = h xor (h ushr 29)
        }
        return h
    }

    private fun seededRandom(assetSymbol: String, seed: Long, bucket: Long, salt: Long): Random {
        val combined = mix(assetSymbol.hashCode().toLong(), seed, bucket, salt)
        return Random(combined)
    }

    private fun gaussian(assetSymbol: String, seed: Long, bucket: Long, salt: Long): Double =
        seededRandom(assetSymbol, seed, bucket, salt).nextGaussian()

    private fun uniform(assetSymbol: String, seed: Long, bucket: Long, salt: Long): Double =
        seededRandom(assetSymbol, seed, bucket, salt).nextDouble()

    /**
     * Price of [asset] at [timestampMillis], anchored to [referenceTimestampMillis]
     * where the price was exactly [referencePrice]. [seed] is a per-user random
     * seed (e.g. derived from the user's profile) so different players see
     * different -- but each individually stable -- price histories.
     */
    fun priceAt(
        asset: Asset,
        seed: Long,
        timestampMillis: Long,
        referenceTimestampMillis: Long,
        referencePrice: Double
    ): Double {
        val vol = asset.volatility
        val elapsedMinutes = (timestampMillis - referenceTimestampMillis) / MINUTE_MS.toDouble()
        val minuteIndex = Math.floorDiv(timestampMillis, MINUTE_MS)
        val z = gaussian(asset.symbol, seed, minuteIndex, salt = 1L)

        // Scale the asset's daily volatility down to a per-minute figure, then
        // apply it Brownian-motion style: sigma * sqrt(elapsed time).
        val perMinuteVol = vol.dailyVolatility / sqrt(1440.0)
        val drift = vol.trendBias * elapsedMinutes
        val diffusion = perMinuteVol * sqrt(abs(elapsedMinutes)) * z
        var price = referencePrice * exp(drift + diffusion)

        // Overlay occasional larger spikes, rolled once per simulated hour.
        if (vol.spikeChancePerHour > 0.0) {
            val hourIndex = Math.floorDiv(timestampMillis, HOUR_MS)
            val roll = uniform(asset.symbol, seed, hourIndex, salt = 2L)
            if (roll < vol.spikeChancePerHour) {
                val spikeZ = gaussian(asset.symbol, seed, hourIndex, salt = 3L)
                price *= exp(vol.spikeMagnitude * spikeZ)
            }
        }

        return price.coerceAtLeast(referencePrice * 0.0001)
    }

    /**
     * Generates OHLC candles covering [fromMillis, toMillis) at a fixed
     * [intervalMillis] step. Open/close come from the boundary prices; a
     * midpoint sample is used to give high/low a touch of intra-candle range.
     */
    fun generateCandles(
        asset: Asset,
        seed: Long,
        fromMillis: Long,
        toMillis: Long,
        intervalMillis: Long,
        referenceTimestampMillis: Long,
        referencePrice: Double
    ): List<Candle> {
        if (toMillis <= fromMillis) return emptyList()
        val candles = mutableListOf<Candle>()
        var t = fromMillis - Math.floorMod(fromMillis, intervalMillis)
        while (t < toMillis) {
            val open = priceAt(asset, seed, t, referenceTimestampMillis, referencePrice)
            val close = priceAt(asset, seed, t + intervalMillis, referenceTimestampMillis, referencePrice)
            val mid = priceAt(asset, seed, t + intervalMillis / 2, referenceTimestampMillis, referencePrice)
            val high = maxOf(open, close, mid)
            val low = minOf(open, close, mid)
            candles.add(Candle(t, open, high, low, close))
            t += intervalMillis
        }
        return candles
    }

    /**
     * Backfills candle history for a gap the user was away (app closed / phone
     * off). Anything older than 24h is generated at hourly resolution (to keep
     * the candle count sane for long gaps); the most recent 24h is generated
     * at 1-minute resolution so a returning user can still zoom into recent
     * detail and analyze what happened while they were away.
     */
    fun backfillCandles(
        asset: Asset,
        seed: Long,
        lastSeenMillis: Long,
        nowMillis: Long,
        referenceTimestampMillis: Long,
        referencePrice: Double
    ): List<Candle> {
        if (nowMillis <= lastSeenMillis) return emptyList()
        val recentWindowStart = maxOf(lastSeenMillis, nowMillis - DAY_MS)
        val result = mutableListOf<Candle>()
        if (lastSeenMillis < recentWindowStart) {
            result += generateCandles(
                asset, seed, lastSeenMillis, recentWindowStart, HOUR_MS,
                referenceTimestampMillis, referencePrice
            )
        }
        result += generateCandles(
            asset, seed, recentWindowStart, nowMillis, MINUTE_MS,
            referenceTimestampMillis, referencePrice
        )
        return result
    }
}
