package com.crossk.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crossk.ui.theme.BrandAccent
import com.crossk.ui.theme.BrandHighlight
import com.crossk.ui.theme.BrandPrimary

/**
 * Onboarding page data.
 */
private data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val iconColor: Color,
)

private val onboardingPages = listOf(
    OnboardingPage(
        icon = Icons.Default.Upload,
        title = "导入你的知识",
        description = "支持 TXT / PDF / DOCX\n文件将自动解析，构建知识图谱",
        iconColor = BrandPrimary,
    ),
    OnboardingPage(
        icon = Icons.Default.AutoAwesome,
        title = "培养知识生命",
        description = "每颗星代表一个概念\n与之互动，让它成长进化",
        iconColor = BrandHighlight,
    ),
    OnboardingPage(
        icon = Icons.Default.Insights,
        title = "探索连接网络",
        description = "在知识图谱中发现隐含关联\n点亮相连的节点，点亮你的宇宙",
        iconColor = BrandAccent,
    ),
)

/**
 * First-launch onboarding experience.
 * Walks the user through the app's core concepts with swipeable pages.
 */
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pageCount = onboardingPages.size

    // Simple page state using a counter inside remember
    var currentPage by remember { mutableIntStateOf(0) }
    val page = onboardingPages[currentPage]

    // Animations
    val iconScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow,
        ),
        label = "iconScale",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(Modifier.weight(1f))

            // Animated icon container
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(iconScale)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                page.iconColor.copy(alpha = 0.15f),
                                page.iconColor.copy(alpha = 0.05f),
                            )
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = page.icon,
                    contentDescription = null,
                    tint = page.iconColor,
                    modifier = Modifier.size(48.dp),
                )
            }

            Spacer(Modifier.height(40.dp))

            // Title
            Text(
                text = page.title,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 26.sp,
                ),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(16.dp))

            // Description
            Text(
                text = page.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
            )

            Spacer(Modifier.weight(1f))

            // Page indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 24.dp),
            ) {
                repeat(pageCount) { index ->
                    val isActive = index == currentPage
                    val width by animateDpAsState(
                        targetValue = if (isActive) 24.dp else 8.dp,
                        animationSpec = androidx.compose.animation.core.tween(250),
                        label = "dotWidth",
                    )
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(
                                if (isActive) page.iconColor
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                            )
                    )
                }
            }

            // Action button
            val isLastPage = currentPage == pageCount - 1
            val buttonColor = if (isLastPage) BrandPrimary else page.iconColor

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(buttonColor.copy(alpha = 0.9f))
                    .clickable {
                        if (isLastPage) {
                            onFinish()
                        } else {
                            currentPage++
                        }
                    }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = if (isLastPage) "开启知识之旅" else "下一步",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}
