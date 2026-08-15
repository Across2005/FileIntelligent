# FileIntelligence v3 — 智能外脑 (Smart Exocortex) 设计文档

> **文档定位**:v3.0 的总体设计 (WHY + WHAT + HOW)
> **撰写日期**:2026-08-15
> **状态**:Draft v1 — 待用户审阅
> **承前**:`docs/V2_UPGRADE_PLAN.md`(2026-08-03)定义的内核修复路径
> **后续**:审阅通过后进入 `writing-plans` 拆解为可独立交付的 phase 实施计划

---

## 0. 摘要

v3 不是 v2 的延续,而是**重新定位**:FileIntelligence 从"AI 辅助的文档分析器"升级为 **"本地优先的智能外脑"** —— 用户每天用 2 秒捕捉任何想法,App 在端侧把它放进一张持续生长的知识图谱,而不是云端。

**v3 北极星指标**:用户在 30 天内**回访至少 5 次**同一个实体或关系(说明图谱已经"被用起来")。

**v3 三大杠杆**:
1. **Ambient Capture**(2 秒入口) — Share Sheet、Quick Settings Tile、语音、OCR,捕获摩擦 < 2s
2. **端侧真 AI**(ML Kit + 关系抽取 v2) — 不再是词典幻觉
3. **零焦虑品牌叙事**(把"本地+零网络+零锁定"从默认行为升格为价值主张)

**v3 不做**(明确取舍):
- 块级粒度重构(留给 v4)
- 端云协同 / 跨设备同步(v3 内"导出 v1 JSON"作为逃生通道)
- Embedding 召回(v3 用 TF-IDF + 关键词权重的简单语义;e5-small 推到 v3.1)
- 社区检测 / 高级可视化(v3.1+)

---

## 1. 背景:为什么是 v3

### 1.1 项目理解(FileIntelligence v1/v2 现状)

FileIntelligence / 文件智析 是一个已上线的 Android 私人知识图谱应用,工程度比一般 side project 深得多。

**三层"既得"**:
- **数据层完整**:Room 实体/边/文件/图谱布局四表,`QuickCaptureActivity`、Widget、SAF 文档导入、Postcard 导出、BackupManager
- **能力层齐备**:Force-directed 图谱、词典式实体抽取、共现关系构建、情感/重要性评分、游戏化(XP/Level/Daily Quest)、明信片(成长纪念)
- **设计系统成型**:`tokens.css`、`design-system.md`、HTML 设计稿

**三层"已识别的病"**(V2 计划已盘清):
- 数据层 10 病(伪 AI、关系不入库、N+1、无 FK、ID 用 hashCode、文件级笛卡尔连边、`fallbackToDestructiveMigration`)
- 前端 10 病(GraphCanvas 状态断流、O(n²) 斥力、双指缩放缺失、硬编码 dp、`object ThemeState` 单例、假进度条)
- 架构(MainViewModel 空心、路由冗余、Repository 直传 mutableStateListOf)

**三个"差异化资产"**:
1. **全本地 + 零网络权限**:2026 隐私焦虑里的金子
2. **游戏化**:PKG 行业几乎没有竞品这么做
3. **明信片导出**:把知识生长可视化、可分享

### 1.2 Code Review 关键发现(2026-08-15)

对 V2 计划 + 5 个关键文件的 code review 暴露了**计划未覆盖的 4 个必修缺陷**,必须在 v3 Phase 0 修复:

| ID | 缺陷 | 文件:行 | 严重度 | 对 v3 的影响 |
|----|------|---------|--------|-------------|
| **CR-1** | 实体 ID 用 hashCode,碰撞导致实体静默合并 | `AnalysisEngine.kt:138-139, 189-190` | 🔴 Critical | 端侧 Embedding 后 ID 方案要重新设计,不能叠加在 hashCode 上 |
| **CR-2** | `saveAll` 自称事务化但**没有 Room @Transaction** | `FileRepository.kt:124-152` | 🔴 Critical | 数据完整性 |
| **CR-3** | `FileParserEngine` 硬编码 UTF-8,中文 Windows 文件全乱码 | `FileParserEngine.kt:45-47` | 🔴 Critical | 中文用户主流程 |
| **CR-4** | `analyzeWithProgress` 抛异常后 `analysisStage` 卡住 | `FileRepository.kt:159-219` | 🔴 Critical | 导入 UX |
| **IM-1** | 关系类型分类逻辑**完全错**:判断"句子是否含'导致'"而非"X 是否是导致 Y 的主语" | `AnalysisEngine.kt:233-236` | 🟠 Important | v3 的"6 种关系"卖点是空话,必须先修 |
| **IM-4** | 文档级兜底连边把 O(n²) 摆烂式拉满 | `AnalysisEngine.kt:262-272` | 🟠 Important | 端侧 ML Kit 接入后要让位 |

其余 IM-2/3/5/6 与 MN-1~5 在 v3 Phase 1 内顺手处理。

### 1.3 前沿调研摘要(2026 PKG 范式)

**玩家地图**:Obsidian(本地+插件)、Logseq(块+日记)、Tana(Super Tags+节点即库)、Capacities(对象化笔记)、Notion、Dendron(10k 不卡)、RemNote(间隔重复)

**2026 关键趋势**:
1. 端侧 AI 成标配(ML Kit Entity Extraction、multilingual-e5-small ONNX、sqlite-vec / ObjectBox 向量检索)
2. Bases / 类数据库视图(Obsidian 1.9+ 原生)
3. Ambient Capture(Share Sheet、Widget、Tile、Quick Settings Tile、Voice、Photo OCR —— 捕获摩擦 <2s)
4. 语义搜索 > 关键词搜索
5. Block 级别粒度
6. 10k 笔记墙(检索是瓶颈)
7. 个性化反馈环
8. 跨设备同步成为基本盘

