package com.crossk.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.crossk.data.FileRepository
import com.crossk.ui.screens.DashboardScreen
import com.crossk.ui.screens.FileDetailScreen
import com.crossk.ui.screens.LibraryScreen
import com.crossk.ui.screens.PostcardEditorScreen
import com.crossk.ui.screens.ReaderScreen
import com.crossk.ui.screens.SettingsScreen
import com.crossk.ui.screens.SpectrumGrowthScreen

private const val NAV_ANIM_DURATION = 300
private const val NAV_ANIM_DURATION_SLOW = 400

private val enterTransition = slideInHorizontally(
    initialOffsetX = { fullWidth -> fullWidth },
    animationSpec = tween(NAV_ANIM_DURATION),
) + fadeIn(animationSpec = tween(NAV_ANIM_DURATION))

private val exitTransition = slideOutHorizontally(
    targetOffsetX = { fullWidth -> -fullWidth / 3 },
    animationSpec = tween(NAV_ANIM_DURATION),
) + fadeOut(animationSpec = tween(NAV_ANIM_DURATION / 2))

private val popEnterTransition = slideInHorizontally(
    initialOffsetX = { fullWidth -> -fullWidth / 3 },
    animationSpec = tween(NAV_ANIM_DURATION),
) + fadeIn(animationSpec = tween(NAV_ANIM_DURATION))

private val popExitTransition = slideOutHorizontally(
    targetOffsetX = { fullWidth -> fullWidth },
    animationSpec = tween(NAV_ANIM_DURATION),
) + fadeOut(animationSpec = tween(NAV_ANIM_DURATION))

private val enterTransitionVertical = slideInVertically(
    initialOffsetY = { fullHeight -> fullHeight },
    animationSpec = tween(NAV_ANIM_DURATION_SLOW),
) + fadeIn(animationSpec = tween(NAV_ANIM_DURATION))

private val popExitTransitionVertical = slideOutVertically(
    targetOffsetY = { fullHeight -> fullHeight },
    animationSpec = tween(NAV_ANIM_DURATION_SLOW),
) + fadeOut(animationSpec = tween(NAV_ANIM_DURATION))

private val fadeInOnly = fadeIn(animationSpec = tween(200)) + scaleIn(
    initialScale = 0.95f,
    animationSpec = tween(200),
)
private val fadeOutOnly = fadeOut(animationSpec = tween(150)) + scaleOut(
    targetScale = 0.95f,
    animationSpec = tween(150),
)

/**
 * v2.0 路由收敛：
 * - 删除 v1 的 Insights / Spectrum / Growth 三条同屏路由，合并为 `Insights`
 * - 内部 tab 状态由 SpectrumGrowthScreen 内部管理
 * - Graph 提升为顶级路由（带可直达的 focus）
 */
sealed class Screen(val route: String, val title: String) {
    object Home : Screen("home", "首页")
    object Library : Screen("library", "文件库")
    object Insights : Screen("insights", "洞察")
    object Graph : Screen("graph", "知识图谱")
    object GraphFocus : Screen("graph/{nodeLabel}", "知识图谱") {
        fun createRoute(nodeLabel: String) = "graph/$nodeLabel"
    }
    object Settings : Screen("settings", "设置")
    object FileDetail : Screen("file_detail/{fileId}", "文件详情") {
        fun createRoute(fileId: String) = "file_detail/$fileId"
    }
    object Reader : Screen("reader/{fileId}", "阅读器") {
        fun createRoute(fileId: String) = "reader/$fileId"
    }
    object Postcard : Screen("postcard", "明信片")
    object Onboarding : Screen("onboarding", "欢迎")
}

@Composable
fun CrossKNavGraph(
    navController: NavHostController,
    repository: FileRepository,
    isDarkTheme: Boolean,
    onToggleTheme: (Boolean) -> Unit,
    onSave: () -> Unit = {},
    shouldShowOnboarding: Boolean = false,
    onOnboardingComplete: () -> Unit = {},
) {
    LaunchedEffect(shouldShowOnboarding) {
        if (shouldShowOnboarding) {
            navController.navigate(Screen.Onboarding.route)
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        enterTransition = { enterTransition },
        exitTransition = { exitTransition },
        popEnterTransition = { popEnterTransition },
        popExitTransition = { popExitTransition },
    ) {
        composable(
            route = Screen.Home.route,
            enterTransition = { fadeInOnly },
            exitTransition = { fadeOutOnly },
            popEnterTransition = { fadeInOnly },
            popExitTransition = { fadeOutOnly },
        ) {
            DashboardScreen(
                navController = navController,
                repository = repository,
                onSave = onSave,
            )
        }

        composable(route = Screen.Library.route) {
            LibraryScreen(
                navController = navController,
                repository = repository,
                onSave = onSave,
            )
        }

        // v2.0：单路由承担历史 Insights/Spectrum/Growth
        composable(
            route = Screen.Insights.route,
            enterTransition = { fadeInOnly },
            exitTransition = { fadeOutOnly },
            popEnterTransition = { fadeInOnly },
            popExitTransition = { fadeOutOnly },
        ) {
            SpectrumGrowthScreen(navController, repository)
        }

        composable(route = Screen.Graph.route) {
            com.crossk.ui.screens.GraphScreen(navController, null, repository)
        }

        composable(
            route = Screen.GraphFocus.route,
            arguments = listOf(navArgument("nodeLabel") { type = NavType.StringType }),
        ) { backStackEntry ->
            val nodeLabel = backStackEntry.arguments?.getString("nodeLabel") ?: ""
            com.crossk.ui.screens.GraphScreen(navController, nodeLabel, repository)
        }

        composable(route = Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
                repository = repository,
            )
        }

        composable(
            route = Screen.FileDetail.route,
            arguments = listOf(navArgument("fileId") { type = NavType.StringType }),
            enterTransition = { enterTransitionVertical },
            exitTransition = { fadeOutOnly },
            popEnterTransition = { fadeInOnly },
            popExitTransition = { popExitTransitionVertical },
        ) { backStackEntry ->
            val fileId = backStackEntry.arguments?.getString("fileId") ?: return@composable
            FileDetailScreen(
                navController = navController,
                repository = repository,
                fileId = fileId,
            )
        }

        composable(
            route = Screen.Reader.route,
            arguments = listOf(navArgument("fileId") { type = NavType.StringType }),
            enterTransition = { enterTransitionVertical },
            exitTransition = { fadeOutOnly },
            popEnterTransition = { fadeInOnly },
            popExitTransition = { popExitTransitionVertical },
        ) { backStackEntry ->
            val fileId = backStackEntry.arguments?.getString("fileId") ?: return@composable
            ReaderScreen(
                navController = navController,
                repository = repository,
                fileId = fileId,
            )
        }

        composable(
            route = Screen.Postcard.route,
            enterTransition = { fadeInOnly },
            exitTransition = { fadeOutOnly },
            popEnterTransition = { fadeInOnly },
            popExitTransition = { fadeOutOnly },
        ) {
            PostcardEditorScreen(navController = navController)
        }

        composable(
            route = Screen.Onboarding.route,
            enterTransition = { fadeInOnly },
            exitTransition = { fadeOutOnly },
            popEnterTransition = { fadeInOnly },
            popExitTransition = { fadeOutOnly },
        ) {
            com.crossk.ui.screens.OnboardingScreen(
                onFinish = {
                    onOnboardingComplete()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                },
            )
        }
    }
}
