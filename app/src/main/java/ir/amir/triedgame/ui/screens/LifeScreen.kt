package ir.amir.triedgame.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.amir.triedgame.model.LifeStats
import ir.amir.triedgame.ui.GameViewModel
import ir.amir.triedgame.ui.theme.TsAccent
import ir.amir.triedgame.ui.theme.TsGold
import ir.amir.triedgame.ui.theme.TsGreen
import ir.amir.triedgame.ui.theme.TsRed
import ir.amir.triedgame.ui.theme.TsSurfaceElevated
import ir.amir.triedgame.ui.theme.statColor

private enum class LifeItemGroup(val icon: ImageVector, val tint: androidx.compose.ui.graphics.Color) {
    FOOD(Icons.Filled.Restaurant, TsGold),
    ENERGY(Icons.Filled.LocalCafe, TsAccent),
    HEALTH(Icons.Filled.Favorite, TsRed),
    ASSET(Icons.Filled.DirectionsCar, TsGreen)
}

private data class LifeItem(
    val title: String,
    val priceToman: Double,
    val xpReward: Int,
    val isHungerItem: Boolean,
    val group: LifeItemGroup,
    val effect: (LifeStats) -> LifeStats,
    val description: String
)

private val lifeItems = listOf(
    LifeItem("قهوه", 30_000.0, 5, false, LifeItemGroup.ENERGY, { it.copy(energy = it.energy + 15) }, "انرژی +۱۵"),
    LifeItem("چای", 15_000.0, 3, false, LifeItemGroup.ENERGY, { it.copy(energy = it.energy + 8) }, "انرژی +۸"),
    LifeItem("انرژی‌زا", 45_000.0, 6, false, LifeItemGroup.ENERGY, { it.copy(energy = it.energy + 25) }, "انرژی +۲۵"),
    LifeItem("ساندویچ فست‌فود", 80_000.0, 10, true, LifeItemGroup.FOOD, { it.copy(hunger = it.hunger + 30) }, "گرسنگی +۳۰"),
    LifeItem("پیتزا", 150_000.0, 15, true, LifeItemGroup.FOOD, { it.copy(hunger = it.hunger + 45) }, "گرسنگی +۴۵"),
    LifeItem("غذای رستوران", 250_000.0, 20, true, LifeItemGroup.FOOD, { it.copy(hunger = it.hunger + 60, health = it.health + 5) }, "گرسنگی +۶۰، سلامتی +۵"),
    LifeItem("میان‌وعده سالم", 60_000.0, 8, true, LifeItemGroup.FOOD, { it.copy(hunger = it.hunger + 20, health = it.health + 5) }, "گرسنگی +۲۰، سلامتی +۵"),
    LifeItem("میوه", 25_000.0, 4, true, LifeItemGroup.FOOD, { it.copy(hunger = it.hunger + 12, health = it.health + 3) }, "گرسنگی +۱۲، سلامتی +۳"),
    LifeItem("استراحت کوتاه", 50_000.0, 5, false, LifeItemGroup.ENERGY, { it.copy(energy = it.energy + 20) }, "انرژی +۲۰"),
    LifeItem("چرت بعدازظهر", 90_000.0, 8, false, LifeItemGroup.ENERGY, { it.copy(energy = it.energy + 35) }, "انرژی +۳۵"),
    LifeItem("شب کامل خواب (هتل)", 300_000.0, 20, false, LifeItemGroup.ENERGY, { it.copy(energy = 100, health = it.health + 10) }, "انرژی پر + سلامتی +۱۰"),
    LifeItem("ویزیت دکتر", 400_000.0, 25, false, LifeItemGroup.HEALTH, { it.copy(health = it.health + 40) }, "سلامتی +۴۰"),
    LifeItem("مکمل ویتامین", 120_000.0, 12, false, LifeItemGroup.HEALTH, { it.copy(health = it.health + 15) }, "سلامتی +۱۵"),
    LifeItem("باشگاه ورزشی", 200_000.0, 18, false, LifeItemGroup.HEALTH, { it.copy(health = it.health + 20, energy = it.energy - 10) }, "سلامتی +۲۰، انرژی -۱۰"),
    LifeItem("سفر داخلی", 3_000_000.0, 100, false, LifeItemGroup.ASSET, { it }, "افزایش تجربه + تزئینی"),
    LifeItem("سفر خارجی", 15_000_000.0, 400, false, LifeItemGroup.ASSET, { it }, "افزایش تجربه‌ی زیاد + تزئینی"),
    LifeItem("موتورسیکلت", 60_000_000.0, 200, false, LifeItemGroup.ASSET, { it }, "دارایی تزئینی"),
    LifeItem("ماشین اقتصادی", 200_000_000.0, 500, false, LifeItemGroup.ASSET, { it }, "دارایی تزئینی، تجربه‌ی زیاد"),
    LifeItem("ماشین لوکس", 1_500_000_000.0, 2000, false, LifeItemGroup.ASSET, { it }, "دارایی تزئینی، تجربه‌ی خیلی زیاد"),
    LifeItem("آپارتمان کوچک", 500_000_000.0, 1000, false, LifeItemGroup.ASSET, { it }, "دارایی تزئینی، تجربه‌ی زیاد"),
    LifeItem("آب‌معدنی", 10_000.0, 2, true, LifeItemGroup.FOOD, { it.copy(hunger = it.hunger + 5, energy = it.energy + 5) }, "گرسنگی +۵، انرژی +۵"),
    LifeItem("اسموتی پروتئینی", 70_000.0, 9, true, LifeItemGroup.FOOD, { it.copy(hunger = it.hunger + 25, health = it.health + 5) }, "گرسنگی +۲۵، سلامتی +۵"),
    LifeItem("ماساژ", 180_000.0, 15, false, LifeItemGroup.HEALTH, { it.copy(energy = it.energy + 20, health = it.health + 10) }, "انرژی +۲۰، سلامتی +۱۰"),
    LifeItem("چکاپ کامل پزشکی", 600_000.0, 35, false, LifeItemGroup.HEALTH, { it.copy(health = 100) }, "سلامتی پر"),
    LifeItem("ساعت هوشمند", 90_000_000.0, 300, false, LifeItemGroup.ASSET, { it }, "دارایی تزئینی"),
    LifeItem("قایق تفریحی", 3_000_000_000.0, 5000, false, LifeItemGroup.ASSET, { it }, "دارایی تزئینی، تجربه‌ی بسیار زیاد")
)

