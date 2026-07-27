package com.fileintelligence.ui.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.fileintelligence.ui.navigation.Screen
import com.fileintelligence.ui.theme.BrandPrimary
import com.fileintelligence.ui.theme.bgDeepest
import com.fileintelligence.ui.theme.textSecondary
import com.fileintelligence.ui.theme.textTertiary

private data class NavTab(
    val label: String,
    val route: String,
    val icon: ImageVector,
    val screen: Screen,
)

private val tabs = listOf(
    NavTab("首页", "home", Icons.Default.Home, Screen.Home),
    NavTab("文件库", "library", Icons.Default.Folder, Screen.Library),
    NavTab("洞察", "insights", Icons.Default.Lightbulb, Screen.Insights),
    NavTab("设置", "settings", Icons.Default.Settings, Screen.Settings),
)

@Composable
fun BottomNav(
    navController: NavController,
    currentRoute: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(bgDeepest.copy(alpha = 0.97f))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEach { tab ->
            val isActive = currentRoute == tab.route
            val activeColor by animateColorAsState(
                targetValue = if (isActive) BrandPrimary else textTertiary,
                animationSpec = spring(dampingRatio = 0.7f),
                label = "tabColor",
            )

            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        if (currentRoute != tab.route) {
                            navController.navigate(tab.screen.route) {
                                popUpTo(Screen.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .size(4.dp, 2.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(BrandPrimary),
                    )
                } else {
                    Box(modifier = Modifier.size(4.dp, 2.dp))
                }

                Icon(
                    imageVector = tab.icon,
                    contentDescription = tab.label,
                    tint = activeColor,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = tab.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = activeColor,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}
