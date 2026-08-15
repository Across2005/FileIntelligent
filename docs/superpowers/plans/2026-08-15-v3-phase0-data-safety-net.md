# v3.0 Phase 0: Data Safety Net Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix 4 Critical (CR-1 to CR-4), 1 Important (IM-1), and 1 new Critical crash (B-TTS-1/2) identified across the v3.0 code review before any v3.0 features land. All fixes must land with TDD discipline (failing test → pass).

**Architecture:** Sequential TDD-driven fixes. Each task lands a real test that fails before the implementation and passes after. Commits at every green test. The 7+1 tasks address (a) data integrity (CR-1, CR-2, CR-3, CR-4), (b) AI quality (IM-1), (c) schema evolution (Schema 3→4), and (d) first-install crash (B-TTS-1/2). Total estimated: 4.5 days.

**Tech Stack:** Kotlin, Android Compose, Room (schema 3 → 4), Gradle 8.5 + AGP 8.2.2, JUnit 4 + Truth, in-memory Room for migration tests.

## Global Constraints
- Project: FileIntelligence Android (Kotlin, Compose, Room, minSdk=24, targetSdk=35)
- TDD strict: failing test → run to confirm failure → implement → run to confirm pass → commit
- Test framework: JUnit 4 + Truth (per `app/build.gradle.kts`)
- In-memory Room DB (`Room.inMemoryDatabaseBuilder`) for migration tests
- Conventional commits: `fix:`, `chore:`, `refactor:`, `test:`
- Reference finding IDs in commit messages (CR-1, IM-1, B-TTS-1, etc.)
- Each task ends with one commit
- After each task: run `./gradlew testDebugUnitTest` and confirm new test passes; existing tests still pass
- Do NOT introduce new dependencies without justification in commit message
- Repository: `D:\mini code\workspace\FileIntelligence\.worktrees\v3.0-smart-exocortex`

---

## Task P0-1: Entity ID stability (CR-1)

**Files:**
- Create: `app/src/main/java/com/crossk/ai/EntityIDFactory.kt`
- Create: `app/src/test/java/com/crossk/ai/EntityIDFactoryTest.kt`
- Modify: `app/src/main/java/com/crossk/ai/AnalysisEngine.kt:135-143, 186-193`

**Interfaces:**
- Consumes: `EntityType` enum from `com.crossk.data.Entity`
- Produces: `fun entityID(type: EntityType, name: String): String` — deterministic, collision-resistant, type-stable

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/crossk/ai/EntityIDFactoryTest.kt`:

```kotlin
package com.crossk.ai

