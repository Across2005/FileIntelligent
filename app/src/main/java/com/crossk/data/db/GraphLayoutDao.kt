package com.crossk.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * DAO for graph layout persistence (node positions).
 */
@Dao
interface GraphLayoutDao {

    @Query("SELECT * FROM graph_layout")
    suspend fun getAll(): List<GraphLayoutEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(layout: GraphLayoutEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(layouts: List<GraphLayoutEntity>)

    @Query("DELETE FROM graph_layout WHERE nodeId = :nodeId")
    suspend fun deleteByNode(nodeId: String)

    @Query("DELETE FROM graph_layout")
    suspend fun deleteAll()
}
