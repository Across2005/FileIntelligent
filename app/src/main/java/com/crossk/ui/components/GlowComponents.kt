package com.crossk.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.crossk.ui.theme.BrandPrimary

fun Modifier.glowBorder(
    color: Color = BrandPrimary,
    glowRadius: Dp = 12.dp,
    borderWidth: Dp = 1.5f.dp,
    alpha: Float = 0.4f,
): Modifier = this.drawBehind {
    val strokeW = borderWidth.toPx()
    val glowR = glowRadius.toPx()
    drawRoundRect(
        color = color.copy(alpha = alpha * 0.15f),
        topLeft = Offset(-glowR / 2f, -glowR / 2f),
        size = Size(size.width + glowR, size.height + glowR),
        cornerRadius = CornerRadius(glowR + strokeW * 2f),
        style = Stroke(width = glowR),
    )
    drawRoundRect(
        color = color.copy(alpha = alpha),
        topLeft = Offset(strokeW / 2f, strokeW / 2f),
        size = Size(size.width - strokeW, size.height - strokeW),
        cornerRadius = CornerRadius(24f),
        style = Stroke(width = strokeW),
    )
}

@Composable
fun GlowFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    glowColor: Color = BrandPrimary,
    icon: @Composable () -> Unit,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = CircleShape,
                ambientColor = glowColor.copy(alpha = 0.3f),
                spotColor = glowColor.copy(alpha = 0.4f),
            )
            .glowBorder(
                color = glowColor,
                glowRadius = 10.dp,
                alpha = 0.3f,
            ),
        containerColor = glowColor,
        contentColor = Color.White,
        shape = CircleShape,
    ) {
        icon()
    }
}

fun Modifier.elevatedGlow(
    color: Color = BrandPrimary,
    alpha: Float = 0.08f,
): Modifier = this
    .border(
        width = 1.dp,
        brush = Brush.linearGradient(
            colors = listOf(
                color.copy(alpha = alpha),
                color.copy(alpha = alpha * 0.3f),
                color.copy(alpha = alpha),
            ),
        ),
        shape = RoundedCornerShape(12.dp),
    )
    .drawBehind {
        drawRoundRect(
            color = color.copy(alpha = alpha * 0.5f),
            topLeft = Offset(-2f, -2f),
            size = Size(size.width + 4f, size.height + 4f),
            cornerRadius = CornerRadius(16f),
            style = Stroke(width = 4f),
        )
    }
