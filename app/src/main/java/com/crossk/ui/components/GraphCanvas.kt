package com.crossk.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crossk.data.EdgeTypeStyle
import com.crossk.data.GraphEdge
import com.crossk.data.GraphEvolutionState
import com.crossk.data.GraphNode
import com.crossk.data.NodeType
import com.crossk.ui.theme.BrandAccent
import com.crossk.ui.theme.BrandHighlight
import com.crossk.ui.theme.BrandPrimary
import com.crossk.ui.theme.Dimens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * v2.0 GraphCanvas：
 * - 节点位置用 SnapshotStateList，引擎 step 自动触发重绘（修复 v1 断流）
 * - 加入 Viewport 概念：scale + translation，封装在 viewportState
 * - 手势改用 detectTransformGestures（pan + zoom + centroid），双指缩放真正生效
 * - 双击空白复位；双击节点聚焦
 * - 边颜色按 RelationType 6 类映射（替换 v1 硬编码 REFERENCES）
 */
@Composable
fun GraphCanvas(
    nodes: List<GraphNode>,
    edges: List<GraphEdge>,
    focusNodeLabel: String? = null,
    onNodeClick: ((String) -> Unit)? = null,
    evolution: GraphEvolutionState = GraphEvolutionState(
        nodeGlowEnabled = true,
        edgeGradientEnabled = true,
        breathingEnabled = true,
        flowParticlesEnabled = false,
        glowIntensityMultiplier = 0.5f,
    ),
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val tertiaryTextColor = onSurfaceVariantColor.copy(alpha = 0.5f)

    val engine = remember { ForceGraphEngine(scope) }
    val physicsNodes = engine.nodes

    // Viewport（v2.0 新增）：scale + translation 由 detectTransformGestures 改写
    var viewportScale by remember { mutableFloatStateOf(1f) }
    var viewportOffset by remember { mutableStateOf(Offset.Zero) }

    // 拖拽状态
    var dragNodeId by remember { mutableStateOf<String?>(null) }
    var isDraggingNode by remember { mutableStateOf(false) }
    var lastPanPosition by remember { mutableStateOf(Offset.Zero) }
    var panVelocity by remember { mutableStateOf(Offset.Zero) }
    var focusNodeId by remember { mutableStateOf<String?>(null) }
    var wateredNodeId by remember { mutableStateOf<String?>(null) }
    var waterAnimProgress by remember { mutableFloatStateOf(0f) }

    // Tooltip
    var tooltipNodeId by remember { mutableStateOf<String?>(null) }
    var tooltipDismissJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    fun dismissTooltip() { tooltipNodeId = null }
    fun showTooltip(nodeId: String) {
        tooltipNodeId = nodeId
        tooltipDismissJob?.cancel()
        tooltipDismissJob = scope.launch {
            delay(3000)
            dismissTooltip()
        }
    }

    val animatedScale by animateFloatAsState(
        targetValue = viewportScale,
        animationSpec = tween(150),
        label = "viewportScale",
    )

    val textPaint = remember {
        android.graphics.Paint().apply {
            isAntiAlias = true
            isFakeBoldText = false
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT
        }
    }

    // Flow particle state
    var flowParticles by remember { mutableStateOf(listOf<FlowParticle>()) }

    // Load graph
    LaunchedEffect(nodes, edges) {
        engine.evolution = evolution
        engine.loadGraph(nodes, edges)
    }

    LaunchedEffect(evolution) { engine.evolution = evolution }

    LaunchedEffect(evolution.flowParticlesEnabled) {
        if (evolution.flowParticlesEnabled) {
            while (true) {
                flowParticles = engine.updateFlowParticles()
                delay(50)
            }
        }
    }

    LaunchedEffect(focusNodeLabel, nodes) {
        if (focusNodeLabel != null) {
            val match = physicsNodes.find { it.label.equals(focusNodeLabel, ignoreCase = true) }
            if (match != null) {
                engine.focusOnNode(match.id)
                focusNodeId = match.id
            }
        }
    }

    LaunchedEffect(wateredNodeId) {
        if (wateredNodeId != null) {
            waterAnimProgress = 1f
            delay(800)
            waterAnimProgress = 0f
            wateredNodeId = null
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(420.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = surfaceVariantColor),
    ) {
        Column(modifier = Modifier.padding(Dimens.SpaceLg)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "知识图谱",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.width(Dimens.SpaceSm))
                Text(
                    if (evolution.nodeGlowEnabled) "✨ 进化·${if (evolution.constellationGridEnabled) "星图" else if (evolution.flowParticlesEnabled) "流辉" else if (evolution.breathingEnabled) "呼吸" else "初级"}"
                    else "拖拽 · 缩放 · 点击浇水",
                    style = MaterialTheme.typography.labelSmall,
                    color = tertiaryTextColor,
                )
                Spacer(Modifier.weight(1f))
                if (focusNodeId != null) {
                    Text(
                        "× 退出聚焦",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.pointerInput(Unit) {
                            detectTapGestures {
                                engine.resetFocus()
                                focusNodeId = null
                            }
                        },
                    )
                }
            }

            Spacer(Modifier.height(Dimens.SpaceSm))

            // Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                        MaterialTheme.shapes.small,
                    ),
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        // 双指缩放 + 拖拽 pan
                        .pointerInput(physicsNodes, edges) {
                            detectTransformGestures(
                                panZoomLock = false,
                            ) { centroid, pan, zoom, _ ->
                                viewportScale = (viewportScale * zoom).coerceIn(0.25f, 4f)
                                // 关键：仅双指缩放时应用 transform pan。
                                // 单指时 transform 的 pan 与下方 detectDragGestures 的画布平移会
                                // 同时触发 → 视口位移双倍应用（v2.1 修复）
                                if (zoom != 1f) {
                                    viewportOffset += pan
                                }
                            }
                        }
                        // 拖拽节点 / 平移画布
                        .pointerInput(physicsNodes, edges) {
                            detectDragGestures(
                                onDragStart = { startPos ->
                                    val adjX = (startPos.x - viewportOffset.x) / animatedScale
                                    val adjY = (startPos.y - viewportOffset.y) / animatedScale
                                    val hitNode = findHitNode(physicsNodes, adjX, adjY)
                                    if (hitNode != null) {
                                        isDraggingNode = true
                                        dragNodeId = hitNode.id
                                        engine.pinNode(hitNode.id)
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    } else {
                                        lastPanPosition = startPos
                                        panVelocity = Offset.Zero
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    if (isDraggingNode && dragNodeId != null) {
                                        val mx = (change.position.x - viewportOffset.x) / animatedScale
                                        val my = (change.position.y - viewportOffset.y) / animatedScale
                                        engine.dragNode(dragNodeId!!, mx, my)
                                    } else {
                                        panVelocity = Offset(
                                            dragAmount.x.coerceIn(-80f, 80f),
                                            dragAmount.y.coerceIn(-80f, 80f),
                                        )
                                        viewportOffset += dragAmount
                                        lastPanPosition = change.position
                                    }
                                },
                                onDragEnd = {
                                    if (isDraggingNode && dragNodeId != null) {
                                        engine.releaseNode(dragNodeId!!)
                                        isDraggingNode = false
                                        dragNodeId = null
                                    }
                                },
                                onDragCancel = {
                                    if (isDraggingNode && dragNodeId != null) {
                                        engine.releaseNode(dragNodeId!!)
                                        isDraggingNode = false
                                        dragNodeId = null
                                    }
                                },
                            )
                        }
                        // 点击 / 双击 / 长按
                        .pointerInput(physicsNodes, edges) {
                            detectTapGestures(
                                onTap = { pos ->
                                    val adjX = (pos.x - viewportOffset.x) / animatedScale
                                    val adjY = (pos.y - viewportOffset.y) / animatedScale
                                    val hitNode = findHitNode(physicsNodes, adjX, adjY)
                                    if (hitNode != null) {
                                        engine.boostNodeVitality(hitNode.id)
                                        wateredNodeId = hitNode.id
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if (focusNodeId == hitNode.id) {
                                            engine.resetFocus()
                                            focusNodeId = null
                                        } else {
                                            engine.focusOnNode(hitNode.id)
                                            focusNodeId = hitNode.id
                                            onNodeClick?.invoke(hitNode.id)
                                        }
                                    } else {
                                        if (focusNodeId != null) {
                                            engine.resetFocus()
                                            focusNodeId = null
                                        }
                                    }
                                },
                                onDoubleTap = { pos ->
                                    val adjX = (pos.x - viewportOffset.x) / animatedScale
                                    val adjY = (pos.y - viewportOffset.y) / animatedScale
                                    val hitNode = findHitNode(physicsNodes, adjX, adjY)
                                    if (hitNode != null) {
                                        // 双击节点：聚焦到 1 跳邻域
                                        engine.focusOnNode(hitNode.id)
                                        focusNodeId = hitNode.id
                                    } else {
                                        // 双击空白：复位
                                        viewportScale = 1f
                                        viewportOffset = Offset.Zero
                                    }
                                },
                                onLongPress = { pos ->
                                    val adjX = (pos.x - viewportOffset.x) / animatedScale
                                    val adjY = (pos.y - viewportOffset.y) / animatedScale
                                    val hitNode = findHitNode(physicsNodes, adjX, adjY)
                                    if (hitNode != null) {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        showTooltip(hitNode.id)
                                    }
                                },
                            )
                        },
                ) {
                    val w = size.width
                    val h = size.height
                    engine.setCanvasSize(w / animatedScale, h / animatedScale)
                    translate(viewportOffset.x, viewportOffset.y) {
                        scale(animatedScale, animatedScale, pivot = Offset.Zero) {
                            drawGraphContent(
                                w = w / animatedScale,
                                h = h / animatedScale,
                                engine = engine,
                                physicsNodes = physicsNodes,
                                edges = edges,
                                evolution = evolution,
                                flowParticles = flowParticles,
                                wateredNodeId = wateredNodeId,
                                waterAnimProgress = waterAnimProgress,
                                focusNodeId = focusNodeId,
                                tooltipNodeId = tooltipNodeId,
                                textPaint = textPaint,
                                density = density,
                            )
                        }
                    }
                }

                // Zoom indicator overlay
                if (viewportScale > 1.05f || viewportScale < 0.95f) {
                    Text(
                        text = "${(viewportScale * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = tertiaryTextColor.copy(alpha = 0.7f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(Dimens.SpaceSm)
                            .background(
                                surfaceVariantColor.copy(alpha = 0.9f),
                                MaterialTheme.shapes.small,
                            )
                            .padding(horizontal = Dimens.SpaceSm, vertical = Dimens.SpaceXs / 2),
                    )
                }
            }

            // ── Legend ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Dimens.SpaceSm),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
                ) {
                    LegendDot(color = BrandPrimary, label = "概念")
                    LegendDot(color = BrandAccent, label = "实体")
                    LegendDot(color = Color(0xFF10B981), label = "方法")
                    LegendDot(color = Color(0xFFEC4899), label = "主题")
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
                ) {
                    Text(
                        "节点越大 = 提及越多",
                        style = MaterialTheme.typography.labelSmall,
                        color = tertiaryTextColor.copy(alpha = 0.7f),
                    )
                    if (evolution.nodeGlowEnabled) {
                        Text(
                            "🟡 光环 = 生命力",
                            style = MaterialTheme.typography.labelSmall,
                            color = tertiaryTextColor.copy(alpha = 0.7f),
                        )
                    }
                    Text(
                        "👆 点击 = 浇水",
                        style = MaterialTheme.typography.labelSmall,
                        color = tertiaryTextColor.copy(alpha = 0.6f),
                    )
                }
            }
        }
    }
}

