package ir.amir.triedgame.model

/** Fixed USD -> Toman conversion rate used throughout the game's economy. */
const val USD_TO_TOMAN_RATE = 220_000.0

data class Wallet(
    val usdBalance: Double = 0.0,
    val tomanBalance: Double = 0.0
) {
    fun convertUsdToToman(amountUsd: Double): Wallet {
        val usable = amountUsd.coerceAtMost(usdBalance)
        return copy(
            usdBalance = usdBalance - usable,
            tomanBalance = tomanBalance + usable * USD_TO_TOMAN_RATE
        )
    }

    fun convertTomanToUsd(amountToman: Double): Wallet {
        val usable = amountToman.coerceAtMost(tomanBalance)
        return copy(
            tomanBalance = tomanBalance - usable,
            usdBalance = usdBalance + usable / USD_TO_TOMAN_RATE
        )
    }

    fun addUsd(amount: Double): Wallet = copy(usdBalance = usdBalance + amount)
    fun addToman(amount: Double): Wallet = copy(tomanBalance = tomanBalance + amount)
    fun spendToman(amount: Double): Wallet? =
        if (tomanBalance >= amount) copy(tomanBalance = tomanBalance - amount) else null
}

enum class PositionSide { LONG, SHORT }

/**
 * A trading position. [marginUsd] is the amount actually held from the
 * wallet; [leverage] (1x = spot-style, >1x = futures-style) multiplies both
 * the exposure and the P&L. A position is liquidated (forced-closed at a
 * total loss of the margin) if losses reach 100% of the margin -- exactly
 * like a real futures exchange. [stopLossPrice]/[takeProfitPrice] are
 * optional; when the current price crosses either, the position auto-closes.
 */
data class Position(
    val id: String,
    val assetSymbol: String,
    val side: PositionSide,
    val entryPrice: Double,
    val marginUsd: Double,
    val leverage: Int = 1,
    val openedAtMillis: Long,
    val stopLossPrice: Double? = null,
    val takeProfitPrice: Double? = null
) {
    val notionalUsd: Double get() = marginUsd * leverage

    fun currentPnlUsd(currentPrice: Double): Double {
        val change = (currentPrice - entryPrice) / entryPrice
        val signedChange = if (side == PositionSide.LONG) change else -change
        return notionalUsd * signedChange
    }

    /** True once losses have wiped out the full margin -- the exchange would force-close here. */
    fun isLiquidated(currentPrice: Double): Boolean = currentPnlUsd(currentPrice) <= -marginUsd

    /** True once the current price has crossed the stop-loss level, if set. */
    fun hitStopLoss(currentPrice: Double): Boolean {
        val sl = stopLossPrice ?: return false
        return if (side == PositionSide.LONG) currentPrice <= sl else currentPrice >= sl
    }

    /** True once the current price has crossed the take-profit level, if set. */
    fun hitTakeProfit(currentPrice: Double): Boolean {
        val tp = takeProfitPrice ?: return false
        return if (side == PositionSide.LONG) currentPrice >= tp else currentPrice <= tp
    }

    /** What the user gets back if they close now: margin + P&L, floored at 0. */
    fun currentValueUsd(currentPrice: Double): Double =
        (marginUsd + currentPnlUsd(currentPrice)).coerceAtLeast(0.0)

    fun pnlPercentOfMargin(currentPrice: Double): Double =
        if (marginUsd <= 0.0) 0.0 else (currentPnlUsd(currentPrice) / marginUsd) * 100.0
}

enum class CloseReason { MANUAL, LIQUIDATED, STOP_LOSS, TAKE_PROFIT }

/** A record of a position after it closed, kept for the trade-history screen. */
data class ClosedTrade(
    val id: String,
    val assetSymbol: String,
    val side: PositionSide,
    val entryPrice: Double,
    val exitPrice: Double,
    val marginUsd: Double,
    val leverage: Int,
    val pnlUsd: Double,
    val reason: CloseReason,
    val openedAtMillis: Long,
    val closedAtMillis: Long
)

/** One sample point for the net-worth-over-time chart. */
data class NetWorthPoint(val timestampMillis: Long, val totalUsd: Double)

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val isUnlockedFrom: (AchievementStats) -> Boolean
)

/** Lifetime counters used to evaluate achievements. */
data class AchievementStats(
    val totalTrades: Int = 0,
    val profitableTrades: Int = 0,
    val liquidations: Int = 0,
    val maxNetWorthUsd: Double = 0.0,
    val daysPlayed: Set<String> = emptySet()
)

object AchievementCatalog {
    val all: List<Achievement> = listOf(
        Achievement("first_trade", "اولین قدم", "اولین معامله‌ات رو باز کن") { it.totalTrades >= 1 },
        Achievement("ten_trades", "معامله‌گر فعال", "۱۰ معامله باز کن") { it.totalTrades >= 10 },
        Achievement("fifty_trades", "حرفه‌ای بازار", "۵۰ معامله باز کن") { it.totalTrades >= 50 },
        Achievement("first_profit", "اولین سود", "یک معامله رو با سود ببند") { it.profitableTrades >= 1 },
        Achievement("ten_profits", "دست طلایی", "۱۰ معامله‌ی سودده داشته باش") { it.profitableTrades >= 10 },
        Achievement("survived_liquidation", "درس عبرت", "یک‌بار لیکویید شو و ادامه بده") { it.liquidations >= 1 },
        Achievement("net_worth_1k", "سرمایه‌دار کوچک", "به ۱٬۰۰۰ دلار دارایی برس") { it.maxNetWorthUsd >= 1000 },
        Achievement("net_worth_10k", "نهنگ در حال ظهور", "به ۱۰٬۰۰۰ دلار دارایی برس") { it.maxNetWorthUsd >= 10000 },
        Achievement("five_days", "بازیکن پیگیر", "۵ روز مختلف بازی کن") { it.daysPlayed.size >= 5 }
    )
}

data class LifeStats(
    val health: Int = 100,
    val hunger: Int = 100,
    val energy: Int = 100
) {
    fun clamp() = copy(
        health = health.coerceIn(0, 100),
        hunger = hunger.coerceIn(0, 100),
        energy = energy.coerceIn(0, 100)
    )
}

data class UserProfile(
    val firstName: String,
    val lastName: String,
    val level: Int = 1,
    val xp: Int = 0,
    val randomSeed: Long,
    val accountCreatedAtMillis: Long
)
