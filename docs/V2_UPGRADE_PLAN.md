# Cross K / FileIntelligence v2.0 升级方案

> 文档定位：v2.0 升级的总体设计文档（WHY + WHAT）。
> 后续实施按 phase 拆解，phase 间保持可独立交付。
> 撰写日期：2026-08-03

---

## 0. 现状摘要（来自勘察报告）

### 0.1 知识图谱数据层（10 大瓶颈）

| # | 瓶颈 | 证据 |
|---|------|------|
| 1 | 伪 AI：纯词典 + 规则，无任何语义模型/Embedding | `AnalysisEngine.kt` 全文 600+ 词静态词典 |
| 2 | 关系不入库：无 `edges` 表，每次启动全量 O(F·E²) 重建 | `GraphReconstructor.kt:55-85` |
| 3 | 注释与实现不符：自称"句子级"实为文件级笛卡尔连边 | `GraphReconstructor.kt:56-58` |
| 4 | `relationPatterns`、`extractRelations` 全项目死代码 | `AnalysisEngine.kt:108-116, 205-229` 全局无调用 |
| 5 | N+1 查询：主循环逐文件查实体，无 JOIN | `FileRepository.kt:147-154` |
| 6 | 假增量保存：无事务，崩溃即数据不一致 | `FileRepository.kt:127-134` |
| 7 | 解析器名不副实：pdf/docx 当 UTF-8 纯文本读 | `FileParserEngine.kt:45-47` |
| 8 | 关系类型两套枚举（6 vs 4），在 UI 全部硬编码 `REFERENCES` | `GraphScreen.kt:77` |
| 9 | 三轨持久化：Room + JSON 全量快照 + SharedPreferences | `PersistenceManager.kt`（无实例化方） |
| 10 | Room `fallbackToDestructiveMigration()` + 无索引/无 FK + 实体 ID 用 `hashCode` | `AppDatabase.kt:33` |

### 0.2 前端 UI 层（10 大瓶颈）

| # | 瓶颈 | 证据 |
|---|------|------|
| 1 | **GraphCanvas 状态断流**：引擎写 StateFlow 全项目无 collect，Canvas 读普通 var 不重绘 | `ForceGraphEngine.kt:307` vs `GraphCanvas.kt:62-63` |
| 2 | 斥力 O(n²) + 边查找 O(E·N)，无空间分区 | `ForceGraphEngine.kt:362-379` |
| 3 | 双指缩放缺失：`scale` 状态存在却无 transform 手势 | `GraphCanvas.kt:66,235,269` |
| 4 | 组合期重计算：`getStats` / `computeGrowthMetrics` / `filteredFiles` 未 remember | Dashboard/Library/Spectrum 多处 |
| 5 | `GlassCard` 每帧双重循环建噪点 Path | `GlassCard.kt:62-82` |
| 6 | `MainViewModel` 空心化：仅 `save()` 透传 | `MainViewModel.kt:20-24` |
| 7 | 路由冗余：insights/spectrum/growth 三路由同屏 | `NavGraph.kt:140-169` |
| 8 | 主题状态用全局可变单例 `object ThemeState`，不读系统设置 | `Theme.kt:71-73` |
| 9 | 缺设计 token：spacing/shape/elevation 全部硬编码 | 全局散落 |
| 10 | 导入错误静默吞掉，假进度条 | `LibraryScreen.kt:690-725` |

### 0.3 调研结论（2026-08-03，详见调研笔记）

- 端侧实体抽取：ML Kit Entity Extraction（+5.6MB，11 类）适合规则词典补充；Embedding 首选 ONNX Runtime Mobile + multilingual-e5-small（int8 ~118MB）
- 端侧向量检索：sqlite-vec（与 Room 栈共存，10 万 256 维 ~72ms）或 ObjectBox 4.x（原生 HNSW，~3MB）
- 图谱存储：保留 Room + 边表是务实选择；Kùzu/RyuGraph 维护风险高，暂不引入
- 图谱渲染：Barnes-Hut + 视口剔除 + `withFrameNanos` 是 Compose 端最佳路径
- 同类范式：Obsidian 验证"本地图 + 1–2 跳邻域聚焦 + 过滤"是日常高频场景，全局图只做健康检查

---

## 1. 设计目标（v2.0 北极星指标）

