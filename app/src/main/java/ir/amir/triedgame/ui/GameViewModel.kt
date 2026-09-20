package ir.amir.triedgame.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.amir.triedgame.data.GameRepository
import ir.amir.triedgame.engine.PriceEngine
import ir.amir.triedgame.model.Achievement
import ir.amir.triedgame.model.AchievementCatalog
import ir.amir.triedgame.model.AchievementStats
import ir.amir.triedgame.model.Asset
import ir.amir.triedgame.model.AssetCatalog
import ir.amir.triedgame.model.Candle
import ir.amir.triedgame.model.ChallengeRecord
import ir.amir.triedgame.model.ClosedTrade
import ir.amir.triedgame.model.CloseReason
import ir.amir.triedgame.model.DailyChallenge
import ir.amir.triedgame.model.DailyChallengeCatalog
import ir.amir.triedgame.model.LifeEvent
import ir.amir.triedgame.model.LifeEventCatalog
import ir.amir.triedgame.model.LifeEventKind
import ir.amir.triedgame.model.LifeStats
import ir.amir.triedgame.model.NetWorthPoint
import ir.amir.triedgame.model.Position
import ir.amir.triedgame.model.PositionSide
import ir.amir.triedgame.model.USD_TO_TOMAN_RATE
import ir.amir.triedgame.model.UserProfile
import ir.amir.triedgame.model.Wallet
import ir.amir.triedgame.model.xpToReachLevel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

/** UI-ready snapshot of a daily challenge: definition + today's live progress. */
data class ChallengeUiState(
    val challenge: DailyChallenge,
    val progress: Int,
    val isComplete: Boolean,
    val isClaimed: Boolean
)

data class AchievementUiState(val achievement: Achievement, val unlocked: Boolean)

/** Candle timeframe options the user can pick on the trading screen. */
enum class Timeframe(val millis: Long, val label: String) {
    M1(PriceEngine.MINUTE_MS, "۱ دقیقه"),
    M5(5 * PriceEngine.MINUTE_MS, "۵ دقیقه"),
    H1(PriceEngine.HOUR_MS, "۱ ساعت"),
    H24(PriceEngine.DAY_MS, "۲۴ ساعت")
}

class GameViewModel(private val repository: GameRepository) : ViewModel() {

    var profile by mutableStateOf<UserProfile?>(null)
        private set

    var wallet by mutableStateOf(Wallet())
        private set

    var lifeStats by mutableStateOf(LifeStats())
        private set

    var positions by mutableStateOf<List<Position>>(emptyList())
        private set

    var selectedAsset by mutableStateOf(AssetCatalog.all.first())
        private set

    var timeframe by mutableStateOf(Timeframe.M1)
        private set

    var candles by mutableStateOf<List<Candle>>(emptyList())
        private set

    /** Live current price per asset symbol, updated every tick. */
    var currentPrices by mutableStateOf<Map<String, Double>>(emptyMap())
        private set

    var challenges by mutableStateOf<List<ChallengeUiState>>(emptyList())
        private set

    var achievements by mutableStateOf<List<AchievementUiState>>(emptyList())
        private set

    var tradeHistory by mutableStateOf<List<ClosedTrade>>(emptyList())
        private set

    var netWorthHistory by mutableStateOf<List<NetWorthPoint>>(emptyList())
        private set

    /** A random life event waiting to be acknowledged by the user (شown as a dismissible card). */
    var pendingLifeEvent by mutableStateOf<LifeEvent?>(null)
        private set

    /** Set briefly when the user levels up, so the UI can show a small celebration. */
    var levelUpEvent by mutableStateOf<Int?>(null)
        private set

    /** Set briefly when a new achievement unlocks. */
    var newAchievementEvent by mutableStateOf<Achievement?>(null)
        private set

