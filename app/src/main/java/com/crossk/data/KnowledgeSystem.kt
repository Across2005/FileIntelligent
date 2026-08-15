package com.crossk.data

import kotlin.math.min
import kotlin.random.Random

/**
 * ===== Knowledge XP & Level System =====
 * 知识经验值与等级系统：给「学习」这个抽象过程量化的成长反馈。
 */

/** XP thresholds for each level (cumulative) */
val LEVEL_THRESHOLDS = listOf(
    0,      // Lv.1 学徒
    100,    // Lv.2 初学者
    250,    // Lv.3 探索者
    500,    // Lv.4 记录员
    800,    // Lv.5 构建者
    1200,   // Lv.6 分析师
    1700,   // Lv.7 联结者
    2300,   // Lv.8 学者
    3000,   // Lv.9 专家
    3800,   // Lv.10 大师
    4800,   // Lv.11 博学者
    6000,   // Lv.12 智者
    7500,   // Lv.13 思想家
    9200,   // Lv.14 探索宗师
    11000,  // Lv.15 知识贤者
    13000,  // Lv.16 启迪者
    15500,  // Lv.17 洞察先知
    18000,  // Lv.18 真理追寻者
    21000,  // Lv.19 智慧化身
    25000,  // Lv.20 活图书馆
)

data class KnowledgeLevel(
    val level: Int,
    val xp: Int,
    val xpToNext: Int,
    val xpProgress: Float, // 0f..1f
) {
    val title: String get() = LEVEL_TITLES.getOrElse(level - 1) { "未知" }
    val isMaxLevel: Boolean get() = level >= LEVEL_THRESHOLDS.size
}

val LEVEL_TITLES = listOf(
    "学徒", "初学者", "探索者", "记录员", "构建者",
    "分析师", "联结者", "学者", "专家", "大师",
    "博学者", "智者", "思想家", "探索宗师", "知识贤者",
    "启迪者", "洞察先知", "真理追寻者", "智慧化身", "活图书馆",
)

/** Events that grant XP */
enum class XpEvent(val xp: Int, val label: String) {
    FILE_ANALYZED(50, "解析新文件"),
    ENTITY_DISCOVERED(10, "发现新实体"),
    CONNECTION_MADE(25, "建立实体连接"),
    FILE_READ(15, "阅读文件"),
    QUEST_COMPLETED(40, "完成任务"),
    STREAK_DAY(30, "连续学习"),
    GRAPH_EXPORTED(100, "导出知识图谱"),
    POSTCARD_CREATED(60, "创建明信片"),
    THEME_MASTERED(150, "主题掌握"),
    QUICK_CAPTURE(10, "快速记录"),
}

fun calculateLevel(xp: Int): KnowledgeLevel {
    // LEVEL_THRESHOLDS[i] = 达到 Lv.(i+1) 所需的累计 XP
    // indexOfLast 返回的是索引 i，等级 = i + 1；xp=0 时 i=0 → Lv.1
    val level = (LEVEL_THRESHOLDS.indexOfLast { it <= xp } + 1).coerceAtLeast(1)
    val currentThreshold = LEVEL_THRESHOLDS[level - 1]
    val nextThreshold = LEVEL_THRESHOLDS.getOrElse(level) { currentThreshold }
    val xpIntoLevel = xp - currentThreshold
    val xpNeeded = nextThreshold - currentThreshold
    return KnowledgeLevel(
        level = level,
        xp = xp,
        xpToNext = if (xpNeeded > 0) xpNeeded else 0,
        xpProgress = if (xpNeeded > 0) (xpIntoLevel.toFloat() / xpNeeded).coerceIn(0f, 1f) else 1f,
    )
}

/**
 * ===== Daily Quests =====
 */
data class DailyQuest(
    val id: String,
    val title: String,
    val description: String,
    val xpReward: Int,
    val iconSymbol: String, // emoji hint
    val condition: QuestCondition,
    var progress: Int = 0,
    var target: Int = 1,
    var completed: Boolean = false,
)

enum class QuestCondition {
    READ_FILE, ANALYZE_FILE, CONNECT_ENTITY,
    CREATE_NOTE, OPEN_GRAPH, EXPORT_POSTCARD,
    VIEW_INSIGHTS,
}

/** Generate today's quests */
fun generateDailyQuests(): List<DailyQuest> {
    val daySeed = System.currentTimeMillis() / 86400000L
    val rng = Random(daySeed)
    val pool = listOf(
        DailyQuest("read1", "翻阅一篇文档", "打开并阅读一份文件", 40, "📖", QuestCondition.READ_FILE, target = 1),
        DailyQuest("read3", "批量阅读", "阅读 3 份不同的文件", 80, "📚", QuestCondition.READ_FILE, target = 3),
        DailyQuest("analyze", "解析新知", "导入并解析一份新文件", 50, "🔬", QuestCondition.ANALYZE_FILE, target = 1),
        DailyQuest("connect", "建立连接", "发现或建立 2 个实体关系", 60, "🔗", QuestCondition.CONNECT_ENTITY, target = 2),
        DailyQuest("graph", "探索图谱", "打开知识图谱并聚焦一个节点", 30, "🌐", QuestCondition.OPEN_GRAPH, target = 1),
        DailyQuest("export", "定格知识", "导出一张明信片或图谱快照", 70, "📮", QuestCondition.EXPORT_POSTCARD, target = 1),
        DailyQuest("insights", "回顾成长", "查看洞察页面", 20, "📊", QuestCondition.VIEW_INSIGHTS, target = 1),
        DailyQuest("quicknote", "快速记录", "通过分享快速记录一条想法", 25, "✏️", QuestCondition.CREATE_NOTE, target = 1),
    )
    // Pick 3 random quests per day
    return pool.shuffled(rng).take(3)
}

/**
 * ===== Streak System =====
 */
data class StreakData(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastActiveDate: Long = 0L, // millis at start of day
    val todayActive: Boolean = false,
)

/**
 * ===== Heatmap Data =====
 */
data class HeatmapDay(
    val date: Long,     // millis
    val count: Int,     // activity count (files read, entities found, etc.)
    val intensity: Float, // 0f..1f — derived from count, for color mapping
)

/**
 * ===== Graph Evolution Levels =====
 * Rules for how the graph looks based on player level.
 */
data class GraphEvolutionState(
    val nodeGlowEnabled: Boolean = false,
    val edgeGradientEnabled: Boolean = false,
    val flowParticlesEnabled: Boolean = false,
    val constellationGridEnabled: Boolean = false,
    val breathingEnabled: Boolean = false,
    val glowIntensityMultiplier: Float = 0f, // 0..1
)

fun getGraphEvolution(level: Int): GraphEvolutionState {
    return GraphEvolutionState(
        nodeGlowEnabled = level >= 2,
        edgeGradientEnabled = level >= 4,
        breathingEnabled = level >= 6,
        flowParticlesEnabled = level >= 8,
        constellationGridEnabled = level >= 10,
        glowIntensityMultiplier = (level / 20f).coerceIn(0f, 1f),
    )
}

/**
 * Aggregate statistics for the knowledge graph.
 */
data class FileStats(
    val totalFiles: Int,
    val totalEntities: Int,
    val totalConnections: Int,
    val topicsCovered: Int,
)
