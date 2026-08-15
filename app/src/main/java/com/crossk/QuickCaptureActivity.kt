package com.crossk

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.crossk.ui.components.GlassCard
import com.crossk.ui.theme.*

/**
 * Activity that receives quick-capture intents (ACTION_SEND, ACTION_PROCESS_TEXT).
 * Allows users to quickly jot down ideas from any app.
 */
class QuickCaptureActivity : ComponentActivity() {

    companion object {
        const val EXTRA_SAVED = "extra_quick_capture_saved"
        private const val MAX_INPUT_LENGTH = 50_000 // 50K chars max
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedText = extractText(intent)?.take(MAX_INPUT_LENGTH) ?: ""

        setContent {
            CrossKTheme {
                QuickCaptureContent(
                    initialText = sharedText,
                    onSave = { text ->
                        val safeText = text.take(MAX_INPUT_LENGTH)
                        if (safeText.isBlank()) return@QuickCaptureContent
                        // Add as a note/file in the repository
                        val app = try {
                            application as? CrossKApp
                        } catch (_: ClassCastException) {
                            null
                        }
                        app?.fileRepository?.addFile(
                            name = "快记_${System.currentTimeMillis() / 1000}",
                            content = safeText,
                            extension = "md",
                            sizeBytes = safeText.length.toLong(),
                        )
                        app?.fileRepository?.gameEngine?.addXpRaw(10)
                        app?.soundManager?.playXpGain()
                        app?.saveState()
                        setResult(RESULT_OK, Intent().putExtra(EXTRA_SAVED, true))
                        finish()
                    },
                    onDismiss = {
                        setResult(RESULT_CANCELED)
                        finish()
                    },
                )
            }
        }
    }

    private fun extractText(intent: Intent?): String? {
        if (intent == null) return null
        return when (intent.action) {
            Intent.ACTION_SEND -> {
                if ("text/plain" == intent.type) {
                    intent.getStringExtra(Intent.EXTRA_TEXT)
                } else null
            }
            Intent.ACTION_PROCESS_TEXT -> {
                intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT)
            }
            else -> null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickCaptureContent(
    initialText: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember(initialText) { mutableStateOf(initialText) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("快速记录", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { onSave(text) },
                        enabled = text.isNotBlank(),
                    ) {
                        Text(
                            "保存 ✨",
                            fontWeight = FontWeight.Bold,
                            color = if (text.isNotBlank()) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
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
                .padding(16.dp),
        ) {
            GlassCard {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp),
                    placeholder = { Text("记录你的想法、灵感或摘录...") },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandPrimary,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                    ),
                )
            }

            Spacer(Modifier.height(12.dp))

            // Quick tips
            Text(
                text = "💡 这段文字将自动解析实体并接入知识图谱",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
        }
    }
}
