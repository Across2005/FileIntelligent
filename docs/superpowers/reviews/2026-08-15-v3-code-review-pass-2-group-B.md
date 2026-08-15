# FileIntelligence v3 — Code Review Pass 2 · Group B (UI Components)

> **Reviewer**: Explore (read-only)
> **Date**: 2026-08-15
> **Scope**: 16 component/screen files under `ui/components/` + `ui/screens/DashboardScreen.kt`
> **Excluded** (already in spec §1.2): AnalysisEngine, GraphReconstructor, GraphCanvas, FileRepository, ForceGraphEngine
> **Methodology**: `code-review` skill — concrete defect → file:line → trigger → impact → fix direction

## Severity legend
- 🔴 **Critical** — crash, data loss, blocked feature
- 🟠 **Important** — visible UX bug, perf risk, accessibility violation
- 🟡 **Minor** — robustness, consistency, future-proofing

---

## 1. TimeTravelSlider.kt — 🔴×1, 🟠×2

| ID | File:Line | Issue | Fix |
|----|-----------|-------|-----|
| TTS-1 | `TimeTravelSlider.kt:113` | `steps = totalWeeks - 2` — when `totalWeeks < 2`, `steps` is negative. Material3 `Slider` requires `steps >= 0`; **throws `IllegalArgumentException` at first compose** for users with <2 weeks of data (i.e. brand-new install). | Guard: `steps = (totalWeeks - 2).coerceAtLeast(0)`. Better: hide slider entirely when `totalWeeks < 2`. |
| TTS-2 | `TimeTravelSlider.kt:100` | `${((currentWeek + 1).toFloat() / totalWeeks * 100).toInt()}%` — **division by zero** when `totalWeeks == 0`. Renders "NaN%" / "Infinity%". | Early-return guard: if `totalWeeks == 0` show "—" or hide. |
| TTS-3 | `TimeTravelSlider.kt:111-112` | `valueRange = 0f..(totalWeeks - 1).coerceAtLeast(1).toFloat()` masks zero-state: when 0 weeks, range becomes `0f..1f` and user can scrub to "week 1" that doesn't exist. The `coerceAtLeast(1)` is patching a bug instead of fixing it. | Make `totalWeeks == 0` a non-render branch. |

## 2. FileCard.kt — 🟠×2, 🟡

| ID | File:Line | Issue | Fix |
|----|-----------|-------|-----|
| FC-1 | `FileCard.kt:111, 122-124` | `deleting = true; onDelete()` is called synchronously inside `onDragEnd`. Parent LazyColumn removes the item immediately, so the `-maxSwipePx` offset animation is **never observed** — the card just vanishes mid-swipe. | Defer `onDelete()` until after `animateTo` (use `Animatable.animateTo`) or call from a `LaunchedEffect(deleting)` after 200ms. |
| FC-2 | `FileCard.kt:140` | `onClick = if (selectionToggle != null) selectionToggle else onClick` — when in selection mode **both** `selectionToggle` and `onClick` are non-null, only `selectionToggle` fires. User can never enter detail view from a selected card. | Add long-press to enter selection, or wire `onClick` based on a real "is selecting" state from a hoisted ViewModel. |
| FC-3 | `FileCard.kt:79-105` | `offsetX` and `deleting` are `remember`ed but the swipe state **does not survive item recycling in LazyColumn**. Use `rememberSaveable`. |

## 3. GrowthCard.kt — 🟠×2, 🟡

| ID | File:Line | Issue | Fix |
|----|-----------|-------|-----|
| GC-1 | `GrowthCard.kt:80-84` | `growthRate` uses `filesAnalyzed` only, but `growthNarrative` (`buildNarrative` line 413) covers all three dimensions. Headline number and subtitle **disagree** about what is growing. | Show three growth rates, or align both to the same dimension. |
| GC-2 | `GrowthCard.kt:82-84` | `if (first > 0) ... else 0` — when `first = 0` and `last > 0` (first import), `growthRate = 0` so headline shows "+0%" while the real change is infinite. | Distinguish: `first == 0 && last > 0` → "首次" or "NEW". |
| GC-3 | `GrowthCard.kt:413-419` | `buildNarrative` priority: `fileInc > 0` returns "files" line even when `entInc + relInc` are much larger. Silent bias toward file count. | Pick the dimension with the largest non-zero increment. |

