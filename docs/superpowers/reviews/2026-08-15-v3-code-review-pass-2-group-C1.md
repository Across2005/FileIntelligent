# Code Review — Group C1 (Data layer + 5 DAOs)

> Scope: 10 files — 5 data classes + 5 Room DAOs.
> Pass: 2 — Group C1.
> Spec 1.2 defects (FileRepository / FileParserEngine / AppDatabase / Migrations) excluded by instruction.
> Source: `bg_b033c76c` explore agent, 2026-08-15.

## Summary

The five data classes mostly look like refactor extractions of in-memory state out of FileRepository; the five DAOs are thin and mostly correct, with one schema-name hazard (KnowledgeEntity uses `rowId` instead of `id`) that will complicate the v3 schema bump. The two real Critical issues are around the on-disk JSON persistence — both `PersistenceManager` and `BackupManager.importBackup` mutate the in-memory list non-atomically (clear, then loop-add), and `PersistenceManager.save` does a non-atomic `writeText` that can corrupt the JSON on kill. `GameEngine` is not thread-safe and will race when called from background scopes. Most other issues are forward-compatibility hazards for the v3 schema migration and missing N+1 helpers in the DAOs.

## 🔴 Critical

### C1-1: `PersistenceManager.save` is not atomic — `writeText` can corrupt the JSON on app kill
- File: `app/src/main/java/com/crossk/data/PersistenceManager.kt:37`
- Trigger: Process is killed (system reclaim, OOM, user swipe) while `dataFile.writeText` is flushing.
- Impact: `dataFile.writeText(...)` truncates-then-writes in place. A mid-write kill leaves a partial JSON that subsequent `load` (line 49) cannot parse — caught at line 72 with `e.printStackTrace()`. **Result: total silent data loss on next launch**, with no backup and no propagation to the caller. `e.printStackTrace()` is also a violation of spec 4.3 "撤 printStackTrace 静默".
- Fix: Write to `dataFile.parent.resolve("repository_state.json.tmp")` first, then `Files.move(tmp, target, ATOMIC_MOVE)` (via `java.nio.file.Files`; on Android 26+ via `Files.move`, with `REPLACE_EXISTING` for older APIs). Also propagate failure via `Result<Unit>` so callers can surface a "save failed" Snackbar.

### C1-2: `BackupManager.importBackup` clears `repository.files` before re-adding — non-atomic, partial restore visible
- File: `app/src/main/java/com/crossk/data/BackupManager.kt:193-234`
- Trigger: User restores a `.fiba` backup of N files. Crash, OOM, or user navigation away mid-loop.
- Impact: `repository.files.clear()` on line 193 wipes the in-memory list, then the loop on 219-233 re-adds one-by-one. If the loop aborts, the user has lost their old library AND has a partial restore. The per-line `mutableStateListOf` mutation also rerenders the UI mid-restore.
- Fix: Parse all `FileItem`s into an in-memory `List<FileItem>` first, then assign `repository.files = newList` in a single collection replacement. For Room, route through `AppDatabase.withTransaction { fileDao.deleteAll(); fileDao.insertAll(...); entityDao.insertAll(...); edgeDao.insertAll(...) }`. Back up existing `files` to `repository_state.json.bak` before clearing.

### C1-3: `GameEngine` is not thread-safe — `activityLog` and `streak` mutated from any caller
- File: `app/src/main/java/com/crossk/data/GameEngine.kt:14-81`
- Trigger: `addXp` called from a background coroutine (e.g. WorkManager after file import) while the UI thread also reads `totalXp` / `streak`.
- Impact: `activityLog` is `mutableListOf<ActivityRecord>()` (line 29) mutated by `logActivity` via `indexOfFirst` + indexer assignment (lines 73-80) with no synchronization. Two concurrent `addXp` calls can lose updates. The `streak` field is a Compose `mutableStateOf` (line 20) — writes happen from `addXp` which is invoked from non-main scopes. Result: torn streak state, double-counted XP.
- Fix: Make `GameEngine` own a `Mutex` and route all writes through a single `suspend fun addXp(event)` that takes the lock. Use `StateFlow<StreakData>` + `update { it.copy(...) }` and project `var totalXp by mutableStateOf` as a read-side projection. Precondition for A-2's lifecycle-save fix.

## 🟠 Important

### C1-4: `KnowledgeEntity.rowId` is misnamed and blocks v3 schema migration
- File: `app/src/main/java/com/crossk/data/db/KnowledgeEntity.kt:12`; DAO queries at `KnowledgeDao.kt:12, 15`
- Trigger: Spec 5.4 plans to rename PK column to `id` in v3 schema version 4.
- Impact: `rowId` is a SQLite-internal alias for the integer primary key. Naming a `String` column `rowId` is semantically misleading. The DAO query `WHERE rowId = 'main'` works only because the default value is `"main"`, but a v3 migration that renames this column to `id` will require updating entity + 2 DAO queries simultaneously. The spec's `id: Int = 1` form is also a PK type change — a more invasive migration than spec acknowledges.
- Fix: Rename `rowId: String = "main"` to `id: String = "main"` in this pass (1-line entity + 2-line DAO). v3 just adds new columns without a rename.