| 维度 | v1.0 现状 | v2.0 目标 |
|------|----------|----------|
| 实体识别 | 词典 + 规则 | 词典 + ML Kit + 可选 Embedding（语义召回） |
| 关系存储 | 内存重建 | Room 边表 + 索引 + 物化二跳邻域 |
| 关系类型 | UI 硬编码 REFERENCES | 6 种类型贯穿 data/ui/ai 全链 |
| 图谱规模 | 节点>200 掉帧（O(n²)） | 节点>2000 流畅（quadtree + 视口剔除） |
| 图谱交互 | 仅拖拽，缩放失效 | 拖拽 + 双指缩放 + 双击复位 + 邻域聚焦 |
| 引擎状态 | 写 StateFlow 无人 collect | Snapshot State → Canvas 真正驱动重绘 |
| 状态管理 | Repository mutableStateListOf 直传 | ViewModel + StateFlow + UI State 分层 |
| 设计系统 | 硬编码 dp | Dimens/Shape/ColorScheme/Type 全 token 化 |
| 导入管线 | 假进度 + 静默错误 | 真实阶段回调 + Snackbar 错误态 |
| 数据迁移 | 升版即清空 | 按 v1→v2 写正式 Migration |

---

## 2. 总体架构

### 2.1 分层

```
┌─────────────────────────────────────────────────────────┐
│ UI Layer (Compose)                                      │
│   Screen → ViewModel(ui state) → Component(Stateless)  │
│   强类型 UI State，remember/derivedStateOf 严格管控     │
├─────────────────────────────────────────────────────────┤
│ Domain Layer                                           │
│   GraphEngine, AnalysisEngine, LayoutEngine, Parser     │
│   纯 Kotlin，无 Compose/Room 依赖（可单测）            │
├─────────────────────────────────────────────────────────┤
│ Data Layer                                              │
│   Room (entities, edges, files, graph_layout)           │
│   + 内存邻接表（按需 build）                             │
│   文件：SAF 真实解析（PDF/DOCX/EPUB/TXT/MD）             │
└─────────────────────────────────────────────────────────┘
```

### 2.2 模块依赖

```
ai/AnalysisEngine ──┐
                    ├─→ domain/Graph ──→ data/repo ──→ Room
data/Parser    ─────┘            ↑
                                ui/ViewModel ──→ Compose Screen
```

### 2.3 关键设计决策

| 决策 | 选型 | 理由 |
|------|------|------|
| 图存储 | Room 节点/边表 + 邻接表缓存 | 10 万节点 BFS 够用，零迁移成本 |
| 实体识别 | 字典 + ML Kit Entity Extraction 兜底 | 离线、低体积、2026 成熟方案 |
| Embedding | v2.0 不引入；v2.1 评估 multilingual-e5-small ONNX | 体积代价大，先把基础管线打通 |
| 力导向 | 自研：quadtree + 冷却调度 + withFrameNanos | 节点<3000 性能足够；超量走聚合 |
| 图谱引擎状态 | 节点位置用 `SnapshotStateList<Offset>` | 位置变动直接触发重绘，无需 Flow collect |
| 主题 | Material3 dynamic + 自定义语义色 + Compose Comp Local | 替代 `ThemeState` 全局单例 |
| 错误处理 | Repository 暴露 `Result<T>` + UI 映射到 Snackbar | 替代 `printStackTrace` 静默 |
| 解析 | iText5 / PdfBox-Android / Apache POI（仅在用户首次导入对应类型时按需加载） | 解决"名不副实" |

---

## 3. 知识图谱子系统设计

### 3.1 数据模型 v2

```kotlin
// 节点表（沿用 v1 但加索引）
@Entity(
    tableName = "entities",
    indices = [Index("fileId"), Index(value=["name","type"], unique=true),
               Index("type"), Index("mentions")]
)
data class EntityEntity(
    @PrimaryKey val id: String,        // 稳定 ID：name+type 哈希或 ULID
    val fileId: Long,
    val name: String,
    val type: EntityType,              // 枚举：PERSON/PLACE/CONCEPT/METHOD/TOOL/EVENT
    val mentions: Int,
    val firstSeen: Long,
    val lastSeen: Long,
    val importance: Float              // 0..1，综合评分
)

// 边表（新增）
@Entity(
    tableName = "edges",
    primaryKeys = ["srcId","dstId","type"],
    foreignKeys = [
        ForeignKey(EntityEntity::class, ["srcId"], ["id"], onDelete=CASCADE),
        ForeignKey(EntityEntity::class, ["dstId"], ["id"], onDelete=CASCADE)
    ],
    indices = [Index("srcId"), Index("dstId"), Index("type"), Index("weight")]
)
data class EdgeEntity(
    val srcId: String,
    val dstId: String,
    val type: RelationType,            // CO_OCCURS/REFERENCES/DERIVES_FROM/BELONGS_TO/CONTRASTS_WITH/SIMILAR_TO
    val weight: Float,
    val evidence: String?,             // JSON：[{fileId, sentence, offset}]
    val createdAt: Long
)

// 文件表（新增 FK）
@Entity(
    tableName = "files",
    foreignKeys = [], indices = [Index("addedAt")]
)
data class FileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uri: String, val title: String, val mime: String,
    val sizeBytes: Long, val addedAt: Long, val lastOpenedAt: Long?
)

// 布局表（扩字段）
@Entity(tableName = "graph_layout", indices=[Index("updatedAt")])
data class GraphLayoutEntity(
    @PrimaryKey val nodeId: String,
    val x: Float, val y: Float,
    val pinX: Float? = null, val pinY: Float? = null,  // 用户拖拽固定的钉位
    val updatedAt: Long
)
```

