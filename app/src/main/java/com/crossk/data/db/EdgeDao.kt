package com.crossk.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * 边表 DAO — v2.0 新增。
 *
 * API 设计原则：
 * - 批量 insert+delete 配 @Transaction，避免单文件分析造成"拆边→旧边残留"
 * - 暴露 Flow 供 UI 订阅（用于图谱实时刷新）
 * - 查询按需：邻域查询 / 类型过滤 / 权重排序
 */
@Dao
interface EdgeDao {

    /** 拉取所有边（用于 Graph 全局构建） */
    @Query("SELECT * FROM edges")
    suspend fun getAll(): List<EdgeEntity>

    @Query("SELECT * FROM edges")
    fun getAllFlow(): Flow<List<EdgeEntity>>

    /** 某节点的 1 跳邻域（双向） */
    @Query("SELECT * FROM edges WHERE srcId = :nodeId OR dstId = :nodeId")
    suspend fun getEdgesForNode(nodeId: String): List<EdgeEntity>

    /** 某文件的全部关联边（任一端为该文件中的实体） */
    @Query(
        """
        SELECT e.* FROM edges e
        INNER JOIN entities s ON e.srcId = s.id
        WHERE s.fileId = :fileId
        UNION
        SELECT e.* FROM edges e
        INNER JOIN entities d ON e.dstId = d.id
        WHERE d.fileId = :fileId
        """,
    )
    suspend fun getEdgesForFile(fileId: String): List<EdgeEntity>

    @Query("SELECT COUNT(*) FROM edges")
    suspend fun count(): Int

    /**
     * 替换某文件涉及的全部边：先删后插。
     * 必须在调用方加 @Transaction 包裹以保证原子性。
     */
    @Query(
        """
        DELETE FROM edges WHERE srcId IN (
            SELECT id FROM entities WHERE fileId = :fileId
        ) OR dstId IN (
            SELECT id FROM entities WHERE fileId = :fileId
        )
        """,
    )
    suspend fun deleteForFile(fileId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(edges: List<EdgeEntity>)

    @Query("DELETE FROM edges")
    suspend fun deleteAll()

    /**
     * 事务化：替换某文件涉及的全部边。
     * 解决 v1 中"deleteForFile + insertAll"两段式无事务的崩溃一致性问题。
     */
    @Transaction
    suspend fun replaceForFile(fileId: String, edges: List<EdgeEntity>) {
        deleteForFile(fileId)
        if (edges.isNotEmpty()) {
            insertAll(edges)
        }
    }
}