**决定"优秀"的 5 维**:捕获(摩擦<2s) / 结构(网络) / 检索(多维) / 智能(端侧) / 情感(成长感)

---

## 2. v3 北极星指标

| 维度 | v2 现状 | v3 目标 |
|------|---------|---------|
| **回访率** | 无衡量 | 30 天内至少 5 次访问同一实体/关系 |
| **捕获摩擦** | 打开 App → 输 → 存(10s+) | 任意入口<2s,Quick Capture < 5s 全流程 |
| **AI 实体识别准确率** | 词典命中,泛化差 | ML Kit + 词典混合,11 类 + 中文专词 |
| **关系类型可信度** | 6 类但分类逻辑错 | 6 类经 IM-1 修复后真分类,UI 着色可信 |
| **图谱规模** | 节点>200 掉帧(O(n²)) | 节点>2000 流畅(quadtree + 视口剔除) |
| **APK 体积增量** | — | +5-10MB(ML Kit + 词库) |
| **崩溃率** | 阶段卡死、ID 漂移 | 0 数据丢失、0 阶段卡死 |

---

## 3. v3 设计原则

1. **端优先于云**:任何能端侧做的,绝不上云;云只在用户主动选择时介入(导出/同步,不在 v3 范围)
2. **增量优于重做**:能叠加在 v1 数据模型上就不动 schema;只有收益明确时才动 schema
3. **摩擦 < 价值**:任何让用户多等 1 秒、新增 1 次点击的设计都要三思;**捕获摩擦永远优先于分析深度**
4. **可信优于丰富**:6 种关系类型宁可少,也不能让用户看到"假"的关系;IM-1 必修
5. **零焦虑优于功能堆叠**:Onboarding、About、Settings 三处必须显式表达"你的数据只属于你"
6. **可逆优于决断**:每个 phase 都有"上一版可回退"的开关(feature flag),不允许 P0 缺陷无处可退

---

## 4. 架构总览

### 4.1 分层

```
┌──────────────────────────────────────────────────────────┐
│ UI Layer (Compose)                                        │
│   Screen → ViewModel (StateFlow<UiState>) → Stateless     │
│   - 强类型 UI State                                        │
│   - 严格 remember / derivedStateOf                         │
│   - Ambient Capture Entry: ShareSheet, Tile, Widget, Voice│
├──────────────────────────────────────────────────────────┤
│ Domain Layer (pure Kotlin)                                │
│   - GraphEngine (force-directed)                          │
│   - AnalysisEngine (rule + ML Kit)                         │
│   - RelationExtractor v2 (句/段/文档级 + 模式 + ML Kit)   │
│   - ParserEngine (UTF-8/GBK 嗅探 + 真 PDF/DOCX)           │
│   - SemanticSearchEngine (TF-IDF v3.0 → e5 v3.1)          │
│   - GameEngine (XP/Level/Quest) v2 — 与生长绑定           │
├──────────────────────────────────────────────────────────┤
│ Data Layer                                                │
│   - Room (files, entities, edges, graph_layout, knowledge)│
│   - Vector Index (v3.1: sqlite-vec / ObjectBox)            │
│   - DataStore (theme, prefs)                               │
│   - 三轨持久化收敛:Room 为唯一真源,DataStore 仅偏好       │
└──────────────────────────────────────────────────────────┘
```

### 4.2 模块依赖

```
ai/AnalysisEngine ─┐
                   ├─→ domain/RelationExtractor ─→ data/repo ─→ Room
data/Parser ───────┘
                                  ↑
ai/MLKitBridge (new) ────────────┘
                                  ↑
ui/AmbientCapture (new) ──→ domain ──→ ViewModel ──→ Screen
                                  ↑
domain/SemanticSearch ────────┘
                                  ↑
domain/GameEngine v2 ──────────┘
```

### 4.3 关键设计决策

| 决策 | 选型 | 理由 |
|------|------|------|
| AI 实体识别 | ML Kit Entity Extraction(11 类) + 词典(中文专词) | 离线、低体积、2026 成熟 |
| 关系分类 v2 | 句级模式 + 段级衰减 + 文档级兜底(仅在低置信时) | IM-1 修复后真分类 |
| 端侧向量检索 | **v3.0 不上**,v3.1 评估 sqlite-vec(10 万 256 维 ~72ms) | 体积代价大,先把基础打通 |
| 语义搜索 v3.0 | TF-IDF + 关键词权重 + 实体 + 关系 4 维联合打分 | 0 模型,0 体积,已够"语义" |
| 力导向 | quadtree(自研) + 视口剔除 + `withFrameNanos` | 节点<3000 性能足够 |
| 图谱状态 | `SnapshotStateList<Offset>` 单元素 wrapper | 位置变动自动触发重绘 |
| 主题 | Material3 dynamic + 自定义语义色 + Composition Local | 撤 `object ThemeState` 单例 |
| 错误处理 | `RepoResult<T>` + UI 映射到 Snackbar | 撤 `printStackTrace` 静默 |
| Ambient Capture | Android Share Sheet + Tile Service + Widget + Voice Intent | 平台标准,不引入第三方依赖 |
| 真实 PDF/DOCX | iText5 / PdfBox-Android / Apache POI(按需懒加载) | 解决"名不副实" |
| 文件编码 | BOM 嗅探 → UTF-8 → GB18030 fallback | 解决中文 Windows 乱码 |
| 实体 ID | `UUID.nameUUIDFromBytes("$type|$name".toByteArray())` | 解决 hashCode 碰撞 |

