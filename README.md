# 文件智析 (File Intelligence)

Android 应用 — 文本存储 + AI 分析 + 知识可视化。

## 快速开始

1. 用 **Android Studio** 打开 `FileIntelligence/` 目录
2. 等待 Gradle 同步完成（Sync Now）
3. 连接设备或启动模拟器，点击 Run

## 项目结构

```
FileIntelligence/
├── app/
│   ├── src/main/
│   │   ├── java/com/fileintelligence/
│   │   │   ├── MainActivity.kt        # 入口
│   │   │   ├── FileIntelligenceApp.kt # Application
│   │   │   ├── ai/
│   │   │   │   └── AnalysisEngine.kt  # AI 分析引擎（关键词提取/实体识别）
│   │   │   ├── data/
│   │   │   │   ├── FileItem.kt        # 文件 & 实体 & 分析结果模型
│   │   │   │   ├── GraphNode.kt       # 知识图谱节点/边模型
│   │   │   │   ├── GrowthMetric.kt    # 成长曲线数据模型
│   │   │   │   └── MockData.kt        # Mock 数据源
│   │   │   └── ui/
│   │   │       ├── components/        # 可复用组件
│   │   │       │   ├── BottomNav.kt
│   │   │       │   ├── FileCard.kt
│   │   │       │   ├── GraphCanvas.kt    # 知识图谱 Canvas 绘制
│   │   │       │   ├── GrowthCard.kt     # 成长曲线 + 统计卡片
│   │   │       │   ├── InsightBanner.kt  # AI 洞察横幅
│   │   │       │   └── SpectrumChart.kt  # 发展光谱 Canvas 绘制
│   │   │       ├── navigation/
│   │   │       │   └── NavGraph.kt       # 导航路由
│   │   │       ├── screens/
│   │   │       │   ├── DashboardScreen.kt      # 首页仪表盘
│   │   │       │   ├── LibraryScreen.kt         # 文件库
│   │   │       │   ├── SpectrumGrowthScreen.kt  # 洞察页(光谱+曲线+图谱)
│   │   │       │   └── GraphScreen.kt           # 知识图谱全屏
│   │   │       └── theme/              # 设计系统（深色主题）
│   │   │           ├── Color.kt
│   │   │           ├── Theme.kt
│   │   │           └── Type.kt
│   │   └── res/
│   │       ├── values/strings.xml
│   │       └── values/themes.xml
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── gradle/
    └── libs.versions.toml
```

## 关键依赖

- **Jetpack Compose** — 全声明式 UI
- **Material 3** — 深色主题 + 组件库
- **Navigation Compose** — 页面路由
- **Canvas API** — 知识图谱 & 发展光谱自绘

## 后续可扩展方向

- 真实文件读写（SAF / FileProvider）
- 接入真实 AI API（OpenAI / Anthropic / 本地 LLM）
- DataStore 持久化分析与索引数据
- 知识图谱手势交互（缩放/拖拽/聚焦）
- Room 数据库 + 离线索引
- 多语言支持

## 构建要求

- Android Studio Hedgehog (2023.1.1+) 或更高
- Gradle 8.7+
- Android SDK 26+
- Kotlin 1.9.24+
- JDK 17
