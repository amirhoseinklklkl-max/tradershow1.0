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
import ir.amir.triedgame.ui.theme.TsTextSecondary
import kotlinx.coroutines.delay

private val headlineTemplates = listOf(
    "حجم معاملات %s ناگهان افزایش یافت 📈",
    "تحلیل‌گران بازار چشم‌انداز %s رو زیر نظر دارن",
    "نوسان بازار %s در ساعات اخیر بیشتر شده",
    "خبر: علاقه‌ی سرمایه‌گذاران به %s رو به افزایشه",
    "بازار امروز آروم‌تر از دیروز به نظر می‌رسه",
    "برخی تریدرها روی %s موقعیت‌های بزرگ باز کردن",
    "هشدار نوسان: %s ممکنه حرکت شدیدی داشته باشه"
)

/** A one-line rotating headline, purely cosmetic narrative flavor -- no
 * effect on the actual (already-simulated) price engine. */
@Composable
fun NewsTicker(assetDisplayName: String, modifier: Modifier = Modifier) {
    var headline by remember { mutableStateOf(headlineTemplates.first().format(assetDisplayName)) }

    LaunchedEffect(assetDisplayName) {
        while (true) {
            headline = headlineTemplates.random().let {
                if (it.contains("%s")) it.format(assetDisplayName) else it
            }
            delay(9000)
        }
    }

    Text(
        "📰 $headline",
        style = MaterialTheme.typography.labelSmall,
        color = TsTextSecondary,
        modifier = modifier.padding(vertical = 2.dp)
    )
}
