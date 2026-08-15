package com.crossk.data

import com.crossk.ai.AnalysisEngine
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v2.0 纯逻辑单测 — 关系抽取 v2（三级权重）与类型映射。
 * 不依赖 Android，可在 JVM 跑。
 */
class RelationExtractionTest {

    @Test
    fun `RepoResult runCatchingResult wraps success`() = runTest {
        val r = RepoResult.runCatchingResult { 42 }
        assertTrue(r is RepoResult.Ok)
        assertEquals(42, (r as RepoResult.Ok).value)
    }

    @Test
    fun `RepoResult runCatchingResult wraps failure`() = runTest {
        val r = RepoResult.runCatchingResult { throw IllegalStateException("boom") }
        assertTrue(r is RepoResult.Err)
        assertEquals("boom", (r as RepoResult.Err).message)
    }

    @Test
    fun `RepoResult map only transforms Ok`() = runTest {
        val ok: RepoResult<Int> = RepoResult.Ok(2)
        val mapped = ok.map { it * 5 }
        assertEquals(10, (mapped as RepoResult.Ok).value)

        val err: RepoResult<Int> = RepoResult.Err("nope")
        val mappedErr = err.map { it * 5 }
        assertTrue(mappedErr is RepoResult.Err)
    }

    @Test
    fun `NodeType fromEntityType maps all 6 types`() {
        val cases = listOf(
            Entity.Type.CONCEPT to NodeType.CONCEPT,
            Entity.Type.PERSON to NodeType.PERSON,
            Entity.Type.PLACE to NodeType.PLACE,
            Entity.Type.METHOD to NodeType.METHOD,
            Entity.Type.TOOL to NodeType.ENTITY,
            Entity.Type.EVENT to NodeType.TOPIC,
        )
        cases.forEach { (entityType, expectedNodeType) ->
            assertEquals(expectedNodeType, NodeType.fromEntityType(entityType))
        }
    }

    @Test
    fun `EdgeTypeStyle fromRelationType maps all 6 types`() {
        val cases = listOf(
            RelationType.CO_OCCURS to EdgeTypeStyle.CO_OCCURS,
            RelationType.REFERENCES to EdgeTypeStyle.REFERENCES,
            RelationType.DERIVES_FROM to EdgeTypeStyle.DERIVES_FROM,
            RelationType.BELONGS_TO to EdgeTypeStyle.BELONGS_TO,
            RelationType.CONTRASTS_WITH to EdgeTypeStyle.CONTRASTS_WITH,
            RelationType.SIMILAR_TO to EdgeTypeStyle.SIMILAR_TO,
        )
        cases.forEach { (relType, expectedStyle) ->
            assertEquals(expectedStyle, EdgeTypeStyle.fromRelationType(relType))
        }
    }

    @Test
    fun `Entity compact constructor creates stable id`() {
        val a = Entity(id = "ent_x", name = "AI", type = Entity.Type.CONCEPT, mentions = 1)
        val b = Entity(id = "ent_x", name = "AI", type = Entity.Type.CONCEPT, mentions = 2)
        assertEquals(a.id, b.id)
        assertEquals(a.name, b.name)
    }

    @Test
    fun `XpBreakdownItem displayText formats single and multi count`() {
        val single = XpBreakdownItem(amount = 10, label = "XP", count = 1)
        assertEquals("+10 XP", single.displayText)
        val multi = XpBreakdownItem(amount = 10, label = "XP", count = 3)
        assertEquals("+30 XP ×3", multi.displayText)
    }

    @Test
    fun `calculateLevel returns L1 for zero xp`() {
        val level = calculateLevel(0)
        assertEquals(1, level.level)
        assertTrue(level.xpProgress in 0f..1f)
    }

    @Test
    fun `calculateLevel respects thresholds`() {
        val l3 = calculateLevel(250)
        assertEquals(3, l3.level)
        val l10 = calculateLevel(3800)
        assertEquals(10, l10.level)
    }

    @Test
    fun `KnowledgeLevel isMaxLevel at top tier`() {
        val max = calculateLevel(LEVEL_THRESHOLDS.last())
        assertTrue(max.isMaxLevel)
    }

    @Test
    fun `HeatmapDay intensity is normalized`() {
        val zero = HeatmapDay(date = 0L, count = 0, intensity = 0f)
        assertEquals(0f, zero.intensity, 0.0001f)
    }

    @Test
    fun `getGraphEvolution unlocks features per level`() {
        val l1 = getGraphEvolution(1)
        // L1: 全部特效未解锁
        assertTrue(!l1.nodeGlowEnabled)
        assertTrue(!l1.edgeGradientEnabled)
        assertTrue(!l1.flowParticlesEnabled)
        assertTrue(!l1.constellationGridEnabled)
        val l4 = getGraphEvolution(4)
        assertTrue(l4.edgeGradientEnabled)
        val l8 = getGraphEvolution(8)
        assertTrue(l8.flowParticlesEnabled)
        val l10 = getGraphEvolution(10)
        assertTrue(l10.constellationGridEnabled)
    }
}
