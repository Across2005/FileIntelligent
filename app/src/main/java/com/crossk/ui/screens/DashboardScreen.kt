package com.crossk.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.crossk.data.FileRepository
import com.crossk.ui.components.BottomNav
import com.crossk.ui.components.DailyQuestPanel
import com.crossk.ui.components.EmptyKnowledgeState
import com.crossk.ui.components.FileCard
import com.crossk.ui.components.GrowthCard
import com.crossk.ui.components.HeatmapChart
import com.crossk.ui.components.InsightBanner
import com.crossk.ui.components.LevelBadge
import com.crossk.ui.components.StatsItem
import com.crossk.ui.components.StatsRow
import com.crossk.ui.components.XpGainToast
import com.crossk.ui.navigation.Screen
import com.crossk.ui.theme.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    repository: FileRepository,
    onSave: () -> Unit = {},
) {
    // v2.0：用 derivedStateOf 包裹重计算，files 集合不变则不重算
    val files = repository.files
    val stats by remember(files) {
        derivedStateOf { repository.getStats() }
    }
    val growthMetrics by remember(files) {
        derivedStateOf { repository.computeGrowthMetrics() }
    }
    val insightText by remember(files) {
        derivedStateOf { repository.insightText }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    val displayStats = remember(stats) {
        listOf(
            StatsItem(Icons.Default.Description, primaryColor.copy(alpha = 0.12f), "${stats.totalFiles}", "已分析文件"),
            StatsItem(Icons.Default.TravelExplore, secondaryColor.copy(alpha = 0.12f), "${stats.totalEntities}", "实体关系"),
            StatsItem(Icons.Default.Psychology, tertiaryColor.copy(alpha = 0.12f), "${stats.topicsCovered}", "覆盖主题"),
        )
    }

    var focusMode by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Cross K",
                        fontWeight = FontWeight.Bold,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
                actions = {
                    IconButton(onClick = { focusMode = !focusMode }) {
                        Icon(
                            if (focusMode) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = if (focusMode) "退出专注" else "专注模式",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { navController.navigate(Screen.Insights.route) }) {
                        Icon(Icons.Default.Assessment, contentDescription = "洞察")
                    }
                    IconButton(onClick = { navController.navigate(Screen.Graph.route) }) {
                        Icon(Icons.Default.AccountTree, contentDescription = "图谱")
                    }
                    IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.Library.route) },
                containerColor = primaryColor,
            ) {
                Icon(Icons.Default.Add, contentDescription = "导入文件", tint = Color.White)
            }
        },
        bottomBar = { BottomNav(navController, "home") },
    ) { innerPadding ->
        if (files.isEmpty()) {
            EmptyKnowledgeState(
                onAction = { navController.navigate(Screen.Library.route) },
                modifier = Modifier.padding(innerPadding),
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Dimens.SpaceLg),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
        ) {
            item {
                Box {
                    LevelBadge(xp = repository.gameEngine.totalXp)
                    XpGainToast(
                        xpFlow = repository.xpBreakdown,
                        modifier = Modifier.align(Alignment.TopEnd),
                    )
                }
            }

            item {
                InsightBanner(text = insightText)
            }

            item {
                GrowthCard(metrics = growthMetrics)
            }

            item { StatsRow(stats = displayStats) }

            if (!focusMode) {
                item {
                    Row(
                        modifier = Modifier.fillParentMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "展开更多",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                        Spacer(Modifier.width(Dimens.SpaceXs))
                        Icon(
                            Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(Dimens.IconLg),
                        )
                    }
                }
            }

            item {
                AnimatedVisibility(
                    visible = !focusMode,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    DailyQuestPanel(
                        quests = repository.gameEngine.quests,
                        onQuestClick = { quest ->
                            repository.gameEngine.completeQuest(quest.id)
                            repository.gameEngine.refreshDailyQuests()
                            onSave()
                        },
                    )
                }
            }

            item {
                AnimatedVisibility(
                    visible = !focusMode,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    HeatmapChart(data = repository.heatmapData)
                }
            }

            item {
                Text(
                    text = if (focusMode) "最近文件" else "继续探索",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            items(
                if (focusMode) files.take(2) else files.take(5),
                key = { it.id },
            ) { file ->
                FileCard(
                    file = file,
                    onClick = { navController.navigate(Screen.FileDetail.createRoute(file.id)) },
                )
            }

            if (focusMode) {
                item {
                    Row(
                        modifier = Modifier.fillParentMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "专注模式中",
                            style = MaterialTheme.typography.labelSmall,
                            color = primaryColor.copy(alpha = 0.7f),
                        )
                        Spacer(Modifier.width(Dimens.SpaceXs))
                        Icon(
                            Icons.Default.ExpandLess,
                            contentDescription = "展开",
                            tint = primaryColor.copy(alpha = 0.6f),
                            modifier = Modifier.size(Dimens.IconLg),
                        )
                    }
                }
            }

            item { Spacer(Modifier.padding(Dimens.SpaceXxl)) }
        }
    }
}