    // --- One-time "rate us 5 stars on Myket" challenge ---
    var reviewChallengeClaimed by mutableStateOf(false)
        private set
    var reviewChallengeReadyToClaim by mutableStateOf(false)
        private set
    private var reviewLinkOpenedAt = 0L

    private var tickerStarted = false
    private var challengeRecord = ChallengeRecord()
    private var achievementStats = AchievementStats()
    private var unlockedAchievementIds: Set<String> = emptySet()
    private var netWorthTickCounter = 0
    private val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private fun todayKey() = dayFormat.format(Date())

    // Precise (fractional) life-stat values the real-time drain math operates
    // on; `lifeStats` above holds the rounded, UI-facing version of these.
    private var preciseHealth = 100.0
    private var preciseHunger = 100.0
    private var preciseEnergy = 100.0

    companion object {
        private const val TICK_MS = 1000L
        private const val ENERGY_FULL_DRAIN_MS = 120L * 60 * 1000       // 120 minutes of active play
        private const val HUNGER_FULL_DRAIN_MS = 150L * 60 * 1000       // 150 minutes of active play
        private const val HEALTH_FULL_DRAIN_MS = 240L * 60 * 1000       // 240 minutes (4h) of active play
        private const val STARVING_HEALTH_DRAIN_MS = 60L * 60 * 1000    // extra loss while hunger is at 0
        private const val NET_WORTH_SAMPLE_EVERY_TICKS = 12              // sample every ~12s (ticks are now 1s apart)
        private const val LOW_STAT_TRADE_BLOCK_THRESHOLD = 20           // below this, trading is blocked
        private const val REVIEW_CHALLENGE_REWARD_TOMAN = 60_000.0
        private const val MIN_REVIEW_WAIT_MS = 15_000L                  // heuristic: must be away at least this long
        const val MYKET_REVIEW_URL = "https://myket.ir/app/ir.amir.triedgame"

        // Internal test-only unlock: registering with this exact first name
        // grants starting USD for testing. Not surfaced anywhere in the UI,
        // strings, or docs -- keep it that way.
        private const val TEST_UNLOCK_CODE = "mnbvchxz7890"
        private const val TEST_UNLOCK_USD = 800.0
    }

    fun loadOrCreateProfile(existing: UserProfile?) {
        if (existing != null) {
            profile = existing
            wallet = repository.loadWallet()
            lifeStats = repository.loadLifeStats()
            val (h, hu, e) = repository.loadLifeStatsPrecise()
            preciseHealth = h; preciseHunger = hu; preciseEnergy = e
            positions = repository.loadPositions()
            tradeHistory = repository.loadClosedTrades()
            netWorthHistory = repository.loadNetWorthHistory()
            achievementStats = repository.loadAchievementStats()
            unlockedAchievementIds = repository.loadUnlockedAchievements()
            loadChallenges()
            refreshAchievementUiState()
            checkDailyLifeEvent()
            reviewChallengeClaimed = repository.loadReviewChallengeClaimed()
            reviewLinkOpenedAt = repository.loadReviewLinkOpenedAt()
            resolveOfflineGapAndStartTicking()
        }
    }

    fun registerNewUser(firstName: String, lastName: String) {
        val created = repository.createProfile(firstName, lastName)
        profile = created
        wallet = Wallet()
        if (firstName.trim() == TEST_UNLOCK_CODE) {
            wallet = wallet.addUsd(TEST_UNLOCK_USD)
            repository.saveWallet(wallet)
        }
        lifeStats = LifeStats()
        preciseHealth = 100.0; preciseHunger = 100.0; preciseEnergy = 100.0
        positions = emptyList()
        tradeHistory = emptyList()
        netWorthHistory = emptyList()
        achievementStats = AchievementStats()
        unlockedAchievementIds = emptySet()
        loadChallenges()
        refreshAchievementUiState()
        checkDailyLifeEvent()
        resolveOfflineGapAndStartTicking()
    }

    fun selectAsset(asset: Asset) {
        selectedAsset = asset
        rebuildChartFor(asset)
    }