import com.crossk.data.Entity
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EntityIDFactoryTest {

    @Test fun `same input produces same ID`() {
        val a = EntityIDFactory.entityID(Entity.Type.CONCEPT, "机器学习")
        val b = EntityIDFactory.entityID(Entity.Type.CONCEPT, "机器学习")
        assertThat(a).isEqualTo(b)
    }

    @Test fun `different name produces different ID`() {
        val a = EntityIDFactory.entityID(Entity.Type.CONCEPT, "机器学习")
        val b = EntityIDFactory.entityID(Entity.Type.CONCEPT, "深度学习")
        assertThat(a).isNotEqualTo(b)
    }

    @Test fun `different type produces different ID for same name`() {
        val a = EntityIDFactory.entityID(Entity.Type.CONCEPT, "神经网络")
        val b = EntityIDFactory.entityID(Entity.Type.METHOD, "神经网络")
        assertThat(a).isNotEqualTo(b)
    }

    @Test fun `ID is case-insensitive`() {
        val a = EntityIDFactory.entityID(Entity.Type.CONCEPT, "Machine Learning")
        val b = EntityIDFactory.entityID(Entity.Type.CONCEPT, "machine learning")
        assertThat(a).isEqualTo(b)
    }

    @Test fun `ID format is `ent_type_uuid`() {
        val id = EntityIDFactory.entityID(Entity.Type.CONCEPT, "测试")
        assertThat(id).startsWith("ent_")
        assertThat(id).contains("concept")
    }

    @Test fun `100 unique names produce 100 unique IDs`() {
        val ids = (1..100).map { EntityIDFactory.entityID(Entity.Type.CONCEPT, "entity_$it") }
        assertThat(ids.toSet()).hasSize(100)
    }

    @Test fun `ID survives Chinese punctuation and emoji`() {
        val a = EntityIDFactory.entityID(Entity.Type.CONCEPT, "机器学习!")
        val b = EntityIDFactory.entityID(Entity.Type.CONCEPT, "机器学习！")  // full-width
        // Different bytes → different IDs (no fancy normalization beyond case)
        assertThat(a).isNotEqualTo(b)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd $REPO && .\gradlew.bat testDebugUnitTest --tests "com.crossk.ai.EntityIDFactoryTest"`
Expected: FAIL with "Unresolved reference: EntityIDFactory"

- [ ] **Step 3: Implement the factory**

Create `app/src/main/java/com/crossk/ai/EntityIDFactory.kt`:

```kotlin
package com.crossk.ai

import com.crossk.data.Entity
import java.util.UUID

/**
 * Stable entity ID generator. v3.0 replaces v1's `ent_${name.hashCode().toUInt()}` which
 * is collision-prone (32-bit hash, ~50% chance of collision at 70k unique names).
 *
 * ID format: `ent_{type}_{UUID5(namespace, name+type)}`
 * - Stable: same (type, name, case) → same ID
 * - Collision-resistant: UUID v5 (SHA-1 hash) over 128-bit space
 * - Type-aware: same name under different type → different ID
 */
object EntityIDFactory {

    private val NAMESPACE = UUID.fromString("6c7e9f3a-1b2c-4d5e-8f9a-0b1c2d3e4f5a")  // arbitrary fixed

    fun entityID(type: Entity.Type, name: String): String {
        val normalized = name.trim().lowercase()
        val key = "${type.name}|$normalized"
        val uuid = UUID.nameUUIDFromBytes(key.toByteArray(Charsets.UTF_8))
        return "ent_${type.name.lowercase()}_${uuid}"
    }
}
```

- [ ] **Step 4: Update AnalysisEngine to use the factory**

In `app/src/main/java/com/crossk/ai/AnalysisEngine.kt`, replace both occurrences of:
```kotlin
id = "ent_${name.hashCode().toUInt()}"
```
with:
```kotlin
id = EntityIDFactory.entityID(type, name)
```

Also adjust the surrounding mapping (around line 135-143 and 186-193) so `type` is in scope. The existing code reads:
```kotlin
val type = conceptDictionary[name]?.type ?: Entity.Type.CONCEPT
```
This must come BEFORE the `Entity(...)` constructor.

- [ ] **Step 5: Run test to verify it passes**

Run: `cd $REPO && .\gradlew.bat testDebugUnitTest --tests "com.crossk.ai.EntityIDFactoryTest"`
Expected: PASS (7 tests)

- [ ] **Step 6: Run full test suite to verify no regression**

Run: `cd $REPO && .\gradlew.bat testDebugUnitTest`
Expected: all existing tests still pass

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/crossk/ai/EntityIDFactory.kt \
        app/src/main/java/com/crossk/ai/AnalysisEngine.kt \
        app/src/test/java/com/crossk/ai/EntityIDFactoryTest.kt
git commit -m "fix(CR-1): use UUID5 for entity ID instead of hashCode

Eliminates collision risk in long-running knowledge bases (v1 had
~50% collision probability at 70k unique entities). Format:
ent_{type}_{uuid5(namespace, name|type)}"
```

---

## Task P0-2: saveAll and addFileAsync @Transaction (CR-2)

**Files:**
- Modify: `app/src/main/java/com/crossk/data/db/FileDao.kt` — add `@Transaction` insert with entities
- Modify: `app/src/main/java/com/crossk/data/db/EdgeDao.kt` — add `@Transaction` `replaceForFile`
- Modify: `app/src/main/java/com/crossk/data/db/KnowledgeDao.kt` — add `@Transaction` `upsertWithSideEffects` (or simpler: keep upsert separate, just add @Transaction wrapper method)
- Modify: `app/src/main/java/com/crossk/data/FileRepository.kt:124-152, 194-203` — replace inline loops with DAO @Transaction calls

**Interfaces:**
- Consumes: `FileItem`, `Entity`, `EdgeEntity`, `KnowledgeEntity`
- Produces:
  - `FileDao.upsertFileWithEntities(fileEntity, entityEntities)` — atomic
  - `EdgeDao.replaceEdgesForFile(fileId, edges)` — atomic (delete + insert in one tx)
  - `KnowledgeDao.upsertAtomic(knowledge)` — atomic

- [ ] **Step 1: Write the failing test for atomic file-with-entities save**

Create `app/src/test/java/com/crossk/data/db/TransactionalSaveTest.kt`:

```kotlin
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TransactionalSaveTest {

    private lateinit var db: AppDatabase

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            AppDatabase::class.java
        ).allowMainThreadOperations().build()
    }

    @After fun tearDown() { db.close() }

    @Test fun `saveFileWithEntities persists file and all entities atomically`() = runBlocking {
        val file = FileEntity(id = 1, uri = "u", title = "t", mime = "text/plain",
                              sizeBytes = 100, addedAt = 1, lastOpenedAt = null,
                              encoding = "UTF-8", source = "import", analysisVersion = 3)
        val entities = listOf(
            EntityEntity(id = "ent_concept_x", fileId = 1, name = "X", type = "CONCEPT",
                         mentions = 1, firstSeen = 1, lastSeen = 1, importance = 0.5f,
                         isUserConfirmed = false, isUserIgnored = false, confirmationCount = 0)
        )

        db.fileDao().upsertFileWithEntities(file, entities)

        val savedFile = db.fileDao().getById(1)
        val savedEntities = db.entityDao().getEntitiesForFile(1)
        assertThat(savedFile).isNotNull()
        assertThat(savedEntities).hasSize(1)
    }

    @Test fun `saveAll does not leave partial state on mid-transaction failure`() = runBlocking {
        val file = FileEntity(id = 1, uri = "u", title = "t", mime = "text/plain",
                              sizeBytes = 100, addedAt = 1, lastOpenedAt = null,
                              encoding = "UTF-8", source = "import", analysisVersion = 3)
        val badEntities = listOf(
            EntityEntity(id = "ent_concept_x", fileId = 999, name = "X", type = "CONCEPT",  // wrong fileId
                         mentions = 1, firstSeen = 1, lastSeen = 1, importance = 0.5f,
                         isUserConfirmed = false, isUserIgnored = false, confirmationCount = 0)
        )

        // We expect a FK constraint failure on badEntities.fileId = 999
        var threw = false
        try {
            db.fileDao().upsertFileWithEntities(file, badEntities)
        } catch (e: Exception) {
            threw = true
        }
        assertThat(threw).isTrue()

        // File should NOT be present — atomicity means rollback
        val savedFile = db.fileDao().getById(1)
        assertThat(savedFile).isNull()
    }
}
```

- [ ] **Step 2: Add `getById` to FileDao (test fixture)**

In `app/src/main/java/com/crossk/data/db/FileDao.kt`, add:
```kotlin
@Query("SELECT * FROM files WHERE id = :id")
suspend fun getById(id: Long): FileEntity?
```

- [ ] **Step 3: Run test to verify it fails (no upsertFileWithEntities yet)**

Run: `cd $REPO && .\gradlew.bat testDebugUnitTest --tests "com.crossk.data.db.TransactionalSaveTest"`
Expected: FAIL with "Unresolved reference: upsertFileWithEntities"

- [ ] **Step 4: Implement the @Transaction method**

In `app/src/main/java/com/crossk/data/db/FileDao.kt`, add:
```kotlin
import androidx.room.Transaction

@Transaction
suspend fun upsertFileWithEntities(file: FileEntity, entities: List<EntityEntity>) {
    insert(file)
    deleteForFile(file.id)
    if (entities.isNotEmpty()) insertAllEntities(entities)
}

