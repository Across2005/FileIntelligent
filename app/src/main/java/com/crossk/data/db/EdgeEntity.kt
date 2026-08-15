package com.crossk.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.ColumnInfo
import com.crossk.data.RelationType
import com.crossk.data.Relation as DomainRelation

/**
 * 关系/边表 — v2.0 关键新增。
 *
 * 关键设计：
 * - 复合主键 (srcId, dstId, type) 替代自增 id；同等 (src,dst,type) 多次入库由 type 联合键抑制，确保幂等
 * - 双向外键 + CASCADE：任意端实体被删除时，对应边自动收
 * - 权重 + 证据 + 创建时间：支持后续 query 过滤与排序
 * - type 字段用 name 字符串持久化（与 EntityEntity.type 保持一致），Converters 处理
 */
@Entity(
    tableName = "edges",
    primaryKeys = ["srcId", "dstId", "type"],
    foreignKeys = [
        ForeignKey(
            entity = EntityEntity::class,
            parentColumns = ["id"],
            childColumns = ["srcId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = EntityEntity::class,
            parentColumns = ["id"],
            childColumns = ["dstId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("srcId"),
        Index("dstId"),
        Index("type"),
        Index("weight"),
    ],
)
data class EdgeEntity(
    val srcId: String,
    val dstId: String,
    val type: String,           // RelationType.name
    val weight: Float,
    val evidence: String?,      // JSON 字符串：[{fileId, sentence, offset}]
    val createdAt: Long,
    // v4: confidence and provenance for the edge
    @ColumnInfo(defaultValue = "0.5") val confidence: Float = 0.5f,
    @ColumnInfo(defaultValue = "'rule'") val source: String = "rule",
)

fun EdgeEntity.toDomain(): DomainRelation = DomainRelation(
    sourceEntityId = srcId,
    targetEntityId = dstId,
    type = runCatching { RelationType.valueOf(type) }.getOrDefault(RelationType.CO_OCCURS),
    weight = weight,
)

fun DomainRelation.toEdge(createdAt: Long = System.currentTimeMillis(), evidence: String? = null): EdgeEntity =
    EdgeEntity(
        srcId = sourceEntityId,
        dstId = targetEntityId,
        type = type.name,
        weight = weight,
        evidence = evidence,
        createdAt = createdAt,
    )
