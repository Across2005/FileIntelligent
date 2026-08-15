package com.crossk.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crossk.ui.theme.BrandAccent
import com.crossk.ui.theme.BrandHighlight
import com.crossk.ui.theme.BrandPrimary
import kotlin.math.cos
import kotlin.math.sin

/**
 * Artistic empty state for when the user has no data yet.
 * Shows floating geometric shapes with micro-animations.
 */
@Composable
fun EmptyKnowledgeState(
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "empty")

    // Floating animation values
    val floatPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "floatPhase",
    )

    val breathe by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathe",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Animated Canvas with floating geometric shapes
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val r = minOf(cx, cy) - 10f

                // Outer ring
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            BrandPrimary.copy(alpha = 0.1f),
                            BrandAccent.copy(alpha = 0.1f),
                            BrandHighlight.copy(alpha = 0.1f),
                            BrandPrimary.copy(alpha = 0.1f),
                        ),
                    ),
                    radius = r,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.5f),
                )

                // Floating shapes
                val shapes = listOf(
                    Triple(0f, -r * 0.5f, BrandPrimary),
                    Triple(r * 0.45f, r * 0.2f, BrandAccent),
                    Triple(-r * 0.45f, r * 0.2f, BrandHighlight),
                    Triple(r * 0.25f, -r * 0.35f, BrandPrimary.copy(alpha = 0.5f)),
                    Triple(-r * 0.25f, -r * 0.55f, BrandAccent.copy(alpha = 0.5f)),
                )

                val angleRad = floatPhase * kotlin.math.PI.toFloat() / 180f
                shapes.forEachIndexed { index, (sx, sy, color) ->
                    val orbit = 5f + index * 2f
                    val px = cx + sx + cos(angleRad + index * 1.5f) * orbit
                    val py = cy + sy + sin(angleRad + index * 1.5f) * orbit
                    val size = 6f + index * 2f

                    // Glow
                    drawCircle(
                        color = color.copy(alpha = 0.15f),
                        radius = size * 3f,
                        center = Offset(px, py),
                    )
                    // Core
                    drawCircle(
                        color = color.copy(alpha = 0.6f),
                        radius = size * 0.5f,
                        center = Offset(px, py),
                    )
                    // Center dot
                    drawCircle(
                        color = color,
                        radius = size * 0.25f,
                        center = Offset(px, py),
                    )
                }

                // Center star
                val starPath = Path()
                val starR = 12f * breathe
                for (i in 0 until 6) {
                    val a = i * 60f * kotlin.math.PI.toFloat() / 180f - kotlin.math.PI.toFloat() / 2f
                    val outerX = cx + cos(a) * starR
                    val outerY = cy + sin(a) * starR
                    val innerA = a + 30f * kotlin.math.PI.toFloat() / 180f
                    val innerX = cx + cos(innerA) * starR * 0.4f
                    val innerY = cy + sin(innerA) * starR * 0.4f
                    if (i == 0) starPath.moveTo(outerX, outerY)
                    else starPath.lineTo(outerX, outerY)
                    starPath.lineTo(innerX, innerY)
                }
                starPath.close()
                drawPath(
                    starPath,
                    brush = Brush.linearGradient(
                        colors = listOf(BrandHighlight, BrandPrimary),
                    ),
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Title
        Text(
            text = "你的知识宇宙尚未诞生",
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 20.sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(8.dp))

        // Description
        Text(
            text = "导入第一份文件，点燃第一颗星。\n你的每一次阅读都在扩张这个宇宙。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
        )

        Spacer(Modifier.height(24.dp))

        // Action button
        if (onAction != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(BrandPrimary, BrandHighlight),
                        ),
                    )
                    .clickable(onClick = onAction)
                    .padding(horizontal = 32.dp, vertical = 12.dp),
            ) {
                Text(
                    text = "✨ 点燃第一颗星",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }
    }
}

/**
 * Empty state for search results — "nothing found but let's not be boring about it"
 */
@Composable
fun EmptySearchState(
    query: String,
    modifier: Modifier = Modifier,
) {
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "🔍",
            style = MaterialTheme.typography.displayLarge,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "未找到「$query」",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "试试其他关键词，或导入相关文件",
            style = MaterialTheme.typography.bodySmall,
            color = onSurfaceVariant.copy(alpha = 0.5f),
        )
    }
}

@Composable
fun EmptyLibraryState(
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "📂",
            style = MaterialTheme.typography.displayLarge,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "文件库空空如也",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "点击下方按钮导入你的第一份文件",
            style = MaterialTheme.typography.bodySmall,
            color = onSurfaceVariant.copy(alpha = 0.5f),
        )
    }
}
