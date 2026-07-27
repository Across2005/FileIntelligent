package com.fileintelligence.data

import androidx.compose.ui.graphics.Color

data class GraphNode(
    val id: String,
    val label: String,
    val type: NodeType,
    val x: Float = 0f,
    val y: Float = 0f,
    val size: Float = 1f,
    val color: Color = type.color,
)

enum class NodeType(val color: Color) {
    CONCEPT(Color(0xFF7C3AED)),
    ENTITY(Color(0xFF06B6D4)),
    FILE(Color(0xFFF59E0B)),
    TOPIC(Color(0xFFEC4899)),
}

data class GraphEdge(
    val source: String,
    val target: String,
    val type: EdgeType,
    val weight: Float = 1f,
)

enum class EdgeType(val color: Float) {
    REFERENCES(1f),
    BELONGS_TO(0.6f),
    DERIVES(0.8f),
    CONTRASTS(0.4f),
}
