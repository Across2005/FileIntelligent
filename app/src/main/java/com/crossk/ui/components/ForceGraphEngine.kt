package com.crossk.ui.components

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.crossk.data.GraphEdge
import com.crossk.data.GraphEvolutionState
import com.crossk.data.GraphNode
import com.crossk.data.RelationType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * v2.0 物理节点 — 节点位置改为 Compose snapshot state，引擎 step 后写入
 * 会触发 Canvas 自动重绘（修复 v1 "_nodePositions 写没人读" 的断流）。
 */
class PhysicsNode(
    val id: String,
    val label: String,
    val color: Color,
    /** x/y 写在 SnapshotStateList 中；Canvas 通过 list[index] 订阅 */
    val xState: SnapshotStateList<Float>,
    val yState: SnapshotStateList<Float>,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var radius: Float = 28f,
    var pinned: Boolean = false,
    var opacity: Float = 1f,
    var glowIntensity: Float = 0f,
    var breathOffsetX: Float = 0f,
    var breathOffsetY: Float = 0f,
    /** Concept Pot 活力 0..1 */
    var vitality: Float = 0.3f,
    var growthStage: Int = 1,
    var interactionCount: Int = 0,
    var lastInteractionTime: Long = 0L,
) {
    val x: Float get() = xState[0]
    val y: Float get() = yState[0]
}

data class PhysicsEdge(
    val sourceId: String,
    val targetId: String,
    val type: RelationType = RelationType.CO_OCCURS,
    val weight: Float = 1f,
)

data class FlowParticle(
    val edgeIndex: Int,
    var progress: Float = Random.nextFloat(),
    val speed: Float = 0.003f + Random.nextFloat() * 0.005f,
    val size: Float = 2.5f + Random.nextFloat() * 2f,
)

/**
 * v2.0 力导向引擎：
 * - 节点位置用 SnapshotStateList 单元素 wrapper，Canvas 订阅触发重绘
 * - 邻接表用 Map<id, Int>（替代 v1 _nodes.indexOfFirst O(E·N)）
 * - 帧驱动用协程 + delay，节点位置变更走 Snapshot 批量写
 * - 保留 v1 的呼吸 / 粒子 / 活力机制
 * - 新增 O(n log n) 简化空间桶（v2.1 替换为正式 quadtree）
 */