### 3.2 关系抽取算法（v2）

> 替代 v1 文件级全组合连边

**层级抽取**（自底向上）：

1. **句级共现窗口**：同一句子内出现的实体两两连边，权重 = `1.0 / sentence_count`
2. **段落级共现衰减**：同段不同句的实体连边，权重 = `0.5 / paragraph_count`
3. **文档级共现衰减**：同文档不同段，权重 = `0.2 / document_count`
4. **关系动词模式**（复活 v1 死代码并扩写）：
   - `X 是/为 Y` → `BELONGS_TO`
   - `X 引用/提到 Y` → `REFERENCES`
   - `X 由/来自 Y` → `DERIVES_FROM`
   - `X 对比/与 Y 不同` → `CONTRASTS_WITH`
   - 模糊匹配 → `SIMILAR_TO`

每次写入事务：清除文件相关旧边 → 重写新边 → 触发 layout 增量更新。

### 3.3 布局引擎（v2）

- **算法**：Barnes-Hut（quadtree）将斥力从 O(n²) 降到 O(n log n)
- **调度**：`withFrameNanos` 驱动每帧 `step()`，先在 `Dispatchers.Default` 计算，提交阶段用 `Snapshot.withMutableSnapshot`
- **持久化**：每 N 帧或 `_dirty=true` 时批量 upsert `graph_layout`
- **聚类**：可选 Louvain 社区检测（用于着色）—— v2.0 留接口，v2.1 实现

### 3.4 存储 Schema 升级

- v2 → v3：新增 `edges` 表 + 加索引 + `entities.fileId` 加 FK（CASCADE）
- Migration 写明：基于 `entities` JOIN `files` 重建 `edges`（用 v1 文件级共现权重初始化）
- 不再使用 `fallbackToDestructiveMigration()`

---

## 4. 前端子系统设计

### 4.1 设计系统 Token

```kotlin
// ui/theme/Dimens.kt
object Dimens {
    val SpaceXs = 4.dp; val SpaceSm = 8.dp; val SpaceMd = 12.dp
    val SpaceLg = 16.dp; val SpaceXl = 24.dp; val SpaceXxl = 32.dp
    val RadiusSm = 8.dp; val RadiusMd = 12.dp; val RadiusLg = 20.dp; val RadiusPill = 999.dp
    val ElevationFlat = 0.dp; val ElevationCard = 2.dp; val ElevationModal = 8.dp
}

// ui/theme/Shape.kt
val CrossKShapes = Shapes(
    small = RoundedCornerShape(Dimens.RadiusSm),
    medium = RoundedCornerShape(Dimens.RadiusMd),
    large = RoundedCornerShape(Dimens.RadiusLg)
)

// ui/theme/Color.kt（保持现有色板语义，新增 neutral 5 级 / surface 4 级）

// ui/theme/Type.kt（保留 5 档，但加 Display/Headline 区分）
```

### 4.2 状态管理重构

```
Repository (data source)
        ↓ 暴露 Flow/Lazy
ViewModel (持有 StateFlow<UiState>)
        ↓
Screen (collectAsStateWithLifecycle)
        ↓
Stateless Component (Composable 参数)
```

- 移除 `object ThemeState`，改读 `isSystemInDarkTheme()` + DataStore 持久化
- `MainViewModel` 拆为 `DashboardViewModel` / `LibraryViewModel` / `GraphViewModel` / `SpectrumViewModel`
- `Repository` 不再持有 UI 状态

