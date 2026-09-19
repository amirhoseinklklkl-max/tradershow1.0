package ir.amir.triedgame.data

import android.content.Context
import android.content.SharedPreferences
import ir.amir.triedgame.model.AchievementStats
import ir.amir.triedgame.model.ChallengeRecord
import ir.amir.triedgame.model.ClosedTrade
import ir.amir.triedgame.model.CloseReason
import ir.amir.triedgame.model.LifeStats
import ir.amir.triedgame.model.NetWorthPoint
import ir.amir.triedgame.model.Position
import ir.amir.triedgame.model.PositionSide
import ir.amir.triedgame.model.UserProfile
import ir.amir.triedgame.model.Wallet
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

/**
 * Simple local persistence for the game's save data. SharedPreferences + JSON
 * is used deliberately instead of a database: the data set is small (one
 * profile, one wallet, a handful of open positions) and this keeps the
 * project dependency-light.
 */
class GameRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("tradershow_save", Context.MODE_PRIVATE)

    fun hasProfile(): Boolean = prefs.contains(KEY_FIRST_NAME)

    fun saveProfile(profile: UserProfile) {
        prefs.edit()
            .putString(KEY_FIRST_NAME, profile.firstName)
            .putString(KEY_LAST_NAME, profile.lastName)
            .putInt(KEY_LEVEL, profile.level)
            .putInt(KEY_XP, profile.xp)
            .putLong(KEY_SEED, profile.randomSeed)
            .putLong(KEY_CREATED_AT, profile.accountCreatedAtMillis)
            .apply()
    }

    fun loadProfile(): UserProfile? {
        if (!hasProfile()) return null
        return UserProfile(
            firstName = prefs.getString(KEY_FIRST_NAME, "") ?: "",
            lastName = prefs.getString(KEY_LAST_NAME, "") ?: "",
            level = prefs.getInt(KEY_LEVEL, 1),
            xp = prefs.getInt(KEY_XP, 0),
            randomSeed = prefs.getLong(KEY_SEED, Random.nextLong()),
            accountCreatedAtMillis = prefs.getLong(KEY_CREATED_AT, System.currentTimeMillis())
        )
    }

    fun createProfile(firstName: String, lastName: String): UserProfile {
        val now = System.currentTimeMillis()
        val profile = UserProfile(
            firstName = firstName,
            lastName = lastName,
            level = 1,
            xp = 0,
            randomSeed = Random.nextLong(),
            accountCreatedAtMillis = now
        )
        saveProfile(profile)
        saveLastSeen(now)
        saveLastDrainTimestamp(now)
        saveWallet(Wallet())
        saveLifeStats(LifeStats())
        saveLifeStatsPrecise(100.0, 100.0, 100.0)
        return profile
    }

    fun saveWallet(wallet: Wallet) {
        prefs.edit()
            .putFloat(KEY_WALLET_USD, wallet.usdBalance.toFloat())
            .putFloat(KEY_WALLET_TOMAN, wallet.tomanBalance.toFloat())
            .apply()
    }

    fun loadWallet(): Wallet = Wallet(
        usdBalance = prefs.getFloat(KEY_WALLET_USD, 0f).toDouble(),
        tomanBalance = prefs.getFloat(KEY_WALLET_TOMAN, 0f).toDouble()
    )

    fun saveLifeStats(stats: LifeStats) {
        prefs.edit()
            .putInt(KEY_HEALTH, stats.health)
            .putInt(KEY_HUNGER, stats.hunger)
            .putInt(KEY_ENERGY, stats.energy)
            .apply()
    }

    fun loadLifeStats(): LifeStats = LifeStats(
        health = prefs.getInt(KEY_HEALTH, 100),
        hunger = prefs.getInt(KEY_HUNGER, 100),
        energy = prefs.getInt(KEY_ENERGY, 100)
    )

    /** Sub-percentage-point precision for the three life stats, so drain that
     * happens in small real-time increments (every few seconds) doesn't get
     * rounded away to zero. The rounded [LifeStats] above is what's shown in
     * the UI; these are the "real" values the drain math operates on. */
    fun saveLifeStatsPrecise(health: Double, hunger: Double, energy: Double) {
        prefs.edit()
            .putFloat(KEY_HEALTH_PRECISE, health.toFloat())
            .putFloat(KEY_HUNGER_PRECISE, hunger.toFloat())
            .putFloat(KEY_ENERGY_PRECISE, energy.toFloat())
            .apply()
    }

    fun loadLifeStatsPrecise(): Triple<Double, Double, Double> = Triple(
        prefs.getFloat(KEY_HEALTH_PRECISE, 100f).toDouble(),
        prefs.getFloat(KEY_HUNGER_PRECISE, 100f).toDouble(),
        prefs.getFloat(KEY_ENERGY_PRECISE, 100f).toDouble()
    )

    fun saveLastDrainTimestamp(timestampMillis: Long) {
        prefs.edit().putLong(KEY_LAST_DRAIN, timestampMillis).apply()
    }

    fun loadLastDrainTimestamp(): Long = prefs.getLong(KEY_LAST_DRAIN, System.currentTimeMillis())

    /** Timestamp of the last moment the game engine was ticked (used to compute offline gaps). */
    fun saveLastSeen(timestampMillis: Long) {
        prefs.edit().putLong(KEY_LAST_SEEN, timestampMillis).apply()
    }

    fun loadLastSeen(): Long = prefs.getLong(KEY_LAST_SEEN, System.currentTimeMillis())

    fun saveDailyAdCount(count: Int, dayKey: String) {
        prefs.edit()
            .putInt(KEY_AD_COUNT, count)
            .putString(KEY_AD_DAY, dayKey)
            .apply()
    }

    fun loadDailyAdCount(dayKey: String): Int =
        if (prefs.getString(KEY_AD_DAY, null) == dayKey) prefs.getInt(KEY_AD_COUNT, 0) else 0

    fun saveLastAdTimestamp(timestampMillis: Long) {
        prefs.edit().putLong(KEY_LAST_AD_TIME, timestampMillis).apply()
    }

    fun loadLastAdTimestamp(): Long = prefs.getLong(KEY_LAST_AD_TIME, 0L)

    fun savePositions(positions: List<Position>) {
        val array = JSONArray()
        positions.forEach { p ->
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("symbol", p.assetSymbol)
            obj.put("side", p.side.name)
            obj.put("entryPrice", p.entryPrice)
            obj.put("marginUsd", p.marginUsd)
            obj.put("leverage", p.leverage)
            obj.put("openedAt", p.openedAtMillis)
            if (p.stopLossPrice != null) obj.put("sl", p.stopLossPrice)
            if (p.takeProfitPrice != null) obj.put("tp", p.takeProfitPrice)
            array.put(obj)
        }
        prefs.edit().putString(KEY_POSITIONS, array.toString()).apply()
    }

    fun loadPositions(): List<Position> {
        val raw = prefs.getString(KEY_POSITIONS, null) ?: return emptyList()
        val array = JSONArray(raw)
        return (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            Position(
                id = obj.getString("id"),
                assetSymbol = obj.getString("symbol"),
                side = PositionSide.valueOf(obj.getString("side")),
                entryPrice = obj.getDouble("entryPrice"),
                marginUsd = if (obj.has("marginUsd")) obj.getDouble("marginUsd") else obj.optDouble("amountUsd", 0.0),
                leverage = if (obj.has("leverage")) obj.getInt("leverage") else 1,
                openedAtMillis = obj.getLong("openedAt"),
                stopLossPrice = if (obj.has("sl")) obj.getDouble("sl") else null,
                takeProfitPrice = if (obj.has("tp")) obj.getDouble("tp") else null
            )
        }
    }

    fun saveChallengeRecord(record: ChallengeRecord, dayKey: String) {
        prefs.edit()
            .putInt(KEY_CH_TRADES, record.tradesOpened)
            .putInt(KEY_CH_PROFITABLE, record.profitableCloses)
            .putBoolean(KEY_CH_FICTIONAL, record.fictionalTraded)
            .putBoolean(KEY_CH_HUNGER, record.hungerItemBought)
            .putFloat(KEY_CH_IAP, record.iapUsdToday.toFloat())
            .putStringSet(KEY_CH_CLAIMED, record.claimedIds)
            .putString(KEY_CH_DAY, dayKey)
            .apply()
    }

    /** Returns the saved record plus the day it was recorded for (caller decides whether it's stale). */
    fun loadChallengeRecord(): Pair<ChallengeRecord, String> {
        val record = ChallengeRecord(
            tradesOpened = prefs.getInt(KEY_CH_TRADES, 0),
            profitableCloses = prefs.getInt(KEY_CH_PROFITABLE, 0),
            fictionalTraded = prefs.getBoolean(KEY_CH_FICTIONAL, false),
            hungerItemBought = prefs.getBoolean(KEY_CH_HUNGER, false),
            iapUsdToday = prefs.getFloat(KEY_CH_IAP, 0f).toDouble(),
            claimedIds = prefs.getStringSet(KEY_CH_CLAIMED, emptySet()) ?: emptySet()
        )
        val day = prefs.getString(KEY_CH_DAY, "") ?: ""
        return record to day
    }

    // --- Trade history ---

    fun appendClosedTrade(trade: ClosedTrade) {
        val list = loadClosedTrades().toMutableList()
        list.add(0, trade) // newest first
        val capped = list.take(MAX_HISTORY)
        val array = JSONArray()
        capped.forEach { t ->
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("symbol", t.assetSymbol)
            obj.put("side", t.side.name)
            obj.put("entry", t.entryPrice)
            obj.put("exit", t.exitPrice)
            obj.put("margin", t.marginUsd)
            obj.put("leverage", t.leverage)
            obj.put("pnl", t.pnlUsd)
            obj.put("reason", t.reason.name)
            obj.put("openedAt", t.openedAtMillis)
            obj.put("closedAt", t.closedAtMillis)
            array.put(obj)
        }
        prefs.edit().putString(KEY_TRADE_HISTORY, array.toString()).apply()
    }

    fun loadClosedTrades(): List<ClosedTrade> {
        val raw = prefs.getString(KEY_TRADE_HISTORY, null) ?: return emptyList()
        val array = JSONArray(raw)
        return (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            ClosedTrade(
                id = obj.getString("id"),
                assetSymbol = obj.getString("symbol"),
                side = PositionSide.valueOf(obj.getString("side")),
                entryPrice = obj.getDouble("entry"),
                exitPrice = obj.getDouble("exit"),
                marginUsd = obj.getDouble("margin"),
                leverage = if (obj.has("leverage")) obj.getInt("leverage") else 1,
                pnlUsd = obj.getDouble("pnl"),
                reason = if (obj.has("reason")) CloseReason.valueOf(obj.getString("reason")) else CloseReason.MANUAL,
                openedAtMillis = obj.getLong("openedAt"),
                closedAtMillis = obj.getLong("closedAt")
            )
        }
    }

    // --- Achievements ---

    fun saveAchievementStats(stats: AchievementStats) {
        prefs.edit()
            .putInt(KEY_ACH_TRADES, stats.totalTrades)
            .putInt(KEY_ACH_PROFITABLE, stats.profitableTrades)
            .putInt(KEY_ACH_LIQUIDATIONS, stats.liquidations)
            .putFloat(KEY_ACH_MAX_NET_WORTH, stats.maxNetWorthUsd.toFloat())
            .putStringSet(KEY_ACH_DAYS, stats.daysPlayed)
            .apply()
    }

    fun loadAchievementStats(): AchievementStats = AchievementStats(
        totalTrades = prefs.getInt(KEY_ACH_TRADES, 0),
        profitableTrades = prefs.getInt(KEY_ACH_PROFITABLE, 0),
        liquidations = prefs.getInt(KEY_ACH_LIQUIDATIONS, 0),
        maxNetWorthUsd = prefs.getFloat(KEY_ACH_MAX_NET_WORTH, 0f).toDouble(),
        daysPlayed = prefs.getStringSet(KEY_ACH_DAYS, emptySet()) ?: emptySet()
    )

    fun saveUnlockedAchievements(ids: Set<String>) {
        prefs.edit().putStringSet(KEY_ACH_UNLOCKED, ids).apply()
    }

    fun loadUnlockedAchievements(): Set<String> = prefs.getStringSet(KEY_ACH_UNLOCKED, emptySet()) ?: emptySet()

    // --- Net worth history ---

    fun appendNetWorthPoint(point: NetWorthPoint) {
        val list = loadNetWorthHistory().toMutableList()
        list.add(point)
        val capped = if (list.size > MAX_NET_WORTH_POINTS) list.takeLast(MAX_NET_WORTH_POINTS) else list
        val array = JSONArray()
        capped.forEach { p ->
            val obj = JSONObject()
            obj.put("t", p.timestampMillis)
            obj.put("v", p.totalUsd)
            array.put(obj)
        }
        prefs.edit().putString(KEY_NET_WORTH_HISTORY, array.toString()).apply()
    }

    fun loadNetWorthHistory(): List<NetWorthPoint> {
        val raw = prefs.getString(KEY_NET_WORTH_HISTORY, null) ?: return emptyList()
        val array = JSONArray(raw)
        return (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            NetWorthPoint(obj.getLong("t"), obj.getDouble("v"))
        }
    }

    // --- Daily random life event ---

    fun loadLastLifeEventDay(): String = prefs.getString(KEY_LIFE_EVENT_DAY, "") ?: ""
    fun saveLastLifeEventDay(dayKey: String) {
        prefs.edit().putString(KEY_LIFE_EVENT_DAY, dayKey).apply()
    }

    /** True forever once the wallet has reached $5 for the first time -- see
     * [ir.amir.triedgame.ui.GameViewModel.checkLifeEventEligibility]. */
    fun loadLifeEventEligible(): Boolean = prefs.getBoolean(KEY_LIFE_EVENT_ELIGIBLE, false)
    fun saveLifeEventEligible(eligible: Boolean) {
        prefs.edit().putBoolean(KEY_LIFE_EVENT_ELIGIBLE, eligible).apply()
    }

    // --- One-time "rate us on Myket" challenge ---

    fun loadReviewChallengeClaimed(): Boolean = prefs.getBoolean(KEY_REVIEW_CLAIMED, false)
    fun saveReviewChallengeClaimed(claimed: Boolean) {
        prefs.edit().putBoolean(KEY_REVIEW_CLAIMED, claimed).apply()
    }

    fun loadReviewLinkOpenedAt(): Long = prefs.getLong(KEY_REVIEW_OPENED_AT, 0L)
    fun saveReviewLinkOpenedAt(timestampMillis: Long) {
        prefs.edit().putLong(KEY_REVIEW_OPENED_AT, timestampMillis).apply()
    }

    companion object {
        private const val MAX_HISTORY = 200
        private const val MAX_NET_WORTH_POINTS = 200
        private const val KEY_FIRST_NAME = "first_name"
        private const val KEY_LAST_NAME = "last_name"
        private const val KEY_LEVEL = "level"
        private const val KEY_XP = "xp"
        private const val KEY_SEED = "seed"
        private const val KEY_CREATED_AT = "created_at"
        private const val KEY_WALLET_USD = "wallet_usd"
        private const val KEY_WALLET_TOMAN = "wallet_toman"
        private const val KEY_HEALTH = "health"
        private const val KEY_HUNGER = "hunger"
        private const val KEY_ENERGY = "energy"
        private const val KEY_HEALTH_PRECISE = "health_precise"
        private const val KEY_HUNGER_PRECISE = "hunger_precise"
        private const val KEY_ENERGY_PRECISE = "energy_precise"
        private const val KEY_LAST_DRAIN = "last_drain"
        private const val KEY_LAST_SEEN = "last_seen"
        private const val KEY_AD_COUNT = "ad_count_today"
        private const val KEY_AD_DAY = "ad_count_day"
        private const val KEY_LAST_AD_TIME = "last_ad_time"
        private const val KEY_POSITIONS = "positions"
        private const val KEY_CH_TRADES = "ch_trades"
        private const val KEY_CH_PROFITABLE = "ch_profitable"
        private const val KEY_CH_FICTIONAL = "ch_fictional"
        private const val KEY_CH_HUNGER = "ch_hunger"
        private const val KEY_CH_IAP = "ch_iap_usd"
        private const val KEY_CH_CLAIMED = "ch_claimed"
        private const val KEY_CH_DAY = "ch_day"
        private const val KEY_TRADE_HISTORY = "trade_history"
        private const val KEY_ACH_TRADES = "ach_trades"
        private const val KEY_ACH_PROFITABLE = "ach_profitable"
        private const val KEY_ACH_LIQUIDATIONS = "ach_liquidations"
        private const val KEY_ACH_MAX_NET_WORTH = "ach_max_net_worth"
        private const val KEY_ACH_DAYS = "ach_days"
        private const val KEY_ACH_UNLOCKED = "ach_unlocked"
        private const val KEY_NET_WORTH_HISTORY = "net_worth_history"
        private const val KEY_LIFE_EVENT_DAY = "life_event_day"
        private const val KEY_LIFE_EVENT_ELIGIBLE = "life_event_eligible"
        private const val KEY_REVIEW_CLAIMED = "review_challenge_claimed"
        private const val KEY_REVIEW_OPENED_AT = "review_link_opened_at"
    }
}
