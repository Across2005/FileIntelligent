# Code Review — Group C2 (Data Models + Converters)

> **Reviewer**: Explore (read-only)
> **Date**: 2026-08-15
> **Scope**: 9 files — 8 data classes + 1 Room converter
> **Excluded** (already in spec §1.2): `FileRepository`, `FileParserEngine`, `AppDatabase`, Migrations
> **Methodology**: `code-review` skill — concrete defect → file:line → trigger → impact → fix direction

## Severity legend
- 🔴 **Critical** — crash, data loss, blocked feature, non-functional code
- 🟠 **Important** — silent correctness, scalability risk, contract violation
- 🟡 **Minor** — robustness, API ergonomics, future-proofing

---

## 1. `SoundManager.kt` — 🔴×1, 🟠×1, 🟡×1

| ID | File:Line | Issue | Fix |
|----|-----------|-------|-----|
| **SM-1** | `SoundManager.kt:18-21, 67-71` | 🔴 **Dead class — no sound is ever loaded.** `parseCompleteId`, `graphConnectId`, `postcardSaveId`, `xpGainId` stay at `0`; no `soundPool.load(...)` call exists. `playIfEnabled` gates on `soundId > 0`, so every play method is a no-op. The whole sound feature is non-functional. | Load the four raw resources in `init` via `soundPool.load(context, R.raw.parse_complete, 1)` etc., assign the returned ids. |
| **SM-2** | `SoundManager.kt:34-36` | 🟠 `setOnLoadCompleteListener` is registered **after** `SoundPool.Builder().build()`. Documented race: load may complete before listener attaches, leaving `loaded = false` permanently. | Register the listener before the first `load()` call. |
| **SM-3** | `SoundManager.kt:15` | 🟡 `var enabled` is publicly mutable with no synchronization. Settings thread vs UI thread can race. | `var enabled: Boolean = true; private set` + a `setEnabled(b)` method. |

## 2. `PostcardExporter.kt` — 🔴×1, 🟠×2

| ID | File:Line | Issue | Fix |
|----|-----------|-------|-----|
| **PE-1** | `PostcardExporter.kt:134-135, 169-170` | 🔴 **Exports written to `context.cacheDir` and silently lost.** OS can evict cacheDir at any time. "Export" in name only — nothing reaches the user's storage. | Save to `MediaStore.Downloads` (Q+) / `Environment.DIRECTORY_DOCUMENTS` (pre-Q) and return a `content://` URI. Wrap in `RepoResult<Uri>`. |
| **PE-2** | `PostcardExporter.kt:20-21, 142-143` | 🟠 No try/catch around `file.writeText`. `IOException` propagates with no actionable context (see A-1 in Group A). | Wrap in `runCatchingResult { ... }`, return `RepoResult<File>`. |
| **PE-3** | `PostcardExporter.kt:124-127 vs 158` | 🟠 HTML truncates to `nodes.take(12)` but Markdown emits all nodes. Inconsistent. 500-node export = 100+ KB Markdown. | Apply the same cap in both formats; document the policy. |

## 3. `KnowledgeSystem.kt` — 🟠×3, 🟡×1

| ID | File:Line | Issue | Fix |
|----|-----------|-------|-----|
| **KS-1** | `KnowledgeSystem.kt:66-80, 104-119` | 🟠 **`DailyQuest` progress resets every call to `generateDailyQuests()`.** `progress` and `completed` default to `0`/`false`. A config-change recreate or process death wipes quest state. Same defect class as Group A's `A-5`. | Persist today's rolled quests in a `QuestRepository` / `StateFlow` keyed by `daySeed`; never call `generateDailyQuests()` outside the repo. |
| **KS-2** | `KnowledgeSystem.kt:104-106` | 🟠 `daySeed = System.currentTimeMillis() / 86400000L` is UTC-naive. A user in `Asia/Shanghai` sees day index flip at 16:00 local. DST / time-zone shifts cause mid-evening quest reset. | `LocalDate.now(ZoneId.systemDefault()).toEpochDay()`. |
| **KS-3** | `KnowledgeSystem.kt:153-162` | 🟠 `getGraphEvolution` uses magic numbers `2/4/6/8/10` inline. Will drift from `LEVEL_THRESHOLDS` tuning. | Extract `EVOLUTION_THRESHOLDS = intArrayOf(2,4,6,8,10)` and use `.last()` for the glow denominator. |
| **KS-4** | `KnowledgeSystem.kt:42` | 🟡 `isMaxLevel = level >= LEVEL_THRESHOLDS.size` (=20). Future "prestige" tier would render "未知" + `isMaxLevel=false` — wrong "still leveling" badge. | Constant `MAX_LEVEL = LEVEL_THRESHOLDS.size`. |