/**
 * 把图谱内容绘制逻辑抽出来，避免 translate+scale 嵌套时引用 viewport 变量污染。
 * 全部坐标系以"画布物理像素"计。
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGraphContent(
    w: Float,
    h: Float,
    engine: ForceGraphEngine,
    physicsNodes: List<PhysicsNode>,
    edges: List<GraphEdge>,
    evolution: GraphEvolutionState,
    flowParticles: List<FlowParticle>,
    wateredNodeId: String?,
    waterAnimProgress: Float,
    focusNodeId: String?,
    tooltipNodeId: String?,
    textPaint: android.graphics.Paint,
    density: androidx.compose.ui.unit.Density,
) {
    // 网格
    if (evolution.constellationGridEnabled) {
        val gridSize = 80f
        val gCountX = (w / gridSize).toInt() + 1
        val gCountY = (h / gridSize).toInt() + 1
        for (gx in 0..gCountX) {
            for (gy in 0..gCountY) {
                val x = gx * gridSize
                val y = gy * gridSize
                drawCircle(color = Color.White.copy(alpha = 0.03f), radius = 1.5f, center = Offset(x, y))
                if (gx < gCountX) {
                    drawLine(
                        Color.White.copy(alpha = 0.01f),
                        Offset(x, y),
                        Offset(x + gridSize, y),
                        strokeWidth = 0.5f,
                    )
                }
                if (gy < gCountY) {
                    drawLine(
                        Color.White.copy(alpha = 0.01f),
                        Offset(x, y),
                        Offset(x, y + gridSize),
                        strokeWidth = 0.5f,
                    )
                }
            }
        }
    } else {
        val gridSize = 60f
        val gCountX = (w / gridSize).toInt() + 1
        val gCountY = (h / gridSize).toInt() + 1
        for (gx in 0..gCountX) {
            val x = gx * gridSize
            drawLine(Color.White.copy(alpha = 0.015f), Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
        }
        for (gy in 0..gCountY) {
            val y = gy * gridSize
            drawLine(Color.White.copy(alpha = 0.015f), Offset(0f, y), Offset(w, y), strokeWidth = 1f)
        }
    }

    // 边
    edges.forEach { edge ->
        val src = physicsNodes.find { it.id == edge.source }
        val dst = physicsNodes.find { it.id == edge.target }
        if (src != null && dst != null && src.opacity > 0.1f && dst.opacity > 0.1f) {
            val srcX = src.x + src.breathOffsetX
            val srcY = src.y + src.breathOffsetY
            val dstX = dst.x + dst.breathOffsetX
            val dstY = dst.y + dst.breathOffsetY
            val baseAlpha = (src.opacity + dst.opacity) / 2f
            val typeStyle = EdgeTypeStyle.fromRelationType(edge.type)
            val alpha = baseAlpha * typeStyle.alpha

            val path = Path().apply {
                moveTo(srcX, srcY)
                lineTo(dstX, dstY)
            }
            if (evolution.edgeGradientEnabled) {
                drawPath(
                    path,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            src.color.copy(alpha = alpha),
                            dst.color.copy(alpha = alpha),
                        ),
                        start = Offset(srcX, srcY),
                        end = Offset(dstX, dstY),
                    ),
                    style = Stroke(width = Dimens.GraphEdgeStroke * edge.weight, cap = StrokeCap.Round),
                )
            } else {
                drawPath(
                    path,
                    color = Color.White.copy(alpha = alpha),
                    style = Stroke(width = Dimens.GraphEdgeStroke * edge.weight, cap = StrokeCap.Round),
                )
            }
            if (src.glowIntensity > 0f && dst.glowIntensity > 0f) {
                drawPath(
                    path,
                    color = Color.White.copy(alpha = 0.12f),
                    style = Stroke(width = 4f * edge.weight, cap = StrokeCap.Round),
                )
            }
        }
    }

    // 节点
    physicsNodes.forEach { node ->
        val drawX = node.x + node.breathOffsetX
        val drawY = node.y + node.breathOffsetY
        val r = node.radius
        val c = node.color
        val glowMult = if (evolution.nodeGlowEnabled) 0.5f + 0.5f * evolution.glowIntensityMultiplier else 0f
        val stage = node.growthStage

        // Water splash
        val isWatered = node.id == wateredNodeId
        if (isWatered && waterAnimProgress > 0f) {
            val splashR = r + 20f * waterAnimProgress
            drawCircle(
                color = BrandAccent.copy(alpha = 0.3f * waterAnimProgress),
                radius = splashR,
                center = Offset(drawX, drawY),
                style = Stroke(width = 2f * waterAnimProgress),
            )
        }

        // Growth stage leaves
        if (stage >= 3) {
            val leafCount = (stage - 1) * 2
            val leafAlpha = ((stage - 2) / 3f).coerceIn(0.2f, 0.8f) * node.opacity
            for (i in 0 until leafCount) {
                val angle = i * (360f / leafCount) * kotlin.math.PI.toFloat() / 180f
                val leafDist = r + 8f + (stage - 2) * 3f
                drawCircle(
                    color = BrandAccent.copy(alpha = leafAlpha),
                    radius = 2.5f + (stage - 2) * 0.5f,
                    center = Offset(drawX + cos(angle) * leafDist, drawY + sin(angle) * leafDist),
                )
            }
        }

        // Glow
        if (node.glowIntensity > 0f || glowMult > 0f) {
            val glow = maxOf(node.glowIntensity, glowMult * 0.3f)
            val vitalityGlow = node.vitality * 0.3f
            val glowR = r + 12f * (glow + vitalityGlow)
            drawCircle(
                color = c.copy(alpha = (0.3f * glow + vitalityGlow * 0.5f)),
                radius = glowR,
                center = Offset(drawX, drawY),
            )
        }

        // Fill
        val fillAlpha = if (evolution.nodeGlowEnabled) 0.25f * node.opacity else 0.2f * node.opacity
        drawCircle(color = c.copy(alpha = fillAlpha), radius = r, center = Offset(drawX, drawY))

        // Border
        val vitalityTint = Color(
            red = BrandPrimary.red * node.vitality + c.red * (1f - node.vitality),
            green = BrandPrimary.green * node.vitality + c.green * (1f - node.vitality),
            blue = BrandPrimary.blue * node.vitality + c.blue * (1f - node.vitality),
            alpha = 0.8f * node.opacity,
        )
        drawCircle(
            color = if (node.vitality > 0.5f) vitalityTint else c.copy(alpha = 0.8f * node.opacity),
            radius = r,
            center = Offset(drawX, drawY),
            style = Stroke(width = 2f),
        )

        // Highlight
        drawCircle(
            color = c.copy(alpha = 0.15f * node.opacity),
            radius = r * 0.6f,
            center = Offset(drawX - r * 0.2f, drawY - r * 0.2f),
        )

        // Label
        if (node.opacity > 0.3f) {
            textPaint.color = node.color.copy(alpha = 0.9f * node.opacity).toArgb()
            textPaint.textSize = 11f * density.density
            drawContext.canvas.nativeCanvas.drawText(node.label, drawX, drawY + r + 14f, textPaint)
        }
    }

    // Focus indicator
    focusNodeId?.let { fid ->
        val focusNode = physicsNodes.find { it.id == fid }
        if (focusNode != null) {
            val fx = focusNode.x + focusNode.breathOffsetX
            val fy = focusNode.y + focusNode.breathOffsetY
            val pulseR = focusNode.radius + 20f
            drawCircle(
                color = focusNode.color.copy(alpha = 0.1f),
                radius = pulseR,
                center = Offset(fx, fy),
                style = Stroke(width = 2f),
            )
        }
    }

    // Tooltip
    tooltipNodeId?.let { tid ->
        val tipNode = physicsNodes.find { it.id == tid }
        if (tipNode != null) {
            val tx = tipNode.x + tipNode.breathOffsetX
            val ty = tipNode.y + tipNode.breathOffsetY - tipNode.radius - 50f
            val tipW = 140f
            val tipH = 70f
            drawRoundRect(
                color = Color(0xCC1A1417),
                topLeft = Offset(tx - tipW / 2f, ty - tipH / 2f),
                size = androidx.compose.ui.geometry.Size(tipW, tipH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f),
            )
            drawRoundRect(
                color = BrandPrimary.copy(alpha = 0.3f),
                topLeft = Offset(tx - tipW / 2f, ty - tipH / 2f),
                size = androidx.compose.ui.geometry.Size(tipW, tipH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f),
                style = Stroke(width = 1f),
            )
            textPaint.color = BrandPrimary.toArgb()
            textPaint.textSize = 12f * density.density
            drawContext.canvas.nativeCanvas.drawText(tipNode.label, tx, ty - 16f, textPaint)
            val barW = 100f
            val barH = 6f
            val barX = tx - barW / 2f
            val barY = ty - 2f
            drawRoundRect(
                color = Color(0x33FFFFFF),
                topLeft = Offset(barX, barY),
                size = androidx.compose.ui.geometry.Size(barW, barH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f),
            )
            val vitalityW = barW * tipNode.vitality
            drawRoundRect(
                color = BrandAccent,
                topLeft = Offset(barX, barY),
                size = androidx.compose.ui.geometry.Size(vitalityW, barH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f),
            )
            val infoText = "${engine.getGrowthStageLabel(tipNode.growthStage)} · ${tipNode.interactionCount}次互动"
            textPaint.color = BrandAccent.copy(alpha = 0.7f).toArgb()
            textPaint.textSize = 9f * density.density
            drawContext.canvas.nativeCanvas.drawText(infoText, tx, ty + 22f, textPaint)
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(Dimens.IconSm / 2)
                .background(color, MaterialTheme.shapes.extraLarge),
        )
        Spacer(Modifier.width(Dimens.SpaceXs))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun findHitNode(
    physicsNodes: List<PhysicsNode>,
    adjX: Float,
    adjY: Float,
    hitRadius: Float = 25f,
): PhysicsNode? {
    val closest = physicsNodes.minByOrNull { n ->
        val dx = n.x - adjX
        val dy = n.y - adjY
        sqrt(dx * dx + dy * dy)
    } ?: return null
    val dist = sqrt(
        (closest.x - adjX) * (closest.x - adjX) +
            (closest.y - adjY) * (closest.y - adjY),
    )
    return if (dist < closest.radius + hitRadius) closest else null
}
