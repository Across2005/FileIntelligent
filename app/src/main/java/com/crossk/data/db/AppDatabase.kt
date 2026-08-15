package com.crossk.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        FileEntity::class,
        EntityEntity::class,
        EdgeEntity::class,
        KnowledgeEntity::class,
        GraphLayoutEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun fileDao(): FileDao
    abstract fun entityDao(): EntityDao
    abstract fun edgeDao(): EdgeDao
    abstract fun knowledgeDao(): KnowledgeDao
    abstract fun graphLayoutDao(): GraphLayoutDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "file_intelligence.db",
                )
                    // v2.0：正式迁移路径，替代 v1 的 destructive fallback
                    .addMigrations(MIGRATION_2_3)
                    // 兜底：仅 downgrade 或无迁移路径时清空
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
