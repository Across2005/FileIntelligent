package com.fileintelligence.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = BrandPrimary,
    onPrimary = Color.White,
    primaryContainer = BrandPrimaryLight,
    onPrimaryContainer = Color.White,
    secondary = BrandAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF0E4D5C),
    onSecondaryContainer = Color.White,
    tertiary = BrandHighlight,
    onTertiary = Color(0xFF1A1A2E),
    tertiaryContainer = Color(0xFF3D2E00),
    onTertiaryContainer = BrandHighlight,
    background = bgDeepest,
    onBackground = textPrimary,
    surface = bgDark,
    onSurface = textPrimary,
    surfaceVariant = bgCard,
    onSurfaceVariant = textSecondary,
    outline = Color(0xFF363650),
    inverseSurface = textPrimary,
    inverseOnSurface = bgDeepest,
    error = BrandDanger,
    onError = Color.White,
)

private val LightColorScheme = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = Color.White,
    primaryContainer = BrandPrimaryLight,
    onPrimaryContainer = Color.White,
    secondary = BrandAccent,
    onSecondary = Color.White,
    background = Color.White,
    onBackground = Color(0xFF111827),
    surface = Color(0xFFF9FAFB),
    onSurface = Color(0xFF111827),
    surfaceVariant = Color(0xFFF3F4F6),
    onSurfaceVariant = Color(0xFF4B5563),
    outline = Color(0xFFD1D5DB),
    error = BrandDanger,
    onError = Color.White,
)

@Composable
fun FileIntelligenceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S -> {
            val ctx = androidx.compose.ui.platform.LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

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
        content = content,
    )
}
