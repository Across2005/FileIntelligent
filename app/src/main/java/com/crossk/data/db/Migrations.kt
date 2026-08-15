package com.crossk.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 → v2：暂未实装 schema 改动（仅加 exportSchema 警示）。
 * 保留空迁移以便 Room 严格校验。
 */

/**
 * v2 → v3：
 * 1. entities 表新增 importance 列（v3 EntityEntity 含该字段，缺列会导致 Room schema 校验失败）
 * 2. 新建 edges 表（FK 指向 entities）
 * 3. 为 entities/edges 加索引
 *
 * 边数据: v3 启动后由 FileRepository 从现有 entities 重建并回灌（应用层触发，非迁移职责）。
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 0. entities 补列（v3 EntityEntity.importance）
        db.execSQL("ALTER TABLE `entities` ADD COLUMN `importance` REAL NOT NULL DEFAULT 0")
        // 1. 新增 edges 表
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `edges` (
                `srcId` TEXT NOT NULL,
                `dstId` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `weight` REAL NOT NULL,
                `evidence` TEXT,
                `createdAt` INTEGER NOT NULL,
                PRIMARY KEY(`srcId`, `dstId`, `type`),
                FOREIGN KEY(`srcId`) REFERENCES `entities`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`dstId`) REFERENCES `entities`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        // 2. edges 索引
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_edges_srcId` ON `edges` (`srcId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_edges_dstId` ON `edges` (`dstId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_edges_type` ON `edges` (`type`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_edges_weight` ON `edges` (`weight`)")
        // 3. entities 索引（type 字段频繁参与查询）
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_entities_type` ON `entities` (`type`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_entities_fileId` ON `entities` (`fileId`)")
    }
}

/**
 * v3 → v4: add user-feedback loop columns + entity_confirmations table.
 * Non-destructive: every ALTER TABLE adds a column with a DEFAULT so
 * existing rows stay valid. New table is empty.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // entities: 3 new columns for user feedback (confirm/ignore/count)
        db.execSQL("ALTER TABLE `entities` ADD COLUMN `isUserConfirmed` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `entities` ADD COLUMN `isUserIgnored` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `entities` ADD COLUMN `confirmationCount` INTEGER NOT NULL DEFAULT 0")

        // edges: confidence + source
        db.execSQL("ALTER TABLE `edges` ADD COLUMN `confidence` REAL NOT NULL DEFAULT 0.5")
        db.execSQL("ALTER TABLE `edges` ADD COLUMN `source` TEXT NOT NULL DEFAULT 'rule'")

        // files: encoding + source + analysisVersion
        db.execSQL("ALTER TABLE `files` ADD COLUMN `encoding` TEXT NOT NULL DEFAULT 'UTF-8'")
        db.execSQL("ALTER TABLE `files` ADD COLUMN `source` TEXT NOT NULL DEFAULT 'import'")
        db.execSQL("ALTER TABLE `files` ADD COLUMN `analysisVersion` INTEGER NOT NULL DEFAULT 3")

        // knowledge: onboarding + capture telemetry
        db.execSQL("ALTER TABLE `knowledge` ADD COLUMN `onboardingCompleted` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `knowledge` ADD COLUMN `firstCaptureAt` INTEGER")
        db.execSQL("ALTER TABLE `knowledge` ADD COLUMN `lastCaptureAt` INTEGER")
        db.execSQL("ALTER TABLE `knowledge` ADD COLUMN `captureStreak` INTEGER NOT NULL DEFAULT 0")

        // new table: entity_confirmations
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `entity_confirmations` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `entityId` TEXT NOT NULL,
                `originalType` TEXT NOT NULL,
                `confirmedType` TEXT,
                `isIgnored` INTEGER NOT NULL,
                `confirmedAt` INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_entity_confirmations_entityId` ON `entity_confirmations` (`entityId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_entity_confirmations_confirmedAt` ON `entity_confirmations` (`confirmedAt`)")
    }
}