---

## 5. 数据模型 v3

### 5.1 实体(Entity)

```kotlin
@Entity(
    tableName = "entities",
    indices = [
        Index("fileId"),
        Index(value = ["name", "type"], unique = true),
        Index("type"),
        Index("mentions"),
        Index("lastSeen"),
    ],
)
data class EntityEntity(
    @PrimaryKey val id: String,                  // 稳定 ID:UUID over "$type|$name"
    val fileId: Long,                            // v3:改为 Long,Room 自增 files.id 关联
    val name: String,
    val type: EntityType,                        // PERSON/PLACE/CONCEPT/METHOD/TOOL/EVENT
    val mentions: Int,
    val firstSeen: Long,
    val lastSeen: Long,
    val importance: Float,                       // 0..1
    val isUserConfirmed: Boolean = false,        // v3 新增:用户是否确认过类型
    val isUserIgnored: Boolean = false,          // v3 新增:用户标记"忽略"
    val confirmationCount: Int = 0,             // v3 新增:确认次数,辅助 ML Kit
)
```

### 5.2 边(Edge)

```kotlin
@Entity(
    tableName = "edges",
    primaryKeys = ["srcId", "dstId", "type"],
    foreignKeys = [
        ForeignKey(EntityEntity::class, ["srcId"], ["id"], onDelete = CASCADE),
        ForeignKey(EntityEntity::class, ["dstId"], ["id"], onDelete = CASCADE),
    ],
    indices = [Index("srcId"), Index("dstId"), Index("type"), Index("weight")],
)
data class EdgeEntity(
    val srcId: String,
    val dstId: String,
    val type: RelationType,                      // CO_OCCURS/REFERENCES/DERIVES_FROM/BELONGS_TO/CONTRASTS_WITH/SIMILAR_TO
    val weight: Float,
    val evidence: String?,                       // JSON:[{fileId, sentence, offset}]
    val confidence: Float,                       // v3 新增:抽取置信度 0..1
    val source: String,                          // v3 新增:"rule" | "mlkit" | "user" 标识来源
    val createdAt: Long,
)
```

### 5.3 文件(File)

```kotlin
@Entity(
    tableName = "files",
    indices = [Index("addedAt")],
)
data class FileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,    // v3 改为 Long autoGen
    val uri: String,
    val title: String,
    val mime: String,
    val sizeBytes: Long,
    val addedAt: Long,
    val lastOpenedAt: Long?,
    val encoding: String = "UTF-8",                       // v3 新增
    val source: String = "import",                        // v3 新增:import/quickcapture/voice/photo
    val analysisVersion: Int = 3,                         // v3 新增:哪个版本分析器处理过
)
```

### 5.4 知识元数据

```kotlin
@Entity(tableName = "knowledge")
data class KnowledgeEntity(
    @PrimaryKey val id: Int = 1,                          // 单行
    val totalXp: Int,
    val graphVisualLevel: Int,
    val streakCurrent: Int,
    val streakLongest: Int,
    val streakLastActive: Long,
    val onboardingCompleted: Boolean = false,             // v3 新增
    val firstCaptureAt: Long? = null,                     // v3 新增
    val lastCaptureAt: Long? = null,                      // v3 新增
    val captureStreak: Int = 0,                           // v3 新增
)
```

### 5.5 图谱布局

```kotlin
@Entity(tableName = "graph_layout", indices = [Index("updatedAt")])
data class GraphLayoutEntity(
    @PrimaryKey val nodeId: String,
    val x: Float,
    val y: Float,
    val pinX: Float? = null,
    val pinY: Float? = null,
    val updatedAt: Long,
    val cluster: String? = null,                          // v3.1 准备:社区检测
)
```

### 5.6 实体确认历史(用户反馈环)

```kotlin
@Entity(
    tableName = "entity_confirmations",
    indices = [Index("entityId"), Index("confirmedAt")],
)
data class EntityConfirmation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entityId: String,
    val originalType: EntityType,
    val confirmedType: EntityType?,                       // null = 用户标记为错误
    val isIgnored: Boolean,
    val confirmedAt: Long,
)
```

### 5.7 Schema 迁移路径(Room schema version 3 → 4)

> 命名约定:本设计文档中 "v3 / v3.0" 指**产品版本**(从 v2 升到 v3.0);Room schema 当前已是 version=3(见 `AppDatabase.kt:17`),本设计需要升到 version=4。两者**不是同一个东西**,请勿混淆。
  - `entities.id` 由 hashCode 迁到 UUID(写 SQL UPDATE)
  - `entities` 加 `isUserConfirmed` / `isUserIgnored` / `confirmationCount` 三列
  - `edges` 加 `confidence` / `source` 两列
  - `files.id` 由 String 改 Long(autoGenerate)
  - `files` 加 `encoding` / `source` / `analysisVersion` 三列
  - 新增 `entity_confirmations` 表
  - `knowledge` 加 `onboardingCompleted` / `firstCaptureAt` / `lastCaptureAt` / `captureStreak` 四列
  - 提供 v3→v4 Migration,失败可回滚(自动 backup 到 JSON)

- **不再使用** `fallbackToDestructiveMigration()`,改用 `fallbackToDestructiveMigrationOnDowngrade()` + 显式 Migration

---

## 6. 端侧 AI 子系统

### 6.1 实体抽取 v3 管线

