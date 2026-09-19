package ir.amir.triedgame.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ir.amir.triedgame.model.Candle
import ir.amir.triedgame.ui.theme.TsGreen
import ir.amir.triedgame.ui.theme.TsRed
import ir.amir.triedgame.ui.theme.TsTextSecondary
import kotlinx.coroutines.delay
import kotlin.math.abs

private val bullishHeadlines = listOf(
    "%s در حال صعود قویه، خریدارها میدون‌دار شدن 📈",
    "حجم معاملات %s با شروع روند صعودی افزایش یافت",
    "تحلیل‌گران چشم‌انداز کوتاه‌مدت %s رو مثبت می‌بینن",
    "برخی تریدرها روی %s پوزیشن Long بزرگ باز کردن",
    "علاقه‌ی سرمایه‌گذاران به %s رو به افزایشه 🔥",
    "%s تونست یک سطح مقاومتی مهم رو رد کنه"
)

private val bearishHeadlines = listOf(
    "%s تحت فشار فروش قرار گرفته 📉",
    "برخی سرمایه‌گذاران روی %s سود شناسایی کردن",
    "%s یک سطح حمایتی رو از دست داد",
    "تحلیل‌گران نسبت به ریسک نزولی %s هشدار دادن",
    "حجم فروش %s در ساعات اخیر بالا رفته"
)

private val volatileHeadlines = listOf(
    "هشدار نوسان: %s ممکنه حرکت شدیدی داشته باشه ⚡",
    "%s این ساعت نوسان غیرعادی داشته، مراقب باش",
    "بازار %s بی‌ثبات به نظر می‌رسه، تصمیم‌گیری دقیق‌تری لازمه"
)

private val calmHeadlines = listOf(
    "بازار %s امروز نسبتاً آروم به نظر می‌رسه",
    "%s توی یک محدوده‌ی باریک در حال نوسانه",
    "معامله‌گران منتظر یک محرک جدید برای %s هستن"
)

private enum class Mood { BULLISH, BEARISH, VOLATILE, CALM }

private fun moodFrom(candles: List<Candle>): Mood {
    if (candles.size < 5) return Mood.CALM
    val recent = candles.takeLast(10)
    val startPrice = recent.first().open
    val endPrice = recent.last().close
    if (startPrice <= 0.0) return Mood.CALM
    val changeFraction = (endPrice - startPrice) / startPrice
    val avgRange = recent.map { abs(it.high - it.low) / startPrice }.average()

    return when {
        avgRange > 0.02 -> Mood.VOLATILE
        changeFraction > 0.004 -> Mood.BULLISH
        changeFraction < -0.004 -> Mood.BEARISH
        else -> Mood.CALM
    }
}

/** A one-line rotating headline. Purely narrative flavor (doesn't feed back
 * into the price engine), but the headline POOL it draws from is chosen
 * based on the selected asset's actual recent candle movement, so it reads
 * as roughly in sync with what's happening on the chart. */
@Composable
fun NewsTicker(assetDisplayName: String, candles: List<Candle>, modifier: Modifier = Modifier) {
    var headline by remember { mutableStateOf("") }

    LaunchedEffect(assetDisplayName) {
        while (true) {
            val pool = when (moodFrom(candles)) {
                Mood.BULLISH -> bullishHeadlines
                Mood.BEARISH -> bearishHeadlines
                Mood.VOLATILE -> volatileHeadlines
                Mood.CALM -> calmHeadlines
            }
            headline = pool.random().format(assetDisplayName)
            delay(8000)
        }
    }

    val mood = moodFrom(candles)
    val color = when (mood) {
        Mood.BULLISH -> TsGreen
        Mood.BEARISH -> TsRed
        else -> TsTextSecondary
    }

    Text(
        "📰 $headline",
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = modifier.padding(vertical = 2.dp)
    )
}
