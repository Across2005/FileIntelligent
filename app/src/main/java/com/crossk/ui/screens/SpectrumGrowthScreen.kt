package com.crossk.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavController
import com.crossk.data.Entity
import com.crossk.data.FileRepository
import com.crossk.data.GraphEdge
import com.crossk.data.GraphNode
import com.crossk.data.NodeType
import com.crossk.ui.components.BottomNav
import com.crossk.ui.components.GraphCanvas
import com.crossk.ui.components.GrowthCard
import com.crossk.ui.components.HeatmapChart
import com.crossk.ui.components.SpectrumChart
import com.crossk.ui.components.SpectrumSeries
import com.crossk.ui.theme.Dimens

/**
 * v2.0：合并 v1 的 Insights/Spectrum/Growth 三条同屏路由，由本 Screen 内部统一承载。
 * - 顶栏：标题 + 副标题（不再有 route 区别）
 * - 列出 4 个数据块：Heatmap / Spectrum / Growth / Graph
 * - 重计算用 derivedStateOf 缓存
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpectrumGrowthScreen(navController: NavController, repository: FileRepository? = null) {
    val entityCount = repository?.globalEntities?.size ?: 0
    val fileCount = repository?.files?.size ?: 0

    val growthData by remember(entityCount, fileCount) {
        derivedStateOf { repository?.computeGrowthMetrics() ?: emptyList() }
    }

    val spectrumSeries by remember(entityCount, fileCount) {
        derivedStateOf {
            if (repository == null || repository.globalEntities.isEmpty()) {
                emptyList()
            } else {
                val entities = repository.globalEntities
                val totalMentions = entities.sumOf { it.mentions }.coerceAtLeast(1)
                val typeGroups = entities.groupBy { it.type }
                typeGroups.map { (type, group) ->
                    val name = when (type) {
                        Entity.Type.CONCEPT -> "概念与理论"
                        Entity.Type.PERSON -> "人物"
                        Entity.Type.PLACE -> "场所与场景"
                        Entity.Type.METHOD -> "方法与技术"
                        Entity.Type.TOOL -> "工具与技术栈"
                        Entity.Type.EVENT -> "事件与活动"
                    }
                    val weight = group.sumOf { it.mentions }.toFloat() / totalMentions
                    val values = (1..12).map { w ->
                        val progress = (w.toFloat() / 12f).coerceAtMost(1f)
                        (weight * progress).coerceIn(0.01f, 1f)
                    }
                    SpectrumSeries(name, type.color, values)
                }.sortedByDescending { it.values.last() }
            }
        }
    }

    val heatmapData = repository?.heatmapData ?: emptyList()

    val graphNodes by remember(entityCount, fileCount) {
        derivedStateOf {
            repository?.globalEntities?.mapIndexed { idx, entity ->
                GraphNode(
                    id = entity.id,
                    label = entity.name,
                    type = NodeType.fromEntityType(entity.type),
                    x = 100f + (idx % 5) * 100f,
                    y = 100f + (idx / 5) * 120f,
                    size = (entity.mentions.toFloat() / 10f).coerceIn(1f, 4f),
                )
            } ?: emptyList()
        }
    }

    val graphEdges by remember(repository?.globalRelations?.size, fileCount) {
        derivedStateOf {
            repository?.globalRelations?.map { rel ->
                GraphEdge(
                    source = rel.sourceEntityId,
                    target = rel.targetEntityId,
                    type = rel.type,
                    weight = rel.weight,
                )
            } ?: emptyList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("洞察", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        bottomBar = { BottomNav(navController, "insights") },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Dimens.SpaceLg)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
        ) {
            HeatmapChart(data = heatmapData)
            SpectrumChart(series = spectrumSeries)
            GrowthCard(metrics = growthData)
            GraphCanvas(nodes = graphNodes, edges = graphEdges)
            Spacer(Modifier.padding(Dimens.SpaceXxl))
        }
    }
}