class ForceGraphEngine(
    private val scope: CoroutineScope,
) {
    var repulsionStrength = 60000f
    var attractionStrength = 0.008f
    var centerGravity = 0.003f
    var damping = 0.85f
    var minDistance = 30f
    var restLength = 160f
    var maxVelocity = 20f
    var energyThreshold = 0.5f
    var boundaryRadiusFraction = 0.42f
    var boundaryRestoreStrength = 0.025f

    var evolution: GraphEvolutionState = GraphEvolutionState()

    private val _nodes = mutableListOf<PhysicsNode>()
    val nodes: List<PhysicsNode> get() = _nodes

    private val _edges = mutableListOf<PhysicsEdge>()
    val edges: List<PhysicsEdge> get() = _edges

    /** id → node 索引（替代 v1 indexOfFirst） */
    private val nodeIndex = mutableMapOf<String, Int>()

    /** srcId/tgtId → edge 索引数组 */
    private val adjacency = mutableMapOf<String, MutableList<Int>>()

    private var simulationJob: Job? = null
    private var breathingJob: Job? = null
    private var vitalityJob: Job? = null
    private var isRunning = false
    private var energy = Float.MAX_VALUE

    private var canvasWidth = 800f
    private var canvasHeight = 1200f

    private var breathPhase = 0f

    private val _flowParticles = mutableListOf<FlowParticle>()
    val flowParticles: List<FlowParticle> get() = _flowParticles

    fun setCanvasSize(w: Float, h: Float) {
        canvasWidth = w
        canvasHeight = h
    }

    fun loadGraph(
        graphNodes: List<GraphNode>,
        graphEdges: List<GraphEdge>,
        centerX: Float = canvasWidth / 2f,
        centerY: Float = canvasHeight / 2f,
    ) {
        stop()

        nodeIndex.clear()
        _nodes.clear()
        graphNodes.forEachIndexed { idx, n ->
            val xList = mutableStateListOf(centerX + Random.nextFloat() * 200f - 100f)
            val yList = mutableStateListOf(centerY + Random.nextFloat() * 200f - 100f)
            val node = PhysicsNode(
                id = n.id,
                label = n.label,
                color = n.color,
                xState = xList,
                yState = yList,
                radius = 16f + n.size * 8f,
                vitality = 0.2f + Random.nextFloat() * 0.4f,
            )
            nodeIndex[n.id] = idx
            _nodes.add(node)
        }
        _nodes.forEach { updateGrowthStage(it) }

        adjacency.clear()
        _edges.clear()
        graphEdges.forEachIndexed { idx, e ->
            _edges.add(
                PhysicsEdge(
                    sourceId = e.source,
                    targetId = e.target,
                    type = e.type,
                    weight = e.weight,
                ),
            )
            adjacency.getOrPut(e.source) { mutableListOf() }.add(idx)
            adjacency.getOrPut(e.target) { mutableListOf() }.add(idx)
        }

        // 初始布局：按度数螺旋
        val placed = mutableSetOf<String>()
        val queue = mutableListOf<String>()
        val adjCount: (String) -> Int = { id -> adjacency[id]?.size ?: 0 }
        val sorted = _nodes.sortedByDescending { adjCount(it.id) }
        if (sorted.isNotEmpty()) queue.add(sorted.first().id)
        var angleStep = 0f
        var radius = min(canvasWidth, canvasHeight) * 0.25f
        var level = 0
        while (queue.isNotEmpty()) {
            // 用 removeAt(0) 而非 removeFirst()：后者在 Java 17 运行时（测试 JVM）
            // 会绑定到 Java 21 才有的 java.util.List.removeFirst()，触发 NoSuchMethodError
            val currentId = queue.removeAt(0)
            if (currentId in placed) continue
            placed.add(currentId)
            val node = _nodes[nodeIndex[currentId] ?: continue]
            if (level == 0) {
                node.xState[0] = centerX
                node.yState[0] = centerY
            } else {
                node.xState[0] = centerX + radius * cos(angleStep)
                node.yState[0] = centerY + radius * sin(angleStep)
                angleStep += 2.3999f
            }
            adjacency[currentId]?.forEach { edgeIdx ->
                val e = _edges[edgeIdx]
                listOf(e.sourceId, e.targetId).forEach { nb ->
                    if (nb != currentId && nb !in placed && nb !in queue) queue.add(nb)
                }
            }
            if (level == 0) {
                level++
                radius = min(canvasWidth, canvasHeight) * 0.35f
                angleStep = 0f
            }
        }
        _nodes.filter { it.id !in placed }.forEachIndexed { i, n ->
            n.xState[0] = centerX + (i - 3f) * 120f
            n.yState[0] = centerY + radius * 1.5f
        }

        initFlowParticles()
        startSimulation()
        startBreathing()
        startVitalityDecay()
    }

    // ── Concept Pot ──

    fun boostNodeVitality(nodeId: String, amount: Float = 0.15f) {
        val idx = nodeIndex[nodeId] ?: return
        val node = _nodes[idx]
        node.vitality = (node.vitality + amount).coerceIn(0f, 1f)
        node.interactionCount++
        node.lastInteractionTime = System.currentTimeMillis()
        updateGrowthStage(node)
    }

    fun getGrowthStageLabel(stage: Int): String = when (stage) {
        1 -> "种子"
        2 -> "嫩芽"
        3 -> "幼苗"
        4 -> "开花"
        5 -> "盛放"
        else -> "未知"
    }

    private fun getStageFromVitality(v: Float): Int = when {
        v < 0.2f -> 1
        v < 0.4f -> 2
        v < 0.6f -> 3
        v < 0.8f -> 4
        else -> 5
    }

    private fun updateGrowthStage(node: PhysicsNode) {
        val newStage = getStageFromVitality(node.vitality)
        if (newStage != node.growthStage) {
            node.growthStage = newStage
            node.radius = node.radius.coerceAtLeast(16f) + (newStage - 1) * 2f
        }
    }

    private fun startVitalityDecay() {
        vitalityJob?.cancel()
        vitalityJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(3000)
                val now = System.currentTimeMillis()
                _nodes.forEach { node ->
                    val timeSinceInteraction = now - node.lastInteractionTime
                    val decayRate = if (timeSinceInteraction > 60_000L) 0.02f else 0.005f
                    node.vitality = (node.vitality - decayRate).coerceIn(0f, 1f)
                    updateGrowthStage(node)
                }
            }
        }
    }

    // ── 粒子 ──

    private fun initFlowParticles() {
        _flowParticles.clear()
        if (_edges.isNotEmpty()) {
            val particleCount = (_edges.size * 2).coerceAtMost(20)
            repeat(particleCount) {
                _flowParticles.add(FlowParticle(edgeIndex = it % _edges.size))
            }
        }
    }

    fun updateFlowParticles(): List<FlowParticle> {
        if (!evolution.flowParticlesEnabled) return emptyList()
        _flowParticles.forEach { particle ->
            particle.progress += particle.speed
            if (particle.progress >= 1f) particle.progress = 0f
        }
        return _flowParticles
    }

    // ── 模拟 ──

    private fun startSimulation() {
        stop()
        isRunning = true
        energy = Float.MAX_VALUE
        simulationJob = scope.launch(Dispatchers.Default) {
            var frameCount = 0
            val maxFrames = 600
            while (isActive && isRunning && energy > energyThreshold && frameCount < maxFrames) {
                frameCount++
                simulateStep()
                energy = if (_nodes.isEmpty()) 0f else {
                    _nodes.sumOf { n -> sqrt(n.vx * n.vx + n.vy * n.vy).toDouble() }.toFloat() / _nodes.size
                }
                delay(16)
            }
            isRunning = false
        }
    }

    private fun startBreathing() {
        breathingJob?.cancel()
        breathingJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                if (evolution.breathingEnabled) {
                    breathPhase += 0.03f
                    _nodes.forEachIndexed { i, node ->
                        val amp = 1.5f + (i % 5) * 0.3f
                        val phase = breathPhase + i * 0.7f
                        val vitalityMult = 0.5f + node.vitality * 0.5f
                        node.breathOffsetX = sin(phase * 0.7f) * amp * 0.5f * vitalityMult
                        node.breathOffsetY = sin(phase) * amp * vitalityMult
                    }
                } else {
                    _nodes.forEach { n ->
                        n.breathOffsetX = 0f
                        n.breathOffsetY = 0f
                    }
                }
                delay(50)
            }
        }
    }

    /**
     * v2.0 简化版：先按 grid bucket 把节点分到 N×N 网格，斥力只算同/邻桶。
     * 实际复杂度大约 O(n · k) 其中 k = 平均桶内节点数，比 O(n²) 显著快。
     */
    private fun simulateStep() {
        val n = _nodes.size
        if (n == 0) return

        val fx = FloatArray(n)
        val fy = FloatArray(n)

        // 1. 空间桶（grid-based）
        val cellSize = restLength
        val cols = (canvasWidth / cellSize).toInt().coerceAtLeast(1)
        val rows = (canvasHeight / cellSize).toInt().coerceAtLeast(1)
        val cellIdx = IntArray(n) { i ->
            val cx = (_nodes[i].x / cellSize).toInt().coerceIn(0, cols - 1)
            val cy = (_nodes[i].y / cellSize).toInt().coerceIn(0, rows - 1)
            cy * cols + cx
        }
        val buckets = Array(cols * rows) { mutableListOf<Int>() }
        for (i in 0 until n) buckets[cellIdx[i]].add(i)

        // 2. 邻桶斥力
        for (i in 0 until n) {
            val ci = cellIdx[i]
            val cx = ci % cols
            val cy = ci / cols
            for (dx in -1..1) {
                for (dy in -1..1) {
                    val nx = cx + dx
                    val ny = cy + dy
                    if (nx !in 0 until cols || ny !in 0 until rows) continue
                    val ni = ny * cols + nx
                    for (j in buckets[ni]) {
                        if (j <= i) continue
                        val niNode = _nodes[i]
                        val njNode = _nodes[j]
                        val ddx = niNode.x - njNode.x
                        val ddy = niNode.y - njNode.y
                        val dist = max(sqrt(ddx * ddx + ddy * ddy), minDistance)
                        val force = repulsionStrength / (dist * dist)
                        fx[i] += force * ddx / dist
                        fy[i] += force * ddy / dist
                        fx[j] -= force * ddx / dist
                        fy[j] -= force * ddy / dist
                    }
                }
            }
        }

        // 3. 边引力（用 Map 索引替代 indexOfFirst）
        for (edge in _edges) {
            val src = nodeIndex[edge.sourceId] ?: continue
            val dst = nodeIndex[edge.targetId] ?: continue
            val ddx = _nodes[dst].x - _nodes[src].x
            val ddy = _nodes[dst].y - _nodes[src].y
            val dist = max(sqrt(ddx * ddx + ddy * ddy), 1f)
            val displacement = dist - restLength
            val force = attractionStrength * edge.weight * displacement
            fx[src] += force * ddx / dist
            fy[src] += force * ddy / dist
            fx[dst] -= force * ddx / dist
            fy[dst] -= force * ddy / dist
        }

        // 4. 中心引力 + 边界
        val cxc = canvasWidth / 2f
        val cyc = canvasHeight / 2f
        val boundaryR = min(canvasWidth, canvasHeight) * boundaryRadiusFraction
        for (i in 0 until n) {
            val ni = _nodes[i]
            fx[i] += (cxc - ni.x) * centerGravity
            fy[i] += (cyc - ni.y) * centerGravity
            val ddx = ni.x - cxc
            val ddy = ni.y - cyc
            val dist = sqrt(ddx * ddx + ddy * ddy)
            if (dist > boundaryR) {
                val overshoot = dist - boundaryR
                val restoreForce = overshoot * boundaryRestoreStrength
                fx[i] -= (ddx / dist) * restoreForce
                fy[i] -= (ddy / dist) * restoreForce
            }
        }

        // 5. 积分（写入 SnapshotStateList → 自动触发重绘）
        val hardPad = 10f
        for (i in 0 until n) {
            val node = _nodes[i]
            if (node.pinned) continue
            val newVx = (node.vx + fx[i]) * damping
            val newVy = (node.vy + fy[i]) * damping
            val speed = sqrt(newVx * newVx + newVy * newVy)
            val scale = if (speed > maxVelocity) maxVelocity / speed else 1f
            node.vx = newVx * scale
            node.vy = newVy * scale
            node.xState[0] = (node.x + node.vx).coerceIn(hardPad, canvasWidth - hardPad)
            node.yState[0] = (node.y + node.vy).coerceIn(hardPad, canvasHeight - hardPad)
        }
    }

    fun pause() { isRunning = false }
    fun resume() { if (!isRunning) startSimulation() }

    fun stop() {
        isRunning = false
        simulationJob?.cancel()
        simulationJob = null
        breathingJob?.cancel()
        breathingJob = null
        vitalityJob?.cancel()
        vitalityJob = null
    }

    fun dragNode(nodeId: String, newX: Float, newY: Float) {
        val idx = nodeIndex[nodeId] ?: return
        val node = _nodes[idx]
        node.xState[0] = newX
        node.yState[0] = newY
        node.vx = 0f
        node.vy = 0f
        if (!isRunning) startSimulation()
    }

    fun releaseNode(nodeId: String) {
        nodeIndex[nodeId]?.let { _nodes[it].pinned = false }
    }

    fun pinNode(nodeId: String) {
        nodeIndex[nodeId]?.let { _nodes[it].pinned = true }
    }

    fun focusOnNode(nodeId: String) {
        _nodes.forEach { n ->
            n.opacity = if (n.id == nodeId) 1f else 0.25f
            n.glowIntensity = if (n.id == nodeId) 1f else 0f
        }
        val connectedIds = _edges
            .filter { it.sourceId == nodeId || it.targetId == nodeId }
            .flatMap { listOf(it.sourceId, it.targetId) }
            .toSet()
        _nodes.forEach { n ->
            if (n.id in connectedIds) {
                n.opacity = 1f
                n.glowIntensity = 0.5f
            }
        }
    }

    fun resetFocus() {
        _nodes.forEach { n ->
            n.opacity = 1f
            n.glowIntensity = 0f
        }
    }
}
