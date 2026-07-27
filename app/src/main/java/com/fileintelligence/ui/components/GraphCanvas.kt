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
import com.fileintelligence.data.GraphEdge
import com.fileintelligence.data.GraphNode
import com.fileintelligence.data.generateMockGraphEdges
import com.fileintelligence.data.generateMockGraphNodes
import com.fileintelligence.ui.theme.BrandAccent
import com.fileintelligence.ui.theme.BrandPrimary
import com.fileintelligence.ui.theme.bgCard
import com.fileintelligence.ui.theme.textSecondary
import com.fileintelligence.ui.theme.textTertiary

@Composable
fun GraphCanvas(
    nodes: List<GraphNode> = generateMockGraphNodes(),
    edges: List<GraphEdge> = generateMockGraphEdges(),
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth().height(400.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgCard),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("知识图谱", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("缩放 · 拖拽", style = MaterialTheme.typography.labelSmall, color = textTertiary)
            }
            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier.fillMaxWidth().height(340.dp),
                contentAlignment = Alignment.Center,
            ) {
                // Canvas drawing
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val nodeMap = nodes.associateBy { it.id }
                    val w = size.width
                    val h = size.height

                    // Scale node positions to canvas
                    val minX = nodes.minOf { it.x }.coerceAtMost(0f)
                    val maxX = nodes.maxOf { it.x }.coerceAtLeast(360f)
                    val minY = nodes.minOf { it.y }.coerceAtMost(0f)
                    val maxY = nodes.maxOf { it.y }.coerceAtLeast(700f)

                    fun scaleX(x: Float) = ((x - minX) / (maxX - minX).coerceAtLeast(1f)) * w
                    fun scaleY(y: Float) = ((y - minY) / (maxY - minY).coerceAtLeast(1f)) * h

                    // Draw edges
                    edges.forEach { edge ->
                        val src = nodeMap[edge.source]
                        val dst = nodeMap[edge.target]
                        if (src != null && dst != null) {
                            drawLine(
                                color = Color.White.copy(alpha = edge.weight.coerceIn(0.1f, 0.4f)),
                                start = Offset(scaleX(src.x), scaleY(src.y)),
                                end = Offset(scaleX(dst.x), scaleY(dst.y)),
                                strokeWidth = 1.5f * edge.weight,
                            )
                        }
                    }

                    // Draw nodes
                    nodes.forEach { node ->
                        val cx = scaleX(node.x)
                        val cy = scaleY(node.y)
                        val r = 8f + node.size * 4f

                        // Glow
                        drawCircle(
                            color = node.color.copy(alpha = 0.15f),
                            radius = r + 6f,
                            center = Offset(cx, cy),
                        )
                        // Fill
                        drawCircle(
                            color = node.color.copy(alpha = 0.3f),
                            radius = r,
                            center = Offset(cx, cy),
                        )
                        // Border
                        drawCircle(
                            color = node.color,
                            radius = r,
                            center = Offset(cx, cy),
                            style = Stroke(width = 1.5f),
                        )
                    }
                }
            }

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                listOf(
                    "概念" to BrandPrimary,
                    "实体" to BrandAccent,
                ).forEach { (label, color) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(color, RoundedCornerShape(999.dp)),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(label, style = MaterialTheme.typography.labelSmall, color = textTertiary)
                    }
                }
            }
        }
    }
}
