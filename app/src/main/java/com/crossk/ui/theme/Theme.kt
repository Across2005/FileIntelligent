package com.crossk.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = BrandPrimary,
    onPrimary = Color(0xFF2D1B24),
    primaryContainer = BrandPrimaryLight,
    onPrimaryContainer = Color(0xFF2D1B24),
    secondary = BrandAccent,
    onSecondary = Color(0xFF1A1417),
    secondaryContainer = Color(0xFF2D3D2E),
    onSecondaryContainer = BrandAccent,
    tertiary = BrandHighlight,
    onTertiary = Color(0xFF2D1B24),
    tertiaryContainer = Color(0xFF3D2E15),
    onTertiaryContainer = BrandHighlight,
    background = bgDeepest,
    onBackground = textPrimary,
    surface = bgDark,
    onSurface = textPrimary,
    surfaceVariant = bgCard,
    onSurfaceVariant = textSecondary,
    outline = Color(0xFF3D3036),
    inverseSurface = textPrimary,
    inverseOnSurface = bgDeepest,
    error = BrandDanger,
    onError = Color(0xFF2D1B24),
)

private val LightColorScheme = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = Color.White,
    primaryContainer = BrandPrimaryLight,
    onPrimaryContainer = Color(0xFF2D1B24),
    secondary = BrandAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD4E8D6),
    onSecondaryContainer = Color(0xFF1A3D2E),
    tertiary = BrandHighlight,
    onTertiary = Color(0xFF2D1B24),
    tertiaryContainer = Color(0xFFFDE8C8),
    onTertiaryContainer = Color(0xFF3D2E15),
    background = lightBg,
    onBackground = lightTextPrimary,
    surface = lightSurface,
    onSurface = lightTextPrimary,
    surfaceVariant = lightElevated,
    onSurfaceVariant = lightTextSecondary,
    outline = Color(0xFFE8D8DE),
    inverseSurface = bgDeepest,
    inverseOnSurface = textPrimary,
    error = BrandDanger,
    onError = Color.White,
)

/**
 * v2.0 主题入口：
 * - 撤掉 v1 的 `object ThemeState` 全局可变单例
 * - 切换主题由调用方传入 `darkTheme: Boolean`（持久化层已替成 DataStore）
 * - SystemUI 跟随注入 windowInsetsController
 */
@Composable
fun CrossKTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalDimens provides Dimens,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = androidx.compose.material3.Typography(
                displayLarge = TextTitle,
                headlineMedium = TextTitle,
                bodyLarge = TextBody,
                bodyMedium = TextBody,
                bodySmall = TextSmall,
                labelSmall = TextCaption,
            ),
            shapes = CrossKShapes,
            content = content,
        )
    }
}
