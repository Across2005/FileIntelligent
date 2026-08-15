package com.crossk.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for knowledge system state (XP, streak, level).
 * Single-row table keyed by "main".
 */
@Entity(tableName = "knowledge")
data class KnowledgeEntity(
    @PrimaryKey val rowId: String = "main",
    val totalXp: Int = 0,
    val graphVisualLevel: Int = 1,
    val streakCurrent: Int = 0,
    val streakLongest: Int = 0,
    val streakLastActive: Long = 0L,
    // v4: onboarding state and capture telemetry
    @ColumnInfo(defaultValue = "0") val onboardingCompleted: Boolean = false,
    @ColumnInfo(defaultValue = "NULL") val firstCaptureAt: Long? = null,
    @ColumnInfo(defaultValue = "NULL") val lastCaptureAt: Long? = null,
    @ColumnInfo(defaultValue = "0") val captureStreak: Int = 0,
)
