package com.crossk.ui.screens

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
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/**
 * Animated launch splash screen.
 * Displays a growing peach-colored light ripple that expands into the app.
 */
@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "splash")

    // Ripple animation
    val rippleProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ripple",
    )

    // Fade for the app name
    val appNameAlpha by animateFloatAsState(
        targetValue = if (rippleProgress > 0.3f) 1f else 0f,
        animationSpec = tween(800),
        label = "appNameFade",
    )

    // Subtitle fade
    val subtitleAlpha by animateFloatAsState(
        targetValue = if (rippleProgress > 0.6f) 0.7f else 0f,
        animationSpec = tween(600),
        label = "subtitleFade",
    )

    // Auto-dismiss after animation
    LaunchedEffect(Unit) {
        delay(2800)
        onSplashComplete()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgDeepest),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .size(280.dp),
        ) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val maxR = size.width / 2f

            // Ripple rings
            val ringCount = 3
            for (i in 0 until ringCount) {
                val phase = (rippleProgress + i.toFloat() / ringCount) % 1f
                val r = maxR * phase
                val alpha = (1f - phase).coerceIn(0f, 0.6f)
                drawCircle(
                    color = BrandPrimary.copy(alpha = alpha),
                    radius = r,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.5f, cap = StrokeCap.Round),
                )
            }

            // Center glow
            val glowR = maxR * (0.1f + rippleProgress * 0.15f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        BrandHighlight.copy(alpha = 0.3f),
                        BrandPrimary.copy(alpha = 0.1f),
                        Color.Transparent,
                    ),
                ),
                radius = glowR * 4f,
                center = Offset(cx, cy),
            )

            // Core light
            drawCircle(
                color = BrandPrimary.copy(alpha = 0.4f * (1f - rippleProgress * 0.5f)),
                radius = glowR,
                center = Offset(cx, cy),
            )

            // Orbiting dots
            val dotCount = 6
            val orbitR = maxR * 0.5f
            for (i in 0 until dotCount) {
                val angle = (i * 60f + rippleProgress * 360f) * kotlin.math.PI.toFloat() / 180f
                val dotX = cx + cos(angle) * orbitR
                val dotY = cy + sin(angle) * orbitR
                val dotAlpha = (0.5f + 0.5f * sin(angle + rippleProgress * 6.28f)).coerceIn(0f, 1f)
                drawCircle(
                    color = BrandAccent.copy(alpha = dotAlpha * 0.4f),
                    radius = 3f,
                    center = Offset(dotX, dotY),
                )
            }
        }

        // App name
        Column(
            modifier = Modifier
                .padding(top = 340.dp)
                .alpha(appNameAlpha),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "文件智析",
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 28.sp),
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "你的私人知识伴侣",
                style = MaterialTheme.typography.bodyMedium,
                color = BrandPrimary.copy(alpha = subtitleAlpha),
                modifier = Modifier.alpha(subtitleAlpha),
            )
        }
    }
}
