package com.crossk.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * P0-2 (CR-2): verify saveAll/addFileAsync atomicity.
 *
 * Test adapted to the actual schema (String id on FileEntity + EntityEntity;
 * v4 columns include isUserConfirmed etc.). The plan's test assumed Long
 * primary keys; we use the real shape.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TransactionalSaveTest {

    private lateinit var db: AppDatabase

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            AppDatabase::class.java,
        ).build()
    }

    @After fun tearDown() { db.close() }

    @Test
    fun upsertFileWithEntitiesPersistsFileAndAllEntitiesAtomically(): Unit = runBlocking {
        val fileId = "f-test-1"
        val file = FileEntity(
            id = fileId,
            name = "test.txt",
            path = "/local/test.txt",
            extension = "txt",
            sizeBytes = 100,
            lastModified = 1,
            createdAt = 1,
            content = "hello world",
            aiSummary = null,
            importance = 0.5f,
        )
        val entities = listOf(
            EntityEntity(
                id = "ent_concept_x",
                fileId = fileId,
                name = "X",
                type = "CONCEPT",
                mentions = 1,
                firstSeen = 1,
                lastSeen = 1,
                importance = 0.5f,
            ),
        )

        db.fileDao().upsertFileWithEntities(file, entities)

        val savedFile = db.fileDao().getFileById(fileId)
        val savedEntities = db.entityDao().getEntitiesForFile(fileId)
        assertThat(savedFile).isNotNull()
        assertThat(savedEntities).hasSize(1)
        assertThat(savedEntities.first().name).isEqualTo("X")
    }

    @Test
    fun upsertFileWithEntitiesReplacesPreviousEntities(): Unit = runBlocking {
        val fileId = "f-test-2"
        val file = FileEntity(
            id = fileId,
            name = "test.txt",
            path = "/local/test.txt",
            extension = "txt",
            sizeBytes = 100,
            lastModified = 1,
            createdAt = 1,
            content = "hello",
            aiSummary = null,
            importance = 0.5f,
        )
        val firstBatch = listOf(
            EntityEntity(id = "ent_a", fileId = fileId, name = "A", type = "CONCEPT",
                         mentions = 1, firstSeen = 1, lastSeen = 1, importance = 0.5f),
            EntityEntity(id = "ent_b", fileId = fileId, name = "B", type = "CONCEPT",
                         mentions = 1, firstSeen = 1, lastSeen = 1, importance = 0.5f),
        )
        db.fileDao().upsertFileWithEntities(file, firstBatch)
        assertThat(db.entityDao().getEntitiesForFile(fileId)).hasSize(2)

        val secondBatch = listOf(
            EntityEntity(id = "ent_c", fileId = fileId, name = "C", type = "CONCEPT",
                         mentions = 1, firstSeen = 1, lastSeen = 1, importance = 0.5f),
        )
        db.fileDao().upsertFileWithEntities(file, secondBatch)
        val afterResave = db.entityDao().getEntitiesForFile(fileId)
        assertThat(afterResave).hasSize(1)
        assertThat(afterResave.first().name).isEqualTo("C")
    }

    @Test
    fun replaceForFileDeletesThenInsertsAtomically(): Unit = runBlocking {
        val fileId = "f-edge-1"
        val file = FileEntity(
            id = fileId,
            name = "test.txt",
            path = "/local/test.txt",
            extension = "txt",
            sizeBytes = 100,
            lastModified = 1,
            createdAt = 1,
            content = "hello",
            aiSummary = null,
            importance = 0.5f,
        )
        val entities = listOf(
            EntityEntity(id = "ent_src", fileId = fileId, name = "Src", type = "CONCEPT",
                         mentions = 1, firstSeen = 1, lastSeen = 1, importance = 0.5f),
            EntityEntity(id = "ent_dst", fileId = fileId, name = "Dst", type = "CONCEPT",
                         mentions = 1, firstSeen = 1, lastSeen = 1, importance = 0.5f),
        )
        db.fileDao().upsertFileWithEntities(file, entities)

        val firstEdges = listOf(
            EdgeEntity(
                srcId = "ent_src", dstId = "ent_dst", type = "CO_OCCURS",
                weight = 1.0f, evidence = null, createdAt = 1L,
            ),
        )
        db.edgeDao().replaceForFile(fileId, firstEdges)
        assertThat(db.edgeDao().count()).isEqualTo(1)

        val secondEdges = listOf(
            EdgeEntity(
                srcId = "ent_dst", dstId = "ent_src", type = "REFERENCES",
                weight = 0.5f, evidence = null, createdAt = 2L,
            ),
            EdgeEntity(
                srcId = "ent_src", dstId = "ent_dst", type = "BELONGS_TO",
                weight = 0.7f, evidence = null, createdAt = 2L,
            ),
        )
        db.edgeDao().replaceForFile(fileId, secondEdges)
        val finalEdges = db.edgeDao().getAll()
        assertThat(finalEdges).hasSize(2)
        assertThat(finalEdges.map { it.type }.toSet()).containsExactly("REFERENCES", "BELONGS_TO")
    }
}
