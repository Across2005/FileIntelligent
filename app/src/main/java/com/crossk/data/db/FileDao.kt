package com.crossk.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface FileDao {

    @Query("SELECT * FROM files ORDER BY lastModified DESC")
    fun getAllFiles(): Flow<List<FileEntity>>

    @Query("SELECT * FROM files ORDER BY lastModified DESC")
    suspend fun getAllFilesSync(): List<FileEntity>

    @Query("SELECT * FROM files WHERE id = :id")
    suspend fun getFileById(id: String): FileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(file: FileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(files: List<FileEntity>)

    @Delete
    suspend fun delete(file: FileEntity)

    @Query("DELETE FROM files WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM files")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM files")
    suspend fun count(): Int

    /**
     * v4: atomic file + entities save (P0-2, CR-2).
     *
     * Wraps `insert(file) + deleteForFile(fileId) + insertAllEntities(entities)`
     * in a single @Transaction. Without this, a crash between the file insert
     * and the entity inserts would leave the database in a half-updated state
     * (file exists but entities are missing or stale).
     *
     * Note: `deleteForFile` on the entities table is duplicated here so the
     * @Transaction can wrap both tables. The EntityDao.deleteForFile remains
     * for callers that don't need file-table atomicity.
     */
    @Transaction
    suspend fun upsertFileWithEntities(file: FileEntity, entities: List<EntityEntity>) {
        insert(file)
        deleteEntitiesForFile(file.id)
        if (entities.isNotEmpty()) insertAllEntities(entities)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllEntities(entities: List<EntityEntity>)

    @Query("DELETE FROM entities WHERE fileId = :fileId")
    suspend fun deleteEntitiesForFile(fileId: String)
}