    fun selectTimeframe(tf: Timeframe) {
        timeframe = tf
        rebuildChartFor(selectedAsset)
    }

    fun clearLevelUpEvent() { levelUpEvent = null }
    fun clearNewAchievementEvent() { newAchievementEvent = null }
    fun dismissLifeEvent() { pendingLifeEvent = null }

    /** Null when trading is allowed; otherwise a short Persian reason (e.g.
     * "انرژی ندارم") for why it's currently blocked, based on whichever of
     * energy/hunger/health has dropped below the low-stat threshold first. */
    fun tradingBlockReason(): String? = when {
        lifeStats.energy < LOW_STAT_TRADE_BLOCK_THRESHOLD -> "انرژی ندارم"
        lifeStats.hunger < LOW_STAT_TRADE_BLOCK_THRESHOLD -> "گرسنه‌ام"
        lifeStats.health < LOW_STAT_TRADE_BLOCK_THRESHOLD -> "حالم خوب نیست"
        else -> null
    }

    private fun referencePriceFor(asset: Asset) = asset.startingPriceUsd

    private fun rebuildChartFor(asset: Asset) {
        val p = profile ?: return
        val now = System.currentTimeMillis()
        // Show ~150 candles at the chosen timeframe.
        val from = now - timeframe.millis * 150
        candles = PriceEngine.generateCandles(
            asset = asset,
            seed = p.randomSeed,
            fromMillis = from,
            toMillis = now,
            intervalMillis = timeframe.millis,
            referenceTimestampMillis = p.accountCreatedAtMillis,
            referencePrice = referencePriceFor(asset)
        )
    }

    /**
     * Called once when a profile becomes available (fresh registration or app
     * resume). Backfills every asset's price up to "now" from the last time we
     * ticked, so positions the user held while the app was closed reflect the
     * time that actually passed -- then starts the live per-second ticker.
     */
    private fun resolveOfflineGapAndStartTicking() {
        val p = profile ?: return
        val now = System.currentTimeMillis()

        val prices = mutableMapOf<String, Double>()
        AssetCatalog.all.forEach { asset ->
            prices[asset.symbol] = PriceEngine.priceAt(
                asset, p.randomSeed, now, p.accountCreatedAtMillis, referencePriceFor(asset)
            )
        }
        currentPrices = prices
        rebuildChartFor(selectedAsset)
        repository.saveLastSeen(now)
        // No life-stat drain here: stats only drain from active ticks below,
        // never for time spent with the app closed.

        if (!tickerStarted) {
            tickerStarted = true
            startTicker()
        }
    }

    private fun startTicker() {
        viewModelScope.launch {
            while (isActive) {
                delay(TICK_MS)
                tickOnce()
            }
        }
    }

    private fun tickOnce() {
        val p = profile ?: return
        val now = System.currentTimeMillis()
        val prices = mutableMapOf<String, Double>()
        AssetCatalog.all.forEach { asset ->
            prices[asset.symbol] = PriceEngine.priceAt(
                asset, p.randomSeed, now, p.accountCreatedAtMillis, referencePriceFor(asset)
            )
        }
        currentPrices = prices
        rebuildChartFor(selectedAsset)
        repository.saveLastSeen(now)
        applyLifeDrain()
        checkPositionTriggers()
        checkLifeEventEligibility()
        refreshChallengeUiState()

        netWorthTickCounter++
        if (netWorthTickCounter >= NET_WORTH_SAMPLE_EVERY_TICKS) {
            netWorthTickCounter = 0
            sampleNetWorth(now)
        }
    }

