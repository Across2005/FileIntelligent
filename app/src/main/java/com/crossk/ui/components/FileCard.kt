package com.crossk.ui.components

import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.crossk.data.FileItem
import kotlin.math.roundToInt

/**
 * FileCard with swipe-to-delete and selection states.
 * When selected, shows a subtle elevated glow to highlight the active item.
 */

@Composable
fun FileCard(
    file: FileItem,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    selectionToggle: (() -> Unit)? = null,
) {
    val density = LocalDensity.current
    val maxSwipePx = with(density) { 100.dp.toPx() }
    val deleteThresholdPx = with(density) { 60.dp.toPx() }

    var offsetX by remember { mutableStateOf(0f) }
    var deleting by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val tertiaryColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    val selectionColor = MaterialTheme.colorScheme.tertiary

    Box(modifier = modifier.fillMaxWidth()) {
        // Delete background (revealed behind the card)
        if (onDelete != null && !deleting) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (offsetX < -20f) MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
                        else Color.Transparent
                    ),
                contentAlignment = Alignment.CenterEnd,
            ) {
                if (offsetX < -20f) {
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "删除",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                        Text("松手删除", color = Color.White, fontSize = MaterialTheme.typography.labelSmall.fontSize)
                    }
                }
            }
        }

        // Sliding card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(if (deleting) -maxSwipePx.roundToInt() else offsetX.roundToInt(), 0) }
                .then(
                    // Subtle glow when selected for visual emphasis
                    if (isSelected) Modifier.elevatedGlow(alpha = 0.10f) else Modifier
                )
                .then(
                    if (onDelete != null) {
                        Modifier.pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    // Auto-delete when past threshold
                                    if (offsetX < -deleteThresholdPx) {
                                        deleting = true
                                        onDelete()
                                    }
                                    offsetX = 0f
                                },
                                onDragCancel = { offsetX = 0f },
                                onHorizontalDrag = { _, dragAmount ->
                                    offsetX = (offsetX + dragAmount).coerceIn(-maxSwipePx, 0f)
                                },
                            )
                        }
                    } else Modifier
                ),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) selectionColor.copy(alpha = 0.15f) else surfaceVariantColor,
            ),
            onClick = if (selectionToggle != null) selectionToggle else onClick,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 4.dp, top = 16.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(primaryColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(22.dp),
                    )
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
                            color = tertiaryColor,
                        )
                        Text(
                            text = file.readSize,
                            style = MaterialTheme.typography.labelSmall,
                            color = tertiaryColor,
                        )
                        // P6: 分析状态指示
                        if (file.aiSummary != null) {
                            Text(
                                text = "${file.entities.size} 实体",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (file.entities.isNotEmpty()) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                       else tertiaryColor.copy(alpha = 0.5f),
                            )
                        } else {
                            Text(
                                text = "待分析",
                                style = MaterialTheme.typography.labelSmall,
                                color = tertiaryColor.copy(alpha = 0.4f),
                            )
                        }
                    }
                }
                // Selection checkbox
                if (selectionToggle != null) {
                    Icon(
                        imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                        contentDescription = if (isSelected) "已选择" else "未选择",
                        tint = if (isSelected) selectionColor else tertiaryColor,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                }
                // Tag + ⋮ menu
                if (file.tags.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(primaryColor.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = file.tags.first(),
                            color = primaryColor,
                            fontSize = MaterialTheme.typography.labelSmall.fontSize,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
                // ⋮ Menu button
                if (onDelete != null) {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "更多",
                                tint = tertiaryColor,
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text("删除文件", color = MaterialTheme.colorScheme.error)
                                    }
                                },
                                onClick = {
                                    showMenu = false
                                    deleting = true
                                    onDelete()
                                },
                            )
                        }
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
