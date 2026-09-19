package ir.amir.triedgame.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.amir.triedgame.ui.AchievementUiState
import ir.amir.triedgame.ui.ChallengeUiState
import ir.amir.triedgame.ui.GameViewModel
import ir.amir.triedgame.ui.theme.TsAccent
import ir.amir.triedgame.ui.theme.TsGold
import ir.amir.triedgame.ui.theme.TsGreen
import ir.amir.triedgame.ui.theme.TsSurfaceElevated
import ir.amir.triedgame.ui.theme.TsTextSecondary

private enum class ChallengesTab { DAILY, ACHIEVEMENTS }

@Composable
fun ChallengesScreen(viewModel: GameViewModel) {
    var tab by remember { mutableStateOf(ChallengesTab.DAILY) }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 14.dp)) {
        Text("چالش‌ها و دستاوردها", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))

        Row {
            TabChip("چالش‌های امروز", tab == ChallengesTab.DAILY, Modifier.weight(1f)) { tab = ChallengesTab.DAILY }
            Spacer(Modifier.width(8.dp))
            TabChip("دستاوردها", tab == ChallengesTab.ACHIEVEMENTS, Modifier.weight(1f)) { tab = ChallengesTab.ACHIEVEMENTS }
        }
        Spacer(Modifier.height(16.dp))

        when (tab) {
            ChallengesTab.DAILY -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(viewModel.challenges) { state ->
                    ChallengeCard(state, onClaim = { viewModel.claimChallenge(state.challenge.id) })
                }
            }
            ChallengesTab.ACHIEVEMENTS -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(viewModel.achievements) { state -> AchievementCard(state) }
            }
        }
    }
}

@Composable
private fun TabChip(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        color = if (selected) TsAccent else TsSurfaceElevated,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Text(
            text,
            modifier = Modifier.padding(vertical = 10.dp).fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ChallengeCard(state: ChallengeUiState, onClaim: () -> Unit) {
    Surface(
        color = TsSurfaceElevated,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(state.challenge.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))

            val progressFraction = (state.progress.toFloat() / state.challenge.target.toFloat()).coerceIn(0f, 1f)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(androidx.compose.ui.graphics.Color(0xFF2A3040))
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progressFraction)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (state.isComplete) TsGreen else TsAccent)
                )
            }
            Spacer(Modifier.height(6.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${state.progress}/${state.challenge.target} • ${state.challenge.rewardToman.toInt()} تومان",
                    style = MaterialTheme.typography.bodySmall
                )
                when {
                    state.isClaimed -> Text("دریافت شد ✓", color = TsGreen, style = MaterialTheme.typography.labelSmall)
                    state.isComplete -> Button(onClick = onClaim, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) {
                        Text("دریافت", style = MaterialTheme.typography.labelSmall)
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun AchievementCard(state: AchievementUiState) {
    Surface(
        color = if (state.unlocked) TsSurfaceElevated else TsSurfaceElevated.copy(alpha = 0.5f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (state.unlocked) Icons.Filled.EmojiEvents else Icons.Filled.Lock,
                contentDescription = null,
                tint = if (state.unlocked) TsGold else TsTextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    state.achievement.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (state.unlocked) MaterialTheme.colorScheme.onSurface else TsTextSecondary
                )
                Text(state.achievement.description, style = MaterialTheme.typography.bodySmall)
            }
            if (state.unlocked) {
                Text("✓", color = TsGreen, fontWeight = FontWeight.Bold)
            }
        }
    }
}
