# Code Review — Group A (Nav + Main + 8 Screens + Widget)

> Scope: 14 files; spec 1.2 defects excluded by instruction.
> Pass: 2 — Group A (UI/Activity layer).
> Source: `bg_62445b1b` explore agent, 2026-08-15.

## Summary

Two real data-loss risks in the Application/ViewModel layer (onCleared save + onTerminate reliability), one broken end-user feature in the postcard editor (no-op Save/Export), and a handful of lifecycle/UX defects in the screens. Most "Important" items are correctness around navigation back-stack, undo semantics, and missing image/* handling on Share Sheet.

## 🔴 Critical

### A-1: Postcard Save & Export are no-ops
- File: `app/src/main/java/com/crossk/ui/screens/PostcardEditorScreen.kt:103-106, 313-327`
- Trigger: User taps the "导出" action icon or the "保存明信片" button.
- Impact: Feature advertised in v3 (明信片 = growth souvenir) is non-functional. The "明信片" route ships with two visible CTAs that do nothing — `onClick = { /* export */ }` and `onClick = { /* Save postcard to local storage */ }`. State is also in-memory only (`remember { mutableStateListOf() }` line 84), so even configuration change wipes the canvas.
- Fix: Wire the buttons to (a) `Bitmap` rendering of the `Canvas` (via `captureToImage` or GraphicsLayer) + `MediaStore.Images` insert for save; (b) `ACTION_SEND` for share. Persist `layers` snapshot to DataStore keyed by project id.

### A-2: `MainViewModel.onCleared` cannot save — viewModelScope is already cancelled
- File: `app/src/main/java/com/crossk/ui/MainViewModel.kt:46-51`
- Trigger: ViewModel destroyed (process background, finish).
- Impact: `onCleared` runs **after** `viewModelScope.cancel()`. The `launch` enqueued in the override is dispatched into a cancelled scope and never runs. Combined with `FileIntelligenceApp.onTerminate` being unreliable on Android (per docs, `onTerminate` is only called in emulators), in-memory mutations made since the last `saveAll()` are lost on app exit.
- Fix: Move last-save duty out of ViewModel lifecycle. Either (a) hook a `ProcessLifecycleOwner` observer in `CrossKApp` to save on `ON_STOP`, or (b) use `ComponentCallbacks2.onTrimMemory`. Do not launch from `onCleared`.

## 🟠 Important

### A-3: Share Sheet image/* path is dropped
- File: `app/src/main/java/com/crossk/QuickCaptureActivity.kt:69-82`
- Trigger: User shares an image (screenshot, photo) to the app.
- Impact: `extractText` only handles `text/plain` and `ACTION_PROCESS_TEXT`. `image/*` returns null and the user sees an empty editor with no explanation. Spec 7.1 lists Photo OCR as a 5th entry and explicitly cites `IntentFilter ACTION_SEND + text/* + image/*`.
- Fix: For `image/*`, copy the URI to internal storage and pass into a ML Kit `TextRecognition` pipeline; on success, put recognized text into `initialText`; on failure, snackbar + still let the user type.

### A-4: Onboarding back-stack — pressing Back returns to empty Home, not exits
- File: `app/src/main/java/com/crossk/ui/navigation/NavGraph.kt:98-102, 208-223`
- Trigger: Onboarding is shown on first launch; user swipes/presses Back instead of completing.
- Impact: Onboarding is pushed onto the back-stack via plain `navigate(route)`, so Back returns to the `startDestination = Home` instead of finishing the app. First-impression "is the app stuck?" moment.
- Fix: In the `LaunchedEffect`, use `navController.navigate(Screen.Onboarding.route) { popUpTo(navController.graph.findStartDestination().id) { inclusive = true } }` so Back exits.

### A-5: Snackbar "撤销删除" loses all analysis metadata
- File: `app/src/main/java/com/crossk/ui/screens/LibraryScreen.kt:289-308`
- Trigger: User deletes a file, then taps "撤销" on the snackbar.
- Impact: Undo calls `repository.addFile(name, content, extension, sizeBytes)` with no URI/addedAt/lastOpenedAt/analysisVersion. The restored file has a new id, re-runs the analysis pipeline from scratch, and lands at the top of the list instead of its original position. Entities/edges are not re-attached to the new id.
- Fix: Persist the deleted `FileItem` to a `pendingUndo` field; on undo, call `repository.upsertFile(deletedFile)` (preserving id, addedAt, entities FK) instead of `addFile`. Add a 5-second window.

### A-6: Settings "清空所有数据" blocks UI / double-writes
- File: `app/src/main/java/com/crossk/ui/screens/SettingsScreen.kt:287-293`
- Trigger: User confirms "确认清空".
- Impact: `repository.files.forEach { repository.deleteFile(it.id) }` runs on the **main thread** inside a `TextButton.onClick` lambda. With 100+ files this freezes the UI for several seconds, and any per-delete cascade (edges, entities, layout) repeats N times. Then `repository.saveAll()` runs — but in-memory state has already mutated per-delete.
- Fix: Wrap in `scope.launch(Dispatchers.IO) { ... }`, batch-delete via a single repo method `clearAll()` that runs one `withTransaction { ... }` over the four Room tables.

### A-7: FileDetail → GraphFocus route uses raw `entity.name` (no URI encoding)
- File: `app/src/main/java/com/crossk/ui/screens/FileDetailScreen.kt:195-198` (paired with `NavGraph.kt:149-155` and `createRoute` at `NavGraph.kt:75`)
- Trigger: Entity name contains `/`, `?`, or other reserved characters.
- Impact: `navController.navigate("graph/${entity.name}")` builds an invalid route string. NavController throws `IllegalArgumentException` for names with `/` or whitespace entities. Reproducible with `"C++/CLI"`, `"X/Y"`, `"A?B"`.
- Fix: Switch `graph/{nodeLabel}` to a query-arg form `graph?label={name}` and pass via `navArgument("label") { type = NavType.StringType }` (Nav handles encoding internally).

## 🟡 Minor

### A-8: ReaderScreen line-spacing icon has no state indicator
- File: `app/src/main/java/com/crossk/ui/screens/ReaderScreen.kt:153-167`
- Impact: Cycles COMPACT (1.3) → DEFAULT (1.6) → RELAXED (2.0) silently; icon stays the same.
- Fix: Show a small label "1.3×" / "1.6×" / "2.0×" next to the icon.

### A-9: ReaderScreen font size can land between presets
- File: `app/src/main/java/com/crossk/ui/screens/ReaderScreen.kt:205, 224, 111`
- Impact: Sizes step by 2 starting from 15 → 13/15/17/19/21. 17 and 19 are not in the `FontSize` enum (13/15/18/21). Display shows raw int, contradicting the "小/中/大/特大" preset labels.
- Fix: Snap to enum values: `fontSize = FontSize.values().minBy { abs(it.size - fontSize) }.size.toFloat()`.

### A-10: PostcardEditor copy lies — "双击编辑文字" with no double-tap handler
- File: `app/src/main/java/com/crossk/ui/screens/PostcardEditorScreen.kt:195, 199`
- Impact: Default text says "double-click to edit" but the BasicTextField at line 155 is always visible when a text layer is selected. No double-tap detection exists.
- Fix: Remove the BasicTextField from the always-visible region and only show it after a `detectTapGestures(onDoubleTap = { editingId = layer.id })`, or change the default text to "点击此处编辑文字".

### A-11: PostcardEditor layer delete has no confirmation / undo
- File: `app/src/main/java/com/crossk/ui/screens/PostcardEditorScreen.kt:271-308`
- Impact: Layer is removed immediately; the click target competes with the "select" use case, so a single mis-tap requires recreating.
- Fix: Split into two icons (select + delete with confirm) or use long-press to delete + snackbar undo.

### A-12: SpectrumGrowthScreen `derivedStateOf` keys are too coarse
- File: `app/src/main/java/com/crossk/ui/screens/SpectrumGrowthScreen.kt:49-51, 53-79, 83-109`
- Impact: Heatmap/spectrum/graph may render stale values when in-place mutations happen at unchanged size. Mostly cosmetic.
- Fix: Use `repository?.globalEntities` (or a `repository.stateVersion` Flow) as the key instead of `.size`.

### A-13: Dead `try/catch` around safe cast `as?`
- Files: `app/src/main/java/com/crossk/QuickCaptureActivity.kt:43-47`, `app/src/main/java/com/crossk/widget/FileIntelligenceWidget.kt:23-27`
- Impact: `as?` never throws — the surrounding `try { ... as? CrossKApp } catch (ClassCastException)` is unreachable.
- Fix: Drop the try/catch. Just write `val app = applicationContext as? CrossKApp`.

### A-14: MainActivity initial state causes first-frame theme flash
- File: `app/src/main/java/com/crossk/MainActivity.kt:37-45`
- Impact: `themePreferences.useSystemTheme.collectAsState(initial = true)` and `darkMode.collectAsState(initial = true)` both start at `true`, so the first frame uses system dark mode regardless of preference. Visible flash on light-mode users.
- Fix: Read first value synchronously via `runBlocking { themePreferences.useSystemTheme.first() }` before `setContent`.

### A-15: SplashScreen doesn't cancel on background
- File: `app/src/main/java/com/crossk/ui/screens/SplashScreen.kt:66-69`
- Impact: `LaunchedEffect(Unit) { delay(2800); onSplashComplete() }` keeps the timer alive across stop/resume. Mostly cosmetic; adds 2.8s to first contentful paint.
- Fix: Shorten delay to 1200ms or restart on `Lifecycle.Event.ON_START`.

### A-16: Widget onUpdate may call suspend `getStats()` on main thread
- File: `app/src/main/java/com/crossk/widget/FileIntelligenceWidget.kt:29-30`
- Impact: `AppWidgetProvider.onUpdate` runs on main thread. If `getStats()` is `suspend` (likely, given Room) or hits DataStore, the BroadcastReceiver's 10s ANR window can be hit.
- Fix: Verify `getStats()` shape; if it does IO, wrap in `runBlocking` only after caching in WorkManager, or migrate to `goAsync()`.

## Clean files
- `OnboardingScreen.kt` — only the no-retrigger icon animation (minor aesthetic). No actionable defects.
- `SplashScreen.kt` — clean; A-15 is a minor latency nit.
- `FileIntelligenceApp.kt` — `onTerminate` is unreliable but `appScope` is otherwise fine; A-2 is the related save-on-exit issue and lives in MainViewModel.
- `NavGraph.kt` — only A-4 (onboarding back-stack) and A-7 (route encoding — partially NavGraph).

---

## Cross-cutting observations
- **Postcard feature is hollow**: A-1 + A-10 + A-11 all point to the postcard editor being more demo than product. Either commit to building it (saving, exporting, persistence) or remove it from the navigation graph until v3.x.
- **Lifecycle of "save"** is broken in 3 places (A-2, A-5, A-6): ViewModel.onCleared is the wrong place; undo re-creates with no metadata; clearAll blocks UI. A single `repository.saveCheckpoint(reason: SaveReason)` triggered by `ProcessLifecycleOwner` + WorkManager would solve all three.
- **Share Sheet receiver** is half-built: spec section 7 promises image/* support but code only handles text. A-3 is a low-effort fix (one branch + ML Kit TextRecognition) and unblocks the entire Photo OCR feature.
- **No real "data loss" in existing code** (group A), but the save-on-exit / undo / clear paths *risk* it on common user actions.