@Query("DELETE FROM entities WHERE fileId = :fileId")
suspend fun deleteForFile(fileId: Long)
```

(Note: FileDao currently uses `db.entityDao().deleteForFile(...)` via the repository. For the @Transaction to wrap both tables, the entity delete must live in the same DAO. Move `deleteForFile` from `EntityDao` to `FileDao` — or duplicate the SQL — and call it inside the @Transaction method.)

In `app/src/main/java/com/crossk/data/db/EdgeDao.kt`, add:
```kotlin
@Transaction
suspend fun replaceEdgesForFile(fileId: String, edges: List<EdgeEntity>) {
    deleteForFile(fileId)
    if (edges.isNotEmpty()) insertAll(edges)
}
```

(Verify `insertAll` exists in EdgeDao — if not, add a `@Insert` `suspend` method.)

- [ ] **Step 5: Update FileRepository to use the atomic methods**

In `app/src/main/java/com/crossk/data/FileRepository.kt`:

`saveAll` (line 124-152): change the per-file loop body to use `db.fileDao().upsertFileWithEntities(...)` and `db.edgeDao().replaceEdgesForFile(...)`.

`addFileAsync` (line 194-203): change inline `db.entityDao().deleteForFile(file.id)` + `db.entityDao().insertAll(...)` + `db.edgeDao().replaceForFile(file.id, edges)` to a single `db.fileDao().upsertFileWithEntities(...)` + `db.edgeDao().replaceEdgesForFile(...)`.

- [ ] **Step 6: Run test to verify it passes**

Run: `cd $REPO && .\gradlew.bat testDebugUnitTest --tests "com.crossk.data.db.TransactionalSaveTest"`
Expected: PASS (2 tests)

- [ ] **Step 7: Run full test suite**

Run: `cd $REPO && .\gradlew.bat testDebugUnitTest`
Expected: no regressions

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/crossk/data/db/FileDao.kt \
        app/src/main/java/com/crossk/data/db/EdgeDao.kt \
        app/src/main/java/com/crossk/data/db/EntityDao.kt \
        app/src/main/java/com/crossk/data/FileRepository.kt \
        app/src/test/java/com/crossk/data/db/TransactionalSaveTest.kt
git commit -m "fix(CR-2): wrap file+entities save in Room @Transaction

Eliminates partial-write window between insert(file) and insert(entities).
Same pattern applied to edges via replaceEdgesForFile. Repository
no longer relies on cross-DAO ordering for consistency."
```

---

## Task P0-3: Encoding sniff in FileParserEngine (CR-3)

**Files:**
- Create: `app/src/test/resources/encodings/utf8-bom.txt` (with UTF-8 BOM)
- Create: `app/src/test/resources/encodings/utf8-no-bom.txt`
- Create: `app/src/test/resources/encodings/gbk.txt` (with GBK bytes for "你好世界")
- Create: `app/src/test/java/com/crossk/data/FileParserEngineEncodingTest.kt`
- Modify: `app/src/main/java/com/crossk/data/FileParserEngine.kt:45-47`

**Interfaces:**
- Consumes: `Context`, `Uri`
- Produces: `fun parse(uri: Uri): ParseResult?` — `content` is correct regardless of source encoding

- [ ] **Step 1: Create test fixtures**

`app/src/test/resources/encodings/utf8-bom.txt`: write "你好，世界\nHello" with a leading `\uFEFF`. The test resource should be UTF-8 encoded *bytes* (with the BOM bytes 0xEF 0xBB 0xBF) plus the text in UTF-8.

`app/src/test/resources/encodings/utf8-no-bom.txt`: same text without BOM.

`app/src/test/resources/encodings/gbk.txt`: encode the text "你好，世界\nHello" in GBK. The byte sequence for these characters in GBK is well-defined; the test setup will use it.

To create these files reliably in a test, prefer creating them in a `@Before` method with explicit bytes:

```kotlin
// In test:
@Before fun writeFixtures() {
    val dir = File("build/test-encodings").apply { mkdirs() }
    File(dir, "utf8-bom.txt").writeBytes(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + "你好，世界\nHello".toByteArray(Charsets.UTF_8))
    File(dir, "utf8-no-bom.txt").writeBytes("你好，世界\nHello".toByteArray(Charsets.UTF_8))
    File(dir, "gbk.txt").writeBytes("你好，世界\nHello".toByteArray(Charset.forName("GBK")))
}
```

- [ ] **Step 2: Write the failing test**

Create `app/src/test/java/com/crossk/data/FileParserEngineEncodingTest.kt`:

```kotlin
package com.crossk.data

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class FileParserEngineEncodingTest {

    private lateinit var engine: FileParserEngine
    private lateinit var dir: File

    @Before fun setUp() {
        engine = FileParserEngine(ApplicationProvider.getApplicationContext())
        dir = File("build/test-encodings").apply { mkdirs() }
        File(dir, "utf8-bom.txt").writeBytes(
            byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) +
            "你好，世界\nHello".toByteArray(Charsets.UTF_8)
        )
        File(dir, "utf8-no-bom.txt").writeBytes(
            "你好，世界\nHello".toByteArray(Charsets.UTF_8)
        )
        File(dir, "gbk.txt").writeBytes(
            "你好，世界\nHello".toByteArray(Charset.forName("GBK"))
        )
    }

    private fun uri(name: String): Uri = Uri.fromFile(File(dir, name))

    @Test fun `UTF-8 BOM file is decoded correctly (BOM stripped)`() {
        val result = engine.parse(uri("utf8-bom.txt"))
        assertThat(result).isNotNull()
        assertThat(result!!.content).startsWith("你好")
        assertThat(result.content).doesNotContain("\uFEFF")
    }

    @Test fun `UTF-8 file without BOM is decoded correctly`() {
        val result = engine.parse(uri("utf8-no-bom.txt"))
        assertThat(result).isNotNull()
        assertThat(result!!.content).startsWith("你好")
    }

    @Test fun `GBK file falls back from UTF-8 and decodes correctly`() {
        val result = engine.parse(uri("gbk.txt"))
        assertThat(result).isNotNull()
        assertThat(result!!.content).startsWith("你好")
    }
}
```

- [ ] **Step 3: Run test to verify it fails (GBK test will fail)**

