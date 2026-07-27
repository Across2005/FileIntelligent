package com.fileintelligence.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.fileintelligence.data.FileRepository
import com.fileintelligence.ui.components.BottomNav
import com.fileintelligence.ui.components.FileCard
import com.fileintelligence.ui.theme.BrandPrimary
import com.fileintelligence.ui.theme.textSecondary
import com.fileintelligence.ui.theme.textTertiary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    navController: NavController,
    repository: FileRepository,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val files = repository.files
    var searchQuery by remember { mutableStateOf("") }

    val filteredFiles = if (searchQuery.isBlank()) files
    else files.filter { it.name.contains(searchQuery, ignoreCase = true) }

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "正在导入文件...",
                    duration = SnackbarDuration.Short,
                )
                importFile(context, repository, uri)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("文件库", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
                actions = {
                    IconButton(onClick = { /* toggle search */ }) {
                        Icon(Icons.Default.Search, contentDescription = "搜索")
                    }
                },
            )
        },
        floatingActionButton = {
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
                containerColor = BrandPrimary,
            ) {
                Icon(Icons.Default.Add, contentDescription = "导入文件", tint = Color.White)
            }
        },
        bottomBar = { BottomNav(navController, "library") },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        if (filteredFiles.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📂", fontSize = MaterialTheme.typography.displayLarge.fontSize)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "还没有文件",
                        style = MaterialTheme.typography.titleMedium,
                        color = textSecondary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "点击右下角 + 导入文件",
                        style = MaterialTheme.typography.bodySmall,
                        color = textTertiary,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    if (searchQuery.isNotBlank()) {
                        Text(
                            "搜索: \"$searchQuery\" — ${filteredFiles.size} 个结果",
                            style = MaterialTheme.typography.labelSmall,
                            color = textTertiary,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }
                }
                items(filteredFiles, key = { it.id }) { file ->
                    var deleted by remember { mutableStateOf(false) }
                    if (!deleted) {
                        FileCard(
                            file = file,
                            onClick = { /* preview */ },
                            onDelete = {
                                val name = file.name
                                repository.deleteFile(file.id)
                                deleted = true
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = "已删除「$name」",
                                        actionLabel = "撤销",
                                        duration = SnackbarDuration.Short,
                                    )
                                }
                            },
                        )
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

private suspend fun importFile(context: Context, repository: FileRepository, uri: Uri) {
    withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver

            // Get file name
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

            // Read content
            val text = contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, "UTF-8")).readText()
            } ?: ""

            val extension = fileName.substringAfterLast('.', "")

            // Add to repository (AI analysis runs automatically)
            repository.addFile(
                name = fileName,
                content = text,
                extension = extension,
                sizeBytes = fileSize.takeIf { it > 0 } ?: text.length.toLong(),
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
