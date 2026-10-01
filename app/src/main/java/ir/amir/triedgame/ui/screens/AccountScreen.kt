package ir.amir.triedgame.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.amir.triedgame.model.USD_TO_TOMAN_RATE
import ir.amir.triedgame.model.xpToReachLevel
import ir.amir.triedgame.ui.GameViewModel
import ir.amir.triedgame.ui.components.NetWorthChart
import ir.amir.triedgame.ui.theme.TsAccent
import ir.amir.triedgame.ui.theme.TsGold
import ir.amir.triedgame.ui.theme.TsGreen
import ir.amir.triedgame.ui.theme.TsSurfaceElevated
import java.util.Locale

@Composable
fun AccountScreen(viewModel: GameViewModel) {
    val profile = viewModel.profile
    val wallet = viewModel.wallet
    var convertAmount by remember { mutableStateOf("") }
    var convertingUsd by remember { mutableStateOf(true) } // true = USD->Toman, false = Toman->USD

    Row(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        // Left column: profile + level
        Column(Modifier.weight(1f)) {
            Text("حساب کاربری", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))

            if (profile != null) {
                ProfileCard(
                    fullName = "${profile.firstName} ${profile.lastName}".trim(),
                    level = profile.level,
                    xp = profile.xp
                )
            }

            Spacer(Modifier.height(20.dp))
            Text("روند خالص دارایی", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Surface(color = TsSurfaceElevated, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                NetWorthChart(
                    points = viewModel.netWorthHistory,
                    modifier = Modifier.fillMaxWidth().height(140.dp).padding(12.dp)
                )
            }

            Spacer(Modifier.height(20.dp))
            Text("سطح زندگی (${viewModel.lifeLevelLabel})", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            if (viewModel.ownedAssets.isEmpty()) {
                Text(
                    "هنوز هیچ دارایی‌ای از «زندگی من» نخریدی",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                // Its own small scroll region so a long list of owned assets
                // doesn't take over the whole page -- nests fine inside the
                // screen's outer scroll (inner list scrolls first).
                Column(
                    Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    viewModel.ownedAssets.forEach { asset ->
                        OwnedAssetRow(asset, onSell = { viewModel.sellLifeAsset(asset.id) })
                    }
                }
            }
        }

        Spacer(Modifier.width(20.dp))

        // Right column: wallet -- always rendered inside fixed-height cards so it
        // can never silently disappear off-screen in landscape mode.
        Column(Modifier.weight(1f)) {
            Text("کیف پول", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))

            BalanceCard(label = "موجودی دلاری (سرمایه‌ی ترید)", value = String.format(Locale.US, "%.2f $", wallet.usdBalance), accent = TsGreen)
            Spacer(Modifier.height(10.dp))
            BalanceCard(label = "موجودی تومانی", value = String.format(Locale.US, "%,.0f تومان", wallet.tomanBalance), accent = TsGold)
            Spacer(Modifier.height(10.dp))
            Text(
                String.format(Locale.US, "نرخ تبدیل ثابت: 1$ = %,.0f تومان", USD_TO_TOMAN_RATE),
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(Modifier.height(20.dp))
            Text("تبدیل ارز", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            // Two clearly-labeled "از" (from) / "به" (to) cards with a swap
            // icon between them -- no arrow glyphs, since an ASCII arrow
            // inside Persian (RTL) text can visually read backwards.
            Row(verticalAlignment = Alignment.CenterVertically) {
                CurrencyBadge(label = "از", currency = if (convertingUsd) "دلار" else "تومان", modifier = Modifier.weight(1f))
                IconButton(onClick = { convertingUsd = !convertingUsd }) {
                    Icon(Icons.Filled.SwapHoriz, contentDescription = "جابه‌جایی جهت تبدیل", tint = TsAccent)
                }
                CurrencyBadge(label = "به", currency = if (convertingUsd) "تومان" else "دلار", modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = convertAmount,
                onValueChange = { convertAmount = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text(if (convertingUsd) "مبلغ به دلار" else "مبلغ به تومان") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = {
                    val amount = convertAmount.toDoubleOrNull()
                    if (amount != null && amount > 0.0) {
                        if (convertingUsd) viewModel.convertUsdToToman(amount) else viewModel.convertTomanToUsd(amount)
                        convertAmount = ""
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("تبدیل کن") }
        }
    }
}

@Composable
private fun CurrencyBadge(label: String, currency: String, modifier: Modifier = Modifier) {
    Surface(color = TsSurfaceElevated, shape = RoundedCornerShape(10.dp), modifier = modifier) {
        Column(Modifier.padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(currency, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ProfileCard(fullName: String, level: Int, xp: Int) {
    val nextLevelXp = xpToReachLevel(level + 1)
    val currentLevelXp = xpToReachLevel(level)
    val span = (nextLevelXp - currentLevelXp).coerceAtLeast(1)
    val progress = ((xp - currentLevelXp).toFloat() / span.toFloat()).coerceIn(0f, 1f)

    Surface(color = TsSurfaceElevated, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Text(fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("لول $level", color = TsAccent, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.weight(1f))
                Text("$xp XP", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = TsAccent
            )
        }
    }
}

@Composable
private fun BalanceCard(label: String, value: String, accent: androidx.compose.ui.graphics.Color) {
    Surface(color = TsSurfaceElevated, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, color = accent, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun OwnedAssetRow(asset: ir.amir.triedgame.model.OwnedLifeAsset, onSell: () -> Unit) {
    Surface(color = TsSurfaceElevated, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(asset.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(asset.tier.label, style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = onSell) {
                Text(String.format(Locale.US, "فروش به %,.0f تومان", asset.sellPriceToman), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
