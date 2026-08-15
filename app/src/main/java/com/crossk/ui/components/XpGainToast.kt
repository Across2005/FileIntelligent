package com.crossk.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crossk.data.XpBreakdownItem
import com.crossk.ui.theme.BrandHighlight
import com.crossk.ui.theme.BrandPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow

/**
 * 浮动 XP 增益提示。订阅 [xpFlow] 事件流，每次收到拆解列表时弹出一条
 * 自动上浮 + 渐隐的动画标签，展示 "+50 解析新文件" 等明细。
 */
@Composable
fun XpGainToast(
    xpFlow: SharedFlow<List<XpBreakdownItem>>,
    modifier: Modifier = Modifier,
) {
    val toasts = remember { mutableStateListOf<ToastItem>() }

    LaunchedEffect(Unit) {
        xpFlow.collect { items ->
            val toastId = System.nanoTime()
            val toast = ToastItem(
                id = toastId,
                items = items,
            )
            toasts.add(toast)
            // 自动移除（动画结束后）
            delay(2200)
            toasts.removeAll { it.id == toastId }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.End,
    ) {
        toasts.forEach { toast ->
            ToastRow(
                items = toast.items,
                key = toast.id,
            )
        }
    }
}

private data class ToastItem(
    val id: Long,
    val items: List<XpBreakdownItem>,
)

@Composable
private fun ToastRow(
    items: List<XpBreakdownItem>,
    key: Long,
) {
    val visible = remember(items, key) { true }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(300),
        ) + fadeIn(animationSpec = tween(200)),
        exit = slideOutVertically(
            targetOffsetY = { -it / 2 },
            animationSpec = tween(500),
        ) + fadeOut(animationSpec = tween(500)),
    ) {
        Row(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(8.dp),
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEachIndexed { idx, item ->
                if (idx > 0) {
                    Spacer(Modifier.width(4.dp))
                    Text("·", fontSize = 12.sp, color = Color.White.copy(alpha = 0.4f))
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    text = item.displayText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = BrandPrimary,
                    fontSize = 12.sp,
                )
            }
        }
    }
}
