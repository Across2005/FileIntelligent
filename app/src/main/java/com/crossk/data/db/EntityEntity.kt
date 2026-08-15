package com.crossk.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.crossk.data.Entity as DataEntity

/**
 * v2.0 实体重写：
 * - 加索引 type（频繁 group-by）、fileId（删文件级联依据）
 * - 加 importance 字段（综合评分，给图谱节点大小提供依据）
 * - id 改为稳定 hash(name+type)，跨会话可一致
 */
@Entity(
    tableName = "entities",
    indices = [
        Index("fileId"),
        Index("type"),
        Index("name"),
    ],
)
data class EntityEntity(
    @PrimaryKey val id: String,
    val fileId: String,
    val name: String,
    val type: String,
    val mentions: Int,
    val firstSeen: Long,
    val lastSeen: Long,
    val importance: Float = 0f,
)

fun EntityEntity.toEntity(): DataEntity = DataEntity(
    id = id,
    name = name,
    type = runCatching { DataEntity.Type.valueOf(type) }.getOrDefault(DataEntity.Type.CONCEPT),
    mentions = mentions,
    firstSeen = firstSeen,
    lastSeen = lastSeen,
)

fun DataEntity.toEntityEntity(fileId: String): EntityEntity = EntityEntity(
    id = id,
    fileId = fileId,
    name = name,
    type = type.name,
    mentions = mentions,
    firstSeen = firstSeen,
    lastSeen = lastSeen,
    importance = mentions.toFloat(),
)
