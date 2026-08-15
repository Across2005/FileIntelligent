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
import androidx.compose.material3.Icon
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

data class SpectrumSeries(
    val name: String,
    val color: Color,
    val values: List<Float>,
)

@Composable
fun SpectrumChart(
    series: List<SpectrumSeries>,
    modifier: Modifier = Modifier,
) {
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val tertiaryTextColor = onSurfaceVariantColor.copy(alpha = 0.5f)
    val primaryColor = MaterialTheme.colorScheme.primary

    var tappedIndex by remember { mutableStateOf(-1) }
    var tapOffsetX by remember { mutableStateOf(0f) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceVariantColor),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "发展光谱",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "12周趋势",
                        style = MaterialTheme.typography.labelSmall,
                        color = tertiaryTextColor,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Canvas Spectrum ──
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp),
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(series) {
                            detectTapGestures { tapOffset ->
                                val count = series.firstOrNull()?.values?.size ?: 0
                                if (count < 2) return@detectTapGestures
                                val w = size.width.toFloat()
                                val pad = 16f
                                val stepX = (w - pad * 2) / (count - 1)
                                val idx = ((tapOffset.x - pad) / stepX).roundToInt()
                                    .coerceIn(0, count - 1)
                                tappedIndex = if (idx == tappedIndex) -1 else idx
                                tapOffsetX = tapOffset.x
                            }
                        },
                ) {
                    val w = size.width
                    val h = size.height
                    val count = series.firstOrNull()?.values?.size ?: 0
                    if (count < 2) return@Canvas

                    val stepX = w / (count - 1)
                    val pad = 16f
                    val effectiveH = h - pad * 2

                    // ── Grid ──
                    for (i in 0..3) {
                        val y = pad + effectiveH * i / 3
                        drawLine(
                            color = Color.White.copy(alpha = 0.04f),
                            start = Offset(pad, y),
                            end = Offset(w - pad, y),
                            strokeWidth = 1f,
                        )
                    }

                    // ── Series ribbons ──
                    series.forEachIndexed { si, s ->
                        val path = Path()
                        val areaPath = Path()

                        s.values.forEachIndexed { i, v ->
                            val x = pad + i * stepX
                            val y = pad + effectiveH * (1f - v.coerceIn(0f, 1f))
                            if (i == 0) {
                                path.moveTo(x, y)
                                areaPath.moveTo(x, h - pad)
                                areaPath.lineTo(x, y)
                            } else {
                                val prevX = pad + (i - 1) * stepX
                                val prevY = pad + effectiveH * (1f - s.values[i - 1].coerceIn(0f, 1f))
                                val cpx = (prevX + x) / 2f
                                path.cubicTo(cpx, prevY, cpx, y, x, y)
                                areaPath.lineTo(x, y)
                            }
                        }
                        areaPath.lineTo(pad + (count - 1) * stepX, h - pad)
                        areaPath.close()

                        // Gradient area fill
                        val areaBrush = Brush.verticalGradient(
                            colors = listOf(
                                s.color.copy(alpha = 0.15f),
                                s.color.copy(alpha = 0.0f),
                            ),
                            startY = pad,
                            endY = h - pad,
                        )
                        drawPath(areaPath, brush = areaBrush)

                        // Glow under line
                        drawPath(
                            path,
                            color = s.color.copy(alpha = 0.2f),
                            style = Stroke(
                                width = 5f,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round,
                            ),
                        )

                        // Main line
                        drawPath(
                            path,
                            color = s.color.copy(alpha = 0.75f),
                            style = Stroke(
                                width = 2.5f,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round,
                            ),
                        )

                        // Data dots
                        s.values.forEachIndexed { i, v ->
                            val x = pad + i * stepX
                            val y = pad + effectiveH * (1f - v.coerceIn(0f, 1f))
                            val isTapped = (i == tappedIndex)

                            if (isTapped) {
                                drawCircle(
                                    color = s.color.copy(alpha = 0.3f),
                                    radius = 8f,
                                    center = Offset(x, y),
                                )
                                drawCircle(
                                    color = s.color,
                                    radius = 5f,
                                    center = Offset(x, y),
                                )
                            } else {
                                drawCircle(
                                    color = s.color.copy(alpha = 0.6f),
                                    radius = 3f,
                                    center = Offset(x, y),
                                )
                            }
                        }
                    }

                    // ── Tap indicator ──
                    if (tappedIndex >= 0) {
                        val tapX = pad + tappedIndex * stepX
                        drawLine(
                            color = primaryColor.copy(alpha = 0.12f),
                            start = Offset(tapX, pad),
                            end = Offset(tapX, h - pad),
                            strokeWidth = 1.5f,
                        )
                    }
                }

                // ── Tooltip Overlay ──
                val tooltipAlpha by animateFloatAsState(
                    targetValue = if (tappedIndex >= 0) 1f else 0f,
                    animationSpec = tween(200),
                    label = "tooltipAlpha",
                )
                if (tappedIndex >= 0 && tooltipAlpha > 0.01f) {
                    val density = LocalDensity.current
                    val tooltipXPx = with(density) {
                        (tapOffsetX - 90.dp.toPx())
                            .coerceAtLeast(4.dp.toPx())
                            .coerceAtMost(maxWidth.toPx() - 190.dp.toPx())
                    }
                    val tooltipYPx = with(density) { 8.dp.toPx() }

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
                                text = "第 ${tappedIndex + 1} 周",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor,
                            )
                            Spacer(Modifier.height(4.dp))
                            series.forEach { s ->
                                val valAtIdx = s.values.getOrNull(tappedIndex) ?: 0f
                                val prevVal = s.values.getOrNull(tappedIndex - 1)
                                val trend = if (prevVal != null) {
                                    when {
                                        valAtIdx > prevVal -> "↑"
                                        valAtIdx < prevVal -> "↓"
                                        else -> "→"
                                    }
                                } else "→"

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(s.color, RoundedCornerShape(999.dp)),
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Text(
                                        text = s.name,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "${(valAtIdx * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = s.color,
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
                        }
                    }
                }
            }

            // ── Date labels ──
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                (0 until (series.firstOrNull()?.values?.size ?: 0) step 2).forEach { i ->
                    Text(
                        "W${i + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = tertiaryTextColor.copy(alpha = 0.6f),
                    )
                }
            }

            // ── Legend ──
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                series.forEach { s ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(s.color, RoundedCornerShape(999.dp)),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            s.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = tertiaryTextColor,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}
