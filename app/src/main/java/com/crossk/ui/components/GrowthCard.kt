package com.crossk.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crossk.data.GrowthMetric
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun GrowthCard(
    metrics: List<GrowthMetric>,
    modifier: Modifier = Modifier,
) {
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val density = LocalDensity.current

    val lastMetric = metrics.lastOrNull()
    val prevMetric = if (metrics.size >= 2) metrics[metrics.size - 2] else null

    // 计算增长叙事
    val growthNarrative = if (lastMetric != null && prevMetric != null) {
        val fileInc = lastMetric.filesAnalyzed - prevMetric.filesAnalyzed
        val entInc = lastMetric.entitiesDiscovered - prevMetric.entitiesDiscovered
        val relInc = lastMetric.connectionsMade - prevMetric.connectionsMade
        buildNarrative(fileInc, entInc, relInc)
    } else if (lastMetric != null) {
        "知识库已初具规模"
    } else {
        "等待第一篇文档..."
    }

    val growthRate = if (metrics.size >= 2) {
        val first = metrics.first().filesAnalyzed
        val last = metrics.last().filesAnalyzed
        if (first > 0) ((last - first).toFloat() / first * 100).toInt() else 0
    } else 0
    val growthLabel = if (growthRate >= 0) "+${growthRate}%" else "${growthRate}%"

    var tappedIndex by remember { mutableStateOf(-1) }
    var tapOffsetX by remember { mutableStateOf(0f) }
    var tapOffsetY by remember { mutableStateOf(0f) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceVariantColor),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // ── 标题行 ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "知识成长档案",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = onSurfaceVariantColor,
                    )
                    Text(
                        text = growthNarrative,
                        style = MaterialTheme.typography.labelSmall,
                        color = onSurfaceVariantColor.copy(alpha = 0.6f),
                    )
                }
                Text(
                    text = growthLabel,
                    color = if (growthRate >= 0) Color(0xFF4ADE80) else Color(0xFFF87171),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineSmall,
                )
            }

            if (lastMetric != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    MiniStat("文件", "${lastMetric.filesAnalyzed}", primaryColor)
                    MiniStat("实体", "${lastMetric.entitiesDiscovered}", tertiaryColor)
                    MiniStat("关系", "${lastMetric.connectionsMade}", secondaryColor)
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── 图表区域：Y 轴标签 + Canvas ──
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
            ) {
                val boxMaxWidth = maxWidth  // Capture at BoxWithConstraints scope level
                if (metrics.size < 2) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "至少需要 2 周数据",
                            style = MaterialTheme.typography.labelSmall,
                            color = onSurfaceVariantColor.copy(alpha = 0.5f),
                        )
                    }
                } else {
                    // 计算全局最大值（避免 Iterable.maxOf 的类型推断问题）
                    var maxValue = 1
                    for (m in metrics) {
                        val localMax = max(max(m.filesAnalyzed, m.entitiesDiscovered), m.connectionsMade)
                        if (localMax > maxValue) maxValue = localMax
                    }

                    Row(modifier = Modifier.fillMaxSize()) {
                        // Y 轴刻度标签
                        Column(
                            modifier = Modifier
                                .width(32.dp)
                                .fillMaxHeight()
                                .padding(end = 4.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                        ) {
                            val labelStyle = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                color = onSurfaceVariantColor.copy(alpha = 0.6f),
                            )
                            val steps = 4
                            for (i in steps downTo 0) {
                                val value = (maxValue * i / steps.toFloat()).roundToInt()
                                Text(
                                    text = formatCompactNumber(value),
                                    style = labelStyle,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }

                        // 图表 Canvas + X 轴标签
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(metrics) {
                                        detectTapGestures { tapOffset ->
                                            val w = size.width.toFloat()
                                            val count = metrics.size
                                            val stepX = w / (count - 1)
                                            val idx = (tapOffset.x / stepX).roundToInt()
                                                .coerceIn(0, count - 1)
                                            if (idx == tappedIndex) {
                                                tappedIndex = -1
                                            } else {
                                                tappedIndex = idx
                                                tapOffsetX = tapOffset.x
                                                tapOffsetY = tapOffset.y
                                            }
                                        }
                                    },
                            ) {
                                val w = size.width
                                val h = size.height
                                val maxValF = maxValue.toFloat()

                                val padT = 8f
                                val padB = 4f
                                val chartH = h - padT - padB
                                val dataCount = metrics.size
                                val stepX = w / (dataCount - 1)

                                fun valueToY(v: Int) = padT + chartH * (1f - v / maxValF)

                                // Grid lines
                                for (gi in 0..4) {
                                    val y = padT + chartH * gi / 4
                                    drawLine(
                                        color = Color.White.copy(alpha = 0.05f),
                                        start = Offset(0f, y),
                                        end = Offset(w, y),
                                        strokeWidth = 1f,
                                    )
                                }

                                // Multi-series
                                data class Series(val label: String, val color: Color, val selector: (GrowthMetric) -> Int)
                                val datasets = listOf(
                                    Series("文件数", primaryColor, { it.filesAnalyzed }),
                                    Series("实体数", tertiaryColor, { it.entitiesDiscovered }),
                                    Series("关系数", secondaryColor, { it.connectionsMade }),
                                )

                                datasets.forEachIndexed { idx, series ->
                                    val path = Path()
                                    val alpha = when (idx) {
                                        0 -> 1f
                                        1 -> 0.7f
                                        else -> 0.5f
                                    }

                                    metrics.forEachIndexed { i, m ->
                                        val x = i * stepX
                                        val y = valueToY(series.selector(m))
                                        if (i == 0) {
                                            path.moveTo(x, y)
                                        } else {
                                            val prev = metrics[i - 1]
                                            val prevX = (i - 1) * stepX
                                            val prevY = valueToY(series.selector(prev))
                                            val cpx1 = (prevX + x) / 2f
                                            path.cubicTo(cpx1, prevY, cpx1, y, x, y)
                                        }
                                    }

                                    if (idx == 0) {
                                        val areaPath = Path()
                                        areaPath.addPath(path)
                                        areaPath.lineTo((dataCount - 1) * stepX, h)
                                        areaPath.lineTo(0f, h)
                                        areaPath.close()
                                        val gradientBrush = Brush.verticalGradient(
                                            colors = listOf(primaryColor.copy(alpha = 0.25f), primaryColor.copy(alpha = 0.0f)),
                                            startY = padT,
                                            endY = h,
                                        )
                                        drawPath(areaPath, brush = gradientBrush)
                                    }

                                    drawPath(path, color = series.color.copy(alpha = alpha * 0.2f), style = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                                    drawPath(path, color = series.color.copy(alpha = alpha), style = Stroke(width = if (idx == 0) 2.5f else 1.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))

                                    metrics.forEachIndexed { i, m ->
                                        val x = i * stepX
                                        val y = valueToY(series.selector(m))
                                        val isTapped = (i == tappedIndex)
                                        val dotRadius = if (idx == 0) { if (isTapped) 6f else 3.5f } else { if (isTapped) 5f else 2.5f }
                                        if (isTapped && idx == 0) {
                                            drawCircle(color = series.color.copy(alpha = 0.3f), radius = dotRadius + 8f, center = Offset(x, y))
                                        }
                                        drawCircle(color = series.color.copy(alpha = if (isTapped) 1f else alpha), radius = dotRadius, center = Offset(x, y))
                                    }
                                }

                                if (tappedIndex >= 0) {
                                    val tapX = tappedIndex * stepX
                                    drawLine(color = primaryColor.copy(alpha = 0.15f), start = Offset(tapX, padT), end = Offset(tapX, h), strokeWidth = 1.5f)
                                }
                            }

                            // X 轴周标签（使用 Compose Text 而非 nativeCanvas）
                            val labelInterval = max(1, metrics.size / 6)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .offset(y = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                var i = 0
                                while (i < metrics.size) {
                                    Text(
                                        text = "W${i + 1}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 8.sp,
                                            color = onSurfaceVariantColor.copy(alpha = 0.5f),
                                        ),
                                    )
                                    i += labelInterval
                                }
                            }

                            // ── Tooltip Overlay ──
                            val tooltipAlpha by animateFloatAsState(
                                targetValue = if (tappedIndex >= 0 && tappedIndex < metrics.size) 1f else 0f,
                                animationSpec = tween(200),
                                label = "tooltipAlpha",
                            )
                            if (tappedIndex >= 0 && tappedIndex < metrics.size && tooltipAlpha > 0.01f) {
                                val m = metrics[tappedIndex.coerceIn(0, metrics.size - 1)]
                                val prev = if (tappedIndex > 0) metrics[tappedIndex - 1] else null
                                val trendIcon = { cur: Int, prevVal: Int? ->
                                    if (prevVal != null) {
                                        if (cur > prevVal) "↑" else if (cur < prevVal) "↓" else "→"
                                    } else "→"
                                }

                                val currentMaxWidth = boxMaxWidth
                                val maxWidthPx = with(density) { currentMaxWidth.toPx() }
                                val tooltipXPx = with(density) {
                                    (tapOffsetX - 80.dp.toPx()).coerceAtLeast(4.dp.toPx())
                                        .coerceAtMost(maxWidthPx - 170.dp.toPx())
                                }
                                val tooltipYPx = with(density) {
                                    (tapOffsetY - 110.dp.toPx()).coerceAtLeast(4.dp.toPx())
                                }

                                Box(
                                    modifier = Modifier
                                        .offset { IntOffset(tooltipXPx.roundToInt(), tooltipYPx.roundToInt()) }
                                        .background(
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.95f * tooltipAlpha),
                                            RoundedCornerShape(10.dp),
                                        )
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                ) {
                                    Column {
                                        Text(
                                            text = "第 ${m.weekIndex} 周",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = primaryColor,
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        TooltipRow(
                                            "文件",
                                            "${m.filesAnalyzed}",
                                            trendIcon(m.filesAnalyzed, prev?.filesAnalyzed),
                                            primaryColor,
                                        )
                                        TooltipRow(
                                            "实体",
                                            "${m.entitiesDiscovered}",
                                            trendIcon(m.entitiesDiscovered, prev?.entitiesDiscovered),
                                            tertiaryColor,
                                        )
                                        TooltipRow(
                                            "关系",
                                            "${m.connectionsMade}",
                                            trendIcon(m.connectionsMade, prev?.connectionsMade),
                                            secondaryColor,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── 图例 ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                LegendItem("知识源", primaryColor)
                LegendItem("关键概念", tertiaryColor)
                LegendItem("概念连接", secondaryColor)
            }
        }
    }
}

/** 将数字压缩为简短格式 */
private fun formatCompactNumber(value: Int): String {
    return when {
        value >= 10000 -> "${value / 1000}k"
        value >= 1000 -> "${value / 1000}.${(value % 1000) / 100}k"
        else -> "$value"
    }
}

/** 生成成长叙事文案 */
private fun buildNarrative(fileInc: Int, entInc: Int, relInc: Int): String {
    val total = fileInc + entInc + relInc
    if (total == 0) return "本周知识库保持稳定"
    if (fileInc > 0) return "知识库扩展中 · +$fileInc 新知识源"
    if (entInc > 0) return "发现 +$entInc 个新概念"
    return "新增 +$relInc 条概念连接"
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, RoundedCornerShape(999.dp)),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun TooltipRow(label: String, value: String, trend: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(color, RoundedCornerShape(999.dp)),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            fontWeight = FontWeight.Bold,
            color = color,
        )
        Spacer(Modifier.width(3.dp))
        Text(
            text = trend,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = when {
                trend == "↑" -> Color(0xFF4ADE80)
                trend == "↓" -> Color(0xFFF87171)
                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            },
        )
    }
}

@Composable
private fun MiniStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
            color = color,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        )
    }
}
