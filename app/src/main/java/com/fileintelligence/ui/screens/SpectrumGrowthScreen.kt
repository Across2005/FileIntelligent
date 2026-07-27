package com.fileintelligence.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.fileintelligence.data.GrowthMetric
import com.fileintelligence.data.generateMockGrowthData
import com.fileintelligence.ui.components.BottomNav
import com.fileintelligence.ui.components.GraphCanvas
import com.fileintelligence.ui.components.SpectrumChart
import com.fileintelligence.ui.components.SpectrumSeries
import com.fileintelligence.ui.components.GrowthCard
import com.fileintelligence.ui.theme.BrandAccent
import com.fileintelligence.ui.theme.BrandPrimary
import com.fileintelligence.ui.theme.BrandHighlight
import com.fileintelligence.ui.theme.TopicNlp
import com.fileintelligence.ui.theme.textSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpectrumGrowthScreen(navController: NavController) {
    val growthData = generateMockGrowthData()
    val spectrumSeries = listOf(
        SpectrumSeries("AI 基础", BrandAccent, listOf(0.15f, 0.2f, 0.35f, 0.5f, 0.55f, 0.6f, 0.7f, 0.8f, 0.8f, 0.85f, 0.9f, 0.92f)),
        SpectrumSeries("深度学习", BrandHighlight, listOf(0.05f, 0.08f, 0.1f, 0.15f, 0.18f, 0.2f, 0.3f, 0.35f, 0.4f, 0.45f, 0.5f, 0.55f)),
        SpectrumSeries("NLP", TopicNlp, listOf(0.02f, 0.03f, 0.05f, 0.06f, 0.1f, 0.12f, 0.15f, 0.12f, 0.18f, 0.2f, 0.25f, 0.2f)),
    )

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
        val scrollState = rememberScrollState()
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SpectrumChart(series = spectrumSeries)
            GrowthCard(metrics = growthData, modifier = Modifier.padding(top = 4.dp))
            GraphCanvas()
            androidx.compose.foundation.layout.Spacer(Modifier.padding(32.dp))
        }
    }
}