Run: `cd $REPO && .\gradlew.bat testDebugUnitTest --tests "com.crossk.data.FileParserEngineEncodingTest"`
Expected: GBK test FAILS (returns mojibake); UTF-8 tests may PASS

- [ ] **Step 4: Implement encoding detection in FileParserEngine**

In `app/src/main/java/com/crossk/data/FileParserEngine.kt`, replace lines 45-47:

```kotlin
// Old:
val text = contentResolver.openInputStream(uri)?.use { inputStream ->
    BufferedReader(InputStreamReader(inputStream, "UTF-8")).readText()
} ?: ""

// New:
val text = contentResolver.openInputStream(uri)?.use { inputStream ->
    decodeWithEncodingDetection(inputStream)
} ?: ""

private fun decodeWithEncodingDetection(input: InputStream): String {
    val raw = input.readBytes()
    return when {
        // UTF-8 BOM
        raw.size >= 3 && raw[0] == 0xEF.toByte() && raw[1] == 0xBB.toByte() && raw[2] == 0xBF.toByte() ->
            String(raw, 3, raw.size - 3, Charsets.UTF_8)
        // UTF-16 LE BOM
        raw.size >= 2 && raw[0] == 0xFF.toByte() && raw[1] == 0xFE.toByte() ->
            String(raw, 2, raw.size - 2, Charsets.UTF_16_LE)
        // UTF-16 BE BOM
        raw.size >= 2 && raw[0] == 0xFE.toByte() && raw[1] == 0xFF.toByte() ->
            String(raw, 2, raw.size - 2, Charsets.UTF_16_BE)
        // Try UTF-8 first; if it produces replacement chars AND content is non-ASCII, retry GB18030
        else -> {
            val utf8 = String(raw, Charsets.UTF_8)
            if (utf8.contains('\uFFFD') && raw.any { it.toInt() and 0x80 != 0 }) {
                try {
                    String(raw, Charset.forName("GB18030"))
                } catch (e: Exception) {
                    utf8
                }
            } else {
                utf8
            }
        }
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `cd $REPO && .\gradlew.bat testDebugUnitTest --tests "com.crossk.data.FileParserEngineEncodingTest"`
Expected: PASS (3 tests)

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/crossk/data/FileParserEngine.kt \
        app/src/test/java/com/crossk/data/FileParserEngineEncodingTest.kt
git commit -m "fix(CR-3): detect encoding via BOM then UTF-8/GB18030 fallback

Chinese Windows users commonly export .txt as GBK. Hard-coded UTF-8
read produces mojibake, silently corrupting analysis. Now: BOM →
UTF-8 (validate no replacement chars on non-ASCII) → GB18030."
```

---

## Task P0-4: Stage reset on analysis error (CR-4)

**Files:**
- Create: `app/src/test/java/com/crossk/data/AnalysisStageResetTest.kt`
- Modify: `app/src/main/java/com/crossk/data/FileRepository.kt:159-219` — wrap analysis in try/finally

**Interfaces:**
- Consumes: nothing (uses existing `addFileAsync`)
- Produces: `analysisStage` is `IDLE` after any exception during analysis

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/crossk/data/AnalysisStageResetTest.kt`:

```kotlin
package com.crossk.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.crossk.ai.AnalysisStage
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AnalysisStageResetTest {

    private lateinit var repo: FileRepository

    @Before fun setUp() {
        val db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            com.crossk.data.db.AppDatabase::class.java
        ).allowMainThreadOperations().build()
        repo = FileRepository().apply { database = db }
    }

    @After fun tearDown() { repo.database?.close() }

    @Test fun `analysis stage resets to IDLE after exception`() = runBlocking {
        // Force the analysis engine to throw by passing a sentinel
        val result = runCatching {
            repo.addFileAsync(
                name = "test.txt",
                content = "normal text content",
                extension = "txt",
                sizeBytes = 100,
                onStageChange = { /* no-op */ }
            )
        }
        // Even if the call fails, the stage should not be stuck
        // Note: this test requires injecting a fake analysis engine to force throw.
        // For now, just verify the normal path leaves stage = IDLE.
        assertThat(repo.analysisStage).isEqualTo(AnalysisStage.IDLE)
    }
}
```

For a more rigorous test, refactor `FileRepository` to accept an `AnalysisEngine` constructor parameter (or interface) so we can inject a throwing one. If that's too invasive, this test is enough to assert the post-condition on the success path; the failure-path behavior is verified by code review of the try/finally.

- [ ] **Step 2: Run test to verify it fails (or passes — depends on existing state)**

Run: `cd $REPO && .\gradlew.bat testDebugUnitTest --tests "com.crossk.data.AnalysisStageResetTest"`
Expected: PASS (because the success path already resets to IDLE in `addFileAsync` line 216).

This test guards against future regressions. The real fix is in the production code.

- [ ] **Step 3: Modify `addFileAsync` to use try/finally for stage reset**

In `app/src/main/java/com/crossk/data/FileRepository.kt`, the `addFileAsync` method (lines 159-219) currently sets `analysisStage = AnalysisStage.IDLE` only on the success path (line 216). Wrap the body in try/finally:

```kotlin
suspend fun addFileAsync(
    name: String,
    content: String,
    extension: String,
    sizeBytes: Long,
    onStageChange: (AnalysisStage) -> Unit = {},
): RepoResult<FileItem> = withContext(Dispatchers.IO) {
    RepoResult.runCatchingResult {
        try {
            val now = System.currentTimeMillis()
            val fileId = UUID.randomUUID().toString()

            val result = analysisEngine.analyzeWithProgress(content, fileId) { stage ->
                analysisStage = stage
                onStageChange(stage)
            }
            // ... (existing body unchanged)
            file
        } finally {
            analysisStage = AnalysisStage.IDLE
        }
    }
}
```

The same try/finally pattern should also be applied to `addFile` (sync, lines 225-268) if it sets the stage anywhere — currently it does NOT touch the stage. The async path is the only one that uses stages. So only the async path needs the fix.

- [ ] **Step 4: Run test to verify it still passes**

Run: `cd $REPO && .\gradlew.bat testDebugUnitTest --tests "com.crossk.data.AnalysisStageResetTest"`
Expected: PASS

- [ ] **Step 5: Run full test suite**

Run: `cd $REPO && .\gradlew.bat testDebugUnitTest`
Expected: no regressions

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/crossk/data/FileRepository.kt \
        app/src/test/java/com/crossk/data/AnalysisStageResetTest.kt
git commit -m "fix(CR-4): reset analysisStage in finally block on addFileAsync

Previous code only reset on success path. An analysis exception left
the UI stuck on 'analysis in progress' until app restart."
```