```
输入:文本
   ↓
[1] 词典快速命中(中文专词、用户自定义词)
   ↓
[2] ML Kit Entity Extraction(11 类,中文需先调用 ModelDownloader)
   ↓
[3] n-gram 候选 + 位置加权(回退)
   ↓
[4] 用户反馈叠加:已确认的实体名 + 类型直接采用
   ↓
输出:Map<UUID, ScoredEntity>
```

**ML Kit 集成要点**:
- 使用 `EntityExtractor` + `ModelIdentifier.CHINESE`(2026 已稳定)
- 模型首次使用懒下载(用户首次开启"增强识别"时)
- 词典优先,ML Kit 兜底——避免模型幻觉覆盖已知专词

### 6.2 关系抽取 v2(真分类)

**修复 IM-1 后的算法**:
```
对每个句子:
  1. 找出该句所有实体(用 entityByName)
  2. 对每个实体对 (a, b):
     a. 优先用 ML Kit 关系分类(若模型支持,2026 仅英文稳定)
     b. 退到规则:扫描 7 个 regex 模式,严格匹配 capture group:
        - X 出现在 group(1) AND Y 出现在 group(2) → 类型 X→Y
        - 反之亦然
     c. 否则 CO_OCCURS,权重 = 1.0 / 该句实体数
  3. 句级权重 1.0,段级衰减 0.5,文档级**仅在低置信时**启用,权重 0.2

每条边的 confidence = max(规则置信度, ML Kit 概率)
每条边的 source ∈ {rule, mlkit, user}
```

**质量门控**:
- 规则匹配必须 capture 双方实体名 → 否则降级 CO_OCCURS
- ML Kit 概率 < 0.6 → 不写入边表
- 用户可在 UI 看到 `source` 标签,质疑某条边时一键删除

### 6.3 端侧 Embedding(v3.1 预备)

v3.0 不集成。v3.1 评估:
- multilingual-e5-small(ONNX, int8 ~118MB)vs bge-small-zh(~46MB)
- 向量索引:sqlite-vec(与 Room 共存)优先
- 仅用于"语义搜索召回"和"相似实体推荐",不替代关系抽取

### 6.4 用户反馈环

- 实体列表 UI:每个实体可点"类型错"→ 弹类型选择
- 边的详情:可点"这条关系不成立"→ 软删除(`source = "user_dismissed"`)
- 用户的确认/否定写入 `entity_confirmations`,作为词典与 ML Kit 的优先级提升信号
- 累计 5 次确认某实体类型 → `isUserConfirmed = true`,后续该实体名出现直接用该类型

---

## 7. Ambient Capture 子系统

### 7.1 四个入口(摩擦 < 2s)

| 入口 | 实现 | 摩擦 | 目标用户 |
|------|------|------|----------|
| **Share Sheet** | `IntentFilter ACTION_SEND` + `text/*` + `image/*` | < 1s | 看到好内容随时存 |
| **Quick Settings Tile** | `TileService` 提供"快速记录"瓦片 | < 1s | Android 通知栏一键 |
| **Home Widget** | 已实现,加 1x1 微型"快记"入口 | < 2s | 桌面直达 |
| **Voice Capture** | 现有 `QuickCaptureActivity` + 录音 + 本地 ASR | < 5s | 走路/通勤 |
| **Photo OCR** | ML Kit Text Recognition v2 + 自动入图谱 | < 8s | 拍书/截图 |

### 7.2 Quick Capture 全流程 < 5s

1. 用户从任意入口触发
2. 弹出"快记"覆盖层(不是 Activity 全屏) —— 关键:不离开当前 App
3. 文本已就位(Share Sheet 自动带过来;语音转写完成)
4. 用户点"存"(可省去一切输入)
5. App 异步分析(后台 WorkManager),主线程立刻"已收下"反馈
6. 实体的入图谱以"种子"状态出现,后续随分析成长

### 7.3 设计要点

- **不阻塞当前 App**:Quick Capture 是 `Activity`(透明主题)而非 fullscreen
- **可离线**:所有路径在无网下工作(本地 ASR 用 Vosk 或 ML Kit on-device)
- **可恢复**:输入中断(突然来电话)后,草稿自动保存到 DataStore,下次回到 QuickCapture 仍在
- **可批量**:支持一次性粘贴/拖入多段文本,自动按段落切分为多条 FileItem

---

## 8. 语义检索子系统 v3.0

### 8.1 检索维度

| 维度 | 实现 | 排序权重 |
|------|------|----------|
| 全文(FTS4) | Room FTS 虚拟表,中文按 n-gram 切分 | 0.35 |
| 实体匹配 | `entities.name LIKE ?` | 0.30 |
| 关系匹配 | 通过 `edges` JOIN `entities` | 0.15 |
| 时间近因 | `lastSeen` / `addedAt` 加权 | 0.10 |
| 重要性 | `importance` × 用户回访次数 | 0.10 |

### 8.2 检索 API

```kotlin
sealed interface SearchScope {
    data object All : SearchScope
    data class Entity(val name: String) : SearchScope
    data class Topic(val topicName: String) : SearchScope
    data class DateRange(val from: Long, val to: Long) : SearchScope
    data class Relation(val type: RelationType) : SearchScope
}

data class SearchHit(
    val fileId: String,
    val title: String,
    val snippet: String,         // 高亮命中片段
    val score: Float,
    val matchedEntities: List<Entity>,   // 命中的实体
    val matchedRelations: List<Relation>,// 命中的关系
    val matchedAt: Long,
)

interface SearchEngine {
    suspend fun search(query: String, scope: SearchScope, limit: Int = 50): List<SearchHit>
    suspend fun suggest(prefix: String, limit: Int = 10): List<String>   // 实体/标签补全
}
```

