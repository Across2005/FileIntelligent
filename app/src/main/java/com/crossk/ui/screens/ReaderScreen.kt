package com.crossk.ui.screens

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.TextFormat
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.crossk.data.FileItem
import com.crossk.data.FileRepository
import com.crossk.ui.theme.BrandAccent
import com.crossk.ui.theme.BrandPrimary
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Line spacing presets for reader comfort.
 */
private enum class LineSpacing(val label: String, val multiplier: Float) {
    COMPACT("紧凑", 1.3f),
    DEFAULT("默认", 1.6f),
    RELAXED("宽松", 2.0f),
}

/**
 * Font size presets.
 */
private enum class FontSize(val label: String, val size: Int) {
    SMALL("小", 13),
    DEFAULT("中", 15),
    LARGE("大", 18),
    XLARGE("特大", 21),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    navController: NavController,
    repository: FileRepository,
    fileId: String,
) {
    val file = remember(fileId) { repository.getFile(fileId) }

    if (file == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("阅读器") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                    ),
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    },
                )
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("文件未找到", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    // Reader settings state
    var fontSize by remember { mutableFloatStateOf(FontSize.DEFAULT.size.toFloat()) }
    var lineSpacing by remember { mutableFloatStateOf(LineSpacing.DEFAULT.multiplier) }
    val scrollState = rememberScrollState()

    // Derived: reading progress 0f..1f
    val progress by remember {
        derivedStateOf {
            val maxS = scrollState.maxValue
            if (maxS <= 0) 0f else (scrollState.value.toFloat() / maxS).coerceIn(0f, 1f)
        }
    }
    val progressAnimated by animateFloatAsState(progress, label = "progress")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = file.displayName,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = buildReaderSubtitle(file),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.98f),
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // Toggle settings panel visibility via icon
                    IconButton(onClick = {
                        // Cycle through line spacings
                        lineSpacing = when (lineSpacing) {
                            LineSpacing.COMPACT.multiplier -> LineSpacing.DEFAULT.multiplier
                            LineSpacing.DEFAULT.multiplier -> LineSpacing.RELAXED.multiplier
                            else -> LineSpacing.COMPACT.multiplier
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.TextFormat,
                            contentDescription = "排版",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // ── Progress bar (thin, at top) ──
            LinearProgressIndicator(
                progress = { progressAnimated },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = BrandPrimary,
                trackColor = BrandPrimary.copy(alpha = 0.1f),
            )

            // ── Reader stats bar ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ReaderChip(Icons.Default.AccessTime, formatReadTime(file.content.length))
                }

                // Font size controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    IconButton(
                        onClick = {
                            fontSize = (fontSize - 2f).coerceAtLeast(FontSize.SMALL.size.toFloat())
                        },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            Icons.Default.Remove,
                            contentDescription = "缩小",
                            modifier = Modifier.size(14.dp),
                        )
                    }
                    Text(
                        text = "${fontSize.roundToInt()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.width(24.dp),
                        fontWeight = FontWeight.Medium,
                    )
                    IconButton(
                        onClick = {
                            fontSize = (fontSize + 2f).coerceAtMost(FontSize.XLARGE.size.toFloat())
                        },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "放大",
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }

                // Progress percentage
                Text(
                    text = "${(progress * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = BrandAccent,
                    fontWeight = FontWeight.Medium,
                )
            }

            // ── Content area ──
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                val displayContent = if (file.content.isNotBlank()) {
                    file.content
                } else {
                    when (file.extension.lowercase()) {
                        "pdf" -> "（PDF 文件预览尚未生成，请在文件详情页查看解析结果）"
                        "docx", "doc" -> "（Word 文档预览尚未生成）"
                        "xlsx", "xls", "csv" -> "（电子表格预览尚未生成）"
                        "pptx", "ppt" -> "（演示文稿预览尚未生成）"
                        else -> "（暂无原文内容）"
                    }
                }

                Text(
                    text = displayContent,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Serif,
                        lineHeight = (fontSize * lineSpacing).sp,
                        fontSize = fontSize.sp,
                        letterSpacing = 0.2.sp,
                    ),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f),
                )
            }
        }
    }
}

@Composable
private fun ReaderChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(12.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        )
    }
}

private fun buildReaderSubtitle(file: FileItem): String {
    val ext = file.extension.uppercase()
    val wordCount = if (file.content.isNotBlank()) {
        val words = file.content.split(Regex("\\s+"))
        "${words.size} 字"
    } else "0 字"
    return "$ext · $wordCount"
}

private fun formatReadTime(charLength: Int): String {
    val minutes = (charLength / 500).coerceAtLeast(1)
    return "${minutes}分钟阅读"
}
