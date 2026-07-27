package com.fileintelligence.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.fileintelligence.data.FileItem
import com.fileintelligence.ui.theme.BrandDanger
import com.fileintelligence.ui.theme.BrandPrimary
import com.fileintelligence.ui.theme.bgCard
import com.fileintelligence.ui.theme.bgElevated
import com.fileintelligence.ui.theme.textSecondary
import com.fileintelligence.ui.theme.textTertiary

@Composable
fun FileCard(
    file: FileItem,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var offsetX by remember { mutableStateOf(0f) }
    val cardOffset by animateDpAsState(
        targetValue = offsetX.dp,
        animationSpec = spring(dampingRatio = 0.8f),
        label = "cardOffset",
    )
    val deleteBgColor by animateColorAsState(
        targetValue = if (offsetX < -20f) BrandDanger.copy(alpha = 0.9f) else Color.Transparent,
        animationSpec = spring(dampingRatio = 0.7f),
        label = "deleteBg",
    )

    Box(modifier = modifier.fillMaxWidth()) {
        // Delete background
        if (onDelete != null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(deleteBgColor),
                contentAlignment = Alignment.CenterEnd,
            ) {
                if (offsetX < -20f) {
                    Box(
                        modifier = Modifier
                            .clickable {
                                onDelete()
                                offsetX = 0f
                            }
                            .padding(horizontal = 24.dp, vertical = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "删除",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp),
                            )
                            Text("删除", color = Color.White, fontSize = MaterialTheme.typography.labelSmall.fontSize)
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(cardOffset.roundToPx(), 0) }
                .pointerInput(onDelete) {
                    if (onDelete != null) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break
                                if (change.pressed) {
                                    val dx = change.position.x - change.previousPosition.x
                                    offsetX = (offsetX + dx).coerceIn(-100f, 0f)
                                } else {
                                    if (offsetX < -60f) {
                                        // Show delete action
                                    } else {
                                        offsetX = 0f
                                    }
                                    break
                                }
                            }
                        }
                    }
                },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = bgCard),
            onClick = onClick,
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(bgElevated),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("📝", fontSize = MaterialTheme.typography.titleLarge.fontSize)
                }
                Spacer(Modifier.width(12.dp))
                // Body
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = file.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = formatRelativeTime(file.lastModified),
                            style = MaterialTheme.typography.labelSmall,
                            color = textTertiary,
                        )
                        Text(
                            text = file.readSize,
                            style = MaterialTheme.typography.labelSmall,
                            color = textTertiary,
                        )
                    }
                }
                // Tag
                if (file.tags.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(BrandPrimary.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = file.tags.first(),
                            color = BrandPrimary,
                            fontSize = MaterialTheme.typography.labelSmall.fontSize,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

private fun formatRelativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000 -> "刚刚"
        diff < 3_600_000 -> "${diff / 60_000} 分钟前"
        diff < 86_400_000 -> "${diff / 3_600_000} 小时前"
        else -> "${diff / 86_400_000} 天前"
    }
}
