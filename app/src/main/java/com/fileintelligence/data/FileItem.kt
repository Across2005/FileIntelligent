package com.fileintelligence.data

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
    val analyzedAt: Long = System.currentTimeMillis(),
)

data class Topic(
    val name: String,
    val weight: Float, // 0-1
    val color: Color,
)
