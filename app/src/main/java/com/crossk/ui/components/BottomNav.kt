package com.crossk.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.crossk.ui.navigation.Screen

private data class NavTab(
    val label: String,
    val route: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector,
    val screen: Screen,
)

private val tabs = listOf(
    NavTab("首页", "home", Icons.Filled.Home, Icons.Outlined.Home, Screen.Home),
    NavTab("文件库", "library", Icons.Filled.Folder, Icons.Outlined.Folder, Screen.Library),
    NavTab("洞察", "insights", Icons.Filled.Assessment, Icons.Outlined.Assessment, Screen.Insights),
    NavTab("设置", "settings", Icons.Filled.Settings, Icons.Outlined.Settings, Screen.Settings),
)

@Composable
fun BottomNav(
    navController: NavController,
    currentRoute: String,
    modifier: Modifier = Modifier,
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(surfaceColor.copy(alpha = 0.97f))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEach { tab ->
            val isActive = currentRoute == tab.route

            val animatedScale by animateFloatAsState(
                targetValue = if (isActive) 1.15f else 1f,
                animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
                label = "tabScale",
            )

            val animatedAlpha by animateFloatAsState(
                targetValue = if (isActive) 1f else 0.5f,
                animationSpec = spring(dampingRatio = 0.7f),
                label = "tabAlpha",
            )

            val bgWidth by animateDpAsState(
                targetValue = if (isActive) 48.dp else 0.dp,
                animationSpec = spring(dampingRatio = 0.7f),
                label = "tabBg",
            )

            Column(
                modifier = Modifier
                    .width(64.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable {
                        if (currentRoute != tab.route) {
                            navController.navigate(tab.screen.route) {
                                popUpTo(Screen.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    // Active background glow
                    if (isActive) {
                        Box(
                            modifier = Modifier
                                .size(bgWidth)
                                .clip(RoundedCornerShape(12.dp))
                                .background(primaryColor.copy(alpha = 0.12f)),
                        )
                    }

                    Icon(
                        imageVector = if (isActive) tab.activeIcon else tab.inactiveIcon,
                        contentDescription = tab.label,
                        tint = if (isActive) primaryColor else tertiaryColor,
                        modifier = Modifier
                            .size(22.dp)
                            .scale(animatedScale)
                            .alpha(animatedAlpha),
                    )
                }

                Text(
                    text = tab.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isActive) primaryColor else tertiaryColor,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.alpha(animatedAlpha),
                )
            }
        }
    }
}
