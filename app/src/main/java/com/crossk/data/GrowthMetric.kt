package com.crossk.data

data class GrowthMetric(
    val weekIndex: Int,
    val filesAnalyzed: Int,
    val entitiesDiscovered: Int,
    val connectionsMade: Int,
    val topicsCovered: Int,
)