## 4. HeatmapChart.kt — 🟠×2, 🟡

| ID | File:Line | Issue | Fix |
|----|-----------|-------|-----|
| HC-1 | `HeatmapChart.kt:130, 140` | Month label row: `Modifier.padding(start = (weekIdx * 12).dp)` — absolute offset. For a 52-week history, week-51 label sits at 28 + 612 = **640dp** from left, off-screen. | Place labels at their column's actual x-coordinate via custom `Layout` or `drawText`. |
| HC-2 | `HeatmapChart.kt:276-288` | `extractMonthLabels` calls `groupIntoWeeks(data)` **again** (`HeatmapChart.kt:279`), even though `weeks` was already computed at line 60. Doubles calendar work. | Pass `weeks` as parameter. |
| HC-3 | `HeatmapChart.kt:77-82` | `LaunchedEffect(stableData)` re-runs the 1.3s entry ripple on every `stableData` reference change. Frequent DB updates will cause constant flicker. | Key on `stableData.size` or only on first composition. |

## 5. GlassCard.kt — 🟠 Perf

| ID | File:Line | Issue | Fix |
|----|-----------|-------|-----|
| GL-1 | `GlassCard.kt:64-78` | Noise texture iterates `width/4 × height/4` rects per recompose (e.g. 270×200 = 54,000 ops) and builds a `Path` for every cell. At alpha `0.02f` the visual effect is **barely perceptible** but the cost is real. | Pre-render noise to a `Bitmap` once, then `drawImage`; or use a shader; or drop the noise entirely. |

## 6. XpGainToast.kt — 🟠

| ID | File:Line | Issue | Fix |
|----|-----------|-------|-----|
| XP-1 | `XpGainToast.kt:87, 89-99` | `val visible = remember(items, key) { true }` — `visible` is **always `true`**. The exit animation only runs because the composable is removed from the `toasts` list. The second toast arriving 200ms after the first must wait behind a 2000ms delay before its exit can play. No queuing, no max-3 cap. | Drive `visible` from a `MutableTransitionState` triggered by `LaunchedEffect`; cap concurrent toasts (e.g., 3). |

## 7. DashboardScreen.kt — 🟠×2, 🟡

| ID | File:Line | Issue | Fix |
|----|-----------|-------|-----|
| DS-1 | `DashboardScreen.kt:142-148` | `if (files.isEmpty()) { EmptyKnowledgeState(...); return@Scaffold }` — early return. The `XpGainToast` mounted inside `LevelBadge` is **never created** in the empty state, so any XP events emitted before the first file imports are silently dropped. | Always mount `XpGainToast` at scaffold level, outside the `if`. |
| DS-2 | `DashboardScreen.kt:159` | `LevelBadge(xp = repository.gameEngine.totalXp)` — direct read, not a `State<>`. If `totalXp` changes via a flow that doesn't also touch `files`, the badge will not redraw. Same anti-pattern as spec §1.2's `Repository 直传 mutableStateListOf`. | Expose `totalXp` as `StateFlow<Int>` collected with `collectAsState()`. |
| DS-3 | `DashboardScreen.kt:227-235` | `focusMode` only limits `files.take(2)` — the toggle does not hide LevelBadge, GrowthCard, HeatmapChart etc. It's a "show fewer files" mode, not a focus mode. | Either rename to "compact mode" or actually hide non-essential cards. |

## 8. LevelBadge.kt — 🟡

| ID | File:Line | Issue | Fix |
|----|-----------|-------|-----|
| LB-1 | `LevelBadge.kt:146` | `fraction = (level.toFloat() / 20f).coerceIn(0f, 1f)` — hard-coded max level = 20. If `KnowledgeLevel` allows >20 (e.g. prestige levels in v3.1), progress ring saturates early. | Pass `maxLevel` from `calculateLevel(...)` and use it. |

## 9. DailyQuestPanel.kt — 🟡

| ID | File:Line | Issue | Fix |
|----|-----------|-------|-----|
| DQ-1 | `DailyQuestPanel.kt:42` | `quests: List<DailyQuest> = generateDailyQuests()` — default parameter invokes a generator on **every recomposition**. If non-pure (e.g. `Random`), list flickers. | Remove default; require caller to pass; or `remember(calDay) { generateDailyQuests() }`. |

