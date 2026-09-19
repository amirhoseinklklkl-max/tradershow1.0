package ir.amir.triedgame.ui.navigation

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ir.amir.triedgame.ads.AdManager
import ir.amir.triedgame.billing.BillingManager
import ir.amir.triedgame.ui.GameViewModel
import ir.amir.triedgame.ui.screens.AboutScreen
import ir.amir.triedgame.ui.screens.AccountScreen
import ir.amir.triedgame.ui.screens.ChallengesScreen
import ir.amir.triedgame.ui.screens.ChargeAccountScreen
import ir.amir.triedgame.ui.screens.HistoryScreen
import ir.amir.triedgame.ui.screens.LifeScreen
import ir.amir.triedgame.ui.screens.TradingScreen
import ir.amir.triedgame.ui.theme.TsAccent
import ir.amir.triedgame.ui.theme.TsSurface
import ir.amir.triedgame.ui.theme.TsTextSecondary

private sealed class Tab(val route: String, val label: String, val icon: ImageVector) {
    object Trading : Tab("trading", "ترید", Icons.Filled.ShowChart)
    object History : Tab("history", "تاریخچه", Icons.Filled.History)
    object Challenges : Tab("challenges", "چالش‌ها", Icons.Filled.EmojiEvents)
    object Charge : Tab("charge", "شارژ حساب", Icons.Filled.AccountBalanceWallet)
    object Life : Tab("life", "زندگی من", Icons.Filled.SportsEsports)
    object Account : Tab("account", "حساب کاربری", Icons.Filled.Person)
    object About : Tab("about", "درباره ما", Icons.Filled.Info)
}

private val tabs = listOf(Tab.Trading, Tab.History, Tab.Challenges, Tab.Charge, Tab.Life, Tab.Account, Tab.About)

@Composable
fun MainNavGraph(
    viewModel: GameViewModel,
    adManager: AdManager?,
    billingManager: BillingManager?
) {
    val navController = rememberNavController()
    val context = LocalContext.current

    LaunchedEffect(viewModel.levelUpEvent) {
        viewModel.levelUpEvent?.let { level ->
            Toast.makeText(context, "🎉 لول بالا رفت! لول $level", Toast.LENGTH_SHORT).show()
            viewModel.clearLevelUpEvent()
        }
    }

    LaunchedEffect(viewModel.newAchievementEvent) {
        viewModel.newAchievementEvent?.let { achievement ->
            Toast.makeText(context, "🏆 دستاورد جدید: ${achievement.title}", Toast.LENGTH_LONG).show()
            viewModel.clearNewAchievementEvent()
        }
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        bottomBar = { CompactBottomBar(navController) }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Tab.Trading.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Tab.Trading.route) { TradingScreen(viewModel) }
            composable(Tab.History.route) { HistoryScreen(viewModel) }
            composable(Tab.Challenges.route) { ChallengesScreen(viewModel) }
            composable(Tab.Charge.route) { ChargeAccountScreen(viewModel, adManager, billingManager) }
            composable(Tab.Life.route) { LifeScreen(viewModel) }
            composable(Tab.Account.route) { AccountScreen(viewModel) }
            composable(Tab.About.route) { AboutScreen() }
        }
    }
}

/**
 * Slim, modern floating pill bar -- deliberately not the stock Material3
 * NavigationBar (which reserves ~80dp and looks like a generic app). Sits
 * with a small margin from the screen edges, rounded corners, and subtle
 * elevation so it reads as a floating control rather than a flat strip.
 */
@Composable
private fun CompactBottomBar(navController: androidx.navigation.NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination

    Surface(
        color = TsSurface,
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            tabs.forEach { tab ->
                val selected = currentRoute?.hierarchy?.any { it.route == tab.route } == true
                CompactTabItem(tab = tab, selected = selected) {
                    navController.navigate(tab.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactTabItem(tab: Tab, selected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) TsAccent.copy(alpha = 0.16f) else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Icon(
            tab.icon,
            contentDescription = tab.label,
            tint = if (selected) TsAccent else TsTextSecondary,
            modifier = Modifier.size(21.dp)
        )
        Spacer(Modifier.height(3.dp))
        Text(
            tab.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) TsAccent else TsTextSecondary
        )
    }
}
