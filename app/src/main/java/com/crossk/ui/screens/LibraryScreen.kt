package com.crossk.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.crossk.ai.AnalysisStage
import com.crossk.data.FileRepository
import com.crossk.ui.components.*
import com.crossk.ui.navigation.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    navController: NavController,
    repository: FileRepository,
    onSave: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val files = repository.files
    var searchQuery by remember { mutableStateOf("") }

    // Search debounce
    var debouncedQuery by remember { mutableStateOf("") }
    LaunchedEffect(searchQuery) {
        delay(300) // 300ms debounce
        debouncedQuery = searchQuery
    }

    // Import state
    var isImporting by remember { mutableStateOf(false) }
    var importStage by remember { mutableStateOf(AnalysisStage.READING) }
    var importingFileName by remember { mutableStateOf("") }

    // Multi-select batch delete state
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var showBatchDeleteDialog by remember { mutableStateOf(false) }

    // Advanced filtering state
    var extensionFilter by remember { mutableStateOf<String?>(null) }
    val availableExtensions = remember(files) {
        files.map { it.extension.lowercase() }.distinct().sorted()
    }

    val filteredFiles = files.filter { file ->
        // Text search filter
        val matchesQuery = if (debouncedQuery.isBlank()) true
        else file.name.contains(debouncedQuery, ignoreCase = true) ||
                file.topics.any { t -> t.contains(debouncedQuery, ignoreCase = true) }
        // Extension filter
        val matchesExtension = extensionFilter == null || file.extension.equals(extensionFilter, ignoreCase = true)
        matchesQuery && matchesExtension
    }

    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                importFileWithProgress(context, repository, uri,
                    onStart = { name ->
                        importingFileName = name
                        isImporting = true
                        importStage = AnalysisStage.READING
                    },
                    onStageChange = { stage ->
                        importStage = stage
                    },
                    onComplete = {
                        isImporting = false
                        onSave()
                    },
                    onError = { msg ->
                        scope.launch {
                            snackbarHostState.showSnackbar(msg)
                        }
                    },
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                if (isSelectionMode) {
                    TopAppBar(
                        title = { Text("已选 ${selectedIds.size} 项", fontWeight = FontWeight.Bold) },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = MaterialTheme.colorScheme.onBackground,
                        ),
                        navigationIcon = {
                            IconButton(onClick = {
                                isSelectionMode = false
                                selectedIds = emptySet()
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "退出选择", tint = onSurfaceVariantColor)
                            }
                        },
                        actions = {
                            if (selectedIds.isNotEmpty()) {
                                IconButton(onClick = { showBatchDeleteDialog = true }) {
                                    Icon(Icons.Default.Delete, contentDescription = "批量删除", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        },
                    )
                } else {
                    TopAppBar(
                        title = { Text("文件库", fontWeight = FontWeight.Bold) },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = MaterialTheme.colorScheme.onBackground,
                        ),
                        actions = {
                            IconButton(onClick = { /* search inline below */ }) {
                                Icon(Icons.Default.Search, contentDescription = "搜索")
                            }
                            IconButton(onClick = {
                                isSelectionMode = true
                                selectedIds = emptySet()
                            }) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "多选", tint = onSurfaceVariantColor)
                            }
                        },
                    )
                }
            },
            floatingActionButton = {
                if (!isSelectionMode && !isImporting) {
                    FloatingActionButton(
                        onClick = {
                            filePickerLauncher.launch(arrayOf(
                                "text/*",
                                "application/pdf",
                                "application/msword",
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                "application/octet-stream",
                            ))
                        },
                        containerColor = primaryColor,
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "导入文件", tint = Color.White)
                    }
                }
            },
            bottomBar = { BottomNav(navController, "library") },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
            ) {
                // ── Search Bar ──
                if (!isSelectionMode) {
                    SearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        modifier = Modifier.padding(bottom = 4.dp),
                    )

                    // ── Filter Bar ──
                    if (availableExtensions.isNotEmpty()) {
                        FilterBar(
                            availableExtensions = availableExtensions,
                            selectedExtension = extensionFilter,
                            onFilterChange = { extensionFilter = it },
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }

                // ── Content Area ──
                if (filteredFiles.isEmpty() && !isImporting) {
                    if (debouncedQuery.isNotBlank()) {
                        EmptySearchState(
                            query = debouncedQuery,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        EmptyImportState(
                            onImport = {
                                filePickerLauncher.launch(arrayOf(
                                    "text/*",
                                    "application/pdf",
                                    "application/msword",
                                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                ))
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp),
                    ) {
                        // ── Import progress card ──
                        if (isImporting) {
                            item {
                                ImportProgressCard(
                                    fileName = importingFileName,
                                    stage = importStage,
                                )
                            }
                        }

                        // ── Search results header ──
                        if (debouncedQuery.isNotBlank()) {
                            item {
                                Text(
                                    "搜索: \"$debouncedQuery\" — ${filteredFiles.size} 个文件",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = onSurfaceVariantColor.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(vertical = 4.dp),
                                )
                            }
                        }

                        // ── File list ──
                        items(filteredFiles, key = { it.id }) { file ->
                            val isSelected = file.id in selectedIds
                            FileCard(
                                file = file,
                                isSelected = isSelected,
                                selectionToggle = if (isSelectionMode) {
                                    {
                                        selectedIds = if (isSelected) selectedIds - file.id
                                        else selectedIds + file.id
                                    }
                                } else null,
                                onClick = {
                                    if (isSelectionMode) {
                                        selectedIds = if (isSelected) selectedIds - file.id
                                        else selectedIds + file.id
                                    } else {
                                        navController.navigate(Screen.FileDetail.createRoute(file.id))
                                    }
                                },
                                onDelete = {
                                    val deletedFile = file
                                    repository.deleteFile(file.id)
                                    onSave()
                                    scope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = "已删除「${deletedFile.name}」",
                                            actionLabel = "撤销",
                                            duration = SnackbarDuration.Short,
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            repository.addFile(
                                                name = deletedFile.name,
                                                content = deletedFile.content,
                                                extension = deletedFile.extension,
                                                sizeBytes = deletedFile.sizeBytes,
                                            )
                                            onSave()
                                        }
                                    }
                                },
                            )
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }

        // Batch delete confirmation dialog
        if (showBatchDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showBatchDeleteDialog = false },
                title = { Text("批量删除 ${selectedIds.size} 项？", fontWeight = FontWeight.Bold) },
                text = { Text("选中的文件和知识记录将被永久删除。此操作不可撤销。") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val count = selectedIds.size
                            selectedIds.forEach { repository.deleteFile(it) }
                            onSave()
                            showBatchDeleteDialog = false
                            isSelectionMode = false
                            selectedIds = emptySet()
                            scope.launch { snackbarHostState.showSnackbar("已删除 $count 项") }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) {
                        Text("确认删除", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBatchDeleteDialog = false }) {
                        Text("取消")
                    }
                },
            )
        }
    }
}

