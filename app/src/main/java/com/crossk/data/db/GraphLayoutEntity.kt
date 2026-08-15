package com.crossk.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * v2.0：
 * - 加 pinX / pinY 字段（用户拖拽钉位，区别于引擎模拟位置）
 * - 加 updatedAt 索引（便于增量 upsert）
 */
@Entity(
    tableName = "graph_layout",
    indices = [Index("updatedAt")],
)
data class GraphLayoutEntity(
    @PrimaryKey val nodeId: String,
    val x: Float,
    val y: Float,
    val pinX: Float? = null,
    val pinY: Float? = null,
    val updatedAt: Long,
)