### C1-5: `EntityDao` and `FileDao` missing the "files with entities" batch query — caller will N+1
- File: `app/src/main/java/com/crossk/data/db/EntityDao.kt:13-16`; paired with `FileDao.kt:14-18`
- Trigger: `LibraryScreen` / `FileDetailScreen` lists files and needs entities per file.
- Impact: Today's API forces `getAllFiles()` → for each file call `getEntitiesForFile(fileId)`. With 200 files × 10 entities = 201 round-trips per recomposition. Spec 1.1 already flagged N+1 as a known v2 defect — the v3 DAOs don't fix it.
- Fix: Add `data class FileWithEntities(val file: FileEntity, val entities: List<EntityEntity>)` and a `@Transaction @Query("SELECT * FROM files ORDER BY lastModified DESC") fun observeAllWithEntities(): Flow<List<FileWithEntities>>` on `FileDao` with `@Relation(parentColumn = "id", entityColumn = "fileId")`. Same pattern for edges.

### C1-6: `PreferenceManager` uses `apply()` (async fsync) for `onboardingCompleted` — first-launch state can be lost
- File: `app/src/main/java/com/crossk/data/PreferenceManager.kt:16`
- Trigger: User finishes onboarding and immediately force-closes the app.
- Impact: `apply()` returns immediately; disk write is queued. On process death before drain, the value reverts to `false` and the user is shown onboarding again. Inconsistent with `ThemePreferences` (same package) which uses DataStore `suspend fun edit` (fsync before return).
- Fix: Replace with DataStore to match `ThemePreferences` and unify persistence. At minimum, use `commit()` for the onboarding key or expose a `suspend fun awaitOnboarding()`.

### C1-7: `ThemePreferences.darkMode` defaults to `true` — wrong UX default for new users
- File: `app/src/main/java/com/crossk/data/ThemePreferences.kt:31`
- Trigger: First app launch on a phone in light mode.
- Impact: `it[darkModeKey] ?: true` means every first-time user starts in **dark mode**. Combined with A-14 (`MainActivity` first-frame uses initial=true), the app is dark on first paint even if the user has the system in light.
- Fix: Change the default to `false` for `darkMode` so the first launch is light. Or expose `useSystemThemeFlow.first()`-style initial value in `MainActivity` (A-14) so the first frame is always correct.

### C1-8: `EdgeDao.getEdgesForFile` uses two `IN (subquery)` clauses — degrades on large `entities` tables
- File: `app/src/main/java/com/crossk/data/db/EdgeDao.kt:33-44, 53-62`
- Trigger: Spec 4.3 targets 2000+ nodes. At that scale, `entities` has 5k+ rows and the subquery in `deleteForFile` materializes the full ID set twice.
- Impact: SQLite rewrites the subquery as a temp B-tree; for 5k IDs × two subqueries per call this is fine, but `replaceForFile` (line 75) does both deletes and inserts, and with 10k entities (spec 1.3 "10k 笔记墙") this becomes a measurable stall.
- Fix: Replace with `EXISTS` correlation, or use Room's `@Transaction` + `@Query` with a `JOIN entities e ON e.id = edges.srcId WHERE e.fileId = :fileId`. Even better: precompute `affectedEntityIds: List<String>` once in the repository and pass as a bind parameter.

## 🟡 Minor

### C1-9: All five DAOs use `String` for `id` / `fileId` — v3 spec changes these to `Long`
- Files: `FileDao.kt:21, 33`, `EntityDao.kt:13, 16, 22`, `EdgeDao.kt:30, 44, 62`, `KnowledgeDao.kt:12-19`
- Impact: Spec 5.3/5.4 says `files.id` becomes `Long autoGenerate` in v3, and `EntityEntity.fileId` follows. Every DAO method listed needs a signature change in the same migration. Mechanical but unblocks the v3 schema bump.
- Fix: Acknowledge in `Migrations` plan. If a v3 preview needs Long IDs earlier, change `fileId: String` → `fileId: Long` now, accepting the type churn once.

