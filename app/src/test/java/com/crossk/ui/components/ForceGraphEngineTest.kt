package com.crossk.ui.components

import com.crossk.data.GraphEdge
import com.crossk.data.GraphNode
import com.crossk.data.NodeType
import com.crossk.data.RelationType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * v2.0 ForceGraphEngine 单测 — 验证空间桶索引、邻接索引、语义的正确性。
 * 不启动协程模拟（仅同步 API）。
 */
class ForceGraphEngineTest {

    private lateinit var scope: CoroutineScope
    private lateinit var engine: ForceGraphEngine

    @Before
    fun setUp() {
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        engine = ForceGraphEngine(scope)
    }

    @After
    fun tearDown() {
        engine.stop()
        scope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
    }

    @Test
    fun `loadGraph creates PhysicsNodes with snapshot state positions`() {
        val nodes = listOf(
            GraphNode(id = "a", label = "A", type = NodeType.CONCEPT),
            GraphNode(id = "b", label = "B", type = NodeType.METHOD),
        )
        engine.loadGraph(nodes, emptyList(), centerX = 100f, centerY = 100f)
        assertEquals(2, engine.nodes.size)
        // v2.0：xState/yState 单元素 wrapper，x() getter 必须可读
        val nodeA = engine.nodes.first { it.id == "a" }
        assertEquals(100f, nodeA.x, 0.001f)
        assertEquals(100f, nodeA.y, 0.001f)
    }

    @Test
    fun `loadGraph builds adjacency map for edges`() {
        val nodes = listOf(
            GraphNode(id = "a", label = "A", type = NodeType.CONCEPT),
            GraphNode(id = "b", label = "B", type = NodeType.METHOD),
            GraphNode(id = "c", label = "C", type = NodeType.ENTITY),
        )
        val edges = listOf(
            GraphEdge(source = "a", target = "b", type = RelationType.REFERENCES, weight = 1f),
            GraphEdge(source = "b", target = "c", type = RelationType.DERIVES_FROM, weight = 0.8f),
        )
        engine.loadGraph(nodes, edges, centerX = 100f, centerY = 100f)
        assertEquals(2, engine.edges.size)
        // edges 保留 RelationType，不再降级
        assertEquals(RelationType.REFERENCES, engine.edges[0].type)
        assertEquals(RelationType.DERIVES_FROM, engine.edges[1].type)
    }

    @Test
    fun `dragNode updates snapshot state position`() {
        val nodes = listOf(GraphNode(id = "a", label = "A", type = NodeType.CONCEPT))
        engine.loadGraph(nodes, emptyList(), centerX = 0f, centerY = 0f)
        engine.dragNode("a", 200f, 150f)
        val nodeA = engine.nodes.first()
        assertEquals(200f, nodeA.x, 0.001f)
        assertEquals(150f, nodeA.y, 0.001f)
    }

    @Test
    fun `pinNode and releaseNode toggle pinned flag`() {
        val nodes = listOf(GraphNode(id = "a", label = "A", type = NodeType.CONCEPT))
        engine.loadGraph(nodes, emptyList(), centerX = 0f, centerY = 0f)
        engine.pinNode("a")
        assertTrue(engine.nodes.first().pinned)
        engine.releaseNode("a")
        assertTrue(!engine.nodes.first().pinned)
    }

    @Test
    fun `focusOnNode dims non-1-hop nodes and highlights connected ones`() {
        val nodes = listOf(
            GraphNode(id = "a", label = "A", type = NodeType.CONCEPT),
            GraphNode(id = "b", label = "B", type = NodeType.METHOD),
            GraphNode(id = "c", label = "C", type = NodeType.ENTITY),
        )
        val edges = listOf(
            GraphEdge(source = "a", target = "b", type = RelationType.REFERENCES),
        )
        engine.loadGraph(nodes, edges, centerX = 0f, centerY = 0f)
        engine.focusOnNode("a")
        val a = engine.nodes.first { it.id == "a" }
        val b = engine.nodes.first { it.id == "b" }
        val c = engine.nodes.first { it.id == "c" }
        assertEquals(1f, a.opacity, 0.001f)
        assertEquals(1f, b.opacity, 0.001f) // 1 跳邻域
        assertEquals(0.25f, c.opacity, 0.001f) // 远端被压暗
    }

    @Test
    fun `boostNodeVitality clamps to 0_1 range`() {
        val nodes = listOf(GraphNode(id = "a", label = "A", type = NodeType.CONCEPT))
        engine.loadGraph(nodes, emptyList(), centerX = 0f, centerY = 0f)
        repeat(20) { engine.boostNodeVitality("a", amount = 0.2f) }
        val node = engine.nodes.first()
        assertTrue(node.vitality in 0f..1f)
        assertTrue(node.interactionCount == 20)
    }

    @Test
    fun `getGrowthStageLabel returns expected Chinese text`() {
        assertEquals("种子", engine.getGrowthStageLabel(1))
        assertEquals("嫩芽", engine.getGrowthStageLabel(2))
        assertEquals("幼苗", engine.getGrowthStageLabel(3))
        assertEquals("开花", engine.getGrowthStageLabel(4))
        assertEquals("盛放", engine.getGrowthStageLabel(5))
        assertEquals("未知", engine.getGrowthStageLabel(99))
    }

    @Test
    fun `resetFocus restores all node opacities to 1`() {
        val nodes = listOf(
            GraphNode(id = "a", label = "A", type = NodeType.CONCEPT),
            GraphNode(id = "b", label = "B", type = NodeType.METHOD),
        )
        engine.loadGraph(nodes, emptyList(), centerX = 0f, centerY = 0f)
        engine.focusOnNode("a")
        engine.resetFocus()
        engine.nodes.forEach { assertEquals(1f, it.opacity, 0.001f) }
    }
}