/**
 * Search bar with animated focus state.
 */
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (query.isEmpty()) "搜索文件、主题..." else query,
            style = MaterialTheme.typography.bodyMedium,
            color = if (query.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (query.isNotEmpty()) {
            Icon(
                Icons.Default.Close,
                contentDescription = "清除",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .size(18.dp)
                    .clickable { onQueryChange("") },
            )
        }
    }
}

/**
 * Horizontal scrollable filter chips bar for file type filtering.
 */
@Composable
private fun FilterBar(
    availableExtensions: List<String>,
    selectedExtension: String?,
    onFilterChange: (String?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // "All" chip
        FilterChip(
            label = "全部",
            isSelected = selectedExtension == null,
            onClick = { onFilterChange(null) },
        )
        // Extension chips
        availableExtensions.forEach { ext ->
            FilterChip(
                label = ext.uppercase(),
                isSelected = selectedExtension == ext,
                onClick = { onFilterChange(if (selectedExtension == ext) null else ext) },
            )
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) primaryColor.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .border(
                width = 1.dp,
                color = if (isSelected) primaryColor.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) primaryColor
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
        )
    }
}

/**
 * Empty state with file import drop zone feel.
 */
@Composable
private fun EmptyImportState(
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Upload icon area
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(24.dp),
                )
                .clickable { onImport() }
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Upload,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp),
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = "导入第一份知识",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "支持 TXT / MD / PDF / DOCX\n导入后将自动分析实体与关系",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(24.dp))

        // Action button
        FloatingActionButton(
            onClick = onImport,
            containerColor = MaterialTheme.colorScheme.primary,
        ) {
            Icon(Icons.Default.Add, contentDescription = "选择文件", tint = Color.White)
        }
    }
}

