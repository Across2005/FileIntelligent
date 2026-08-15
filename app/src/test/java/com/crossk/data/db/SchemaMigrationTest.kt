package com.crossk.data.db

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * P0-6: Room schema 3 → 4 migration test.
 *
 * Verifies that MIGRATION_3_4:
 * 1. Preserves existing files (and renames) data
 * 2. Adds the new columns to entities/edges/files/knowledge
 * 3. Creates the entity_confirmations table
 *
 * Uses Robolectric to provide the Application context for MigrationTestHelper.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SchemaMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    @Test fun `migrate 3 to 4 preserves files data and adds new columns`() {
        helper.createDatabase("test-v3-files", 3).use { db ->
            db.execSQL(
                """
                INSERT INTO `files` (id, name, path, extension, sizeBytes, lastModified, createdAt, content, importance, topics, tags)
                VALUES ('legacy-id', 'test.txt', '/local/test.txt', 'txt', 100, 1000, 1000, 'hello world', 0.5, '[]', '[]')
                """.trimIndent(),
            )
        }
        val db = helper.runMigrationsAndValidate("test-v4-files", 4, true, MIGRATION_3_4)
        val cursor = db.query("SELECT id, encoding, source, analysisVersion FROM files")
        cursor.moveToFirst()
        val columnNames = cursor.columnNames.toSet()
        assertThat(columnNames).contains("encoding")
        assertThat(columnNames).contains("source")
        assertThat(columnNames).contains("analysisVersion")
        // Default values applied
        assertThat(cursor.getString(cursor.getColumnIndexOrThrow("encoding"))).isEqualTo("UTF-8")
        assertThat(cursor.getString(cursor.getColumnIndexOrThrow("source"))).isEqualTo("import")
        assertThat(cursor.getInt(cursor.getColumnIndexOrThrow("analysisVersion"))).isEqualTo(3)
        // Original row preserved
        assertThat(cursor.getString(cursor.getColumnIndexOrThrow("id"))).isEqualTo("legacy-id")
        cursor.close()
        db.close()
    }

    @Test fun `migrate 3 to 4 adds entity user-feedback columns`() {
        helper.createDatabase("test-v3-entities", 3).use { /* no rows needed */ }
        val db = helper.runMigrationsAndValidate("test-v4-entities", 4, true, MIGRATION_3_4)
        val cursor = db.query("SELECT isUserConfirmed, isUserIgnored, confirmationCount FROM entities LIMIT 1")
        val columnNames = cursor.columnNames.toSet()
        assertThat(columnNames).contains("isUserConfirmed")
        assertThat(columnNames).contains("isUserIgnored")
        assertThat(columnNames).contains("confirmationCount")
        cursor.close()
        db.close()
    }

    @Test fun `migrate 3 to 4 adds edge confidence and source columns`() {
        helper.createDatabase("test-v3-edges", 3).use { /* no rows needed */ }
        val db = helper.runMigrationsAndValidate("test-v4-edges", 4, true, MIGRATION_3_4)
        val cursor = db.query("SELECT confidence, source FROM edges LIMIT 1")
        val columnNames = cursor.columnNames.toSet()
        assertThat(columnNames).contains("confidence")
        assertThat(columnNames).contains("source")
        cursor.close()
        db.close()
    }

    @Test fun `migrate 3 to 4 creates entity_confirmations table`() {
        helper.createDatabase("test-v3-ec", 3).use { /* no rows needed */ }
        val db = helper.runMigrationsAndValidate("test-v4-ec", 4, true, MIGRATION_3_4)
        val cursor = db.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='entity_confirmations'",
        )
        assertThat(cursor.count).isEqualTo(1)
        cursor.close()
        db.close()
    }

    @Test fun `migrate 3 to 4 adds knowledge onboarding and capture columns`() {
        helper.createDatabase("test-v3-knowledge", 3).use { /* no rows needed */ }
        val db = helper.runMigrationsAndValidate("test-v4-knowledge", 4, true, MIGRATION_3_4)
        val cursor = db.query(
            "SELECT onboardingCompleted, firstCaptureAt, lastCaptureAt, captureStreak FROM knowledge LIMIT 1",
        )
        val columnNames = cursor.columnNames.toSet()
        assertThat(columnNames).contains("onboardingCompleted")
        assertThat(columnNames).contains("firstCaptureAt")
        assertThat(columnNames).contains("lastCaptureAt")
        assertThat(columnNames).contains("captureStreak")
        cursor.close()
        db.close()
    }
}
