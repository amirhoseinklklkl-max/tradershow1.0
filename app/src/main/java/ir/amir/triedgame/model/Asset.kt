package ir.amir.triedgame.model

/**
 * Category of a tradable asset, used for grouping in the market list UI.
 */
enum class AssetCategory {
    CRYPTO, FOREX, COMMODITY, FICTIONAL
}

/**
 * Defines how an asset's simulated price behaves over time.
 *
 * @param dailyVolatility approximate standard deviation of price change per simulated day,
 *   expressed as a fraction of price (e.g. 0.03 = ~3% typical daily move).
 * @param trendBias a small constant drift added every step; positive values create a
 *   slow upward bias (e.g. GLXY), negative a downward one, zero is neutral.
 * @param spikeChancePerHour probability that, in any given simulated hour, a sudden
 *   larger move ("spike") is injected on top of the normal random walk.
 * @param spikeMagnitude how large a spike is, as a fraction of price.
 */
data class VolatilityProfile(
    val dailyVolatility: Double,
    val trendBias: Double = 0.0,
    val spikeChancePerHour: Double = 0.0,
    val spikeMagnitude: Double = 0.0
)

data class Asset(
    val symbol: String,
    val displayName: String,
    val category: AssetCategory,
    val startingPriceUsd: Double,
    val volatility: VolatilityProfile
)

/**
 * The full market list available in TraderShow: well-known real assets plus
 * a handful of in-game fictional ones for identity/variety.
 */
object AssetCatalog {
    val all: List<Asset> = listOf(
        // Crypto
        Asset("BTC", "بیت‌کوین", AssetCategory.CRYPTO, 65000.0, VolatilityProfile(0.035, spikeChancePerHour = 0.02, spikeMagnitude = 0.05)),
        Asset("ETH", "اتریوم", AssetCategory.CRYPTO, 3400.0, VolatilityProfile(0.04, spikeChancePerHour = 0.02, spikeMagnitude = 0.05)),
        Asset("BNB", "بایننس کوین", AssetCategory.CRYPTO, 580.0, VolatilityProfile(0.035)),
        Asset("SOL", "سولانا", AssetCategory.CRYPTO, 150.0, VolatilityProfile(0.055, spikeChancePerHour = 0.03, spikeMagnitude = 0.06)),
        Asset("XRP", "ریپل", AssetCategory.CRYPTO, 0.55, VolatilityProfile(0.045)),
        Asset("DOGE", "دوج‌کوین", AssetCategory.CRYPTO, 0.12, VolatilityProfile(0.06, spikeChancePerHour = 0.04, spikeMagnitude = 0.08)),

        // Forex
        Asset("EURUSD", "یورو/دلار", AssetCategory.FOREX, 1.08, VolatilityProfile(0.006)),
        Asset("GBPUSD", "پوند/دلار", AssetCategory.FOREX, 1.27, VolatilityProfile(0.007)),
        Asset("USDJPY", "دلار/ین", AssetCategory.FOREX, 149.0, VolatilityProfile(0.006)),

        // Commodities
        Asset("XAU", "طلا", AssetCategory.COMMODITY, 2350.0, VolatilityProfile(0.01)),
        Asset("XAG", "نقره", AssetCategory.COMMODITY, 28.0, VolatilityProfile(0.018)),
        Asset("OIL", "نفت خام", AssetCategory.COMMODITY, 78.0, VolatilityProfile(0.02)),

        // Fictional in-game assets
        Asset("TSC", "TraderShow Coin", AssetCategory.FICTIONAL, 4.5, VolatilityProfile(0.03, trendBias = 0.0005)),
        Asset("MEME", "ShowMeme", AssetCategory.FICTIONAL, 0.02, VolatilityProfile(0.09, spikeChancePerHour = 0.08, spikeMagnitude = 0.15)),
        Asset("GLXY", "Galaxy Token", AssetCategory.FICTIONAL, 12.0, VolatilityProfile(0.015, trendBias = 0.0008)),
        Asset("VLT", "VoltCoin", AssetCategory.FICTIONAL, 7.0, VolatilityProfile(0.05, spikeChancePerHour = 0.05, spikeMagnitude = 0.1))
    )

    fun bySymbol(symbol: String): Asset? = all.find { it.symbol == symbol }
}
