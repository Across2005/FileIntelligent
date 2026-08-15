package com.crossk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.crossk.data.PreferenceManager
import com.crossk.data.ThemePreferences
import com.crossk.ui.MainViewModel
import com.crossk.ui.navigation.CrossKNavGraph
import com.crossk.ui.screens.SplashScreen
import com.crossk.ui.theme.CrossKTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val preferenceManager = PreferenceManager(this)
        val themePreferences = ThemePreferences(this)

        setContent {
            // v2.0 主题：跟随系统 / 用户手动
            val useSystemTheme by themePreferences.useSystemTheme.collectAsState(initial = true)
            val savedDarkMode by themePreferences.darkMode.collectAsState(initial = true)
            val systemDark = isSystemInDarkTheme()
            val effectiveDark = if (useSystemTheme) systemDark else savedDarkMode

            CrossKTheme(darkTheme = effectiveDark) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var showSplash by remember { mutableStateOf(true) }
                    var showOnboarding by remember { mutableStateOf(!preferenceManager.onboardingCompleted) }

                    AnimatedContent(
                        targetState = showSplash,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(500)) togetherWith
                                fadeOut(animationSpec = tween(300))
                        },
                        label = "splashTransition",
                    ) { isSplash ->
                        if (isSplash) {
                            SplashScreen(
                                onSplashComplete = { showSplash = false },
                            )
                        } else {
                            val mainViewModel: MainViewModel = viewModel()
                            val navController = rememberNavController()
                            CrossKNavGraph(
                                navController = navController,
                                repository = mainViewModel.repository,
                                isDarkTheme = effectiveDark,
                                onToggleTheme = { newDark ->
                                    // 切换时关闭"跟随系统"
                                    lifecycleScope.launch {
                                        themePreferences.setUseSystemTheme(false)
                                        themePreferences.setDarkMode(newDark)
                                    }
                                },
                                onSave = { mainViewModel.save() },
                                shouldShowOnboarding = showOnboarding,
                                onOnboardingComplete = {
                                    preferenceManager.onboardingCompleted = true
                                    showOnboarding = false
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