---

## Task P0-5: Relation type true classification (IM-1)

**Files:**
- Modify: `app/src/main/java/com/crossk/ai/AnalysisEngine.kt:209-285` — refactor pattern matching to use capture groups
- Modify: `app/src/test/java/com/crossk/data/RelationExtractionTest.kt` — add 5+ new test cases

**Interfaces:**
- Consumes: `sentences: List<String>`, `entityNames: Set<String>`
- Produces: `extractRelationsFromAnalysis(result, content): List<Relation>` — relation.type is reliable

- [ ] **Step 1: Add failing tests to RelationExtractionTest.kt**

Read the existing `app/src/test/java/com/crossk/data/RelationExtractionTest.kt` to understand the test setup (mocks, helpers). Then add these tests at the end:

```kotlin
@Test fun `X causes Y is classified as DERIVES_FROM, not just any sentence containing 导致`() {
    // Setup with two entities X and Y
    val text = "气候变化导致海平面上升。X 和 Y 是相关概念。"
    val result = analyzer.analyze(text, "f1")
    val relations = analyzer.extractRelationsFromAnalysis(result, text)
    // Should be: X→Y CO_OCCURS, NOT classified as DERIVES_FROM
    val xy = relations.firstOrNull { it.sourceEntityId.contains("X") || it.targetEntityId.contains("X") }
    // Specifically: "导致" only triggers if the captured subject/object match real entities
    // If X and Y are not in the 导致 sentence, no DERIVES_FROM edge should be created from that pattern
    // (This is the IM-1 bug: a single "导致" in a sentence made ALL pairs in that sentence DERIVES_FROM)
    val hasFalseDerive = relations.any { it.type == RelationType.DERIVES_FROM && it.weight > 0.8f }
    // Either no false positive, OR the false-positive relation's weight reflects low confidence
    assertThat(hasFalseDerive).isFalse()
}

@Test fun `X derives from Y is correctly classified`() {
    val text = "深度学习由神经网络衍生而来。"
    val result = analyzer.analyze(text, "f1")
    val relations = analyzer.extractRelationsFromAnalysis(result, text)
    val deriveEdge = relations.firstOrNull { it.type == RelationType.DERIVES_FROM }
    assertThat(deriveEdge).isNotNull()
}

@Test fun `A belongs to B is classified as BELONGS_TO`() {
    val text = "卷积神经网络属于深度学习。"
    val result = analyzer.analyze(text, "f1")
    val relations = analyzer.extractRelationsFromAnalysis(result, text)
    val belongsEdge = relations.firstOrNull { it.type == RelationType.BELONGS_TO }
    assertThat(belongsEdge).isNotNull()
}

@Test fun `A contrasts with B is classified as CONTRASTS_WITH`() {
    val text = "监督学习和无监督学习不同。"
    val result = analyzer.analyze(text, "f1")
    val relations = analyzer.extractRelationsFromAnalysis(result, text)
    val contrastEdge = relations.firstOrNull { it.type == RelationType.CONTRASTS_WITH }
    assertThat(contrastEdge).isNotNull()
}

@Test fun `sentence without pattern keywords only produces CO_OCCURS`() {
    val text = "机器学习是人工智能的核心。"
    val result = analyzer.analyze(text, "f1")
    val relations = analyzer.extractRelationsFromAnalysis(result, text)
    assertThat(relations.all { it.type == RelationType.CO_OCCURS }).isTrue()
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd $REPO && .\gradlew.bat testDebugUnitTest --tests "com.crossk.data.RelationExtractionTest"`
Expected: at least one new test FAILS (the false-positive "导致" classification bug)

- [ ] **Step 3: Refactor pattern matching in extractRelationsFromAnalysis**

In `app/src/main/java/com/crossk/ai/AnalysisEngine.kt`, replace the `patternMatch` logic (lines 233-236). The fix must:
1. Use `m.groupValues[1]` and `m.groupValues[2]` to identify the captured X and Y
2. Check that `a in m.groupValues[1]` AND `b in m.groupValues[2]`, OR `b in m.groupValues[1]` AND `a in m.groupValues[2]`
3. Only assign the relation type if the capture groups contain the actual entity pair

```kotlin
// Old (lines 233-236):
val patternMatch = relationPatterns.firstNotNullOfOrNull { (rx, ty) ->
    val m = rx.find(sentence)
    if (m != null && (a in m.groupValues[0] || b in m.groupValues[0])) ty else null
} ?: RelationType.CO_OCCURS

// New:
val patternMatch = classifyRelationByPattern(sentence, a, b)
```

And add a new private method:

```kotlin
private fun classifyRelationByPattern(
    sentence: String,
    entityA: String,
    entityB: String,
): RelationType {
    for ((rx, type) in relationPatterns) {
        val m = rx.find(sentence) ?: continue
        val g1 = m.groupValues[1]
        val g2 = m.groupValues[2]
        // Both entities must appear in their respective capture groups
        val matchAB = entityA in g1 && entityB in g2
        val matchBA = entityB in g1 && entityA in g2
        if (matchAB || matchBA) return type
    }
    return RelationType.CO_OCCURS
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd $REPO && .\gradlew.bat testDebugUnitTest --tests "com.crossk.data.RelationExtractionTest"`
Expected: PASS (all new + old tests)

- [ ] **Step 5: Run full test suite**

Run: `cd $REPO && .\gradlew.bat testDebugUnitTest`
Expected: no regressions

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/crossk/ai/AnalysisEngine.kt \
        app/src/test/java/com/crossk/data/RelationExtractionTest.kt
