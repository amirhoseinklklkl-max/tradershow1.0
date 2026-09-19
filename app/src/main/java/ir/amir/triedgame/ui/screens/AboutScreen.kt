package ir.amir.triedgame.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.amir.triedgame.ui.theme.TsAccent
import ir.amir.triedgame.ui.theme.TsSurfaceElevated

private const val RUBIKA_CHANNEL_URL = "https://rubika.ir/Amirhosin_studio"

@Composable
fun AboutScreen() {
    val uriHandler = LocalUriHandler.current

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("درباره ما", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        Text(
            "تریدر شو یک بازی شبیه‌سازی ترید است که تجربه‌ی واقعی بازارهای مالی رو، " +
                "بدون هیچ ریسک مالی واقعی، در اختیارت می‌ذاره. از تحلیل چارت کندل‌استیک و " +
                "باز کردن پوزیشن روی ده‌ها ارز واقعی و اختصاصی، تا مدیریت زندگی روزمره‌ت با " +
                "پولی که از ترید به دست میاری — همه‌چیز دقیقاً شبیه دنیای واقعیه، فقط سرمایه‌ش مجازیه.",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "هدف ما ساختن یه محیط آموزشی و سرگرم‌کننده‌ست تا با مفاهیم واقعی ترید مثل " +
                "لانگ/شورت، اهرم، لیکویید شدن، مدیریت سرمایه و نوسانات بازار آشنا بشی — بدون " +
                "اینکه پول واقعیت رو به خطر بندازی. تریدر شو همیشه به‌روزرسانی می‌شه و ویژگی‌های " +
                "جدید بر اساس بازخورد بازیکن‌ها بهش اضافه می‌شه.",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(Modifier.height(20.dp))
        Surface(
            color = TsSurfaceElevated,
            shape = RoundedCornerShape(16.dp),
            onClick = { uriHandler.openUri(RUBIKA_CHANNEL_URL) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Send, contentDescription = null, tint = TsAccent)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("کانال روبیکا", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("اخبار و آپدیت‌های بازی رو اینجا دنبال کن", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("راهنمای سریع بازی", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "۱. از تب «ترید» یه ارز رو انتخاب کن، مقدار سرمایه و حالت اسپات یا فیوچرز رو مشخص کن و پوزیشن Long یا Short باز کن — می‌تونی حد ضرر و حد سود هم تعیین کنی تا خودکار بسته بشه.\n" +
                "۲. مراقب نوارهای سلامتی، گرسنگی و انرژی‌ت باش — توی تب «زندگی من» می‌تونی غذا بخری و استراحت کنی.\n" +
                "۳. هر روز سر بزن به تب «چالش‌ها» تا پاداش تومانی و تجربه (XP) بگیری و دستاوردهای دائمی‌ت رو ببینی.\n" +
                "۴. توی تب «تاریخچه» می‌تونی نرخ برد و سود/ضرر کل معاملات گذشته‌ت رو بررسی کنی.\n" +
                "۵. اگه سرمایه‌ت کم شد، از تب «شارژ حساب» با دیدن تبلیغ یا خرید بسته، کیف پولت رو شارژ کن.\n" +
                "۶. با کسب تجربه لول بالا می‌ری و روند خالص دارایی‌ت رو توی تب «حساب کاربری» می‌تونی دنبال کنی.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
