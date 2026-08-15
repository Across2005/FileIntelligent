package com.crossk.ai

import com.crossk.data.RelationType
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * IM-1 unit tests for classifyRelationByPattern helper.
 *
 * The full RelationExtractionTest.kt integration test (calling analyze() then
 * extractRelationsFromAnalysis()) is slow because the production code path
 * involves heavy Chinese NLP. This file tests the helper directly so we
 * exercise just the pattern-classification logic in milliseconds.
 *
 * BUG (IM-1, pre-fix): production code did
 *     `m != null && (a in m.groupValues[0] || b in m.groupValues[0])`
 * checking entities against the FULL regex match — which is the entire
 * substring that matched the pattern. A single "导致" anywhere in a
 * sentence would set the type to DERIVES_FROM for every entity pair.
 *
 * FIX: check entities against CAPTURE GROUPS (g1, g2) so the entity
 * pair must actually occupy the documented grammatical roles.
 */
class RelationClassifierTest {

    @Test
    fun `X derives from Y — pattern captures both X and Y`() {
        // "深度学习" + "神经网络" are in the AI dictionary.
        // "基于" is a DERIVES_FROM trigger. Both entities in their
        // capture groups → DERIVES_FROM.
        val type = AnalysisEngine.classifyRelationByPatternStatic(
            sentence = "深度学习基于神经网络。",
            entityA = "深度学习",
            entityB = "神经网络",
        )
        assertEquals(RelationType.DERIVES_FROM, type)
    }

    @Test
    fun `A belongs to B — pattern captures both A and B`() {
        // "属于" is a BELONGS_TO trigger.
        val type = AnalysisEngine.classifyRelationByPatternStatic(
            sentence = "卷积神经网络属于深度学习。",
            entityA = "卷积神经网络",
            entityB = "深度学习",
        )
        assertEquals(RelationType.BELONGS_TO, type)
    }

    @Test
    fun `A contrasts with B — pattern captures both A and B`() {
        // "不同" is a CONTRASTS_WITH trigger.
        // Use a clear "X 不同于 Y" structure so the regex capture groups
        // separate the entities cleanly. (Plain "X 和 Y 不同" packs both
        // entities into group 1 with the original greedy regex, which is
        // a separate test-data concern not covered by IM-1.)
        val type = AnalysisEngine.classifyRelationByPatternStatic(
            sentence = "机器学习不同于深度学习。",
            entityA = "机器学习",
            entityB = "深度学习",
        )
        assertEquals(RelationType.CONTRASTS_WITH, type)
    }

    @Test
    fun `sentence without any pattern keyword — falls back to CO_OCCURS`() {
        val type = AnalysisEngine.classifyRelationByPatternStatic(
            sentence = "机器学习是人工智能的核心。",
            entityA = "机器学习",
            entityB = "人工智能",
        )
        assertEquals(RelationType.CO_OCCURS, type)
    }

    @Test
    fun `IM-1 bug — '基于' in different sentence does NOT classify unrelated pair`() {
        // Two sentences. "基于" appears only in sentence 1 (深度学习 ↔ 神经网络).
        // Sentence 2 has 机器学习 ↔ 人工智能 but NO pattern keyword.
        // The fix must NOT misclassify 机器学习↔人工智能 as DERIVES_FROM
        // just because "基于" is somewhere in the document.
        val sentence1 = "深度学习基于神经网络。"
        val sentence2 = "机器学习和人工智能是相关概念。"

        val type1 = AnalysisEngine.classifyRelationByPatternStatic(sentence1, "深度学习", "神经网络")
        val type2 = AnalysisEngine.classifyRelationByPatternStatic(sentence2, "机器学习", "人工智能")

        assertEquals(RelationType.DERIVES_FROM, type1)
        assertEquals(RelationType.CO_OCCURS, type2)
    }

    @Test
    fun `order independence — A in g1 B in g2 OR A in g2 B in g1 both match`() {
        // Sentence: "X 基于 Y" — captures (X, Y)
        // Pair (A=X, B=Y) should match: A in g1, B in g2
        val typeAB = AnalysisEngine.classifyRelationByPatternStatic(
            sentence = "深度学习基于神经网络。",
            entityA = "深度学习",
            entityB = "神经网络",
        )
        // Same sentence, A and B swapped
        val typeBA = AnalysisEngine.classifyRelationByPatternStatic(
            sentence = "深度学习基于神经网络。",
            entityA = "神经网络",
            entityB = "深度学习",
        )
        assertEquals(RelationType.DERIVES_FROM, typeAB)
        assertEquals(RelationType.DERIVES_FROM, typeBA)
    }
}