## 10. StatsRow.kt — 🟡

| ID | File:Line | Issue | Fix |
|----|-----------|-------|-----|
| SR-1 | `StatsRow.kt:67` | `Icon(... tint = MaterialTheme.colorScheme.primary, ...)` — icon is always tinted with `primary` regardless of `stat.iconBg`. The `iconBg` (per-stat soft color) sets the chip background but the icon glyph is the same color on every chip, breaking visual association. | Tint icon with `stat.iconBg` (saturated) or a color that contrasts the `iconBg`. |

## 11. EmptyState.kt — 🟡

| ID | File:Line | Issue | Fix |
|----|-----------|-------|-----|
| ES-1 | `EmptyState.kt:101-107` | `shapes` list of `Triple`s allocated on **every recomposition**. | `remember { listOf(Triple(...), ...) }`. |
| ES-2 | `EmptyState.kt:212-243, 246-276` | `EmptySearchState` and `EmptyLibraryState` hard-code the same pattern but are duplicate composables. `EmptySearchState` ignores `modifier` for sizing. | Extract a single `EmptyState(emoji, title, subtitle, action?)` with a controlled `modifier`. |

## 12. CognitiveBreathLoading.kt — 🟡

| ID | File:Line | Issue | Fix |
|----|-----------|-------|-----|
| CB-1 | `CognitiveBreathLoading.kt:198-214` | When `isFullScreen = false`, no `fillMaxSize` is added. Caller must pass size modifiers or the loader collapses to the 96dp canvas. Contract is split. | Document or add a sensible default size when `isFullScreen = false`. |

## 13. BottomNav.kt — 🟡

| ID | File:Line | Issue | Fix |
|----|-----------|-------|-----|
| BN-1 | `BottomNav.kt:79` | `val isActive = currentRoute == tab.route` — pure string equality. Sub-route (e.g. `fileDetail/123`) from a tab is marked inactive. | Use `hierarchy.any { it.route == currentRoute }` or `currentRoute.startsWith(tab.route)`. |
| BN-2 | `BottomNav.kt:106` | `popUpTo(Screen.Home.route)` always pops to Home on every tab switch — deep navigation within a tab loses its place. | Confirm with nav spec; use `navController.graph.findStartDestination().id` for consistency. |

## 14. GlowComponents.kt — 🟡

| ID | File:Line | Issue | Fix |
|----|-----------|-------|-----|
| GC-2 | `GlowComponents.kt:42` | `cornerRadius = CornerRadius(24f)` — hard-coded. Inconsistent with `shapeRadius` parameter. | `CornerRadius(shapeRadius.toPx())`. |

## 15. InsightBanner.kt — ✅ no new issues
## 16. SpectrumChart.kt — ✅ no new issues (outside the FocusArea for this pass)

---

## Cross-cutting observations

- **State hoisting** is mostly good; `XpGainToast` and `FileCard` mix owned state with key-derived state in ways that break under recomposition (FC-3, XP-1).
- **`rememberSaveable` is missing** in `FileCard.kt` and `HeatmapChart.kt` — list recycling + process death lose ephemeral UI state.
- **No `derivedStateOf`** in `HeatmapChart.kt:60-61, 73` — `weeks`, `maxCount`, `monthLabels` recompute when `stableData` reference changes (any list copy).
- **Direct repository reads in DashboardScreen** (`repository.files`, `repository.gameEngine.totalXp`, `repository.heatmapData`) bypass Flow collection — repeats the spec §1.2 anti-pattern (DS-2).
- **Test gaps**: none of these components have a `@Preview` with multiple states (loading, empty, error).

---

## Severity counts

- 🔴 Critical: **3** (all in `TimeTravelSlider`)
- 🟠 Important: **9**
- 🟡 Minor: **12**

## Top priority finding
**`TimeTravelSlider.kt` crashes for first-time users** (TTS-1, TTS-2): `Slider(steps = -1)` throws on the first install with <2 weeks of data, and the percentage display divides by zero — both visible immediately on onboarding. Must fix before any Phase 0 release.
