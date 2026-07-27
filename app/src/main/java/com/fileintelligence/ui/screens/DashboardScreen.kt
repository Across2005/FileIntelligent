package com.fileintelligence.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.fileintelligence.data.generateMockFiles
import com.fileintelligence.data.generateMockGrowthData
import com.fileintelligence.ui.components.BottomNav
import com.fileintelligence.ui.components.FileCard
import com.fileintelligence.ui.components.GrowthCard
import com.fileintelligence.ui.components.InsightBanner
import com.fileintelligence.ui.components.StatsItem
import com.fileintelligence.ui.components.StatsRow
import com.fileintelligence.ui.theme.BrandAccent
import com.fileintelligence.ui.theme.BrandHighlight
import com.fileintelligence.ui.theme.BrandPrimary
import com.fileintelligence.ui.theme.bgCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavController) {
    val files = generateMockFiles()
    val growthData = generateMockGrowthData()
    val stats = listOf(
        StatsItem("📄", BrandPrimary.copy(alpha = 0.15f), "47", "已分析文件"),
        StatsItem("🔗", BrandAccent.copy(alpha = 0.15f), "312", "实体关系"),
        StatsItem("💡", BrandHighlight.copy(alpha = 0.15f), "18", "新增洞察"),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "文件智析",
                        fontWeight = FontWeight.Bold,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
                actions = {
                    IconButton(onClick = { navController.navigate("insights") }) {
                        Icon(Icons.Default.Assessment, contentDescription = "洞察")
                    }
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* import */ },
                containerColor = BrandPrimary,
            ) {
                Icon(Icons.Default.Add, contentDescription = "导入文件", tint = Color.White)
            }
        },
        bottomBar = { BottomNav(navController, "home") },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                InsightBanner(
                    text = "AI 洞察：检测到 3 份新文件中的「认知架构」概念出现频率上升 40%，与上周的「深度学习」主题形成新的关联链路。"
                )
            }
            item { GrowthCard(metrics = growthData) }
            item { StatsRow(stats = stats) }
            item {
                Text(
                    text = "最近文件",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            items(files.take(5)) { file ->
                FileCard(file = file, onClick = { })
            }

            item { androidx.compose.foundation.layout.Spacer(Modifier.padding(32.dp)) }
        }
    }
}
