package com.fileintelligence.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.fileintelligence.ui.navigation.Screen
import com.fileintelligence.ui.theme.BrandPrimary
import com.fileintelligence.ui.theme.bgCard
import com.fileintelligence.ui.theme.textSecondary
import com.fileintelligence.ui.theme.textTertiary

@Composable
fun BottomNav(
    navController: NavController,
    currentRoute: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Color(0xFF0F0F1A).copy(alpha = 0.95f))
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NavItem("🏠\n首页", "home", currentRoute) { navController.navigate(Screen.Home.route) { popUpTo(Screen.Home.route) { inclusive = true } } }
        NavItem("📁\n文件库", "library", currentRoute) { navController.navigate(Screen.Library.route) { popUpTo(Screen.Home.route) } }
        NavItem("🔬\n洞察", "insights", currentRoute) { navController.navigate(Screen.Insights.route) { popUpTo(Screen.Home.route) } }
        NavItem("⚙️\n设置", "settings", currentRoute) { }
    }
}

@Composable
private fun NavItem(
    label: String,
    route: String,
    currentRoute: String,
    onClick: () -> Unit,
) {
    val isActive = currentRoute == route
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = if (isActive) BrandPrimary else textTertiary,
        maxLines = 2,
        modifier = Modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}