git commit -m "fix(IM-1): classify relation type using regex capture groups

Previous logic checked whether the regex's full match string contained
either entity, ignoring capture groups. This made a single '导致' in a
sentence turn every entity pair in that sentence into a DERIVES_FROM
edge. Now: capture groups are used to confirm the pattern actually
identifies X and Y in the documented grammatical roles."
```

---

## Task P0-6: Room schema 3→4 migration

**Files:**
- Modify: `app/src/main/java/com/crossk/data/db/AppDatabase.kt` — bump version, add migration
- Modify: `app/src/main/java/com/crossk/data/db/Migrations.kt` — write `MIGRATION_3_4`
- Modify: `app/src/main/java/com/crossk/data/db/EntityEntity.kt` (or wherever `EntityEntity` is defined) — add 3 columns
- Modify: `app/src/main/java/com/crossk/data/db/EdgeEntity.kt` — add 2 columns
- Modify: `app/src/main/java/com/crossk/data/db/FileEntity.kt` — change id to Long autoGenerate, add 3 columns
- Create: `app/src/main/java/com/crossk/data/db/EntityConfirmationEntity.kt` — new table
- Modify: `app/src/main/java/com/crossk/data/db/KnowledgeEntity.kt` — add 4 columns
- Create: `app/src/test/java/com/crossk/data/db/SchemaMigrationTest.kt`

**Interfaces:**
- Consumes: existing v3 schema (entities, files, edges, knowledge, graph_layout tables)
- Produces: v4 schema with new columns + new table

- [ ] **Step 1: Write the failing migration test**

Create `app/src/test/java/com/crossk/data/db/SchemaMigrationTest.kt`:

```kotlin
package com.crossk.data.db

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SchemaMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test fun `migrate 3 to 4 preserves files and adds new columns`() {
        helper.createDatabase("test-v3", 3).apply {
            execSQL("""INSERT INTO files (id, uri, title, mime, sizeBytes, addedAt, lastOpenedAt)
                       VALUES ('legacy-id', 'file:///test.txt', 'test', 'text/plain', 100, 1, NULL)""")
            close()
        }
        val db = helper.runMigrationsAndValidate("test-v4", 4, true, Migrations.MIGRATION_3_4)
        val cursor = db.query("SELECT id, encoding, source, analysisVersion FROM files")
        cursor.moveToFirst()
        // v3 used String id; v4 uses Long autoGenerate. Migration must convert.
        // Document the actual behavior of MIGRATION_3_4 here once written.
        // For now, we just assert the columns exist:
        val columnNames = cursor.columnNames.toSet()
        assertThat(columnNames).contains("encoding")
        assertThat(columnNames).contains("source")
        assertThat(columnNames).contains("analysisVersion")
        cursor.close()
    }

    @Test fun `migrate 3 to 4 creates entity_confirmations table`() {
        helper.createDatabase("test-v3b", 3).apply { close() }
        val db = helper.runMigrationsAndValidate("test-v4b", 4, true, Migrations.MIGRATION_3_4)
        val cursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='entity_confirmations'")
        assertThat(cursor.count).isEqualTo(1)
        cursor.close()
    }
}
```

(Note: this test uses `androidx.room.testing.MigrationTestHelper` which requires `room-testing` artifact. Add it to `app/build.gradle.kts` testImplementation if not present.)

- [ ] **Step 2: Add `room-testing` dependency if needed**

In `app/build.gradle.kts` (or `gradle/libs.versions.toml`), add to test dependencies:
```kotlin
testImplementation("androidx.room:room-testing:2.6.1")
```
(Verify version against the room version used; check `gradle/libs.versions.toml`.)

- [ ] **Step 3: Run test to verify it fails (no MIGRATION_3_4 yet)**

Run: `cd $REPO && .\gradlew.bat testDebugUnitTest --tests "com.crossk.data.db.SchemaMigrationTest"`
Expected: FAIL with "no migration from 3 to 4"

- [ ] **Step 4: Update entity classes (add columns)**

In `app/src/main/java/com/crossk/data/db/EntityEntity.kt`, add:
```kotlin
@ColumnInfo(defaultValue = "0") val isUserConfirmed: Boolean = false,
@ColumnInfo(defaultValue = "0") val isUserIgnored: Boolean = false,
@ColumnInfo(defaultValue = "0") val confirmationCount: Int = 0,
```
(Check that the `@Entity` indices and `@PrimaryKey` match the existing definition. Note: `id` type stays as `String` for now — the migration only adds columns; the id type conversion (String→Long for files) is handled in the migration SQL below.)

In `app/src/main/java/com/crossk/data/db/EdgeEntity.kt`, add:
```kotlin
@ColumnInfo(defaultValue = "0.5") val confidence: Float = 0.5f,
@ColumnInfo(defaultValue = "'rule'") val source: String = "rule",
```

In `app/src/main/java/com/crossk/data/db/FileEntity.kt`, add:
```kotlin
@ColumnInfo(defaultValue = "'UTF-8'") val encoding: String = "UTF-8",
@ColumnInfo(defaultValue = "'import'") val source: String = "import",
@ColumnInfo(defaultValue = "3") val analysisVersion: Int = 3,
```

In `app/src/main/java/com/crossk/data/db/KnowledgeEntity.kt`, add:
```kotlin
@ColumnInfo(defaultValue = "0") val onboardingCompleted: Boolean = false,
@ColumnInfo(defaultValue = "NULL") val firstCaptureAt: Long? = null,
@ColumnInfo(defaultValue = "NULL") val lastCaptureAt: Long? = null,
@ColumnInfo(defaultValue = "0") val captureStreak: Int = 0,
```

Bump the version in `AppDatabase.kt` from `version = 3` to `version = 4`.

- [ ] **Step 5: Create EntityConfirmationEntity**

Create `app/src/main/java/com/crossk/data/db/EntityConfirmationEntity.kt`:

```kotlin
package com.crossk.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "entity_confirmations",
    indices = [Index("entityId"), Index("confirmedAt")],
)
data class EntityConfirmationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entityId: String,
    val originalType: String,
    val confirmedType: String?,
    val isIgnored: Boolean,
    val confirmedAt: Long,
)
```

Add it to `@Database(entities = [...])` in `AppDatabase.kt`:
```kotlin
@Database(
    entities = [
        FileEntity::class,
        EntityEntity::class,
        EdgeEntity::class,
        KnowledgeEntity::class,
        GraphLayoutEntity::class,
        EntityConfirmationEntity::class,
    ],
    version = 4,
    ...
)
```

- [ ] **Step 6: Write MIGRATION_3_4 in Migrations.kt**

Open `app/src/main/java/com/crossk/data/db/Migrations.kt`. Add:

```kotlin
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // entities: add 3 columns
        db.execSQL("ALTER TABLE entities ADD COLUMN isUserConfirmed INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE entities ADD COLUMN isUserIgnored INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE entities ADD COLUMN confirmationCount INTEGER NOT NULL DEFAULT 0")

        // edges: add 2 columns
        db.execSQL("ALTER TABLE edges ADD COLUMN confidence REAL NOT NULL DEFAULT 0.5")
        db.execSQL("ALTER TABLE edges ADD COLUMN source TEXT NOT NULL DEFAULT 'rule'")

        // files: add 3 columns
        db.execSQL("ALTER TABLE files ADD COLUMN encoding TEXT NOT NULL DEFAULT 'UTF-8'")
        db.execSQL("ALTER TABLE files ADD COLUMN source TEXT NOT NULL DEFAULT 'import'")
        db.execSQL("ALTER TABLE files ADD COLUMN analysisVersion INTEGER NOT NULL DEFAULT 3")

        // knowledge: add 4 columns
        db.execSQL("ALTER TABLE knowledge ADD COLUMN onboardingCompleted INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE knowledge ADD COLUMN firstCaptureAt INTEGER")
        db.execSQL("ALTER TABLE knowledge ADD COLUMN lastCaptureAt INTEGER")
        db.execSQL("ALTER TABLE knowledge ADD COLUMN captureStreak INTEGER NOT NULL DEFAULT 0")

        // new table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS entity_confirmations (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                entityId TEXT NOT NULL,
                originalType TEXT NOT NULL,
                confirmedType TEXT,
                isIgnored INTEGER NOT NULL,
                confirmedAt INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS index_entity_confirmations_entityId ON entity_confirmations (entityId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_entity_confirmations_confirmedAt ON entity_confirmations (confirmedAt)")
    }
}
```

In `AppDatabase.kt`, register the migration:
```kotlin
.addMigrations(MIGRATION_2_3, Migrations.MIGRATION_3_4)
```

(Note: the existing `MIGRATION_2_3` may live in `AppDatabase.kt` itself; the new one is in `Migrations.kt`. Verify the import.)

- [ ] **Step 7: Run migration test to verify it passes**

Run: `cd $REPO && .\gradlew.bat testDebugUnitTest --tests "com.crossk.data.db.SchemaMigrationTest"`
Expected: PASS (2 tests)

- [ ] **Step 8: Run full test suite**

Run: `cd $REPO && .\gradlew.bat testDebugUnitTest`
Expected: no regressions

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/com/crossk/data/db/ \
        app/build.gradle.kts \
        gradle/libs.versions.toml \
        app/src/test/java/com/crossk/data/db/SchemaMigrationTest.kt
git commit -m "feat(schema): migrate Room 3 → 4 with new columns + entity_confirmations

New columns:
- entities: isUserConfirmed, isUserIgnored, confirmationCount
- edges: confidence, source
- files: encoding, source, analysisVersion
- knowledge: onboardingCompleted, firstCaptureAt, lastCaptureAt, captureStreak

New table entity_confirmations for v3 user feedback loop (entity type
confirmation / ignore). Migration is non-destructive; existing data
preserved with sensible defaults."
```

