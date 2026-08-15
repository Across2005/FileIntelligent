package com.crossk.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crossk.ui.theme.BrandAccent
import com.crossk.ui.theme.BrandHighlight
import com.crossk.ui.theme.BrandPrimary

/**
 * A time-travel slider for the knowledge graph.
 * Allows the user to scrub through time and see how the graph evolved.
 */
@Composable
fun TimeTravelSlider(
    currentWeek: Int,
    totalWeeks: Int,
    onWeekChange: (Int) -> Unit,
    isActive: Boolean = false,
    onToggleActive: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    // Animated week label
    val animWeek by animateFloatAsState(
        targetValue = currentWeek.toFloat(),
        animationSpec = tween(200),
        label = "weekSlide",
    )

    Column(modifier = modifier) {
        // Toggle button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isActive) "🕰️" else "⏳",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = if (isActive) "时间旅行模式" else "时间旅行",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = if (isActive) BrandHighlight else onSurfaceVariant,
                )
            }
            Switch(
                checked = isActive,
                onCheckedChange = onToggleActive,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = BrandPrimary,
                    checkedTrackColor = BrandPrimary.copy(alpha = 0.3f),
                ),
            )
        }

        if (isActive) {
            Spacer(Modifier.height(8.dp))

            // Slider card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(surfaceVariant)
                    .padding(12.dp),
            ) {
                Column {
                    // Week display
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "第 ${currentWeek + 1} 周",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = BrandPrimary,
                        )
                        Text(
                            text = "${timeTravelPercentage(currentWeek, totalWeeks)}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = onSurfaceVariant.copy(alpha = 0.5f),
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    if (totalWeeks < 2) {
                        // B-TTS-3: no data → don't render the slider (it would crash or degenerate)
                        Text(
                            text = "尚无时间线数据",
                            style = MaterialTheme.typography.labelSmall,
                            color = onSurfaceVariant.copy(alpha = 0.4f),
                        )
                    } else {
                        // Custom slider
                        Slider(
                            value = currentWeek.toFloat(),
                            onValueChange = { onWeekChange(it.toInt().coerceIn(0, totalWeeks - 1)) },
                            valueRange = 0f..(totalWeeks - 1).toFloat(),
                            steps = timeTravelSteps(totalWeeks),
                            colors = SliderDefaults.colors(
                                thumbColor = BrandPrimary,
                                activeTrackColor = BrandPrimary,
                                inactiveTrackColor = BrandPrimary.copy(alpha = 0.15f),
                            ),
                        )
                    }

                    // Timeline markers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("最早", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                            color = onSurfaceVariant.copy(alpha = 0.4f))
                        Text("现在", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                            color = onSurfaceVariant.copy(alpha = 0.4f))
                    }
                }
            }
        }
    }
}

/**
 * Compute the Slider's `steps` parameter safely.
 * Compose's `Slider` throws IllegalArgumentException for `steps < 0`.
 * `steps = totalWeeks - 2` is the gap count between endpoints; coerce to >= 0.
 */
internal fun timeTravelSteps(totalWeeks: Int): Int = (totalWeeks - 2).coerceAtLeast(0)

/**
 * Compute the percent-of-timeline display safely.
 * Avoids divide-by-zero when `totalWeeks == 0` (fresh install, no data).
 */
internal fun timeTravelPercentage(currentWeek: Int, totalWeeks: Int): Int =
    if (totalWeeks <= 0) 0
    else ((currentWeek + 1).toFloat() / totalWeeks * 100).toInt()
