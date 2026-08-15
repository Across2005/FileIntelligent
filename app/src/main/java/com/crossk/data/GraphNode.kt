package com.crossk.data

import androidx.compose.ui.graphics.Color

/**
 * v2.0 图谱节点。
 *
 * id 一律使用稳定的实体 id（由 AnalysisEngine 产出），保证跨会话一致；
 * 引擎内部用 Map<id, PhysicsNode> 索引，避免 O(n) 查找。
 */
data class GraphNode(
    val id: String,
    val label: String,
    val type: NodeType,
    val x: Float = 0f,
    val y: Float = 0f,
    val size: Float = 1f,
    val color: Color = type.color,
)

/**
 * 节点类型枚举 — v2.0 统一为 6 类（与 Entity.Type 对齐），UI 配色按类区分。
 *
 * 来源：Entity.Type → NodeType 的映射由 `fromEntityType()` 兜底，避免硬编码对照。
 */
enum class NodeType(val color: Color) {
    CONCEPT(Color(0xFF7C3AED)),
    ENTITY(Color(0xFF06B6D4)),
    FILE(Color(0xFFF59E0B)),
    TOPIC(Color(0xFFEC4899)),
    METHOD(Color(0xFF10B981)),
    PERSON(Color(0xFFF43F5E)),
    PLACE(Color(0xFF0EA5E9)),
    ;

    companion object {
        /**
         * 实体类型 → 节点类型映射。
         * UI 层不再硬编码 listOf(when(entity.type) { ... })，统一走此函数。
         */
        fun fromEntityType(type: Entity.Type): NodeType = when (type) {
            Entity.Type.CONCEPT -> CONCEPT
            Entity.Type.PERSON -> PERSON
            Entity.Type.PLACE -> PLACE
            Entity.Type.METHOD -> METHOD
            Entity.Type.TOOL -> ENTITY
            Entity.Type.EVENT -> TOPIC
        }
    }
}

/**
 * 图谱边 UI 模型 — v2.0 把 data.RelationType 完整透传（v1 强制降级为 REFERENCES 的 bug 修复）。
 */
data class GraphEdge(
    val source: String,
    val target: String,
    val type: RelationType,
    val weight: Float = 1f,
)

/**
 * v2.0 边类型 UI 映射 — 替代 v1 的 4 类硬编码。
 * RelationType 6 类全列；颜色 alpha 用于边不透明度。
 */
enum class EdgeTypeStyle(val alpha: Float) {
    CO_OCCURS(0.25f),
    REFERENCES(1.0f),
    DERIVES_FROM(0.8f),
    BELONGS_TO(0.6f),
    CONTRASTS_WITH(0.4f),
    SIMILAR_TO(0.5f),
    ;

    companion object {
        fun fromRelationType(type: RelationType): EdgeTypeStyle = when (type) {
            RelationType.CO_OCCURS -> CO_OCCURS
            RelationType.REFERENCES -> REFERENCES
            RelationType.DERIVES_FROM -> DERIVES_FROM
            RelationType.BELONGS_TO -> BELONGS_TO
            RelationType.CONTRASTS_WITH -> CONTRASTS_WITH
            RelationType.SIMILAR_TO -> SIMILAR_TO
        }
    }
}
