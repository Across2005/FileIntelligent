package com.crossk.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.crossk.data.FileItem

/**
 * v2.0：
 * - 加 createdAt 索引（成长曲线/时间排序高频）
 * - topics/tags 字段保留（Converters JSON 序列化）
 */
@Entity(
    tableName = "files",
    indices = [
        Index("createdAt"),
        Index("lastModified"),
    ],
)
data class FileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val path: String,
    val extension: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val createdAt: Long,
    val content: String,
    val aiSummary: String?,
    val importance: Float,
    val topics: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
)

fun FileEntity.toFileItem(): FileItem = FileItem(
    id = id,
    name = name,
    path = path,
    extension = extension,
    sizeBytes = sizeBytes,
    lastModified = lastModified,
    createdAt = createdAt,
    content = content,
    aiSummary = aiSummary?.ifBlank { null },
    importance = importance,
    topics = topics,
    tags = tags,
    entities = emptyList(), // loaded separately
)

fun FileItem.toFileEntity(): FileEntity = FileEntity(
    id = id,
    name = name,
    path = path,
    extension = extension,
    sizeBytes = sizeBytes,
    lastModified = lastModified,
    createdAt = createdAt,
    content = content,
    aiSummary = aiSummary,
    importance = importance,
    topics = topics,
    tags = tags,
)
