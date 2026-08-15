package com.crossk.data

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import java.util.UUID

/**
 * Data model for the postcard (明信片) editor.
 */
data class PostcardProject(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "我的知识明信片",
    val createdAt: Long = System.currentTimeMillis(),
    val backgroundType: PostcardBackground = PostcardBackground.Gradient(
        startColor = Color(0xFF1A1417),
        endColor = Color(0xFF2D1B24),
    ),
    val layers: List<PostcardLayer> = emptyList(),
    val width: Int = 1080,
    val height: Int = 720,
)

sealed class PostcardBackground {
    data class Gradient(
        val startColor: Color,
        val endColor: Color,
    ) : PostcardBackground()

    data class Solid(val color: Color) : PostcardBackground()
}

data class PostcardLayer(
    val id: String = UUID.randomUUID().toString(),
    val type: LayerType,
    val zIndex: Int = 0,
    val translation: Offset = Offset.Zero,
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val content: LayerContent,
)

enum class LayerType {
    TEXT, GRAPH_SNAPSHOT, STAT_BADGE
}

sealed class LayerContent {
    data class TextContent(
        val text: String,
        val fontSize: Float = 18f,
        val fontWeight: FontWeight = FontWeight.Normal,
        val color: Color = Color.White,
        val fontName: String = "default",
    ) : LayerContent()

    data class GraphSnapshot(
        val title: String = "知识图谱快照",
        val nodeCount: Int = 0,
        val edgeCount: Int = 0,
    ) : LayerContent()

    data class StatBadge(
        val label: String,
        val value: String,
        val color: Color,
    ) : LayerContent()
}
