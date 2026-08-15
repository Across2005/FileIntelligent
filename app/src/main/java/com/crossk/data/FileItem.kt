package com.crossk.data

import androidx.compose.ui.graphics.Color

data class FileItem(
    val id: String,
    val name: String,
    val path: String,
    val extension: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val tags: List<String> = emptyList(),
    val aiSummary: String? = null,
    val content: String = "",
    val entities: List<Entity> = emptyList(),
    val topics: List<String> = emptyList(),
    val importance: Float = 0f,
) {
    val displayName get() = name.removeSuffix(".$extension")
    val readSize get() = formatSize(sizeBytes)

    companion object {
        fun formatSize(bytes: Long): String {
            return when {
                bytes < 1024 -> "${bytes}B"
                bytes < 1024 * 1024 -> "${bytes / 1024}KB"
                else -> "${bytes / (1024 * 1024)}MB"
            }
        }
    }
}

data class Entity(
    val id: String,
    val name: String,
    val type: Type,
    val mentions: Int = 1,
    val firstSeen: Long = System.currentTimeMillis(),
    val lastSeen: Long = System.currentTimeMillis(),
) {
    val color: Color get() = type.color

    enum class Type(val color: Color) {
        CONCEPT(Color(0xFF7C3AED)),
        PERSON(Color(0xFFEC4899)),
        PLACE(Color(0xFF06B6D4)),
        METHOD(Color(0xFFF59E0B)),
        TOOL(Color(0xFF8B5CF6)),
        EVENT(Color(0xFF10B981)),
    }
}

data class AnalysisResult(
    val fileId: String,
    val summary: String,
    val entities: List<Entity>,
    val topics: List<Topic>,
    val sentiment: Float, // -1 to 1
    val keyPhrases: List<String>,
    val relations: List<Relation> = emptyList(), // 实体间共现关系
    val analyzedAt: Long = System.currentTimeMillis(),
)

data class Topic(
    val name: String,
    val weight: Float, // 0-1
    val color: Color,
)

/**
 * 实体间关系（从共现分析产出）。
 */
data class Relation(
    val sourceEntityId: String,
    val targetEntityId: String,
    val type: RelationType = RelationType.CO_OCCURS,
    val weight: Float = 1f,
)

enum class RelationType {
    CO_OCCURS,       // 实体间共现关系（基于句子级共现分析）
    REFERENCES,      // 显式引用
    DERIVES_FROM,    // 衍生于
    BELONGS_TO,      // 属于
    CONTRASTS_WITH,  // 对比关系
    SIMILAR_TO,      // 相似关系
}
