package com.fileintelligence.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fileintelligence.ui.theme.BrandAccent
import com.fileintelligence.ui.theme.BrandHighlight
import com.fileintelligence.ui.theme.BrandPrimary
import com.fileintelligence.ui.theme.bgCard
import com.fileintelligence.ui.theme.textSecondary
import com.fileintelligence.ui.theme.textTertiary

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
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgCard),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("发展光谱", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier.fillMaxWidth().height(140.dp),
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val count = series.firstOrNull()?.values?.size ?: 0
                    if (count < 2) return@Canvas

                    val stepX = w / (count - 1)
                    val pad = 16f

                    // Grid
                    for (i in 0..3) {
                        val y = pad + (h - 2 * pad) * i / 3
                        drawLine(
                            color = Color.White.copy(alpha = 0.04f),
                            start = Offset(pad, y),
                            end = Offset(w - pad, y),
                            strokeWidth = 1f,
                        )
                    }

                    // Each series
                    series.forEach { s ->
                        val path = Path()
                        val area = Path()
                        s.values.forEachIndexed { i, v ->
                            val x = pad + i * stepX
                            val y = pad + (h - 2 * pad) * (1f - v.coerceIn(0f, 1f))
                            if (i == 0) {
                                path.moveTo(x, y)
                                area.moveTo(x, h - pad)
                                area.lineTo(x, y)
                            } else {
                                path.lineTo(x, y)
                                area.lineTo(x, y)
                            }
                        }
                        area.lineTo(pad + (count - 1) * stepX, h - pad)
                        area.close()

                        drawPath(
                            area,
                            color = s.color.copy(alpha = 0.08f),
                        )
                        drawPath(
                            path,
                            color = s.color.copy(alpha = 0.5f),
                            style = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                        )
                    }
                }
            }

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                series.forEach { s ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(8.dp).background(s.color, RoundedCornerShape(999.dp)),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(s.name, style = MaterialTheme.typography.labelSmall, color = textTertiary)
                    }
                }
            }
        }
    }
}
