package com.fileintelligence.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fileintelligence.data.GrowthMetric
import com.fileintelligence.ui.theme.BrandHighlight
import com.fileintelligence.ui.theme.BrandPrimary
import com.fileintelligence.ui.theme.BrandSuccess
import com.fileintelligence.ui.theme.bgCard
import com.fileintelligence.ui.theme.textSecondary

@Composable
fun GrowthCard(
    metrics: List<GrowthMetric>,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgCard),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "知识成长曲线",
                    color = textSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "+127%",
                    color = BrandSuccess,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
            Spacer(Modifier.height(12.dp))
            // Mini chart area
            val maxVal = metrics.maxOf { it.filesAnalyzed }.coerceAtLeast(1)
            Row(
                modifier = Modifier.fillMaxWidth().height(60.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                metrics.forEach { m ->
                    val fraction = m.filesAnalyzed.toFloat() / maxVal
                    val heightPx = (fraction * 56).coerceAtLeast(4f)
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .height(heightPx.dp)
                            .clip(RoundedCornerShape(2.dp, 2.dp, 4.dp, 4.dp))
                            .background(
                                if (m == metrics.last()) BrandPrimary
                                else BrandPrimary.copy(alpha = 0.4f)
                            ),
                    )
                }
            }
        }
    }
}

@Composable
fun StatsRow(
    stats: List<StatsItem>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        stats.forEach { stat ->
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = bgCard),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(stat.iconBg),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(stat.icon, fontSize = MaterialTheme.typography.bodyLarge.fontSize)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stat.value,
                        fontWeight = FontWeight.Bold,
                        fontSize = MaterialTheme.typography.titleLarge.fontSize,
                    )
                    Text(
                        text = stat.label,
                        color = textSecondary,
                        fontSize = MaterialTheme.typography.labelSmall.fontSize,
                    )
                }
            }
        }
    }
}

data class StatsItem(
    val icon: String,
    val iconBg: Color,
    val value: String,
    val label: String,
)
