package com.qzone.feature.rewards.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.qzone.data.model.Reward
import com.qzone.feature.rewards.RewardsUiState
import com.qzone.ui.components.QzoneElevatedSurface
import com.qzone.ui.components.QzoneTag
import com.qzone.ui.components.qzoneScreenBackground
import kotlinx.coroutines.flow.StateFlow

import androidx.compose.ui.graphics.Color

@Composable
fun RewardsScreen(
    state: StateFlow<RewardsUiState>,
    onRewardSelected: (String) -> Unit,
    onRedeem: (Reward) -> Unit
) {
    val uiState by state.collectAsState()
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    var rewardToRedeem by remember { mutableStateOf<Reward?>(null) }

    if (rewardToRedeem != null) {
        AlertDialog(
            onDismissRequest = { rewardToRedeem = null },
            title = { Text("Redeem Reward") },
            text = { Text("Are you sure you want to redeem ${rewardToRedeem?.brandName} for ${rewardToRedeem?.pointsCost} points?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        rewardToRedeem?.let { onRedeem(it) }
                        rewardToRedeem = null
                    }
                ) {
                    Text("Redeem")
                }
            },
            dismissButton = {
                TextButton(onClick = { rewardToRedeem = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .qzoneScreenBackground()
            .padding(horizontal = 24.dp)
            .padding(top = topPadding + 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            items(uiState.rewards, key = { it.id }) { reward ->
                RewardCard(
                    reward = reward,
                    onClick = { onRewardSelected(reward.id) },
                    onRedeemClick = { rewardToRedeem = reward }
                )
            }
        }
    }
}

@Composable
private fun RewardCard(
    reward: Reward,
    onClick: () -> Unit,
    onRedeemClick: () -> Unit
) {
    QzoneElevatedSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val imageModel = reward.imageResId ?: reward.imageUrl
            if (imageModel != null) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = reward.brandName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(Color(0xFFF5F5F5)) // Off-white background
                        .padding(16.dp),
                    contentScale = ContentScale.Fit,
                    error = rememberVectorPainter(Icons.Default.BrokenImage),
                    placeholder = rememberVectorPainter(Icons.Default.Image)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = reward.brandName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = reward.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                QzoneTag(
                    text = "${reward.pointsCost} pts",
                    emphasize = true
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Expires ${reward.expiryDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                QzoneTag(
                    text = "Tap to view details",
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    contentColor = MaterialTheme.colorScheme.primary
                )
                Button(
                    onClick = onRedeemClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Redeem")
                }
            }
        }
    }
}