### 8.3 UI 体验

- 顶部固定搜索栏(全部 tab 都可唤起)
- 命中按"文件 / 实体 / 关系"分组,不是平铺
- 实体命中可"查看图谱"直接定位
- 历史搜索本地保存,可清空

---

## 9. 情感化游戏化 v2(与生长绑定)

### 9.1 当前问题

- XP/Level 与"知识生长"指标脱节
- 任务(Quest)是"今日导入 3 份文件"这种**操作驱动**,不是"知识生长驱动"
- 用户看不到自己真实的成长曲线

### 9.2 v3 改造

**新 XP 事件**:
| 事件 | XP | 触发 | 与生长绑定 |
|------|-----|------|----------|
| FILE_ANALYZED | 5 | 单文件完成分析 | ✓ |
| ENTITY_DISCOVERED | 2 | 每发现一个新实体 | ✓ |
| RELATION_DISCOVERED | 3 | 每发现一条新关系 | ✓ |
| THEME_FORMED | 50 | 跨 3+ 文件的主题首次浮现 | ✓ |
| WEEK_STREAK | 30 | 连续 7 天有捕获 | — |
| ENTITY_RETURN | 5 | 30 天内回访同一实体 | ✓ |
| USER_CONFIRM | 1 | 用户确认一个实体类型 | ✓ |

**新 Level 体系**(替代"知识图谱维度"):
- 1-10:**种子 → 嫩芽 → 苗 → 灌木 → 树 → 林 → 园 → 园境 → 园主 → 知识外脑**
- 解锁物:
  - Lv3 实体可"收藏"(钉到主屏)
  - Lv5 解锁"图谱主题导出 PNG/PDF"
  - Lv7 解锁"年度报告"
  - Lv10 解锁"实验性:Bases 视图"(v3.1)

**Quest 改造**:从"操作"换成"生长"
- ✗ "今日导入 3 份文件"
- ✓ "让 1 个主题的实体数翻倍"
- ✓ "建立 10 条新关系"
- ✓ "回访 3 个你 30 天前的实体"

### 9.3 视觉

- 等级图标:不同形态的树/植物,与现有"种子/嫩芽"叙事衔接
- 升级动画:种子破土 → 抽枝 → 开花(对应 Lv 1/3/5)
- 任务完成:XP toast 改成"长出一片新叶"

---

## 10. 品牌叙事子系统

### 10.1 v3 主张

> **"你的知识,只属于你。"**
> FileIntelligence 不联网、不传云、不锁数据。导入即本地,删除即清空,导出即带走。

### 10.2 落点

| 场景 | 文案 / 行为 |
|------|------------|
| **Onboarding** | 第 1 屏"你的知识,只属于你" + 三个图标(🔒 不联网 / 📦 不锁数据 / 🚀 不收费) |
| **About** | "技术原则" 页:列出 Room + DataStore、零网络权限、零遥测、源码许可证 |
| **Settings 顶部** | 醒目"零云"徽章 + 当前数据库大小 |
| **每次导入** | 小徽章 "✓ 本地保存" |
| **每次导出** | 强调"完整 JSON / Markdown,可在 Obsidian / Logseq / Notion 打开" |
| **升级 / 内购** | 永久声明"一次性付费,无订阅" (如适用) |

### 10.3 外部可见的"零网络"证据

- `AndroidManifest.xml` 无 INTERNET 权限(V2 已规划,必须保留)
- 在 About 页提供"数据完全本地"白皮书链接
- 公开隐私白皮书:任何第三方 SDK、任何遥测接口都没有

---

## 11. 组件 & 数据流

### 11.1 核心组件清单

| 组件 | 位置 | 职责 |
|------|------|------|
| `FileRepository` | data/ | 唯一真源入口,所有变更走 RepoResult |
| `AnalysisEngine` | ai/ | 词典 + ML Kit 实体抽取 |
| `RelationExtractor` | ai/ (新) | 句/段/文档级真分类关系抽取 |
| `MLKitBridge` | ai/ (新) | ML Kit 模型下载、调用、缓存、降级 |
| `ForceGraphEngine` | ui/components/ | 力导向物理引擎(quadtree) |
| `GraphCanvas` | ui/components/ | 渲染 + 手势(已修缩放) |
| `SemanticSearchEngine` | data/search/ (新) | TF-IDF + 多维联合打分 |
| `AmbientCaptureRouter` | ui/ambient/ (新) | 统一 4 个入口的路由 + 草稿恢复 |
| `QuickCaptureActivity` | ui/ambient/ (重写) | 透明覆盖层,5s 全流程 |
| `TileService` | service/ (新) | Quick Settings 瓦片 |
| `ShareSheetReceiver` | receiver/ (新) | 接收 SEND intent |
| `GameEngine v2` | data/ (重写) | XP/Level/Quest 与生长绑定 |
| `OnboardingScreen` | ui/screens/ (重写) | 零焦虑叙事 |
| `AboutScreen` | ui/screens/ (新) | 技术原则 + 许可证 + 导出工具 |
| `DataExporter` | data/ (新) | JSON / Markdown / Obsidian-vault 三种导出 |
| `DataImporter` | data/ (新) | 反向:Obsidian vault 导入(可选 v3.0,v3.1 必有) |

### 11.2 数据流(导入流程)

