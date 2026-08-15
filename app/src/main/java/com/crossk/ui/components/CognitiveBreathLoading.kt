package com.crossk.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crossk.ui.theme.BrandAccent
import com.crossk.ui.theme.BrandHighlight
import com.crossk.ui.theme.BrandPrimary
import com.crossk.ui.theme.bgDeepest
import kotlin.math.cos
import kotlin.math.sin

/**
 * Cognitive Breath Loading — 认知呼吸加载动画。
 *
 * A branded peach-themed loading animation featuring a gentle breathing
 * peach blossom core with orbiting petal particles, designed to convey
 * a sense of calm contemplation while data loads.
 *
 * Can be used as a fullscreen overlay or inline loading indicator.
 *
 * @param caption Optional loading text shown below the animation
 * @param isFullScreen When true, occupies full screen with dark backdrop
 * @param modifier Additional modifier
 */
@Composable
fun CognitiveBreathLoading(
    caption: String = "认知呼吸中…",
    isFullScreen: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "breathLoading")

    // Core breathing animation — expands and contracts gently
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathScale",
    )

    // Secondary subtle pulse for the outer glow
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowPulse",
    )

    // Orbital rotation angle
    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "orbitAngle",
    )

    // Caption fade
    val captionAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "captionFade",
    )

    val content = @Composable {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Breathing peach blossom core
            Canvas(
                modifier = Modifier.size(96.dp),
            ) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val baseR = size.width / 2f * 0.38f
                val currentR = baseR * breathScale

                // ── Outer glow ──
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            BrandPrimary.copy(alpha = glowPulse * 0.25f),
                            BrandPrimary.copy(alpha = glowPulse * 0.08f),
                            Color.Transparent,
                        ),
                    ),
                    radius = currentR * 3.5f,
                    center = Offset(cx, cy),
                )

                // ── Breathing ring 1 (outer) ──
                drawCircle(
                    color = BrandPrimary.copy(alpha = glowPulse * 0.2f),
                    radius = currentR * 1.5f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.5f, cap = StrokeCap.Round),
                )

                // ── Breathing ring 2 (inner) ──
                drawCircle(
                    color = BrandAccent.copy(alpha = glowPulse * 0.15f),
                    radius = currentR * 1.1f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1f, cap = StrokeCap.Round),
                )

                // ── Core blossom (petal-like shapes) ──
                val petalCount = 6
                val angleRad = orbitAngle * kotlin.math.PI.toFloat() / 180f
                for (i in 0 until petalCount) {
                    val petalAngle = i * (360f / petalCount) * kotlin.math.PI.toFloat() / 180f
                    val pulseOffset = sin(angleRad + i * 1.2f) * 2f
                    val petalR = (currentR * 0.6f + pulseOffset).coerceAtLeast(2f)
                    val petalDist = currentR * 0.5f
                    drawCircle(
                        color = BrandPrimary.copy(alpha = (0.5f + 0.3f * sin(angleRad + i.toFloat())) * breathScale),
                        radius = petalR,
                        center = Offset(
                            cx + cos(petalAngle) * petalDist,
                            cy + sin(petalAngle) * petalDist,
                        ),
                    )
                }

                // ── Center core ──
                drawCircle(
                    color = BrandHighlight.copy(alpha = 0.6f * breathScale),
                    radius = currentR * 0.3f,
                    center = Offset(cx, cy),
                )

                // ── Orbiting micro-particles ──
                val particleCount = 4
                for (i in 0 until particleCount) {
                    val pAngle = angleRad + i * (360f / particleCount) * kotlin.math.PI.toFloat() / 180f
                    val pDist = currentR * 1.8f + sin(angleRad * 1.5f + i * 0.8f) * 6f
                    val pSize = 2f + sin(angleRad + i.toFloat()) * 0.5f
                    drawCircle(
                        color = BrandAccent.copy(alpha = (0.4f + 0.3f * sin(angleRad + i.toFloat())) * glowPulse),
                        radius = pSize,
                        center = Offset(
                            cx + cos(pAngle) * pDist,
                            cy + sin(pAngle) * pDist,
                        ),
                    )
                }

                // ── Subtle inner glow dot ──
                drawCircle(
                    color = BrandPrimary.copy(alpha = 0.15f * breathScale),
                    radius = currentR * 0.15f,
                    center = Offset(cx, cy),
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Caption ──
            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Light,
                modifier = Modifier.alpha(captionAlpha),
            )
        }
    }

    if (isFullScreen) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(bgDeepest.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    } else {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

/**
 * Inline breath loading — a compact version for embedding in cards or buttons.
 */
@Composable
fun InlineBreathIndicator(
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "inlineBreath")

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "inlinePulse",
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Canvas(modifier = Modifier.size(12.dp)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val r = size.width / 2f * 0.6f * pulse
            drawCircle(
                color = BrandPrimary.copy(alpha = 0.6f * pulse),
                radius = r,
                center = Offset(cx, cy),
            )
            drawCircle(
                color = BrandAccent.copy(alpha = 0.3f * pulse),
                radius = r * 1.4f,
                center = Offset(cx, cy),
                style = Stroke(width = 1.2f),
            )
        }
        Text(
            text = "加载中",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f * pulse),
        )
    }
}
