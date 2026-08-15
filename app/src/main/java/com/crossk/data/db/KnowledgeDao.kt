package com.crossk.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface KnowledgeDao {

    @Query("SELECT * FROM knowledge WHERE rowId = 'main'")
    fun getKnowledge(): Flow<KnowledgeEntity?>

    @Query("SELECT * FROM knowledge WHERE rowId = 'main'")
    suspend fun getKnowledgeSync(): KnowledgeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(knowledge: KnowledgeEntity)

    @Query("DELETE FROM knowledge")
    suspend fun deleteAll()
}
