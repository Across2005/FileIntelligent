package com.crossk.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.max

/**
 * GameEngine — 负责所有游戏化系统逻辑：XP、Streak、Heatmap、Quests。
 * 从 FileRepository 拆分出来，使各组件可独立测试。
 */
class GameEngine {

    var totalXp by mutableStateOf(0)
        private set

    var quests by mutableStateOf(generateDailyQuests())
        private set

    var streak by mutableStateOf(StreakData())
        private set

    /** 基于真实活动日期的热图数据 */
    val heatmapData: List<HeatmapDay>
        get() = computeHeatmap()

    val level: KnowledgeLevel get() = calculateLevel(totalXp)

    private val activityLog = mutableListOf<ActivityRecord>()

    // ── XP 系统 ──

    fun addXp(event: XpEvent): KnowledgeLevel {
        totalXp += event.xp
        recordActivity()
        return level
    }

    fun addXpRaw(amount: Int): KnowledgeLevel {
        totalXp = (totalXp + amount).coerceAtMost(LEVEL_THRESHOLDS.last())
        recordActivity()
        return level
    }

    fun restoreXp(amount: Int) {
        totalXp = amount.coerceIn(0, LEVEL_THRESHOLDS.last())
    }

    fun restoreStreak(data: StreakData) {
        streak = data
    }

    // ── Streak & Heatmap ──

    private fun recordActivity() {
        val today = System.currentTimeMillis() / 86400000L * 86400000L
        if (streak.lastActiveDate < today) {
            val isConsecutive = (today - streak.lastActiveDate) <= 86400000L
            streak = streak.copy(
                currentStreak = if (isConsecutive) streak.currentStreak + 1 else 1,
                longestStreak = max(streak.longestStreak, if (isConsecutive) streak.currentStreak + 1 else 1),
                lastActiveDate = today,
                todayActive = true,
            )
        } else if (streak.lastActiveDate == today && !streak.todayActive) {
            streak = streak.copy(todayActive = true)
        }
        logActivity()
    }

    private fun logActivity() {
        val today = System.currentTimeMillis() / 86400000L * 86400000L
        val existing = activityLog.indexOfFirst { it.date == today }
        if (existing >= 0) {
            activityLog[existing] = activityLog[existing].copy(
                count = activityLog[existing].count + 1
            )
        } else {
            activityLog.add(ActivityRecord(today, 1))
        }
    }

    private fun computeHeatmap(): List<HeatmapDay> {
        val now = System.currentTimeMillis()
        val dayMs = 86400000L
        val weeks = 20
        val maxCount = activityLog.maxOfOrNull { it.count } ?: 1

        return (0 until weeks * 7).map { daysAgo ->
            val date = now - daysAgo * dayMs
            val dayStart = date - (date % dayMs)
            val record = activityLog.find { it.date == dayStart }
            val count = record?.count ?: 0
            HeatmapDay(
                date = dayStart,
                count = count,
                intensity = if (maxCount > 0) (count.toFloat() / maxCount).coerceIn(0f, 1f) else 0f,
            )
        }.reversed()
    }

    // ── 任务系统 ──

    fun completeQuest(questId: String) {
        quests = quests.map {
            if (it.id == questId) it.copy(completed = true, progress = it.target)
            else it
        }
        addXp(XpEvent.QUEST_COMPLETED)
    }

    fun updateQuestProgress(condition: QuestCondition, increment: Int = 1) {
        quests = quests.map { quest ->
            if (quest.condition == condition && !quest.completed) {
                val newProgress = (quest.progress + increment).coerceAtMost(quest.target)
                if (newProgress >= quest.target) {
                    addXp(XpEvent.QUEST_COMPLETED)
                    quest.copy(progress = newProgress, completed = true)
                } else {
                    quest.copy(progress = newProgress)
                }
            } else quest
        }
    }

    fun refreshDailyQuests() {
        quests = generateDailyQuests()
    }

    private data class ActivityRecord(
        val date: Long,
        val count: Int,
    )
}
