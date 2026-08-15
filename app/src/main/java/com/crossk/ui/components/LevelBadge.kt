package com.crossk.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crossk.data.KnowledgeLevel
import com.crossk.data.calculateLevel
import com.crossk.ui.theme.BrandAccent
import com.crossk.ui.theme.BrandHighlight
import com.crossk.ui.theme.BrandPrimary

@Composable
fun LevelBadge(
    xp: Int,
    modifier: Modifier = Modifier,
) {
    val level = calculateLevel(xp)
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceVariantColor),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Level badge circle
            LevelCircle(level = level.level)

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Lv.${level.level}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = BrandPrimary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = level.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = BrandHighlight,
                        fontWeight = FontWeight.Medium,
                    )
                }

                Spacer(Modifier.height(4.dp))

                // XP Progress bar
                val progress by animateFloatAsState(
                    targetValue = level.xpProgress,
                    animationSpec = tween(800),
                    label = "xpProgress",
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(onSurfaceVariantColor.copy(alpha = 0.1f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(BrandPrimary, BrandHighlight),
                                ),
                            ),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "${level.xp} XP",
                        style = MaterialTheme.typography.labelSmall,
                        color = BrandPrimary.copy(alpha = 0.7f),
                    )
                    if (!level.isMaxLevel) {
                        Text(
                            text = "${level.xpToNext} XP → Lv.${level.level + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = onSurfaceVariantColor.copy(alpha = 0.5f),
                        )
                    } else {
                        Text(
                            text = "已达到最高等级",
                            style = MaterialTheme.typography.labelSmall,
                            color = BrandHighlight,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LevelCircle(
    level: Int,
    size: Int = 52,
) {
    val primaryColor = BrandPrimary
    val accentColor = BrandAccent
    val fraction = (level.toFloat() / 20f).coerceIn(0f, 1f)

    Box(
        modifier = Modifier.size(size.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Arc progress ring
        Canvas(modifier = Modifier.size(size.dp)) {
            val strokeWidth = 3f
            val arcSize = size.toFloat().coerceAtLeast(1f)

            // Background ring
            drawCircle(
                color = Color.White.copy(alpha = 0.06f),
                radius = arcSize / 2f - strokeWidth,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )

            // Progress arc
            val sweep = fraction * 360f
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(primaryColor, accentColor, BrandHighlight),
                    center = Offset(size.dp.toPx() / 2f, size.dp.toPx() / 2f),
                ),
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                style = Stroke(width = 2.5f, cap = StrokeCap.Round),
                topLeft = Offset(strokeWidth, strokeWidth),
                size = androidx.compose.ui.geometry.Size(
                    arcSize - strokeWidth * 2,
                    arcSize - strokeWidth * 2,
                ),
            )
        }

        // Level number
        Text(
            text = "$level",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = primaryColor,
        )
    }
}