## 4. `FileItem.kt` — 🟠×2, 🟡×1

| ID | File:Line | Issue | Fix |
|----|-----------|-------|-----|
| **FI-1** | `FileItem.kt:24-29` | 🟠 **`formatSize` overflows silently for files ≥ 1 GB.** A 2.4 GB file renders `2457MB` instead of `2.4 GB`. Bad for video/dataset workloads. | Add GB branch with `Double` math: `bytes < 1024L*1024*1024 -> "...MB" else "%.1fGB".format(...)`. |
| **FI-2** | `FileItem.kt:12, 39-40, 62` | 🟠 `System.currentTimeMillis()` defaults on `FileItem.createdAt`, `Entity.firstSeen/lastSeen`, `AnalysisResult.analyzedAt`. These are in `equals/hashCode`, so two semantically-equal `FileItem`s get different identities if defaults fire at different times. Gson round-trip also breaks stable identity. | Make timestamp a required field; inject at call site. |
| **FI-3** | `FileItem.kt:13, 16-17, 42, 68` | 🟡 `Entity.color: Color` and `Topic.color: Color` — Compose `Color` leaks into a data class. Any JVM test / non-Android module that imports `data.*` pulls `androidx.compose.ui.graphics`. | Move color mapping to UI layer (`fun Entity.Type.toColor(): Color` in `ui/mapping/`), or accept `Int` argb here. |

## 5. `GraphNode.kt` — 🟠×1, 🟡×2

| ID | File:Line | Issue | Fix |
|----|-----------|-------|-----|
| **GN-1** | `GraphNode.kt:26-50` | 🟠 **`NodeType` is lossy — `fromEntityType` collapses 6 → 5.** `TOOL → ENTITY` and `EVENT → TOPIC` are conflated. v3's "6 categories" North-Star silently drops to 5 in the graph view, the one place the categories matter most. | Add `NodeType.TOOL` and `NodeType.EVENT` (distinct colors) and map 1:1. |
| **GN-2** | `GraphNode.kt:55-60` | 🟡 `GraphEdge` has no `id`. Two edges with identical `source/target/type/weight` are equal; a DAO that upserts by `equals` silently dedupes parallel edges. | Add `val id: String = UUID.randomUUID().toString()`; equality by id only. |
| **GN-3** | `GraphNode.kt:11-19, 55-60` | 🟡 No `@Serializable` contract. `Color` is not `@Serializable`; a future `BackupManager` will silently lose information on read. | Add `@Serializable` + `Color` serializer when persistence lands; in the meantime, kdoc "do not serialize — UI model only". |

## 6. `PostcardData.kt` — 🟠×1, 🟡×1

| ID | File:Line | Issue | Fix |
|----|-----------|-------|-----|
| **PC-1** | `PostcardData.kt:13-24, 35-43, 49-68` | 🟠 Same Compose-leak pattern as `FileItem.kt` (`Offset`, `Color`, `FontWeight`). No `@Serializable`, no way to round-trip a `PostcardProject` to disk. Spec §4.3 has postcard in the persistence track. | Add `@Serializable` with custom serializers, or store as `Canvas` snapshot (`Bitmap` + JSON metadata). |
| **PC-2** | `PostcardData.kt:64-68` | 🟡 `StatBadge.value: String` is stringly-typed. Can't be programmatically aggregated. | `value: Number` + `format(locale)` extension. |

