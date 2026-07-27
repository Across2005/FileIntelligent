package com.fileintelligence.data

data class GrowthMetric(
    val weekIndex: Int,
    val filesAnalyzed: Int,
    val entitiesDiscovered: Int,
    val connectionsMade: Int,
    val topicsCovered: Int,
)

fun generateMockGrowthData(): List<GrowthMetric> {
    return listOf(
        GrowthMetric(1, 2, 8, 5, 2),
        GrowthMetric(2, 4, 15, 12, 3),
        GrowthMetric(3, 3, 22, 18, 3),
        GrowthMetric(4, 5, 28, 25, 4),
        GrowthMetric(5, 4, 35, 30, 4),
        GrowthMetric(6, 6, 42, 38, 5),
        GrowthMetric(7, 5, 48, 45, 5),
        GrowthMetric(8, 7, 55, 52, 5),
        GrowthMetric(9, 6, 60, 58, 6),
        GrowthMetric(10, 8, 68, 65, 6),
        GrowthMetric(11, 7, 73, 70, 6),
        GrowthMetric(12, 9, 80, 78, 7),
    )
}
