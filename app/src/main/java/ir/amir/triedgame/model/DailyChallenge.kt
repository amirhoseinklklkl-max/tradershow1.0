package ir.amir.triedgame.model

/** Tracks today's raw counters used to evaluate challenge progress. Reset every new day. */
data class ChallengeRecord(
    val tradesOpened: Int = 0,
    val profitableCloses: Int = 0,
    val fictionalTraded: Boolean = false,
    val hungerItemBought: Boolean = false,
    val iapUsdToday: Double = 0.0,
    val claimedIds: Set<String> = emptySet()
)

data class DailyChallenge(
    val id: String,
    val title: String,
    val target: Int,
    val rewardToman: Double,
    val progressFrom: (ChallengeRecord, LifeStats) -> Int
)

/** The fixed daily challenge lineup (trading + زندگی من). */
object DailyChallengeCatalog {
    fun today(): List<DailyChallenge> = listOf(
        DailyChallenge(
            id = "trades_3",
            title = "۳ معامله امروز باز کن",
            target = 3,
            rewardToman = 150_000.0,
            progressFrom = { record, _ -> record.tradesOpened }
        ),
        DailyChallenge(
            id = "profitable_close_1",
            title = "یک پوزیشن رو با سود ببند",
            target = 1,
            rewardToman = 200_000.0,
            progressFrom = { record, _ -> record.profitableCloses }
        ),
        DailyChallenge(
            id = "fictional_trade",
            title = "روی یکی از ارزهای ساختگی بازی (TSC/MEME/GLXY/VLT) معامله کن",
            target = 1,
            rewardToman = 100_000.0,
            progressFrom = { record, _ -> if (record.fictionalTraded) 1 else 0 }
        ),
        DailyChallenge(
            id = "energy_above_50",
            title = "انرژیت رو بالای ۵۰٪ نگه دار",
            target = 1,
            rewardToman = 60_000.0,
            progressFrom = { _, life -> if (life.energy >= 50) 1 else 0 }
        ),
        DailyChallenge(
            id = "eat_something",
            title = "یک وعده غذایی یا میان‌وعده بخر",
            target = 1,
            rewardToman = 50_000.0,
            progressFrom = { record, _ -> if (record.hungerItemBought) 1 else 0 }
        ),
        DailyChallenge(
            id = "charge_25usd",
            title = "حسابت رو با خرید بسته حداقل ۲۵ دلار شارژ کن",
            target = 25,
            rewardToman = 100_000.0,
            progressFrom = { record, _ -> record.iapUsdToday.toInt() }
        )
    )
}

/** XP required cumulatively to reach a given level. Level 1 starts at 0 XP;
 * each level needs progressively more (simple linear-growth curve: level*500). */
fun xpToReachLevel(level: Int): Int {
    if (level <= 1) return 0
    return (level - 1) * 500
}