```
[入口: Share / Tile / Widget / Voice / OCR / File]
   ↓
AmbientCaptureRouter.route(intent)
   ↓
QuickCaptureActivity 接收 content (草稿自动存)
   ↓
用户点 "保存" (摩擦点)
   ↓
[WorkManager] IngestWorker
   ├─ FileParserEngine.parse(uri)  ← v3 修编码
   ├─ FileRepository.addFileAsync()
   │    ├─ analysisEngine.analyzeWithProgress(content)
   │    │    ├─ 词典命中
   │    │    ├─ MLKitBridge.extractEntities(content)  ← v3 新增
   │    │    └─ 用户反馈环查询
   │    ├─ RelationExtractor.extract(content, entities)
   │    │    ├─ 句级规则(真分类,修 IM-1)
   │    │    ├─ 段级衰减
   │    │    └─ 文档级兜底(仅低置信)
   │    ├─ @Transaction:
   │    │    ├─ db.fileDao().insert(file)
   │    │    ├─ db.entityDao().insertAll(entities)
   │    │    └─ db.edgeDao().replaceForFile(fileId, edges)
   │    ├─ mergeRelations(edges)  ← 用 Map 索引,O(n) 而非 O(n²)
   │    └─ GameEngine.addXp(... ENTITY_DISCOVERED, RELATION_DISCOVERED)
   └─ Snackbar: "已存 · 找到 N 个实体,M 条关系"
   ↓
[Flow] graphDataChanged → ViewModel → Screen 自动重绘
```

### 11.3 错误流

```
任意层错误
   ↓
统一包装:RepoResult.Err(message, cause)
   ↓
ViewModel 捕获 → Snackbar 显示(message, retry action)
   ↓
不静默,不 printStackTrace
关键路径埋点(本地文件 logger):
  - 导入耗时
  - 实体数 / 边数
  - 图谱帧率
  - ML Kit 模型加载耗时
```

---

## 12. 路线图(分 Phase)

> 总时间估算:8.5 周(B 方向全部 + V2 Phase A 收尾;含 Phase 0 4 天 + Phase 1.5 周扩到 1.5 周)
> 每个 Phase 独立可发版(Feature Flag 控制),允许回退

### Phase 0 — 数据安全网(必须先做,3-4 天)

**目标**:Code Review CR-1 ~ CR-4 全部修复,IM-1 修复

| Task | 描述 | 估时 | 验收 |
|------|------|------|------|
| P0-1 | 实体 ID 改 UUID.nameUUIDFromBytes("$type\|$name") | 0.5d | 单元测试:100 个不同实体名无碰撞 |
| P0-2 | `saveAll` 下沉到 DAO `@Transaction` | 0.5d | 中断测试:kill -9 后数据一致 |
| P0-3 | `FileParserEngine` 编码嗅探(BOM→UTF-8→GB18030) | 0.5d | 测试:GBK 文件解码正确 |
| P0-4 | `addFileAsync` 用 try/finally 复位 analysisStage | 0.5d | 测试:分析异常后 stage=IDLE |
| P0-5 | 修 IM-1 关系分类逻辑(真用 capture group) | 1d | 单元测试:同一句"X 导致 Y"和"X 和 Y 一起"分类不同 |
| P0-6 | Room schema 3→4 Migration(写正式迁移,不再 destructive) | 1d | 手动测:从 schema v3 升 v4 不丢数据 |
| P0-7 | `loadFromRoom` 统一 `RepoResult` 包装 | 0.5d | — |

**Feature Flag**:`v3.phase0_enabled` (默认 true,v3.0 不可关闭)

### Phase 1 — 基础架构收尾(V2 Phase A 余项,1.5 周)

| Task | 描述 | 估时 |
|------|------|------|
| P1-1 | `object ThemeState` 单例撤掉,改 `isSystemInDarkTheme` + DataStore | 1d |
| P1-2 | `Dimens` / `Shape` token 化,批量替换硬编码 dp | 1d |
| P1-3 | 路由合并:`insights` / `spectrum` / `growth` → `spectrum_graph` 单一路由 + 内部 tab | 1d |
| P1-4 | 5 处组合期重计算(remember / derivedStateOf) | 0.5d |
| P1-5 | `GlassCard` 噪点 Path 改 Shader 一次性生成 | 0.5d |
| P1-6 | `MainViewModel` 拆 `DashboardVM` / `LibraryVM` / `GraphVM` | 1d |
| P1-7 | `mergeRelations` 改 `Map<Triple, Int>` 索引(修 IM-3) | 0.5d |
| P1-8 | 文档级兜底仅在低置信时启用(修 IM-4) | 0.5d |
| P1-9 | 真实 PDF/DOCX 解析(iText5 + Apache POI 懒加载) | 1.5d |
| P1-10 | **quadtree 力导向**(替代 `ForceGraphEngine.simulateStep` 内的 grid bucket)+ 视口剔除 | 2d |
| P1-11 | quadtree 单元测试 + 2000 节点性能基准 | 1d |

### Phase 2 — Ambient Capture(1.5 周)

