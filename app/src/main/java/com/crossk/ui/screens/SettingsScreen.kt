package com.crossk.ui.screens

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.crossk.data.BackupManager
import com.crossk.data.FileRepository
import com.crossk.ui.components.LevelBadge
import com.crossk.ui.theme.BrandAccent
import com.crossk.ui.theme.BrandHighlight
import com.crossk.ui.theme.BrandPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    isDarkTheme: Boolean,
    onToggleTheme: (Boolean) -> Unit,
    repository: FileRepository,
) {
    val stats = repository.getStats()
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val tertiaryColor = onSurfaceVariantColor.copy(alpha = 0.5f)
    val context = LocalContext.current

    var soundEnabled by remember { mutableStateOf(repository.soundManager?.enabled ?: true) }
    var backupStatus by remember { mutableStateOf("") }
    var showClearDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Backup launchers
    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        val result = BackupManager(context).exportBackup(repository, stream)
                        backupStatus = if (result.isSuccess) "✅ 备份成功" else "❌ 备份失败"
                    }
                } catch (e: Exception) {
                    backupStatus = "❌ 备份失败: ${e.message}"
                }
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val result = BackupManager(context).importBackup(repository, stream)
                        backupStatus = if (result.isSuccess) "✅ 恢复成功" else "❌ 恢复失败"
                    }
                } catch (e: Exception) {
                    backupStatus = "❌ 恢复失败: ${e.message}"
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = onSurfaceVariantColor,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── Level Badge (Phase 5 integration) ──
            LevelBadge(xp = repository.gameEngine.totalXp)

            // ── Knowledge Stats ──
            SectionHeader("知识统计", onSurfaceVariantColor)
            GlassSettingsCard(surfaceVariantColor) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconBox(Icons.Default.Storage, secondaryColor)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("文件库统计", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(
                            "${stats.totalFiles} 个文件 · ${stats.totalEntities} 个实体 · ${stats.topicsCovered} 个主题",
                            style = MaterialTheme.typography.bodySmall,
                            color = tertiaryColor,
                        )
                    }
                }
                HorizontalDivider(tertiaryColor)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconBox(Icons.Default.Star, BrandHighlight)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("当前等级", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(
                            "Lv.${repository.level.level} ${repository.level.title} · ${repository.gameEngine.totalXp} XP",
                            style = MaterialTheme.typography.bodySmall,
                            color = tertiaryColor,
                        )
                    }
                }
            }

            // ── Appearance ──
            SectionHeader("外观", onSurfaceVariantColor)
            GlassSettingsCard(surfaceVariantColor) {
                SettingsToggle(
                    icon = Icons.Default.Brightness6,
                    title = "深色模式",
                    subtitle = if (isDarkTheme) "当前：深色主题" else "当前：浅色主题",
                    checked = isDarkTheme,
                    onCheckedChange = onToggleTheme,
                    accentColor = primaryColor,
                    tertiaryColor = tertiaryColor,
                )
            }

            // ── Sound (Phase 7) ──
            SectionHeader("声音", onSurfaceVariantColor)
            GlassSettingsCard(surfaceVariantColor) {
                SettingsToggle(
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    title = "音效反馈",
                    subtitle = "操作提示音（解析完成、图谱连接等）",
                    checked = soundEnabled,
                    onCheckedChange = {
                        soundEnabled = it
                        repository.soundManager?.enabled = it
                    },
                    accentColor = primaryColor,
                    tertiaryColor = tertiaryColor,
                )
            }

            // ── Backup & Restore (Phase 7) ──
            SectionHeader("数据管理", onSurfaceVariantColor)
            GlassSettingsCard(surfaceVariantColor) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconBox(Icons.Default.Backup, BrandAccent)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("备份与恢复", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text("导出 .fiba 备份文件 / 从备份恢复", style = MaterialTheme.typography.bodySmall, color = tertiaryColor)
                    }
                }
                HorizontalDivider(tertiaryColor)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            backupLauncher.launch(BackupManager(context).generateBackupFilename())
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandPrimary),
                    ) {
                        Text("导出备份", fontWeight = FontWeight.Medium)
                    }
                    OutlinedButton(
                        onClick = {
                            restoreLauncher.launch(arrayOf("application/octet-stream", "*/*"))
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandAccent),
                    ) {
                        Text("恢复备份", fontWeight = FontWeight.Medium)
                    }
                }
                if (backupStatus.isNotEmpty()) {
                    Text(
                        text = backupStatus,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (backupStatus.startsWith("✅")) BrandAccent else BrandPrimary,
                        modifier = Modifier.padding(start = 16.dp, bottom = 12.dp),
                    )
                }
                // Clear all data
                HorizontalDivider(tertiaryColor)
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { showClearDialog = true }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconBox(Icons.Default.DeleteForever, MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("清空所有数据", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text("删除全部文件和知识记录", style = MaterialTheme.typography.bodySmall, color = tertiaryColor)
                    }
                }
            }

            // ── About ──
            SectionHeader("关于", onSurfaceVariantColor)
            GlassSettingsCard(surfaceVariantColor) {
                SettingsInfoRow(
                    icon = Icons.Filled.Info,
                    title = "Cross K",
                    subtitle = "版本 1.0.0 · 已编译全部功能",
                    accentColor = primaryColor,
                )
                HorizontalDivider(tertiaryColor)
                SettingsInfoRow(
                    icon = Icons.Filled.Info,
                    title = "技术栈",
                    subtitle = "Kotlin + Jetpack Compose + Material 3",
                    accentColor = primaryColor,
                )
                HorizontalDivider(tertiaryColor)
                SettingsInfoRow(
                    icon = Icons.Default.Psychology,
                    title = "覆盖功能",
                    subtitle = "Phase 1-7 全部完成",
                    accentColor = BrandHighlight,
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    // Confirm clear data dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清空所有数据？", fontWeight = FontWeight.Bold) },
            text = { Text("此操作将删除全部文件、知识记录和成长数据。建议先导出备份。此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        repository.files.forEach { repository.deleteFile(it.id) }
                        repository.gameEngine.restoreXp(0)
                        repository.gameEngine.refreshDailyQuests()
                        scope.launch { repository.saveAll() }
                        showClearDialog = false
                        backupStatus = "✅ 已清空所有数据"
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("确认清空", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("取消")
                }
            },
        )
    }
}

@Composable
private fun SectionHeader(title: String, onSurfaceVariantColor: Color) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = onSurfaceVariantColor,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp),
    )
}

@Composable
private fun GlassSettingsCard(surfaceVariantColor: Color, content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceVariantColor),
    ) {
        content()
    }
}

@Composable
private fun SettingsToggle(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accentColor: Color,
    tertiaryColor: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBox(icon, accentColor)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = tertiaryColor)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = accentColor,
                checkedTrackColor = accentColor.copy(alpha = 0.3f),
            ),
        )
    }
}

@Composable
private fun SettingsInfoRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBox(icon, accentColor)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = accentColor.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun IconBox(icon: ImageVector, color: Color) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun HorizontalDivider(tertiaryColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .padding(horizontal = 16.dp)
            .background(tertiaryColor.copy(alpha = 0.2f)),
    )
}