/**
 * Import progress card showing analysis pipeline stages.
 */
@Composable
private fun ImportProgressCard(
    fileName: String,
    stage: AnalysisStage,
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.TextSnippet,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = fileName.ifEmpty { "正在导入..." },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(12.dp))

            // Pipeline stage indicators
            AnalysisStage.entries.filter { it != AnalysisStage.DONE }.forEach { s ->
                val isActive = s == stage
                val isPast = AnalysisStage.entries.indexOf(s) < AnalysisStage.entries.indexOf(stage)

                Row(
                    modifier = Modifier.padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.size(8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isPast) {
                            // Completed
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(primaryColor, RoundedCornerShape(999.dp)),
                            )
                        } else if (isActive) {
                            // Active — pulsing dot
                            PulsingDot(color = primaryColor)
                        } else {
                            // Pending
                            Box(
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                                        RoundedCornerShape(999.dp),
                                    )
                                    .size(6.dp),
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = s.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            isPast -> primaryColor.copy(alpha = 0.7f)
                            isActive -> MaterialTheme.colorScheme.onSurface
                            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        },
                        fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

/**
 * Small pulsing dot animation for active pipeline stage.
 */
@Composable
private fun PulsingDot(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(800),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dotPulse",
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(800),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dotAlpha",
    )

    Box(
        modifier = Modifier
            .size(12.dp * scale)
            .background(color.copy(alpha = alpha * 0.8f), RoundedCornerShape(999.dp)),
    )
}

/**
 * Import file with real progress from AnalysisEngine.analyzeWithProgress.
 * v2.0: 撤掉假 delay 进度，改用 onStageChange 真实回调。
 */
private suspend fun importFileWithProgress(
    context: Context,
    repository: FileRepository,
    uri: Uri,
    onStart: (String) -> Unit,
    onStageChange: (AnalysisStage) -> Unit,
    onComplete: () -> Unit,
    onError: (String) -> Unit = {},
) {
    try {
        val contentResolver = context.contentResolver

        var fileName = "unknown"
        var fileSize = 0L
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex >= 0) fileName = cursor.getString(nameIndex) ?: "unknown"
                if (sizeIndex >= 0) fileSize = cursor.getLong(sizeIndex)
            }
        }

        onStart(fileName)
        onStageChange(AnalysisStage.READING)

        // 读取 + 解析（IO 线程）
        val (text, extension) = withContext(Dispatchers.IO) {
            val t = contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, "UTF-8")).readText()
            } ?: ""
            val ext = fileName.substringAfterLast('.', "")
            t to ext
        }

        onStageChange(AnalysisStage.TOKENIZING)

        // v2.0：真实进度回调
        val result = repository.addFileAsync(
            name = fileName,
            content = text,
            extension = extension,
            sizeBytes = fileSize.takeIf { it > 0 } ?: text.length.toLong(),
            onStageChange = onStageChange,
        )

        when (result) {
            is com.crossk.data.RepoResult.Ok -> {
                onStageChange(AnalysisStage.DONE)
            }
            is com.crossk.data.RepoResult.Err -> {
                onError(result.message)
            }
        }
    } catch (e: Exception) {
        onError(e.message ?: "导入失败")
    } finally {
        onComplete()
    }
}
