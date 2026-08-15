package com.crossk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Glass card with:
 * - Subtle top-edge primary highlight line
 * - Noise texture overlay (via drawBehind)
 * - Micro-gradient for depth
 * - Elegant shadow
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shapeRadius: androidx.compose.ui.unit.Dp = 12.dp,
    content: @Composable () -> Unit,
) {
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface

    Card(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                // Top-edge highlight line (1px primary glow)
                val lineY = 0f
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            primary.copy(alpha = 0.15f),
                            Color.Transparent,
                        ),
                    ),
                    start = androidx.compose.ui.geometry.Offset(0f, lineY),
                    end = androidx.compose.ui.geometry.Offset(size.width, lineY),
                    strokeWidth = 1.5f,
                )

                // Noise texture overlay (subtle grain)
                val noiseSize = 4
                val noisePath = Path()
                for (x in 0 until (size.width.toInt() / noiseSize)) {
                    for (y in 0 until (size.height.toInt() / noiseSize)) {
                        if ((x * 31 + y * 71) % 7 < 2) {
                            noisePath.addRect(
                                androidx.compose.ui.geometry.Rect(
                                    offset = androidx.compose.ui.geometry.Offset(
                                        (x * noiseSize).toFloat(),
                                        (y * noiseSize).toFloat(),
                                    ),
                                    size = Size(1f, 1f),
                                ),
                            )
                        }
                    }
                }
                drawPath(
                    path = noisePath,
                    color = Color.White.copy(alpha = 0.02f),
                )

                // Bottom subtle border
                drawLine(
                    color = onSurface.copy(alpha = 0.03f),
                    start = androidx.compose.ui.geometry.Offset(0f, size.height - 0.5f),
                    end = androidx.compose.ui.geometry.Offset(size.width, size.height - 0.5f),
                    strokeWidth = 0.5f,
                )
            },
        shape = RoundedCornerShape(shapeRadius),
        colors = CardDefaults.cardColors(containerColor = surfaceVariant),
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}
