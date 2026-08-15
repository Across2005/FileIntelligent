package com.crossk.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crossk.data.DailyQuest
import com.crossk.data.generateDailyQuests
import com.crossk.ui.theme.BrandAccent
import com.crossk.ui.theme.BrandHighlight
import com.crossk.ui.theme.BrandPrimary

@Composable
fun DailyQuestPanel(
    quests: List<DailyQuest> = generateDailyQuests(),
    onQuestClick: ((DailyQuest) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    val completedCount = quests.count { it.completed }
    val totalCount = quests.size

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceVariantColor),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "📋 今日知识任务",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = onSurfaceVariantColor,
                )
                Text(
                    text = "$completedCount / $totalCount",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (completedCount == totalCount) BrandAccent
                           else onSurfaceVariantColor.copy(alpha = 0.5f),
                    fontWeight = if (completedCount == totalCount) FontWeight.Bold else FontWeight.Normal,
                )
            }

            Spacer(Modifier.height(10.dp))

            // Quest items
            quests.forEach { quest ->
                QuestRow(
                    quest = quest,
                    onClick = { onQuestClick?.invoke(quest) },
                )
                if (quest != quests.last()) {
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun QuestRow(
    quest: DailyQuest,
    onClick: () -> Unit,
) {
    val completedColor = BrandAccent
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val primaryColor = BrandPrimary

    // Animated colors for completion
    val bgColor by animateColorAsState(
        targetValue = if (quest.completed) completedColor.copy(alpha = 0.08f)
                     else Color.Transparent,
        animationSpec = tween(400),
        label = "questBg",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable(enabled = !quest.completed, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Check circle
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(
                    if (quest.completed) completedColor
                    else primaryColor.copy(alpha = 0.15f),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (quest.completed) {
                Text(
                    text = "✓",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            } else {
                Text(
                    text = "${quest.progress}/${quest.target}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    color = primaryColor.copy(alpha = 0.6f),
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        // Title + description
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = quest.title,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                fontWeight = if (quest.completed) FontWeight.Normal else FontWeight.Medium,
                color = if (quest.completed) textColor.copy(alpha = 0.5f) else textColor,
                textDecoration = if (quest.completed) TextDecoration.LineThrough else TextDecoration.None,
            )
            Text(
                text = quest.description,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = textColor.copy(alpha = 0.4f),
            )
        }

        // XP reward
        Text(
            text = "+${quest.xpReward} XP",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            fontWeight = FontWeight.Bold,
            color = if (quest.completed) completedColor.copy(alpha = 0.6f)
                   else BrandHighlight.copy(alpha = 0.7f),
        )
    }
}