| Task | 描述 | 估时 |
|------|------|------|
| P2-1 | Share Sheet Receiver(ACTION_SEND text/*) | 1d |
| P2-2 | Quick Settings Tile Service | 0.5d |
| P2-3 | `QuickCaptureActivity` 重写为透明覆盖层 + 草稿恢复 | 1d |
| P2-4 | 现有 Home Widget 加 1x1 "快记" 入口 | 0.5d |
| P2-5 | Voice Capture:集成 ML Kit on-device ASR(Vosk 中文 ~50MB) | 2d |
| P2-6 | Photo OCR:ML Kit Text Recognition v2 接入 | 1.5d |
| P2-7 | WorkManager `IngestWorker` 统一异步管线 | 1d |

**Feature Flag**:`v3.ambient_capture_enabled`
**不阻塞**:P2-5 / P2-6 可后置(单独开关)

### Phase 3 — 端侧 AI(1.5 周)

| Task | 描述 | 估时 |
|------|------|------|
| P3-1 | `MLKitBridge` 设计(模型下载、缓存、降级) | 0.5d |
| P3-2 | `EntityExtractor` v3:词典 + ML Kit + 用户反馈环融合 | 2d |
| P3-3 | `RelationExtractor` 重写:句级真分类 + 段级衰减 + 低置信兜底 | 1.5d |
| P3-4 | 实体确认 UI(每个实体可改类型、可标记忽略) | 1.5d |
| P3-5 | 边的 source 标签 UI(可看到这条边从哪来) | 0.5d |
| P3-6 | 中文模型首次懒下载引导(用户可控) | 0.5d |
| P3-7 | `RelationExtractionTest` 大幅扩展(覆盖真分类) | 1d |

**Feature Flag**:`v3.mlkit_enabled`(默认 true,可降级纯词典)

### Phase 4 — 语义检索 + UI 收尾(1 周)

| Task | 描述 | 估时 |
|------|------|------|
| P4-1 | Room FTS 虚拟表 + 中文 n-gram 分词 | 1d |
| P4-2 | `SemanticSearchEngine` v3.0:TF-IDF + 4 维联合打分 | 1.5d |
| P4-3 | 搜索 UI:按"文件 / 实体 / 关系"分组 | 1d |
| P4-4 | 搜索栏所有 tab 顶部固定 | 0.5d |
| P4-5 | 撤回状态:`analysisStage` 卡死保护 | 0.5d |
| P4-6 | Onboarding 重写(零焦虑叙事 4 屏) | 1d |
| P4-7 | About / 技术原则 屏 | 0.5d |
| P4-8 | `DataExporter` v1:JSON / Markdown / Obsidian-vault | 1.5d |

### Phase 5 — 情感化游戏化 v2(0.5 周)

| Task | 描述 | 估时 |
|------|------|------|
| P5-1 | 新 XP 事件(`THEME_FORMED` / `ENTITY_RETURN` / `USER_CONFIRM`) | 0.5d |
| P5-2 | Level 体系重做(种子→外脑 10 阶 + 解锁物) | 1d |
| P5-3 | Quest 改"生长驱动" | 0.5d |
| P5-4 | 升级动画 / 视觉收尾 | 0.5d |

### Phase 6 — 收尾与发布(0.5 周)

| Task | 描述 | 估时 |
|------|------|------|
| P6-1 | 全量测试 + monkey + 中断测试 | 1d |
| P6-2 | 性能基准(图谱 2000 节点,导入 100 文件) | 0.5d |
| P6-3 | 隐私白皮书 + About 终稿 | 0.5d |
| P6-4 | Release Note + 用户迁移说明 | 0.5d |

---

## 13. 错误处理 & 可观测性

### 13.1 错误分类

| 等级 | 例子 | UI 反馈 |
|------|------|---------|
| **P0 数据丢失** | 事务回滚失败 | Snackbar + 提示"已自动备份到 v1 JSON" |
| **P1 主流程阻塞** | ML Kit 模型下载失败 | 降级到纯词典,提示"已切换本地模式" |
| **P2 单文件失败** | 某 PDF 解析失败 | Snackbar,继续下一个 |
| **P3 性能告警** | 图谱 2000 节点 FPS < 30 | 静默降级(关闭 flowParticles) |

### 13.2 可观测性

- **本地 Logger**:`FileLogger`,写到 `context.filesDir/logs/{date}.log`,仅本地
- **关键埋点**(全部本地,不外传):
  - `import.duration_ms`
  - `import.entity_count`, `import.relation_count`
  - `analysis.mlkit_fallback_count`
  - `graph.fps`, `graph.node_count`
  - `capture.entry_point`(share/tile/widget/voice/photo)
- **不开 Crashlytics / Firebase / 任何第三方遥测** —— 零网络承诺的一部分

### 13.3 撤退路径

每个 Phase 都有 Feature Flag 开关:
- v3.phase0_enabled
- v3.phase1_arch
- v3.ambient_capture_enabled
- v3.ambient_capture.voice_enabled
- v3.ambient_capture.photo_enabled
- v3.mlkit_enabled
- v3.semantic_search_enabled
- v3.gamification_v2_enabled

任一 Phase 引入 P0 缺陷 → 关 Flag,回到上一稳定点,用户无感

---

## 14. 测试策略

| 层 | 工具 | 覆盖目标 |
|----|------|----------|
| 单元测试 | JUnit 5 + Truth | Domain 层 ≥ 80% 行覆盖 |
| 集成测试 | Room in-memory + Turbine | Repository + DAO @Transaction |
| UI 测试 | Compose UI Test + Roborazzi 截图 | 关键 Screen 烟雾测试 |
| 性能测试 | Macrobenchmark | 图谱 2000 节点 FPS ≥ 55 |
| 中断测试 | 手动 + `kill -9` 模拟 | 事务回滚、数据一致 |
| 兼容性测试 | 中文 Windows GBK 文件、纯 emoji 文件、超大文件(50MB) | 编码、内存 |
| 实机测试 | 5 款设备(中低端到旗舰) | 手势、Quick Capture、Tile |

**关键新增测试**:
- `RelationExtractionTest` 大幅扩展(IM-1 修复的回归)
- `MLKitBridgeTest`:mock 注入,测降级路径
- `AmbientCaptureTest`:5 个入口的 intent 解析
- `FileParserEncodingTest`:GBK / GB18030 / UTF-8-BOM 三种
- `RepoTransactionTest`:kill -9 模拟

---

## 15. 风险与对策

| 风险 | 概率 | 影响 | 对策 |
|------|------|------|------|
| ML Kit 中文模型下载失败 | 中 | 中 | 提供"仅词典"降级 + 重试 + 离线包 |
| 中文 ASR(Vosk)模型太大(~50MB) | 高 | 中 | 首次使用时下载,提供开关 |
| Quick Capture 透明 Activity 在某些厂商 ROM 上行为异常 | 中 | 中 | 备选:轻量级 Dialog 风格 |
| 图谱 2000 节点仍掉帧 | 中 | 高 | quadtree + 视口剔除 + flowParticles 关闭 |
| 用户已用 v2 大数据,Room schema 3 升 4 迁移失败 | 低 | 高 | 提供 schema 3→4 Migration 自动化 + 失败回滚 JSON |
| 关系抽取修复后,**旧数据的关系类型不可信** | 高 | 中 | Migration 时:edges.type 全部重置为 CO_OCCURS,UI 提示"已重分析中" |
| APK 体积 +5-10MB 引发用户流失 | 中 | 中 | 提供"轻量模式":首次启动可关闭 ML Kit,APK 减 3MB |

---

## 16. 验收标准(v3.0 整体)

- [ ] `assembleDebug` 0 错误 0 警告
- [ ] P0 ~ P2 全部单元测试通过
- [ ] 4 个 Ambient Capture 入口可用(Share/Tile/Widget/Voice 至少 3 个)
- [ ] ML Kit + 词典 + 用户反馈环融合运行
- [ ] 关系抽取 6 类真分类(IM-1 修复),`RelationExtractionTest` 覆盖率 ≥ 90%
- [ ] 图谱 2000 节点 30s FPS ≥ 55
- [ ] Quick Capture 端到端 < 5s(从入口到"已收下"反馈)
- [ ] 中文 GBK 文件导入正确解码
- [ ] 导入失败显示 Snackbar,stage 不卡死
- [ ] 0 数据丢失(中断测试 100 次)
- [ ] 6 种关系类型在 UI 着色真实可信(用户能看出 `source` 字段)
- [ ] Room schema 3 → 4 Migration 自动化(失败可回滚)
- [ ] quadtree 力导向 + 视口剔除启用,2000 节点不掉帧
- [ ] Onboarding / About / Settings 三处显式"零焦虑"叙事
- [ ] Quest / Level / XP 与"知识生长"真实绑定
- [ ] 完整导出 JSON / Markdown / Obsidian-vault 工具链
- [ ] 隐私白皮书公开
- [ ] APK 体积增量 ≤ 10MB
- [ ] 5 款真机设备烟雾测试通过
- [ ] Room schema 3→4 Migration 不丢用户数据

---

## 17. 开放问题(待用户/实施期回答)

1. **ML Kit 中文模型是否打包进 APK?**
   - 选项 A:APK 内(~5MB 增量,首次启动快)
   - 选项 B:首次使用懒下载(APK 小,首次启动有等待)
   - 建议:选项 B,体验更好,且用户可控

2. **Vosk 中文 ASR 模型是否打包?**
   - 同上问题,模型约 50MB
   - 建议:首次使用懒下载,提供"语音输入"开关

3. **导出 Markdown 的目标格式?**
   - 选项 A:Obsidian-flavored(含 `[[双链]]`)
   - 选项 B:Logseq-flavored(含 block ID)
   - 选项 C:纯 Markdown
   - 建议:v3.0 提供 A + C,v3.1 加 B

4. **Quest / Level 的"知识生长"具体指标?**
   - 主题涌现如何检测?(3+ 文件?还是 5+?)
   - 实体回访如何定义?(30 天内点击 ≥ 1 次?)
   - 这些在 Phase 5 实施时细化

5. **是否在 v3.0 加入跨设备同步(实验性)?**
   - 例如 WebDAV / 本地热点
   - 建议:v3.0 **不加**,v3.1 评估,避免扩张失控

6. **明信片导出是否升级为"年度报告"?**
   - 建议:Phase 6 末尾做 MVP,作为 v3.0 的彩蛋

---

## 18. 附录

### 18.1 不在 v3 范围(明确放弃)

- 块级粒度重构(v4)
- 端云协同 / 跨设备同步(v3.1+)
- Embedding 召回(v3.0 用 TF-IDF,v3.1 评估 e5)
- 社区检测 / 高级聚类(v3.1+)
- 实时协作(v4+)

### 18.2 关键参考

- V2 计划:`docs/V2_UPGRADE_PLAN.md`
- 设计系统:`design/design-system.md`, `design/tokens.css`
- 设计稿:`design/index.html`, `design/dashboard.html`, `design/knowledge-graph.html`, `design/peach/index.html`
- Obsidian 1.10+ Mobile 特性
- 2026 端侧 AI 模型对比:ML Kit Entity Extraction, multilingual-e5-small ONNX, sqlite-vec

### 18.3 变更摘要(对用户的承诺)

- 零网络权限继续保留(已在 manifest)
- 一次性付费模型维持(如有)
- 数据导出完整性保证(DataExporter v1)
- 用户数据所有权显式(About + 白皮书)

---

**Status**:待用户审阅
**Next**:审阅通过后,进入 `writing-plans` 拆解为每个 Phase 的详细实施计划
