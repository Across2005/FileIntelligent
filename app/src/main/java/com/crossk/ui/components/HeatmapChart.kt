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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crossk.data.HeatmapDay
import com.crossk.ui.theme.BrandAccent
import com.crossk.ui.theme.BrandHighlight
import com.crossk.ui.theme.BrandPrimary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * GitHub-style contribution heatmap for reading activity.
 * Shows daily activity intensity over the past N weeks.
 */
@Composable
fun HeatmapChart(
    data: List<HeatmapDay>? = null,
    modifier: Modifier = Modifier,
) {
    val stableData = remember(data) { data ?: emptyList() }
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    // Group by weeks
    val weeks = remember(stableData) { groupIntoWeeks(stableData) }
    val maxCount = remember(stableData) { stableData.maxOfOrNull { it.count } ?: 1 }

    val colors = remember {
        listOf(
            BrandPrimary.copy(alpha = 0.08f),  // level 0 (very light)
            BrandPrimary.copy(alpha = 0.25f),   // level 1
            BrandPrimary.copy(alpha = 0.45f),   // level 2
            BrandPrimary.copy(alpha = 0.55f),   // level 3
            BrandPrimary.copy(alpha = 0.85f),   // level 4
        )
    }

    val monthLabels = remember(stableData) { extractMonthLabels(stableData) }

    // ── Entry animation: ripple reveal from right to left ──
    var entryProgress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(stableData) {
        entryProgress = 0f
        // Animate over ~1.5s with a slight delay per cell
        kotlinx.coroutines.delay(100)
        entryProgress = 1f
    }
    val animatedProgress by animateFloatAsState(
        targetValue = entryProgress,
        animationSpec = tween(durationMillis = 1200, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "heatmapEntry",
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceVariantColor),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with streak info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "📊 阅读热力图",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = onSurfaceVariantColor,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "今日",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = onSurfaceVariantColor.copy(alpha = 0.5f),
                    )
                    Spacer(Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (stableData.lastOrNull()?.count?:0 > 0) colors[getIntensityLevel(stableData.lastOrNull()?.count ?: 0, maxCount)]
                                else colors[0],
                            ),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Month labels
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                var lastMonth = -1
                monthLabels.forEach { (weekIdx, month) ->
                    if (month != lastMonth) {
                        Text(
                            text = monthLabel(month),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                            color = onSurfaceVariantColor.copy(alpha = 0.4f),
                            modifier = Modifier.padding(start = (weekIdx * 12).dp),
                        )
                        lastMonth = month
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // Heatmap grid
            Row(modifier = Modifier.fillMaxWidth()) {
                // Day labels column
                Column(
                    modifier = Modifier.width(24.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    listOf("一", "三", "五").forEach { day ->
                        Text(
                            text = day,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                            color = onSurfaceVariantColor.copy(alpha = 0.3f),
                            modifier = Modifier.height(12.dp),
                        )
                    }
                }

                // Week columns with staggered entry animation
                weeks.forEachIndexed { colIdx, week ->
                    val colAlpha = ((animatedProgress * weeks.size) - (weeks.size - 1 - colIdx))
                        .coerceIn(0f, 1f) * 0.95f + 0.05f // never fully transparent
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier
                            .width(12.dp)
                            .graphicsLayer { alpha = colAlpha },
                    ) {
                        week.forEach { day ->
                            val level = if (day != null) {
                                getIntensityLevel(day.count, maxCount)
                            } else 0
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(colors[level.coerceIn(0, colors.size - 1)]),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "少",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    color = onSurfaceVariantColor.copy(alpha = 0.3f),
                )
                colors.forEach { color ->
                    Spacer(Modifier.width(2.dp))
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(color),
                    )
                }
                Spacer(Modifier.width(2.dp))
                Text(
                    text = "多",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    color = onSurfaceVariantColor.copy(alpha = 0.3f),
                )
            }
        }
    }
}

private fun getIntensityLevel(count: Int, maxCount: Int): Int {
    if (count == 0 || maxCount == 0) return 0
    val ratio = count.toFloat() / maxCount
    return when {
        ratio <= 0.2f -> 1
        ratio <= 0.4f -> 2
        ratio <= 0.6f -> 3
        else -> 4
    }
}

/** Group heatmap days into weeks (Sun-Sat) for grid layout. */
private fun groupIntoWeeks(data: List<HeatmapDay>): List<List<HeatmapDay?>> {
    if (data.isEmpty()) return emptyList()
    val cal = Calendar.getInstance()
    val weeks = mutableListOf<MutableList<HeatmapDay?>>()
    var currentWeek = mutableListOf<HeatmapDay?>()

    data.forEachIndexed { index, day ->
        cal.time = Date(day.date)
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        // Calendar.SUNDAY = 1, MONDAY = 2, ...
        val dow = (dayOfWeek - 1 + 7) % 7 // 0=Sun, 1=Mon, ..., 6=Sat

        if (index == 0) {
            // Pad start of first week
            repeat(dow) { currentWeek.add(null) }
        }

        currentWeek.add(day)

        if (dow == 6) {
            // End of week (Saturday)
            weeks.add(currentWeek)
            currentWeek = mutableListOf()
        }
    }

    // Pad end of last week
    while (currentWeek.size < 7) {
        currentWeek.add(null)
    }
    if (currentWeek.any { it != null }) {
        weeks.add(currentWeek)
    }

    return weeks
}

/** Extract month labels for the chart header. */
private data class MonthLabel(val weekIndex: Int, val month: Int)

private fun extractMonthLabels(data: List<HeatmapDay>): List<MonthLabel> {
    val cal = Calendar.getInstance()
    val labels = mutableListOf<MonthLabel>()
    val weeks = groupIntoWeeks(data)
    weeks.forEachIndexed { weekIdx, week ->
        val firstDay = week.firstNotNullOfOrNull { it } ?: return@forEachIndexed
        cal.time = Date(firstDay.date)
        val month = cal.get(Calendar.MONTH)
        if (labels.none { it.month == month }) {
            labels.add(MonthLabel(weekIdx, month))
        }
    }
    return labels
}

private fun monthLabel(month: Int): String = when (month) {
    0 -> "1月"; 1 -> "2月"; 2 -> "3月"; 3 -> "4月"
    4 -> "5月"; 5 -> "6月"; 6 -> "7月"; 7 -> "8月"
    8 -> "9月"; 9 -> "10月"; 10 -> "11月"; 11 -> "12月"
    else -> ""
}