    /**
     * Drains health/hunger/energy by a fixed amount per active tick (every
     * [TICK_MS]) -- NOT based on real wall-clock time. This is deliberate:
     * the ticker only runs while the app is open and the game loop is alive,
     * so stats stay frozen while the app is closed and only deplete during
     * actual play time. At this pace, energy empties after ~45 min of active
     * play, hunger after ~90 min, health after ~3h baseline, plus faster
     * health loss while starving (hunger at 0).
     */
    private fun applyLifeDrain() {
        preciseEnergy = (preciseEnergy - 100.0 * TICK_MS / ENERGY_FULL_DRAIN_MS).coerceIn(0.0, 100.0)
        preciseHunger = (preciseHunger - 100.0 * TICK_MS / HUNGER_FULL_DRAIN_MS).coerceIn(0.0, 100.0)
        preciseHealth = (preciseHealth - 100.0 * TICK_MS / HEALTH_FULL_DRAIN_MS).coerceIn(0.0, 100.0)
        if (preciseHunger <= 0.0) {
            preciseHealth = (preciseHealth - 100.0 * TICK_MS / STARVING_HEALTH_DRAIN_MS).coerceIn(0.0, 100.0)
        }
        lifeStats = LifeStats(
            health = preciseHealth.roundToInt(),
            hunger = preciseHunger.roundToInt(),
            energy = preciseEnergy.roundToInt()
        ).clamp()
        repository.saveLifeStats(lifeStats)
        repository.saveLifeStatsPrecise(preciseHealth, preciseHunger, preciseEnergy)
    }

    /** Checks every open position for liquidation, stop-loss, or take-profit triggers. */
    private fun checkPositionTriggers() {
        val stillOpen = mutableListOf<Position>()
        var changed = false
        positions.forEach { pos ->
            val price = currentPrices[pos.assetSymbol]
            if (price == null) {
                stillOpen.add(pos)
                return@forEach
            }
            when {
                pos.isLiquidated(price) -> {
                    finalizeClose(pos, price, CloseReason.LIQUIDATED, payoutOverride = 0.0)
                    changed = true
                }
                pos.hitStopLoss(price) -> {
                    finalizeClose(pos, price, CloseReason.STOP_LOSS)
                    changed = true
                }
                pos.hitTakeProfit(price) -> {
                    finalizeClose(pos, price, CloseReason.TAKE_PROFIT)
                    changed = true
                }
                else -> stillOpen.add(pos)
            }
        }
        if (changed) {
            positions = stillOpen
            repository.savePositions(positions)
        }
    }

    // --- Trading ---

    fun openPosition(
        side: PositionSide,
        marginUsd: Double,
        leverage: Int,
        stopLossPrice: Double? = null,
        takeProfitPrice: Double? = null
    ): Boolean {
        if (marginUsd <= 0.0) return false
        if (tradingBlockReason() != null) return false
        val price = currentPrices[selectedAsset.symbol] ?: return false
        val w = wallet.spendUsdForMargin(marginUsd) ?: return false
        wallet = w
        repository.saveWallet(wallet)
        val position = Position(
            id = UUID.randomUUID().toString(),
            assetSymbol = selectedAsset.symbol,
            side = side,
            entryPrice = price,
            marginUsd = marginUsd,
            leverage = leverage,
            openedAtMillis = System.currentTimeMillis(),
            stopLossPrice = stopLossPrice,
            takeProfitPrice = takeProfitPrice
        )
        positions = positions + position
        repository.savePositions(positions)

        challengeRecord = challengeRecord.copy(
            tradesOpened = challengeRecord.tradesOpened + 1,
            fictionalTraded = challengeRecord.fictionalTraded ||
                selectedAsset.category == ir.amir.triedgame.model.AssetCategory.FICTIONAL
        )
        saveChallengeRecord()
        refreshChallengeUiState()
        return true
    }

    /** User-initiated close at the current live price. */
    fun closePosition(position: Position) {
        val price = currentPrices[position.assetSymbol] ?: return
        positions = positions.filterNot { it.id == position.id }
        repository.savePositions(positions)
        finalizeClose(position, price, CloseReason.MANUAL)
    }

