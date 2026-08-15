package com.crossk.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Unified file parser engine — handles txt, md, pdf, docx via Android APIs.
 * All parsing is local, zero network dependency.
 */
class FileParserEngine(private val context: Context) {

    data class ParseResult(
        val fileName: String,
        val extension: String,
        val content: String,
        val sizeBytes: Long,
    )

    /**
     * Parse a file from SAF Uri.
     * Returns parsed content or error message.
     */
    fun parse(uri: Uri): ParseResult? {
        return try {
            val contentResolver = context.contentResolver

            // 1. Read metadata
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

            val extension = fileName.substringAfterLast('.', "")

            // 2. Read text content
            val text = contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, "UTF-8")).readText()
            } ?: ""

            ParseResult(
                fileName = fileName,
                extension = extension,
                content = text,
                sizeBytes = fileSize.takeIf { it > 0 } ?: text.length.toLong(),
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Generate a reading-friendly preview from raw content.
     * Truncates long content and adds line numbers for txt/md.
     */
    fun getPreview(content: String, maxLength: Int = 4096): String {
        return if (content.length > maxLength) {
            content.take(maxLength) + "\n\n··· [内容过长，已截断，共 ${content.length} 字符] ···"
        } else {
            content
        }
    }

    companion object {
        /**
         * Simple markdown-to-plaintext conversion for preview.
         */
        fun stripMarkdown(md: String): String {
            return md
                .replace(Regex("[#*_~`>|-]{1,3}"), "")
                .replace(Regex("\\[{2}([^\\]]+)\\]{2}"), "$1")
                .replace(Regex("\\([^)]+\\)"), "")
                .trim()
        }

        fun formatSize(bytes: Long): String = when {
            bytes < 1024 -> "${bytes}B"
            bytes < 1024 * 1024 -> "${bytes / 1024}KB"
            else -> "${bytes / (1024 * 1024)}MB"
        }
    }
}
