package ir.amir.triedgame.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.amir.triedgame.model.AssetCatalog
import ir.amir.triedgame.model.Position
import ir.amir.triedgame.model.PositionSide
import ir.amir.triedgame.ui.GameViewModel
import ir.amir.triedgame.ui.Timeframe
import ir.amir.triedgame.ui.components.CandlestickChart
import ir.amir.triedgame.ui.components.NewsTicker
import ir.amir.triedgame.ui.theme.TsAccent
import ir.amir.triedgame.ui.theme.TsGreen
import ir.amir.triedgame.ui.theme.TsRed
import ir.amir.triedgame.ui.theme.TsSurfaceElevated
import java.util.Locale

private enum class TradingMode { SPOT, FUTURES }

@Composable
fun TradingScreen(viewModel: GameViewModel) {
    val price = viewModel.currentPrices[viewModel.selectedAsset.symbol] ?: viewModel.selectedAsset.startingPriceUsd
    var customAmountText by remember { mutableStateOf("") }
    var selectedPreset by remember { mutableStateOf(10.0) }
    var leverage by remember { mutableIntStateOf(1) }
    var mode by remember { mutableStateOf(TradingMode.SPOT) }
    var stopLossText by remember { mutableStateOf("") }
    var takeProfitText by remember { mutableStateOf("") }

    // Spot trading never uses leverage -- it's a plain 1x position with no
    // liquidation risk beyond losing the capital you put in.
    val effectiveLeverage = if (mode == TradingMode.SPOT) 1 else leverage
    val tradeAmount = customAmountText.toDoubleOrNull() ?: selectedPreset

    // The chart's gesture handler is direction-aware (see CandlestickChart),
    // so wrapping the middle column in vertical scroll below is safe and
    // doesn't fight pinch-zoom / horizontal pan on the chart.
    Row(Modifier.fillMaxSize()) {
        // Asset list
        LazyColumn(
            modifier = Modifier
                .width(140.dp)
                .fillMaxHeight()
                .background(TsSurfaceElevated)
                .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(AssetCatalog.all) { asset ->
                val isSelected = asset.symbol == viewModel.selectedAsset.symbol
                val assetPrice = viewModel.currentPrices[asset.symbol] ?: asset.startingPriceUsd
                Surface(
                    color = if (isSelected) TsAccent.copy(alpha = 0.18f) else androidx.compose.ui.graphics.Color.Transparent,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectAsset(asset) }
                ) {
                    Column(Modifier.padding(8.dp)) {
                        Text(asset.symbol, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = if (isSelected) TsAccent else MaterialTheme.colorScheme.onSurface)
                        Text(formatPrice(assetPrice), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // Middle: chart + controls. Scrollable (per user request) -- the
        // chart's gesture handler only claims horizontal drags/pinches, so a
        // vertical drag anywhere (including over the chart) scrolls this
        // column normally instead of fighting the chart.
        Column(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "${viewModel.selectedAsset.displayName} (${viewModel.selectedAsset.symbol})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(formatPrice(price), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    NewsTicker(assetDisplayName = viewModel.selectedAsset.displayName)
                }
                Row {
                    Timeframe.entries.forEach { tf ->
                        val selected = tf == viewModel.timeframe
                        Surface(
                            color = if (selected) TsAccent else TsSurfaceElevated,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(horizontal = 2.dp).clickable { viewModel.selectTimeframe(tf) }
                        ) {
                            Text(
                                tf.label,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))

            CandlestickChart(
                candles = viewModel.candles,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Spacer(Modifier.height(8.dp))

            // Spot / Futures mode toggle
            Row {
                ModeChip("اسپات", selected = mode == TradingMode.SPOT, modifier = Modifier.weight(1f)) { mode = TradingMode.SPOT }
                Spacer(Modifier.width(6.dp))
                ModeChip("فیوچرز (تعهدی)", selected = mode == TradingMode.FUTURES, modifier = Modifier.weight(1f)) { mode = TradingMode.FUTURES }
            }
            Spacer(Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                listOf(10.0, 50.0, 100.0).forEach { amt ->
                    val selected = customAmountText.isEmpty() && selectedPreset == amt
                    Surface(
                        color = if (selected) TsAccent else TsSurfaceElevated,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 5.dp).clickable { selectedPreset = amt; customAmountText = "" }
                    ) {
                        Text(
                            "$${amt.toInt()}",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (selected) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                OutlinedTextField(
                    value = customAmountText,
                    onValueChange = { customAmountText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("دلخواه", style = MaterialTheme.typography.labelSmall) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(100.dp).height(52.dp)
                )
                Spacer(Modifier.weight(1f))
                Text("موجودی: ${String.format(Locale.US, "%.2f", viewModel.wallet.usdBalance)}$", style = MaterialTheme.typography.bodySmall)
            }

            if (mode == TradingMode.FUTURES) {
                Spacer(Modifier.height(6.dp))
                Row {
                    listOf(1, 5, 10, 20).forEach { lev ->
                        val selected = leverage == lev
                        Surface(
                            color = if (selected) TsAccent else TsSurfaceElevated,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(end = 5.dp).clickable { leverage = lev }
                        ) {
                            Text(
                                "${lev}x",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (selected) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                if (leverage > 1) {
                    Text(
                        "حجم معامله: ${(tradeAmount * leverage).toInt()}$ — اگه ضرر به مارجین برسه، لیکویید می‌شی.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TsRed
                    )
                }
            } else {
                Spacer(Modifier.height(6.dp))
                Text("اسپات: بدون اهرم، ریسک فقط به اندازه‌ی همون سرمایه‌ی واردشده", style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = stopLossText,
                    onValueChange = { stopLossText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("حد ضرر (اختیاری)", style = MaterialTheme.typography.labelSmall) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f).height(52.dp)
                )
                Spacer(Modifier.width(6.dp))
                OutlinedTextField(
                    value = takeProfitText,
                    onValueChange = { takeProfitText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("حد سود (اختیاری)", style = MaterialTheme.typography.labelSmall) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f).height(52.dp)
                )
            }

            Spacer(Modifier.height(8.dp))
            val blockReason = viewModel.tradingBlockReason()
            if (blockReason != null) {
                Text(
                    "❗ $blockReason — برو زندگی من و رسیدگی کن، الان نمی‌تونی ترید کنی",
                    style = MaterialTheme.typography.bodySmall,
                    color = TsRed
                )
                Spacer(Modifier.height(6.dp))
            }
            Row(Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        val opened = viewModel.openPosition(
                            PositionSide.LONG, tradeAmount, effectiveLeverage,
                            stopLossText.toDoubleOrNull(), takeProfitText.toDoubleOrNull()
                        )
                        if (opened) { stopLossText = ""; takeProfitText = "" }
                    },
                    enabled = blockReason == null,
                    colors = ButtonDefaults.buttonColors(containerColor = TsGreen),
                    modifier = Modifier.weight(1f)
                ) { Text("خرید / Long", color = androidx.compose.ui.graphics.Color.Black, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        val opened = viewModel.openPosition(
                            PositionSide.SHORT, tradeAmount, effectiveLeverage,
                            stopLossText.toDoubleOrNull(), takeProfitText.toDoubleOrNull()
                        )
                        if (opened) { stopLossText = ""; takeProfitText = "" }
                    },
                    enabled = blockReason == null,
                    colors = ButtonDefaults.buttonColors(containerColor = TsRed),
                    modifier = Modifier.weight(1f)
                ) { Text("فروش / Short", fontWeight = FontWeight.Bold) }
            }
        }

        // Open positions
        Column(
            modifier = Modifier.width(220.dp).fillMaxHeight().background(TsSurfaceElevated).padding(8.dp)
        ) {
            Text("پوزیشن‌های باز", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(viewModel.positions) { position ->
                    PositionRow(position, viewModel)
                }
            }
        }
    }
}

@Composable
private fun ModeChip(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        color = if (selected) TsAccent else TsSurfaceElevated,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Text(
            text,
            modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun PositionRow(position: Position, viewModel: GameViewModel) {
    val price = viewModel.currentPrices[position.assetSymbol] ?: position.entryPrice
    val pnl = position.currentPnlUsd(price)
    val pnlPercent = position.pnlPercentOfMargin(price)
    val remaining = position.currentValueUsd(price)
    val color = if (pnl >= 0) TsGreen else TsRed

    Surface(color = androidx.compose.ui.graphics.Color(0xFF232A3A), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${position.assetSymbol} • ${if (position.side == PositionSide.LONG) "Long" else "Short"}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                if (position.leverage > 1) {
                    Text("${position.leverage}x", color = TsAccent, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text("سرمایه‌ی واردشده: ${String.format(Locale.US, "%.2f$", position.marginUsd)}", style = MaterialTheme.typography.bodySmall)
            Text("باقی‌مانده: ${String.format(Locale.US, "%.2f$", remaining)}", style = MaterialTheme.typography.bodySmall)
            if (position.stopLossPrice != null || position.takeProfitPrice != null) {
                Text(
                    listOfNotNull(
                        position.stopLossPrice?.let { "SL: ${formatPrice(it)}" },
                        position.takeProfitPrice?.let { "TP: ${formatPrice(it)}" }
                    ).joinToString("  "),
                    style = MaterialTheme.typography.labelSmall,
                    color = TsAccent
                )
            }
            Text(
                String.format(Locale.US, "%+.2f$ (%+.1f%%)", pnl, pnlPercent),
                color = color,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )
            TextButton(onClick = { viewModel.closePosition(position) }, contentPadding = PaddingValues(0.dp)) {
                Text("بستن پوزیشن", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

private fun formatPrice(price: Double): String {
    val decimals = if (price < 1) 4 else 2
    return String.format(Locale.US, "%.${decimals}f $", price)
}