### 4.3 图谱交互（GraphCanvas v2）

```kotlin
// 手势层
detectTransformGestures { centroid, pan, zoom, _ ->
    viewport.scale = (viewport.scale * zoom).coerceIn(0.25f, 4f)
    viewport.translation += pan
}

// 关键修复：节点位置用 SnapshotStateList<Offset>
val nodePositions: SnapshotStateList<Offset> = ...
// 引擎 step() 后 updatePositions { ... } → Snapshot 触发 Canvas 重绘
```

新增交互：
- 双指缩放（带中心点）
- 双击节点聚焦（1 跳邻域高亮，其余 60% 透明）
- 双击空白复位
- 长按节点弹 sheet（pin / focus / dismiss）
- 工具栏：聚焦模式 / 邻域半径 / 节点着色（按类型 / 按社区）/ 重力 / 缩放

### 4.4 路由收敛

- 合并 `insights` / `spectrum` / `growth` → 单一 `spectrum_graph` 路由 + 内部 tab 状态
- 底部导航维持 4 tab，但"图谱"提升为 top-level（5 个 tab），加图标

### 4.5 性能清单（落地）

- 所有 `*Screen` 重计算 → `remember(key)` / `derivedStateOf`
- `GraphCanvas` 走 `drawWithCache` 缓存噪点 Path
- `GlassCard` 噪点改为 Shader 一次性生成
- `LibraryScreen` 过滤 → `derivedStateOf` 包裹
- `SpectrumGrowthScreen` 图谱节点/边构建 → `remember` + key

---

## 5. 错误与可观测性

- `Repository` 操作统一返回 `sealed interface RepoResult<out T> { Ok; Err(message, cause) }`
- UI 层映射到 `SnackbarHostState.showSnackbar(...)`
- `Logger` 单例（写到本地文件 + Logcat），替代 `printStackTrace`
- 关键路径埋点：导入耗时、实体数、边数、图谱帧率

---

## 6. 风险与对策

| 风险 | 概率 | 影响 | 对策 |
|------|------|------|------|
| 大数据迁移失败 | 中 | 高 | Migration 前自动备份；失败可回滚 |
| 真实 PDF 解析体积过大 | 高 | 中 | 按需懒加载（首次使用才下载） |
| 自研 quadtree 出 bug | 中 | 中 | 先写单元测试再集成 |
| ML Kit 11 类不全 | 高 | 低 | 与词典合并，词典优先 |
| 用户已有数据格式不兼容 | 低 | 高 | 提供"导出 v1 JSON"工具 |

---

## 7. 实施路线（分 Phase）

### Phase A（本次会话）— 核心修复
1. Room schema v2→v3：新增 `edges` 表 + 索引 + FK + 正式 Migration
2. 边表 DAO + 物化邻域 + 事务化导入
3. 关系抽取 v2：句级/段级/文档级三级权重 + 复活动词模式
4. 修复 GraphCanvas 状态断流：节点位置改 `SnapshotStateList`
5. 修复双指缩放：补 `detectTransformGestures`
6. 引入 `Dimens` / `Shape` token；批量替换硬编码 dp
7. 撤掉 `ThemeState` 单例，改 system + DataStore
8. 合并三条冗余路由 → single `spectrum_graph`
9. 解决 5 处组合期重计算（remember/derivedStateOf）
10. Repository 错误态 → `RepoResult` + Snackbar
11. 撤掉假进度条，改真实阶段回调

### Phase B（下一会话）— 智能化
- ML Kit Entity Extraction 集成
- 真实 PDF/DOCX 解析（Apache POI / iText5）
- 视图层算法优化（quadtree）
- 邻域聚焦 / 聚类着色 / 时间轴过滤

### Phase C（远期）— 高级
- Embedding 召回 + 相似关系
- 社区检测（Louvain）
- 离线模型嵌入（多语 e5-small）
- 端云协同（可选云端同步 + 端侧加密）

---

## 8. 验收标准

- [ ] `./gradlew assembleDebug` 0 错误 0 警告
- [ ] 简单测试：导入 100 个文件，实体数与边数与历史对照不出现退化
- [ ] 图谱 200 节点模拟 30 秒不掉帧（FPS ≥ 55）
- [ ] 双指缩放、双击复位、邻域聚焦在真机正常
- [ ] 切换深浅主题立即生效，重启后保留
- [ ] 导入失败显示 Snackbar，不再静默
- [ ] v1 → v2 迁移不丢用户数据

