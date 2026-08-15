package com.crossk.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EntityDao {

    @Query("SELECT * FROM entities WHERE fileId = :fileId")
    suspend fun getEntitiesForFile(fileId: String): List<EntityEntity>

    @Query("SELECT * FROM entities WHERE fileId = :fileId")
    fun getEntitiesForFileFlow(fileId: String): Flow<List<EntityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<EntityEntity>)

    @Query("DELETE FROM entities WHERE fileId = :fileId")
    suspend fun deleteForFile(fileId: String)

    @Query("DELETE FROM entities")
    suspend fun deleteAll()
}