    /**
     * Shared close logic for manual closes and auto-triggers (liquidation,
     * stop-loss, take-profit): credits the wallet, records history, updates
     * achievements/challenges/XP. Callers are responsible for removing the
     * position from [positions] themselves (manual close does it before
     * calling this; [checkPositionTriggers] does it in a batch afterwards).
     */
    private fun finalizeClose(position: Position, price: Double, reason: CloseReason, payoutOverride: Double? = null) {
        val pnl = position.currentPnlUsd(price)
        val payout = payoutOverride ?: position.currentValueUsd(price)
        if (payout > 0.0) {
            wallet = wallet.addUsd(payout)
            repository.saveWallet(wallet)
        }

        val trade = ClosedTrade(
            id = position.id,
            assetSymbol = position.assetSymbol,
            side = position.side,
            entryPrice = position.entryPrice,
            exitPrice = price,
            marginUsd = position.marginUsd,
            leverage = position.leverage,
            pnlUsd = pnl,
            reason = reason,
            openedAtMillis = position.openedAtMillis,
            closedAtMillis = System.currentTimeMillis()
        )
        tradeHistory = (listOf(trade) + tradeHistory).take(200)
        repository.appendClosedTrade(trade)

        achievementStats = achievementStats.copy(
            totalTrades = achievementStats.totalTrades + 1,
            profitableTrades = achievementStats.profitableTrades + if (pnl > 0) 1 else 0,
            liquidations = achievementStats.liquidations + if (reason == CloseReason.LIQUIDATED) 1 else 0
        )
        repository.saveAchievementStats(achievementStats)
        refreshAchievementUiState()

        if (pnl > 0) {
            challengeRecord = challengeRecord.copy(profitableCloses = challengeRecord.profitableCloses + 1)
            saveChallengeRecord()
        }
        gainXp((pnl.coerceAtLeast(0.0) / 5.0).roundToInt().coerceIn(0, 200))
        refreshChallengeUiState()
    }

    // --- Wallet ---

    fun creditUsd(amount: Double) {
        wallet = wallet.addUsd(amount)
        repository.saveWallet(wallet)
    }

    /** Same as [creditUsd] but also counts toward the "charge $25+ via IAP"
     * daily challenge -- use this specifically for Myket purchase credits. */
    fun creditUsdFromPurchase(amount: Double) {
        creditUsd(amount)
        challengeRecord = challengeRecord.copy(iapUsdToday = challengeRecord.iapUsdToday + amount)
        saveChallengeRecord()
        refreshChallengeUiState()
    }

    fun creditToman(amount: Double) {
        wallet = wallet.addToman(amount)
        repository.saveWallet(wallet)
    }

    fun convertUsdToToman(amount: Double) {
        wallet = wallet.convertUsdToToman(amount)
        repository.saveWallet(wallet)
    }

    fun convertTomanToUsd(amount: Double) {
        wallet = wallet.convertTomanToUsd(amount)
        repository.saveWallet(wallet)
    }

    // --- Net worth ---

    private fun sampleNetWorth(now: Long) {
        val positionsValue = positions.sumOf { pos ->
            val price = currentPrices[pos.assetSymbol] ?: pos.entryPrice
            pos.currentValueUsd(price)
        }
        val total = wallet.usdBalance + wallet.tomanBalance / USD_TO_TOMAN_RATE + positionsValue
        val point = NetWorthPoint(now, total)
        netWorthHistory = (netWorthHistory + point).takeLast(200)
        repository.appendNetWorthPoint(point)

        if (total > achievementStats.maxNetWorthUsd) {
            achievementStats = achievementStats.copy(maxNetWorthUsd = total)
            repository.saveAchievementStats(achievementStats)
            refreshAchievementUiState()
        }
    }

    // --- Life / زندگی من ---

