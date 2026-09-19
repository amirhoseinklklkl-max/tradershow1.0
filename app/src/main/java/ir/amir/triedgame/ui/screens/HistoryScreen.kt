package ir.amir.triedgame.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.amir.triedgame.model.CloseReason
import ir.amir.triedgame.model.ClosedTrade
import ir.amir.triedgame.model.PositionSide
import ir.amir.triedgame.ui.GameViewModel
import ir.amir.triedgame.ui.theme.TsGreen
import ir.amir.triedgame.ui.theme.TsRed
import ir.amir.triedgame.ui.theme.TsSurfaceElevated
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(viewModel: GameViewModel) {
    val trades = viewModel.tradeHistory
    val total = trades.size
    val wins = trades.count { it.pnlUsd > 0 }
    val winRate = if (total > 0) wins * 100.0 / total else 0.0
    val totalPnl = trades.sumOf { it.pnlUsd }

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("تاریخچه‌ی معاملات", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))

        Row(Modifier.fillMaxWidth()) {
            SummaryCard("تعداد معاملات", "$total", Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            SummaryCard("نرخ برد", String.format(Locale.US, "%.0f%%", winRate), Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            SummaryCard(
                "مجموع سود/ضرر",
                String.format(Locale.US, "%+.2f$", totalPnl),
                Modifier.weight(1f),
                valueColor = if (totalPnl >= 0) TsGreen else TsRed
            )
        }
        Spacer(Modifier.height(16.dp))

        if (trades.isEmpty()) {
            Text("هنوز معامله‌ای نبستی — تاریخچه‌ت اینجا نشون داده می‌شه.", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(trades) { trade -> TradeRow(trade) }
            }
        }
    }
}

@Composable
private fun SummaryCard(label: String, value: String, modifier: Modifier = Modifier, valueColor: androidx.compose.ui.graphics.Color? = null) {
    Surface(color = TsSurfaceElevated, shape = RoundedCornerShape(14.dp), modifier = modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = valueColor ?: MaterialTheme.colorScheme.onSurface)
        }
    }
}

private val timeFormat = SimpleDateFormat("MM/dd HH:mm", Locale.US)

@Composable
private fun TradeRow(trade: ClosedTrade) {
    val color = if (trade.pnlUsd >= 0) TsGreen else TsRed
    Surface(color = TsSurfaceElevated, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(
                    "${trade.assetSymbol} • ${if (trade.side == PositionSide.LONG) "Long" else "Short"}${if (trade.leverage > 1) " • ${trade.leverage}x" else ""}",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(reasonLabel(trade.reason) + " • " + timeFormat.format(Date(trade.closedAtMillis)), style = MaterialTheme.typography.bodySmall)
            }
            Text(
                String.format(Locale.US, "%+.2f$", trade.pnlUsd),
                color = color,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall
            )
        }
    }
}

private fun reasonLabel(reason: CloseReason): String = when (reason) {
    CloseReason.MANUAL -> "بسته‌شده دستی"
    CloseReason.LIQUIDATED -> "لیکویید شد"
    CloseReason.STOP_LOSS -> "حد ضرر خورد"
    CloseReason.TAKE_PROFIT -> "حد سود خورد"
}