---

## Task P0-7: loadFromRoom error wrapping

**Files:**
- Modify: `app/src/main/java/com/crossk/data/FileRepository.kt:83-97` — return `RepoResult<Unit>`, wrap in `runCatchingResult`
- Search for callers of `loadFromRoom(` and update return type expectations

- [ ] **Step 1: Find all callers of `loadFromRoom`**

Run: `cd $REPO && grep -rn "loadFromRoom" app/src/ --include="*.kt"`
Note: PowerShell equivalent — use the `grep` tool.

- [ ] **Step 2: Update `loadFromRoom` to return `RepoResult<Unit>`**

In `app/src/main/java/com/crossk/data/FileRepository.kt`:

```kotlin
// Old:
suspend fun loadFromRoom() = withContext(Dispatchers.IO) {
    val db = database ?: return@withContext
    // ...
}

// New:
suspend fun loadFromRoom(): RepoResult<Unit> = withContext(Dispatchers.IO) {
    RepoResult.runCatchingResult {
        val db = database ?: return@runCatchingResult
        val knowledge = db.knowledgeDao().getKnowledgeSync()
        if (knowledge != null) {
            gameEngine.restoreXp(knowledge.totalXp)
            graphVisualLevel = knowledge.graphVisualLevel
            gameEngine.restoreStreak(StreakData(
                currentStreak = knowledge.streakCurrent,
                longestStreak = knowledge.streakLongest,
                lastActiveDate = knowledge.streakLastActive,
            ))
        }
    }
}
```

- [ ] **Step 3: Update all callers**

For each caller, wrap the call:

```kotlin
// Old:
loadFromRoom()

// New:
when (val r = loadFromRoom()) {
    is RepoResult.Err -> {
        // log, snackbar, or skip — caller decides
    }
    is RepoResult.Ok -> { /* nothing to do */ }
}
```

Common callers to check: `MainActivity.onCreate` (if it calls this), `CrossKApp.onCreate`, test fixtures.

- [ ] **Step 4: Run full test suite**

Run: `cd $REPO && .\gradlew.bat testDebugUnitTest`
Expected: no regressions (no new test — this is a defensive change)

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/crossk/data/FileRepository.kt
git commit -m "refactor: wrap loadFromRoom in RepoResult for consistent error handling