    fun spendTomanInLife(amount: Double, xpReward: Int, isHungerItem: Boolean, applyEffect: (LifeStats) -> LifeStats): Boolean {
        val newWallet = wallet.spendToman(amount) ?: return false
        wallet = newWallet
        repository.saveWallet(wallet)
        val updated = applyEffect(lifeStats).clamp()
        lifeStats = updated
        preciseHealth = updated.health.toDouble()
        preciseHunger = updated.hunger.toDouble()
        preciseEnergy = updated.energy.toDouble()
        repository.saveLifeStats(lifeStats)
        repository.saveLifeStatsPrecise(preciseHealth, preciseHunger, preciseEnergy)
        gainXp(xpReward)

        if (isHungerItem) {
            challengeRecord = challengeRecord.copy(hungerItemBought = true)
            saveChallengeRecord()
            refreshChallengeUiState()
        }
        return true
    }

    /** Runs once per real day: records the day for the "played N different
     * days" achievement (always), then separately decides whether to roll a
     * random life event for زندگی من (gated -- see [maybeRollLifeEvent]). */
    private fun checkDailyLifeEvent() {
        val today = todayKey()
        if (repository.loadLastLifeEventDay() == today) return
        repository.saveLastLifeEventDay(today)

        achievementStats = achievementStats.copy(daysPlayed = achievementStats.daysPlayed + today)
        repository.saveAchievementStats(achievementStats)
        refreshAchievementUiState()

        maybeRollLifeEvent()
    }

    /** Random "life events" (job offer, unexpected bill, etc.) only start
     * appearing once the user's wallet has reached $5 for the first time
     * ever (so a brand-new player never gets hit with a bill they can't
     * possibly pay), and even then only on some days of the week -- not
     * every single day -- so they don't feel relentless. Bonuses are
     * credited immediately; expenses wait for the user to resolve them with
     * a button in زندگی من (see [resolveLifeEventExpense]). */
    private fun maybeRollLifeEvent() {
        if (!repository.loadLifeEventEligible()) return
        val dayOfWeek = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)
        val eventDays = setOf(
            java.util.Calendar.SUNDAY, java.util.Calendar.TUESDAY,
            java.util.Calendar.THURSDAY, java.util.Calendar.SATURDAY
        )
        if (dayOfWeek !in eventDays) return

