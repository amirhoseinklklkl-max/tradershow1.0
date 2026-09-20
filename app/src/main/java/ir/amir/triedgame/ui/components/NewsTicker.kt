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
    "%s تونست یک سطح مقاومتی مهم رو رد کنه",
    "شایعاتی از یک خبر مثبت برای %s در بازار پیچیده",
    "نمودار %s نشونه‌های ادامه‌ی روند صعودی رو نشون می‌ده",
    "خریداران %s قدرت گرفتن، فشار فروش کم شده",
    "%s در آستانه‌ی شکست یک سطح مقاومتی کلیدیه"
)

private val bearishHeadlines = listOf(
    "%s تحت فشار فروش قرار گرفته 📉",
    "برخی سرمایه‌گذاران روی %s سود شناسایی کردن",
    "%s یک سطح حمایتی رو از دست داد",
    "تحلیل‌گران نسبت به ریسک نزولی %s هشدار دادن",
    "حجم فروش %s در ساعات اخیر بالا رفته",
    "شایعاتی از یک خبر منفی برای %s در بازار پیچیده",
    "نمودار %s نشونه‌های ادامه‌ی روند نزولی رو نشون می‌ده",
    "فروشندگان %s قدرت گرفتن، فشار خرید کم شده",
    "%s در آستانه‌ی شکست یک سطح حمایتی کلیدیه"
)

private val volatileHeadlines = listOf(
    "هشدار نوسان: %s ممکنه حرکت شدیدی داشته باشه ⚡",
    "%s این ساعت نوسان غیرعادی داشته، مراقب باش",
    "بازار %s بی‌ثبات به نظر می‌رسه، تصمیم‌گیری دقیق‌تری لازمه",
    "نقدینگی %s این لحظه پایینه، نوسان می‌تونه شدید بشه",
    "یه خبر غیرمنتظره ممکنه %s رو تکون بده"
)

private val calmHeadlines = listOf(
    "بازار %s امروز نسبتاً آروم به نظر می‌رسه",
    "%s توی یک محدوده‌ی باریک در حال نوسانه",
    "معامله‌گران منتظر یک محرک جدید برای %s هستن",
    "حجم معاملات %s پایین‌تر از میانگین روزانه‌ست"
)

private enum class Mood { BULLISH, BEARISH, VOLATILE, CALM }

/**
 * A one-line rotating headline. Its mood is driven by [predictedChangeFraction]
 * -- a genuine short lookahead into the deterministic price engine for the
 * selected asset (see GameViewModel.predictedNearFutureChangeFraction) -- so
 * the news reads as foreshadowing what's about to happen on the chart,
 * rather than describing what already happened. [candles] is used only to
 * detect unusually wide recent ranges for the "volatile" mood.
 */
@Composable
fun NewsTicker(
    assetDisplayName: String,
    candles: List<Candle>,
    predictedChangeFraction: () -> Double,
    modifier: Modifier = Modifier
) {
    var headline by remember { mutableStateOf("") }
    var mood by remember { mutableStateOf(Mood.CALM) }

    LaunchedEffect(assetDisplayName) {
        while (true) {
            val avgRange = if (candles.size >= 5) {
                candles.takeLast(10).map { c ->
                    val base = c.open.takeIf { it > 0.0 } ?: 1.0
                    abs(c.high - c.low) / base
                }.average()
            } else 0.0
            val predicted = predictedChangeFraction()

            mood = when {
                avgRange > 0.02 -> Mood.VOLATILE
                predicted > 0.003 -> Mood.BULLISH
                predicted < -0.003 -> Mood.BEARISH
                else -> Mood.CALM
            }
            val pool = when (mood) {
                Mood.BULLISH -> bullishHeadlines
                Mood.BEARISH -> bearishHeadlines
                Mood.VOLATILE -> volatileHeadlines
                Mood.CALM -> calmHeadlines
            }
            headline = pool.random().format(assetDisplayName)
            delay(7000)
        }
    }

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
