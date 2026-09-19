package ir.amir.triedgame.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.amir.triedgame.ads.AdManager
import ir.amir.triedgame.billing.BillingManager
import ir.amir.triedgame.ui.GameViewModel
import ir.amir.triedgame.ui.theme.TsAccent
import ir.amir.triedgame.ui.theme.TsGold
import ir.amir.triedgame.ui.theme.TsGreen
import kotlinx.coroutines.delay

@Composable
fun ChargeAccountScreen(
    viewModel: GameViewModel,
    adManager: AdManager?,
    billingManager: BillingManager?
) {
    var remainingAds by remember { mutableIntStateOf(adManager?.remainingToday() ?: 0) }
    var cooldownSeconds by remember { mutableIntStateOf(0) }

    LaunchedEffect(adManager) {
        while (true) {
            remainingAds = adManager?.remainingToday() ?: 0
            cooldownSeconds = ((adManager?.cooldownRemainingMillis() ?: 0L) / 1000L).toInt()
            delay(1000)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text("شارژ حساب", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        // Rewarded ad card
        GradientCard(colors = listOf(Color(0xFF1E3A2E), Color(0xFF15251C))) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(icon = Icons.Filled.PlayCircle, tint = TsGreen)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("دیدن تبلیغ برای دریافت پول", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Text("هر بار: ${AdManager.REWARD_TOMAN.toInt()} تومان  •  باقی‌مانده امروز: $remainingAds از ${AdManager.DAILY_LIMIT}", style = MaterialTheme.typography.bodySmall)
                    if (cooldownSeconds > 0) {
                        Text("تبلیغ بعدی تا ${cooldownSeconds}s دیگر", style = MaterialTheme.typography.bodySmall, color = TsGold)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    adManager?.showRewardedAd { rewarded ->
                        if (rewarded) viewModel.creditToman(AdManager.REWARD_TOMAN)
                    }
                },
                enabled = adManager?.canShowNow() == true,
                colors = ButtonDefaults.buttonColors(containerColor = TsGreen),
                modifier = Modifier.fillMaxWidth()
            ) { Text("دیدن تبلیغ", color = Color.Black, fontWeight = FontWeight.Bold) }
        }

        Spacer(Modifier.height(20.dp))
        Text("خرید بسته‌ی شارژ (مایکت)", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))

        BillingManager.SKUS.forEach { sku ->
            val usd = BillingManager.USD_REWARD[sku] ?: 0.0
            val toman = BillingManager.TOMAN_PRICE[sku] ?: 0L
            val isSpecial = BillingManager.SPECIAL_OFFER_SKUS.contains(sku)
            Spacer(Modifier.height(4.dp))
            GradientCard(
                colors = if (isSpecial) listOf(Color(0xFF4A3B12), Color(0xFF2A2110)) else listOf(Color(0xFF2A2440), Color(0xFF1C1830))
            ) {
                if (isSpecial) {
                    Surface(color = TsGold, shape = RoundedCornerShape(6.dp)) {
                        Text(
                            "⭐ پیشنهاد ویژه",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(icon = Icons.Filled.AttachMoney, tint = if (isSpecial) TsGold else TsAccent)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${usd.toInt()}$ به کیف پول", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("قیمت: ${toman} تومان", style = MaterialTheme.typography.bodySmall)
                    }
                    Button(
                        onClick = { billingManager?.purchase(sku) },
                        colors = ButtonDefaults.buttonColors(containerColor = TsAccent)
                    ) { Text("خرید") }
                }
            }
        }
    }
}

@Composable
private fun GradientCard(colors: List<Color>, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier
                .background(Brush.horizontalGradient(colors))
                .padding(16.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun IconBadge(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
    }
}