        val event = LifeEventCatalog.random()
        if (event.kind == LifeEventKind.BONUS) {
            wallet = wallet.addToman(event.amountToman)
            repository.saveWallet(wallet)
        }
        pendingLifeEvent = event
    }

    /** Checked on every active tick: the first moment the wallet's USD
     * balance reaches $5, life events become permanently eligible to occur
     * from then on -- even if the balance later drops back below $5. */
    private fun checkLifeEventEligibility() {
        if (!repository.loadLifeEventEligible() && wallet.usdBalance >= 5.0) {
            repository.saveLifeEventEligible(true)
        }
    }

    /** Called when the user taps "پرداخت و رفع مشکل" on a pending expense event. */
    fun resolveLifeEventExpense() {
        val event = pendingLifeEvent ?: return
        if (event.kind != LifeEventKind.EXPENSE) return
        wallet = wallet.spendToman(event.amountToman) ?: wallet.copy(tomanBalance = 0.0)
        repository.saveWallet(wallet)
        pendingLifeEvent = null
    }

    // --- One-time "rate us 5 stars on Myket" challenge ---
    // NOTE: there is no way to verify from inside the app that a review was
    // actually submitted -- Myket exposes no such API to third-party apps.
    // This is a best-effort heuristic: the reward only becomes claimable
    // after the user has left the app (presumably to the Myket page) and
    // came back at least MIN_REVIEW_WAIT_MS later, which a simple "tap and
    // immediately return" can't satisfy. It discourages casual abuse without
    // claiming to be foolproof.

    /** Call when the "انجام دادن" button is tapped, right before opening the Myket link. */
    fun markReviewLinkOpened() {
        if (reviewChallengeClaimed) return
        reviewLinkOpenedAt = System.currentTimeMillis()
        repository.saveReviewLinkOpenedAt(reviewLinkOpenedAt)
    }

    /** Call from the Activity's onResume. */
    fun onAppResumed() {
        if (reviewChallengeClaimed || reviewLinkOpenedAt <= 0L) return
        if (System.currentTimeMillis() - reviewLinkOpenedAt >= MIN_REVIEW_WAIT_MS) {
            reviewChallengeReadyToClaim = true
        }
    }

    fun claimReviewChallenge() {
        if (reviewChallengeClaimed || !reviewChallengeReadyToClaim) return
        wallet = wallet.addToman(REVIEW_CHALLENGE_REWARD_TOMAN)
        repository.saveWallet(wallet)
        reviewChallengeClaimed = true
        repository.saveReviewChallengeClaimed(true)
        gainXp(50)
    }

    // --- Level / XP ---

    private fun gainXp(amount: Int) {
        if (amount <= 0) return
        val p = profile ?: return
        var newXp = p.xp + amount
        var newLevel = p.level
        while (newXp >= xpToReachLevel(newLevel + 1)) {
            newLevel += 1
        }
        val leveledUp = newLevel > p.level
        val updated = p.copy(xp = newXp, level = newLevel)
        profile = updated
        repository.saveProfile(updated)
        if (leveledUp) levelUpEvent = newLevel
    }

    // --- Achievements ---

    private fun refreshAchievementUiState() {
        val newlyUnlocked = mutableListOf<Achievement>()
        val updatedIds = unlockedAchievementIds.toMutableSet()
        AchievementCatalog.all.forEach { ach ->
            if (!updatedIds.contains(ach.id) && ach.isUnlockedFrom(achievementStats)) {
                updatedIds.add(ach.id)
                newlyUnlocked.add(ach)
            }
        }
        if (newlyUnlocked.isNotEmpty()) {
            unlockedAchievementIds = updatedIds
            repository.saveUnlockedAchievements(unlockedAchievementIds)
            newAchievementEvent = newlyUnlocked.first()
        }
        achievements = AchievementCatalog.all.map { AchievementUiState(it, unlockedAchievementIds.contains(it.id)) }
    }

    // --- Daily challenges ---

    private fun loadChallenges() {
        val (record, day) = repository.loadChallengeRecord()
        challengeRecord = if (day == todayKey()) record else ChallengeRecord()
        if (day != todayKey()) saveChallengeRecord()
        refreshChallengeUiState()
    }

    private fun saveChallengeRecord() {
        repository.saveChallengeRecord(challengeRecord, todayKey())
    }

    private fun refreshChallengeUiState() {
        challenges = DailyChallengeCatalog.today().map { challenge ->
            val progress = challenge.progressFrom(challengeRecord, lifeStats)
            ChallengeUiState(
                challenge = challenge,
                progress = progress.coerceAtMost(challenge.target),
                isComplete = progress >= challenge.target,
                isClaimed = challengeRecord.claimedIds.contains(challenge.id)
            )
        }
    }

    fun claimChallenge(challengeId: String) {
        val state = challenges.find { it.challenge.id == challengeId } ?: return
        if (!state.isComplete || state.isClaimed) return
        wallet = wallet.addToman(state.challenge.rewardToman)
        repository.saveWallet(wallet)
        challengeRecord = challengeRecord.copy(claimedIds = challengeRecord.claimedIds + challengeId)
        saveChallengeRecord()
        gainXp(30)
        refreshChallengeUiState()
    }
}

/** Wallet needs a margin-hold helper distinct from a plain spend, since the
 * amount is returned (plus/minus P&L) when the position closes rather than
 * being permanently gone. Kept here to avoid widening Wallet's own API. */
private fun Wallet.spendUsdForMargin(amount: Double): Wallet? =
    if (usdBalance >= amount) copy(usdBalance = usdBalance - amount) else null
