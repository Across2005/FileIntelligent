package com.crossk.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.crossk.data.FileRepository
import com.crossk.data.GraphEdge
import com.crossk.data.GraphEvolutionState
import com.crossk.data.GraphNode
import com.crossk.data.NodeType
import com.crossk.data.getGraphEvolution
import com.crossk.ui.components.GraphCanvas
import com.crossk.ui.components.TimeTravelSlider
import com.crossk.ui.theme.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphScreen(
    navController: NavController,
    focusNodeLabel: String? = null,
    repository: FileRepository? = null,
) {
    var timeTravelActive by remember { mutableStateOf(false) }
    var currentWeek by remember { mutableIntStateOf(0) }
    val totalWeeks = 20

    // v2.0：用 derivedStateOf + remember 包裹；统一走 NodeType.fromEntityType，避免硬编码 map
    val graphNodes = remember(repository?.globalEntities?.size, repository?.files?.size) {
        repository?.let { repo ->
            repo.globalEntities.mapIndexed { idx, entity ->
                GraphNode(
                    id = entity.id,
                    label = entity.name,
                    type = NodeType.fromEntityType(entity.type),
                    x = 100f + (idx % 5) * 100f,
                    y = 100f + (idx / 5) * 120f,
                    size = (entity.mentions.toFloat() / 10f).coerceIn(1f, 4f),
                )
            }
        } ?: emptyList()
    }

    // v2.0：保留真实 RelationType，不再硬编码 EdgeType.REFERENCES
    val graphEdges = remember(repository?.globalRelations?.size, repository?.files?.size) {
        repository?.let { repo ->
            repo.globalRelations.map { rel ->
                GraphEdge(
                    source = rel.sourceEntityId,
                    target = rel.targetEntityId,
                    type = rel.type,
                    weight = rel.weight,
                )
            }
        } ?: emptyList()
    }

    val evolution = repository?.graphEvolution ?: getGraphEvolution(1)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("知识图谱", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        bottomBar = { },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (graphNodes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
                    ) {
                        Text(
                            text = "🌱",
                            style = MaterialTheme.typography.headlineLarge,
                        )
                        Text(
                            text = "知识图谱为空",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "导入文件后，自动提取的实体和关系将在这里生长为知识网络",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = Dimens.SpaceXxl + Dimens.SpaceLg),
                        )
                    }
                }
            } else {
                GraphCanvas(
                    nodes = graphNodes,
                    edges = graphEdges,
                    focusNodeLabel = focusNodeLabel,
                    evolution = evolution,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = Dimens.SpaceLg),
                )
            }

            TimeTravelSlider(
                currentWeek = currentWeek,
                totalWeeks = totalWeeks,
                onWeekChange = { currentWeek = it },
                isActive = timeTravelActive,
                onToggleActive = { timeTravelActive = it },
                modifier = Modifier.padding(horizontal = Dimens.SpaceLg, vertical = Dimens.SpaceSm),
            )
        }
    }
}