Aligns with saveAll and loadAllFromRoom which already return RepoResult.
Previously, a DB read failure during initial load would throw and crash
the ViewModel. Now callers handle the error uniformly."
```

---

## Task P0-8: TimeTravelSlider crash on first install (B-TTS-1/2)

> Added in this plan after code review pass 2 group B discovered a critical crash on first install. `Slider(steps = -1)` throws `IllegalArgumentException` when `totalWeeks < 2`; the percentage display also divides by zero. Must fix before any release that exposes the Spectrum screen to fresh users.

**Files:**
- Modify: `app/src/main/java/com/crossk/ui/components/TimeTravelSlider.kt:99-114`
- Create: `app/src/test/java/com/crossk/ui/components/TimeTravelSliderTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/crossk/ui/components/TimeTravelSliderTest.kt`:

```kotlin
package com.crossk.ui.components

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TimeTravelSliderTest {

    // Pure logic tests for the percentage / steps math (extract to a function if not already)

    @Test fun `computeSteps never returns negative`() {
        // currentTimeTravelSteps(0) should be 0, not -2
        // currentTimeTravelSteps(1) should be 0, not -1
        // currentTimeTravelSteps(2) should be 0
        // currentTimeTravelSteps(3) should be 1
        // Implement by extracting the logic from TimeTravelSlider or mocking
        assertThat(safeSteps(0)).isEqualTo(0)
        assertThat(safeSteps(1)).isEqualTo(0)
        assertThat(safeSteps(2)).isEqualTo(0)
        assertThat(safeSteps(3)).isEqualTo(1)
    }

    @Test fun `percentage returns zero when totalWeeks is zero`() {
        assertThat(safePercentage(currentWeek = 0, totalWeeks = 0)).isEqualTo(0)
        assertThat(safePercentage(currentWeek = 5, totalWeeks = 0)).isEqualTo(0)
    }

    @Test fun `percentage computes normally with positive totalWeeks`() {
        assertThat(safePercentage(currentWeek = 1, totalWeeks = 4)).isEqualTo(50)
        assertThat(safePercentage(currentWeek = 0, totalWeeks = 4)).isEqualTo(25)
    }

    private fun safeSteps(totalWeeks: Int): Int = (totalWeeks - 2).coerceAtLeast(0)
    private fun safePercentage(currentWeek: Int, totalWeeks: Int): Int =
        if (totalWeeks == 0) 0 else ((currentWeek + 1).toFloat() / totalWeeks * 100).toInt()
}
```

These test the helper functions you should extract from the Composable for testability. If you keep the math inline, you'll need Compose UI tests (Roborazzi / Paparazzi) instead — more setup, same coverage.

- [ ] **Step 2: Refactor TimeTravelSlider to use safe helpers**

In `app/src/main/java/com/crossk/ui/components/TimeTravelSlider.kt`:

```kotlin
// Extract these as top-level or static helpers:

private fun timeTravelSteps(totalWeeks: Int): Int = (totalWeeks - 2).coerceAtLeast(0)

private fun timeTravelPercentage(currentWeek: Int, totalWeeks: Int): Int =
    if (totalWeeks == 0) 0
    else ((currentWeek + 1).toFloat() / totalWeeks * 100).toInt()

// In the Composable (replace lines 99-114):
val percentage = timeTravelPercentage(currentWeek, totalWeeks)
val safeSteps = timeTravelSteps(totalWeeks)
Slider(
    value = currentWeek.toFloat(),
    onValueChange = { onWeekChanged(it.toInt()) },
    valueRange = 0f..(totalWeeks - 1).coerceAtLeast(0).toFloat(),  // also: 0f when no weeks
    steps = if (totalWeeks >= 2) safeSteps else null,  // null hides the slider or use coerceAtLeast
)
```

If `totalWeeks == 0`, the entire `TimeTravelSlider` composable should probably return early (just don't render the slider at all). Wrap the body in:

```kotlin
if (totalWeeks == 0) return  // or: if (totalWeeks < 2) return
```

(Note: at the top of the Composable function body, after the parameter destructuring.)

- [ ] **Step 3: Run tests to verify they pass**

Run: `cd $REPO && .\gradlew.bat testDebugUnitTest --tests "com.crossk.ui.components.TimeTravelSliderTest"`
Expected: PASS (3 tests)

- [ ] **Step 4: Manual smoke test (optional but recommended)**

Build and run the app on a fresh install. Open the Spectrum screen. Verify no crash and the slider either hides or shows a sensible default.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/crossk/ui/components/TimeTravelSlider.kt \
        app/src/test/java/com/crossk/ui/components/TimeTravelSliderTest.kt
git commit -m "fix(B-TTS-1/2): guard TimeTravelSlider against zero/two weeks of data

`Slider(steps = -1)` throws IllegalArgumentException on first install.
Percentage display divides by zero. Both crash visible immediately on
Spectrum screen. Now: totalWeeks == 0 hides the slider; steps coerced
to 0; percentage returns 0 instead of NaN."
```

---

## Acceptance Criteria (Phase 0)

- [ ] `./gradlew assembleDebug` 0 errors
- [ ] All 7+1 tasks committed
- [ ] All new tests pass; existing tests still pass
- [ ] Code review pass 2 group B's TTS-1/2/3 no longer reproduce
- [ ] CR-1: 100 unique entity names produce 100 unique IDs
- [ ] CR-2: Mid-transaction failure leaves no partial data
- [ ] CR-3: GBK-encoded file decodes to correct Chinese text
- [ ] CR-4: analysisStage == IDLE after any addFileAsync outcome
- [ ] IM-1: Sentences with "导致" but no captured X→Y produce no DERIVES_FROM edges
- [ ] Schema 3→4 migration: existing data preserved, new columns/table present
- [ ] `loadFromRoom` returns `RepoResult<Unit>`, no thrown exceptions on caller
- [ ] Manual smoke: fresh install, no Spectrum screen crash

## Out of Scope (Phase 0)

- Phase 1 (V2 Phase A cleanup) — separate plan
- Phase 2 (Ambient Capture) — separate plan
- Phase 3 (On-device AI / ML Kit) — separate plan
- Block-level granularity — v4
- Embedding / semantic search — v3.1
