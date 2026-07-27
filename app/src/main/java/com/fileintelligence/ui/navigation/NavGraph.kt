package com.fileintelligence.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.fileintelligence.ui.screens.DashboardScreen
import com.fileintelligence.ui.screens.LibraryScreen
import com.fileintelligence.ui.screens.SpectrumGrowthScreen

sealed class Screen(val route: String, val title: String) {
    object Home : Screen("home", "首页")
    object Library : Screen("library", "文件库")
    object Insights : Screen("insights", "洞察")
    object Spectrum : Screen("spectrum", "发展光谱")
    object Growth : Screen("growth", "成长曲线")
    object Graph : Screen("graph", "知识图谱")
}

@Composable
fun FileIntelligenceNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
    ) {
        composable(Screen.Home.route) { DashboardScreen(navController) }
        composable(Screen.Library.route) { LibraryScreen(navController) }
        composable(Screen.Insights.route) { SpectrumGrowthScreen(navController) }
        composable(Screen.Spectrum.route) { SpectrumGrowthScreen(navController) }
        composable(Screen.Growth.route) { SpectrumGrowthScreen(navController) }
        composable(Screen.Graph.route) { com.fileintelligence.ui.screens.GraphScreen(navController) }
    }
}
