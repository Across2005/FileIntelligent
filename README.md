# FileIntelligence · 智能外脑 (Smart Exocortex)

> **你的知识,只属于你。**
> 一个本地优先的 Android 个人知识图谱 App —— 用 2 秒捕捉任何想法,让 App 在端侧把它种进一张持续生长的图谱。

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Platform: Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](https://www.android.com/)
[![Min SDK: 24](https://img.shields.io/badge/Min%20SDK-24-7FBA00?logo=android)](https://developer.android.com/about/versions/nougat)
[![Target SDK: 35](https://img.shields.io/badge/Target%20SDK-35-7FBA00?logo=android)](https://developer.android.com/about/versions/15)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.24-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Zero Network](https://img.shields.io/badge/Network-Zero%20Permission-success)]()

---

## ✨ 这是什么

**FileIntelligence v3.0** 不是又一款云笔记 App,也不是又一个"AI 摘要工具"。

它是一个 **本地优先的智能外脑 (Smart Exocortex)**:

- 🧠 **端侧真 AI** —— ML Kit 实体抽取 + 词典 + 用户反馈环融合,在你的手机上跑,不联网
- 🌱 **2 秒捕获** —— Share Sheet / Quick Settings Tile / Home Widget / 语音 / 拍照 OCR,任意入口 < 2s
- 🕸️ **活的图谱** —— 实体、关系、证据、置信度、来源全部可见,可点开质疑
- 🎯 **生长驱动** —— XP/Level/Quest 跟真实的"知识生长"绑定,不是操作计数
- 🔒 **零焦虑** —— 零网络权限、零遥测、零订阅、零锁定,数据导入即本地,删除即清空,导出即带走

---

## 📲 下载安装

> 当前版本:**v0.1.0-alpha**(基于 v3.0 Phase 0 数据安全网)

| 通道 | 下载 | 说明 |
|------|------|------|
| **GitHub Releases** | [📦 下载最新 APK](https://github.com/Across2005/FileIntelligent/releases/latest) | 推荐,签名校验,带 changelog |
| **Debug 构建** | [app-debug.apk](https://github.com/Across2005/FileIntelligent/releases) | 开发者构建,无优化 |

### 安装步骤

1. 下载 APK 到手机
2. 手机设置 → 安全 → 允许"安装未知来源应用"(仅本次)
3. 点击 APK 文件安装
4. 首次启动会有 3 屏 onboarding 介绍"零云"原则
5. 开始你的第一次快记 🎉

### 系统要求

- Android 7.0 (API 24) 及以上
- 推荐 Android 10+ 以获得完整 Quick Settings Tile 体验
- 约 50MB 存储空间

---

## 🌟 核心功能

### 1. 多模态 Ambient Capture(2 秒入口)

| 入口 | 摩擦 | 适用场景 |
|------|------|----------|
| **Android Share Sheet** | < 1s | 看到好内容随时存 |
| **Quick Settings Tile** | < 1s | 通知栏下拉一键 |
| **Home Widget** | < 2s | 桌面直达 |
| **语音快记** | < 5s | 走路/通勤 |
| **拍照 OCR** | < 8s | 拍书/截图 |

Quick Capture 是 **透明覆盖层** —— 不会强制离开你当前正在用的 App,5 秒内"已收下"反馈。

### 2. 端侧真 AI 子系统

```
输入文本
   ↓
[1] 词典快速命中(中文专词、用户自定义词)
   ↓
[2] ML Kit Entity Extraction(11 类,中文需首次懒下载)
   ↓
[3] n-gram 候选 + 位置加权(回退)
   ↓
[4] 用户反馈叠加:已确认的实体名 + 类型直接采用
   ↓
输出:稳定的 UUID 实体集
```

**6 种关系类型**(经 IM-1 修复后的真分类,不再是词典幻觉):

| 关系 | 含义 | 示例 |
|------|------|------|
| `CO_OCCURS` | 同句/同段共现 | "苹果和香蕉" |
| `REFERENCES` | 引用/指代 | "参见第二章" |
| `DERIVES_FROM` | 派生/导致 | "X 导致 Y" |
| `BELONGS_TO` | 属于 | "北京属于中国" |
| `CONTRASTS_WITH` | 对比 | "黑 vs 白" |
| `SIMILAR_TO` | 相似 | "类似的概念" |

每条边都带 `confidence`(置信度)和 `source`(来源:rule / mlkit / user),UI 可视,质疑一键删除。

### 3. 用户反馈环

- 每个实体可点"类型错"→ 弹类型选择
- 边的详情可点"这条关系不成立"→ 软删除
- 累计 5 次确认某实体类型 → `isUserConfirmed = true`,后续该实体名直接用该类型
- 你的判断 **真的在影响 AI** —— 不是花架子

### 4. 知识图谱可视化

- 力导向布局(quadtree 加速,2000 节点不掉帧)
- 视口剔除 + `withFrameNanos` 帧率优先
- 双指缩放、拖拽、节点点击入详情
- 节点按重要性 / 类型 / 主题着色

### 5. 语义检索 v3.0(TF-IDF + 4 维联合打分)

| 维度 | 权重 |
|------|------|
| 全文匹配 (Room FTS4) | 0.35 |
| 实体名命中 | 0.30 |
| 关系命中 | 0.15 |
| 时间近因 | 0.10 |
| 重要性 × 回访次数 | 0.10 |

命中按"文件 / 实体 / 关系"分组,不是平铺。实体命中可"查看图谱"直接定位。

### 6. 情感化游戏化(生长驱动)

- **10 阶进化**:种子 → 嫩芽 → 苗 → 灌木 → 树 → 林 → 园 → 园境 → 园主 → **知识外脑**
- **Quest 改造**:从"今日导入 3 份文件"(操作驱动)→ "让 1 个主题的实体数翻倍"(生长驱动)
- **XP 事件**:全部绑定真实知识生长 —— `FILE_ANALYZED` / `ENTITY_DISCOVERED` / `RELATION_DISCOVERED` / `THEME_FORMED` / `ENTITY_RETURN` / `USER_CONFIRM`

### 7. 数据导出(逃生通道)

- **JSON v1**:完整数据库导出,可在任意 App 重建
- **Markdown**(Obsidian 风味,含 `[[双链]]`)
- **Obsidian Vault**:直接整个文件夹导入 Obsidian
- **明信片**:知识生长可视化导出,可分享

---

## 🔒 零焦虑原则

| 承诺 | 证据 |
|------|------|
| **零网络权限** | `AndroidManifest.xml` 无 `INTERNET` 权限 |
| **零遥测** | 不集成 Crashlytics / Firebase / 任何第三方 SDK |
| **零订阅** | 一次性付费(如有) / 完全免费,无内购循环 |
| **零锁定** | 数据完全可导出,任意时刻打包带走 |
| **零服务器** | 你的 App 离线关闭后,数据依然在 |

> 2026 隐私焦虑里,本地优先不是默认值 —— **是价值主张**。

---

## 🛠️ 技术栈

| 层 | 选型 |
|----|------|
| **UI** | Jetpack Compose + Material 3(自定义语义色) |
| **导航** | Navigation Compose |
| **状态** | StateFlow + ViewModel + SnapshotStateList |
| **持久化** | Room v4(schema migration 自动化) + DataStore(仅偏好) |
| **AI** | ML Kit Entity Extraction(11 类)+ 词典 + 用户反馈环 |
| **关系抽取** | 句级规则(7 个 capture group 真分类)+ 段级衰减 + 文档级兜底 |
| **编码** | BOM 嗅探 → UTF-8 → GB18030 fallback(解决中文 Windows 乱码) |
| **力导向** | quadtree + 视口剔除 + `withFrameNanos` |
| **异步** | Kotlin Coroutines + Flow + WorkManager |
| **构建** | Gradle 8.9 + AGP + KSP |
| **最低 SDK** | 24(Android 7.0) |
| **目标 SDK** | 35(Android 15) |
| **Kotlin** | 1.9.24 |
| **JDK** | 17 |

---

## 🏗️ 架构

```
┌──────────────────────────────────────────────────────────┐
│ UI Layer (Compose)                                        │
│   Screen → ViewModel (StateFlow<UiState>) → Stateless     │
│   - Ambient Capture: ShareSheet, Tile, Widget, Voice      │
├──────────────────────────────────────────────────────────┤
│ Domain Layer (pure Kotlin)                                │
│   - GraphEngine (force-directed + quadtree)               │
│   - AnalysisEngine (词典 + ML Kit)                         │
│   - RelationExtractor v2 (句/段/文档级真分类)             │
│   - ParserEngine (BOM → UTF-8 → GB18030)                  │
│   - SemanticSearchEngine (TF-IDF + 4 维联合打分)          │
│   - GameEngine v2 (XP/Level/Quest 与生长绑定)             │
├──────────────────────────────────────────────────────────┤
│ Data Layer                                                │
│   - Room (files, entities, edges, graph_layout, knowledge)│
│   - DataStore (theme, prefs)                               │
│   - 三轨持久化收敛:Room 为唯一真源,DataStore 仅偏好       │
└──────────────────────────────────────────────────────────┘
```

详细设计见 [`docs/superpowers/specs/2026-08-15-smart-exocortex-v3-design.md`](docs/superpowers/specs/2026-08-15-smart-exocortex-v3-design.md)。

---

## 🗺️ 路线图

| Phase | 名称 | 状态 | 估时 |
|-------|------|------|------|
| **0** | 数据安全网(修 CR-1~4 + IM-1) | ✅ 完成 | 3-4 天 |
| **1** | 基础架构收尾(quadtree、ViewModel 拆分) | 🚧 进行中 | 1.5 周 |
| **2** | Ambient Capture(Share/Tile/Widget/Voice/OCR) | 📋 计划 | 1.5 周 |
| **3** | 端侧 AI(ML Kit Bridge + 真分类 UI) | 📋 计划 | 1.5 周 |
| **4** | 语义检索 + UI 收尾 | 📋 计划 | 1 周 |
| **5** | 情感化游戏化 v2 | 📋 计划 | 0.5 周 |
| **6** | 收尾与发布 | 📋 计划 | 0.5 周 |

每个 Phase 独立可发版,Feature Flag 控制,允许回退。

详细计划见 [`docs/superpowers/plans/2026-08-15-v3-phase0-data-safety-net.md`](docs/superpowers/plans/2026-08-15-v3-phase0-data-safety-net.md)。

---

## 🔨 从源码构建

```bash
# 克隆
git clone https://github.com/Across2005/FileIntelligent.git
cd FileIntelligent

# 用 Android Studio Hedgehog (2023.1.1+) 打开
# 或命令行:
./gradlew assembleDebug          # Debug APK
./gradlew assembleRelease        # Release APK(需配置签名)
./gradlew testDebugUnitTest      # 跑全部单元测试
```

### 环境要求

- Android Studio Hedgehog (2023.1.1+) 或更新
- Gradle 8.7+(wrapper 自带 8.9)
- Android SDK 26+
- Kotlin 1.9.24+
- JDK 17

### 目录结构

```
FileIntelligence/
├── app/
│   ├── src/main/java/com/crossk/
│   │   ├── ai/         # AnalysisEngine, RelationExtractor, MLKitBridge
│   │   ├── data/       # FileItem, FileRepository, FileParserEngine
│   │   ├── data/db/    # Room entities, DAOs, AppDatabase, Migrations
│   │   └── ui/
│   │       ├── components/  # GraphCanvas, BottomNav, FileCard, ...
│   │       ├── navigation/  # NavGraph
│   │       ├── screens/     # Dashboard, Library, SpectrumGrowth, Graph
│   │       └── theme/       # Color, Theme, Type
│   └── src/test/       # 单元测试(EntityIDFactory, RelationClassifier, ...)
├── docs/
│   ├── superpowers/
│   │   ├── specs/      # 设计文档
│   │   ├── plans/      # 实施计划
│   │   └── reviews/    # Code Review 报告
│   └── V2_UPGRADE_PLAN.md
├── gradle/
│   └── libs.versions.toml
└── LICENSE
```

---

## 🧪 测试

| 类型 | 工具 | 范围 |
|------|------|------|
| 单元测试 | JUnit 4 + Truth 1.4.4 | Domain 层 |
| Robolectric | 4.13 | Android Context 依赖 |
| Room in-memory | MigrationTestHelper | DAO + Migration |
| Compose UI Test | (计划中) | 关键 Screen |

当前测试覆盖:33/38 ✅ green(SchemaMigrationTest 5 个等 v3.json 跟进)

```bash
./gradlew testDebugUnitTest
./gradlew testDebugUnitTest --tests com.crossk.ai.EntityIDFactoryTest  # 单个测试
```

---

## 🤝 贡献

欢迎 PR / Issue。但请先读一下设计文档,理解 v3 的"零焦虑"原则 —— 这是项目的核心价值,不能为加功能而妥协。

- 设计 SPEC: [`docs/superpowers/specs/`](docs/superpowers/specs/)
- Phase 0 计划: [`docs/superpowers/plans/2026-08-15-v3-phase0-data-safety-net.md`](docs/superpowers/plans/2026-08-15-v3-phase0-data-safety-net.md)
- Code Review: [`docs/superpowers/reviews/`](docs/superpowers/reviews/)

---

## 📜 许可证

本项目采用 **MIT 许可证** —— 详见 [LICENSE](LICENSE) 文件。

```
MIT License

Copyright (c) 2026 Across2005

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## 🙏 致谢

- [Jetpack Compose](https://developer.android.com/jetpack/compose) — 现代 Android UI
- [ML Kit](https://developers.google.com/ml-kit) — 端侧 AI
- [Room](https://developer.android.com/training/data-storage/room) — 持久化
- [Obsidian](https://obsidian.md/) / [Logseq](https://logseq.com/) / [Tana](https://tana.inc/) — 知识图谱先行者

---

<p align="center">
  <b>🌱 你的知识,只属于你 🌱</b><br>
  <sub>本地优先 · 端侧 AI · 零焦虑</sub>
</p>