### C1-10: `GameEngine.recordActivity` "consecutive" check is hard to read (and the `longestStreak` update is correct but obscured)
- File: `app/src/main/java/com/crossk/data/GameEngine.kt:55-69`
- Impact: `(today - streak.lastActiveDate) <= 86400000L` is inclusive and behaves correctly on the 24h boundary, but the inline `max(...)` with `if (isConsecutive) streak.currentStreak + 1 else 1` is hard to audit. Behavior is correct (because of `max()` against the old `longestStreak`), just hard to read.
- Fix: Inline as `val newCurrent = if (isConsecutive) streak.currentStreak + 1 else 1; val newLongest = max(streak.longestStreak, newCurrent)`.

### C1-11: `BackupManager.importBackup` reads entire entries into `String` — no streaming for very large file content
- File: `app/src/main/java/com/crossk/data/BackupManager.kt:162`
- Impact: 50MB entry cap is per-entry, but `files.json` is a single entry and can hold 10000 files × 50MB = 500MB logical. `JSONArray(filesJson)` will OOM before the cap kicks in. Surfaces as a generic `Result.failure`.
- Fix: Validate `total manifest.fileCount` against a per-backup size budget in `manifest.json` first (manifest is small and read first), and refuse to start if the budget exceeds 200MB.

### C1-12: `EdgeDao` missing Flow version of `getEdgesForNode`; `GraphLayoutDao` has no FK + no `deleteByNodeId` (orphans accumulate)
- File: `app/src/main/java/com/crossk/data/db/EdgeDao.kt`; `GraphLayoutDao.kt:23-27`
- Impact: When an entity is deleted (cascading to its edges), the corresponding `graph_layout` row is orphaned because `GraphLayoutEntity` declares no FK. Layouts accumulate as ghost rows.
- Fix: Add `@Query("DELETE FROM graph_layout WHERE nodeId = :nodeId") suspend fun deleteByNodeId(nodeId: String)` to `GraphLayoutDao`, invoke from a `replaceForFile`-style `@Transaction` on the entity side. Also add a Flow `getEdgesForNode` to `EdgeDao`.

### C1-13: `KnowledgeDao` has no "get-or-init" — first launch returns null, UI in undefined state
- File: `app/src/main/java/com/crossk/data/db/KnowledgeDao.kt:12-19`
- Impact: Fresh install with no `knowledge` row sees `getKnowledge()` emit `null`. Caller must guard for this and lazily insert defaults. Spec 5.4 has a single-row `id = 1`; v2 skips initialization entirely.
- Fix: Add `suspend fun getOrCreate(): KnowledgeEntity` doing `getKnowledgeSync() ?: upsert(KnowledgeEntity(rowId = "main")).also { getKnowledgeSync()!! }`, call once at app startup.

### C1-14: `BackupManager.exportBackup` reads `repository.gameEngine.totalXp` and `repository.files` without a snapshot
- File: `app/src/main/java/com/crossk/data/BackupManager.kt:52-53, 69, 104-107`
- Impact: `withContext(Dispatchers.IO)` is a thread switch, not a memory barrier. While the zip is being written, a background coroutine could add a file or award XP. Manifest's `fileCount = repository.files.size` (line 52) is stale vs. the loop on line 69. "Lying backup" — shows "10 files" in manifest, contains 9.
- Fix: Snapshot state in one pass at the top: `val snapshotFiles = repository.files.toList(); val snapshotXp = repository.gameEngine.totalXp; val snapshotStreak = repository.gameEngine.streak` — use throughout. Moot once C1-2 is fixed by routing through a Room transaction capturing a consistent WAL position.

## Clean files
- `FileDao.kt` — correct indices (createdAt, lastModified), proper suspend/Flow split. No N+1 in its own surface (the N+1 is across files; see C1-5). Only issue is the String→Long hazard (C1-9).
- `GraphLayoutDao.kt` — minimal and correct for its scope. `upsertAll` is a single SQL, atomic. No defensive N+1 risk; layout lookups are single-row.

## Cross-cutting observations

- **State is fragmented across three persistence stacks**: Room (entities/edges/files), `PersistenceManager` JSON on disk (duplicate of Room), and DataStore (`ThemePreferences`) plus SharedPreferences (`PreferenceManager`). Spec 4.1 calls for "Room 为唯一真源". C1-1, C1-6, and the spec's CR-2/CR-3 all point to the same conclusion: `PersistenceManager` should be deleted (or reduced to a one-shot v1 migration shim), and `PreferenceManager` merged into `ThemePreferences` under a single DataStore.
- **Most v3 schema hazards are pre-payable**: C1-4, C1-9, C1-12 are cheap renames/additions today that become 10× more expensive in the v3 migration window. Worth doing in a "v3 prep" pass before the entity rework begins.
- **No tests exist for the DAOs** (verified by absence in the repo). A Room in-memory test for each DAO (esp. `replaceForFile`, `getEdgesForFile`, `getKnowledge`) would have caught C1-13 and most of C1-5 immediately. Recommend as a Phase 0 deliverable alongside spec 1.2's CR-1..CR-4.