## 7. `GrowthMetric.kt` — 🟡×1

| ID | File:Line | Issue | Fix |
|----|-----------|-------|-----|
| **GM-1** | `GrowthMetric.kt:1-8` | 🟡 No validation that counters are ≥ 0. A negative `connectionsMade` from an off-by-one in the time-bucket aggregator renders fine but breaks `sum / growthRate`. | `init { require(... >= 0) }` per field, or document the contract. |

## 8. `RepoResult.kt` — 🟠×2, 🟡×1

| ID | File:Line | Issue | Fix |
|----|-----------|-------|-----|
| **RR-1** | `RepoResult.kt:11-13` | 🟠 `getOrNull()` cannot distinguish `Ok(null)` from `Err` — both return `null`. Kdoc says `T` may be null (for "delete"), so a caller doing `repo.delete(id).getOrNull()` cannot tell success-with-no-data from failure. | Add `isOk`/`isErr`/`exceptionOrNull()`. Or split out a separate `Empty` variant. |
| **RR-2** | `RepoResult.kt:11-37` | 🟠 **No `flatMap` / `recover` / `getOrElse` / `onSuccess` / `onFailure`.** Every chain requires manual `when (it) { is Ok -> ...; is Err -> return it }`. Barely better than `try/catch`. | Add at minimum `flatMap` + `recover`; ideal is `onSuccess { }` / `onFailure { }` for UI snackbar hooks. |
| **RR-3** | `RepoResult.kt:35` | 🟡 `Err(... e)` preserves cause but UI shows only `message`. Stack trace invisible to developer. | `Log.w("RepoResult", "operation failed", e)` in `runCatchingResult`. |

## 9. `Converters.kt` — 🔴×1, 🟠×1

| ID | File:Line | Issue | Fix |
|----|-----------|-------|-----|
| **CV-1** | `Converters.kt:9-21` | 🔴 **Only `List<String>` is supported.** The schema needs `List<String>` (tags/topics), `List<Entity>`, `Map<String, Float>` (TF-IDF, spec §4.3), `Instant`/`Long` (timestamps), `RelationType` enum. This converter handles **one** of those five+ types. Other fields either NPE, get denormalized, or silently fail to persist. | Add `List<String>`, `Map<String, Float>`, `Instant ↔ Long`, `RelationType ↔ String` converters. Mirror with `fromX`/`toX` and the same JSON error path. |
| **CV-2** | `Converters.kt:12-14, 17-21` | 🟠 **Null-safety contract is wrong.** Room can pass `null` for nullable columns. `List<String>` (non-nullable) NPEs; `toStringList` throws `JSONException` on `"null"` / empty / invalid JSON — crashes on legacy/migrated rows. | Make signatures `List<String>?`; in `toStringList` try/catch → `emptyList()` + `Log.w`. |

---

## Cross-cutting observations

- **Spec §1.2 anti-pattern repeats**: data layer persists Compose types (`Color`, `Offset`, `FontWeight`) as if they were model types. Without a translation layer, spec §4.1's "Room 为唯一真源" is unreachable for postcard, file, and graph state.
- **No `@Serializable` contract** on any data class — every one will need a decision when persistence is wired. Decide now (kotlinx + custom `Color`/`Offset` serializers) rather than retrofit.
- **`RepoResult` is under-built**: missing `flatMap`/`recover` means call sites will keep using `try/catch`. Spec §4.3 lists `RepoResult` as the canonical error path; it isn't ergonomic enough yet to win that role.
- **System-clock defaults**: 3 files (`FileItem`, `Entity`, `AnalysisResult`) use `System.currentTimeMillis()` as a field default; `KnowledgeSystem` uses it for day-rollover. Each is a foot-gun for serialization and timezone correctness.
- **No tests visible** in the data package — a small `KnowledgeSystem` test (level math + day rollover) would have caught KS-2 and KS-4.
