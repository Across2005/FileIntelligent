package com.crossk.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.crossk.data.GraphNode
import com.crossk.data.LayerContent
import com.crossk.data.LayerType
import com.crossk.data.PostcardBackground
import com.crossk.data.PostcardLayer
import com.crossk.data.PostcardProject
import com.crossk.ui.theme.BrandAccent
import com.crossk.ui.theme.BrandHighlight
import com.crossk.ui.theme.BrandPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostcardEditorScreen(
    navController: NavController,
) {
    val layers = remember { mutableStateListOf<PostcardLayer>() }
    var selectedLayerId by remember { mutableStateOf<String?>(null) }
    var editText by remember { mutableStateOf("") }
    var bgType by remember { mutableStateOf(0) } // 0=渐变, 1=纯色, 2=暖色

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("明信片编辑器", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { /* export */ }) {
                        Icon(Icons.Default.Download, contentDescription = "导出")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── Postcard Canvas ──
            PostcardCanvas(
                layers = layers,
                selectedLayerId = selectedLayerId,
                backgroundType = bgType,
                onLayerSelect = { selectedLayerId = it },
                onLayerDrag = { id, offset ->
                    val idx = layers.indexOfFirst { it.id == id }
                    if (idx >= 0) {
                        layers[idx] = layers[idx].copy(translation = offset)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .padding(horizontal = 16.dp),
            )

            // ── Text input (when text layer selected) ──
            val selectedLayer = layers.find { it.id == selectedLayerId }
            if (selectedLayer?.content is LayerContent.TextContent) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "编辑文字",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                        BasicTextField(
                            value = editText,
                            onValueChange = { newText ->
                                editText = newText
                                val idx = layers.indexOfFirst { it.id == selectedLayerId }
                                if (idx >= 0 && layers[idx].content is LayerContent.TextContent) {
                                    layers[idx] = layers[idx].copy(
                                        content = (layers[idx].content as LayerContent.TextContent).copy(text = newText)
                                    )
                                }
                            },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .background(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                    RoundedCornerShape(8.dp),
                                )
                                .padding(12.dp),
                        )
                    }
                }
            }

            // ── Toolbar ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Add text layer
                Button(
                    onClick = {
                        val newLayer = PostcardLayer(
                            type = LayerType.TEXT,
                            zIndex = layers.size,
                            content = LayerContent.TextContent(text = "双击编辑文字"),
                        )
                        layers.add(newLayer)
                        selectedLayerId = newLayer.id
                        editText = "双击编辑文字"
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    ),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Icon(Icons.Default.TextFields, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("文字", style = MaterialTheme.typography.labelSmall)
                }

                // Add graph snapshot layer
                Button(
                    onClick = {
                        val newLayer = PostcardLayer(
                            type = LayerType.GRAPH_SNAPSHOT,
                            zIndex = layers.size,
                            content = LayerContent.GraphSnapshot(
                                nodeCount = 8,
                                edgeCount = 10,
                            ),
                        )
                        layers.add(newLayer)
                        selectedLayerId = newLayer.id
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandAccent.copy(alpha = 0.15f),
                    ),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("图谱", style = MaterialTheme.typography.labelSmall)
                }

                // Toggle background
                Button(
                    onClick = { bgType = (bgType + 1) % 3 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandHighlight.copy(alpha = 0.15f),
                    ),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Icon(Icons.Default.Brush, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        when (bgType) {
                            0 -> "渐变"
                            1 -> "暗色"
                            else -> "暖色"
                        },
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }

            // ── Layer list ──
            if (layers.isNotEmpty()) {
                Text(
                    "图层 (点击删除)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(layers) { layer ->
                        Card(
                            modifier = Modifier
                                .width(100.dp)
                                .clickable {
                                    layers.remove(layer)
                                    if (selectedLayerId == layer.id) {
                                        selectedLayerId = null
                                    }
                                },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (layer.id == selectedLayerId)
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            ),
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = when (layer.type) {
                                        LayerType.TEXT -> "📝"
                                        LayerType.GRAPH_SNAPSHOT -> "🕸️"
                                        LayerType.STAT_BADGE -> "📊"
                                    },
                                    fontSize = 18.sp,
                                )
                                Text(
                                    text = layer.type.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            // ── Save / Export ──
            Button(
                onClick = { /* Save postcard to local storage */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandPrimary,
                ),
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("保存明信片", fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun PostcardCanvas(
    layers: List<PostcardLayer>,
    selectedLayerId: String?,
    backgroundType: Int,
    onLayerSelect: (String) -> Unit,
    onLayerDrag: (String, Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(surfaceVariantColor),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .clip(RoundedCornerShape(8.dp))
                .pointerInput(layers) {
                    detectDragGestures { change, dragAmount ->
                        // Find which layer was tapped (topmost first)
                        val tappedLayer = layers.lastOrNull { layer ->
                            val pos = layer.translation
                            val rect = androidx.compose.ui.geometry.Rect(
                                offset = pos,
                                size = androidx.compose.ui.geometry.Size(200f, 60f),
                            )
                            rect.contains(change.position)
                        }
                        tappedLayer?.let {
                            onLayerDrag(it.id, it.translation + dragAmount)
                            onLayerSelect(it.id)
                        }
                    }
                },
        ) {
            val w = size.width
            val h = size.height

            // ── Background ──
            val bgBrush = when (backgroundType) {
                0 -> Brush.linearGradient(
                    listOf(Color(0xFF2D1B24), Color(0xFF1A1417), Color(0xFF241C20)),
                )
                1 -> Brush.linearGradient(
                    listOf(Color(0xFF1A1417), Color(0xFF1A1417)),
                )
                else -> Brush.linearGradient(
                    listOf(Color(0xFF3D2E15), Color(0xFF2D1B24)),
                )
            }
            drawRect(brush = bgBrush, size = size)

            // Subtle decorative border
            drawRoundRect(
                color = Color.White.copy(alpha = 0.06f),
                size = size,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f),
            )

            // ── Draw layers ──
            val textPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.WHITE
            }

            layers.forEach { layer ->
                val alpha = if (layer.id == selectedLayerId) 1f else 0.7f

                when (val content = layer.content) {
                    is LayerContent.TextContent -> {
                        drawContext.canvas.nativeCanvas.apply {
                            textPaint.color = content.color.copy(alpha = alpha).toArgb()
                            textPaint.textSize = content.fontSize * density.density
                            textPaint.isFakeBoldText = content.fontWeight >= FontWeight.Bold

                            // Draw selection indicator
                            if (layer.id == selectedLayerId) {
                                val textWidth = textPaint.measureText(content.text)
                                drawRoundRect(
                                    color = Color.White.copy(alpha = 0.1f),
                                    topLeft = Offset(
                                        layer.translation.x - 8f,
                                        layer.translation.y - 4f,
                                    ),
                                    size = androidx.compose.ui.geometry.Size(
                                        textWidth + 16f,
                                        content.fontSize * density.density + 12f,
                                    ),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f),
                                )
                            }

                            drawText(
                                content.text,
                                layer.translation.x + 20f,
                                layer.translation.y + content.fontSize * density.density + 20f,
                                textPaint,
                            )
                        }
                    }

                    is LayerContent.GraphSnapshot -> {
                        // Draw a mini graph visualization
                        val cx = w / 2f + layer.translation.x
                        val cy = h / 2f + layer.translation.y

                        // Draw mini nodes
                        val colors = listOf(BrandPrimary, BrandAccent, BrandHighlight)
                        for (i in 0 until minOf(content.nodeCount, 8)) {
                            val angle = i * 2.3999f
                            val r = 40f
                            val nx = cx + kotlin.math.cos(angle) * r
                            val ny = cy + kotlin.math.sin(angle) * r
                            drawCircle(
                                color = colors[i % colors.size].copy(alpha = 0.5f * alpha),
                                radius = 6f,
                                center = Offset(nx, ny),
                            )
                            // Mini edges
                            if (i > 0) {
                                val prevAngle = (i - 1) * 2.3999f
                                val pnx = cx + kotlin.math.cos(prevAngle) * r
                                val pny = cy + kotlin.math.sin(prevAngle) * r
                                drawLine(
                                    color = Color.White.copy(alpha = 0.08f * alpha),
                                    start = Offset(pnx, pny),
                                    end = Offset(nx, ny),
                                    strokeWidth = 0.8f,
                                )
                            }
                        }

                        // Center label
                        textPaint.textSize = 9f * density.density
                        textPaint.color = Color.White.copy(alpha = 0.5f * alpha).toArgb()
                        drawContext.canvas.nativeCanvas.drawText(
                            content.title,
                            cx - 30f,
                            cy + 35f,
                            textPaint,
                        )
                    }

                    is LayerContent.StatBadge -> {
                        // Stat badge rendering
                        val badgeX = w - 80f + layer.translation.x
                        val badgeY = 20f + layer.translation.y

                        drawRoundRect(
                            color = content.color.copy(alpha = 0.2f * alpha),
                            topLeft = Offset(badgeX, badgeY),
                            size = androidx.compose.ui.geometry.Size(70f, 40f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f),
                        )
                        textPaint.textSize = 10f * density.density
                        textPaint.color = content.color.copy(alpha = alpha).toArgb()
                        drawContext.canvas.nativeCanvas.drawText(
                            content.label,
                            badgeX + 35f,
                            badgeY + 18f,
                            textPaint,
                        )
                        drawContext.canvas.nativeCanvas.drawText(
                            content.value,
                            badgeX + 35f,
                            badgeY + 34f,
                            textPaint,
                        )
                    }
                }
            }

            // ── Hint text ──
            if (layers.isEmpty()) {
                textPaint.textSize = 13f * density.density
                textPaint.color = Color.White.copy(alpha = 0.2f).toArgb()
                drawContext.canvas.nativeCanvas.drawText(
                    "点击下方按钮添加内容",
                    w / 2f,
                    h / 2f,
                    textPaint,
                )
            }
        }
    }
}