@Composable
fun LifeScreen(viewModel: GameViewModel) {
    val stats = viewModel.lifeStats
    var message by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("زندگی من", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))

        // Stats in one thin horizontal row: سلامتی، گرسنگی، انرژی -- takes far
        // less vertical space than the old stacked layout, leaving the shop
        // room to breathe.
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniStatChip("سلامتی", stats.health, Modifier.weight(1f))
            MiniStatChip("گرسنگی", stats.hunger, Modifier.weight(1f))
            MiniStatChip("انرژی", stats.energy, Modifier.weight(1f))
        }

        viewModel.pendingLifeEvent?.let { event ->
            Spacer(Modifier.height(8.dp))
            LifeEventCard(
                event,
                onResolveExpense = { viewModel.resolveLifeEventExpense() },
                onDismissBonus = { viewModel.dismissLifeEvent() }
            )
        }
        message?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(12.dp))
        Text("مغازه", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(lifeItems) { item ->
                Surface(color = TsSurfaceElevated, shape = RoundedCornerShape(14.dp), shadowElevation = 2.dp) {
                    Column(Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(item.group.icon, contentDescription = null, tint = item.group.tint, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        }
                        Text(item.description, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(4.dp))
                        Text(String.format("%,.0f تومان", item.priceToman), style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(6.dp))
                        Button(
                            onClick = {
                                val ok = viewModel.spendTomanInLife(item.priceToman, item.xpReward, item.isHungerItem, item.effect)
                                message = if (ok) null else "موجودی تومانی کافی نیست"
                            },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) { Text("خرید", style = MaterialTheme.typography.labelMedium) }
                    }
                }
            }
        }
    }
}

@Composable
private fun LifeEventCard(
    event: ir.amir.triedgame.model.LifeEvent,
    onResolveExpense: () -> Unit,
    onDismissBonus: () -> Unit
) {
    val isBonus = event.kind == ir.amir.triedgame.model.LifeEventKind.BONUS
    val color = if (isBonus) TsGreen else TsRed
    Surface(color = TsSurfaceElevated, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(event.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text(
                    event.description + "  " + (if (isBonus) "+" else "-") + String.format("%,.0f تومان", event.amountToman),
                    color = color,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (isBonus) {
                TextButton(onClick = onDismissBonus) { Text("باشه") }
            } else {
                Button(onClick = onResolveExpense, colors = ButtonDefaults.buttonColors(containerColor = TsRed)) {
                    Text("پرداخت و رفع مشکل", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun MiniStatChip(label: String, value: Int, modifier: Modifier = Modifier) {
    val color = statColor(value)
    Surface(color = TsSurfaceElevated, shape = RoundedCornerShape(10.dp), modifier = modifier) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(label, style = MaterialTheme.typography.labelMedium)
                Text("$value%", style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(4.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(androidx.compose.ui.graphics.Color(0xFF2A3040))
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth((value / 100f).coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(3.dp))
                        .background(color)
                )
            }
        }
    }
}
