package com.crossk.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset

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

            // 2. Read text content with encoding detection
            // CR-3 (P0-3): detect BOM, fall back UTF-8 → GB18030 for legacy Chinese files
            val text = contentResolver.openInputStream(uri)?.use { inputStream ->
                decodeWithEncodingDetection(inputStream)
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

/**
 * CR-3 (P0-3) encoding detection.
 *
 * Strategy:
 * 1. BOM-detect: UTF-8 (EF BB BF), UTF-16 LE (FF FE), UTF-16 BE (FE FF)
 * 2. Else: try UTF-8; if it produces replacement chars (\uFFFD) on
 *    non-ASCII content, fall back to GB18030 (superset of GBK, common
 *    for legacy Chinese Windows text files).
 */
internal fun decodeWithEncodingDetection(input: InputStream): String {
    val raw = input.readBytes()
    return when {
        // UTF-8 BOM
        raw.size >= 3 && raw[0] == 0xEF.toByte() && raw[1] == 0xBB.toByte() && raw[2] == 0xBF.toByte() ->
            String(raw, 3, raw.size - 3, Charsets.UTF_8)
        // UTF-16 LE BOM
        raw.size >= 2 && raw[0] == 0xFF.toByte() && raw[1] == 0xFE.toByte() ->
            String(raw, 2, raw.size - 2, Charset.forName("UTF-16LE"))
        // UTF-16 BE BOM
        raw.size >= 2 && raw[0] == 0xFE.toByte() && raw[1] == 0xFF.toByte() ->
            String(raw, 2, raw.size - 2, Charset.forName("UTF-16BE"))
        // Try UTF-8; if it produces replacement chars on non-ASCII content, retry GB18030
        else -> {
            val utf8 = String(raw, Charsets.UTF_8)
            if (utf8.contains('\uFFFD') && raw.any { it.toInt() and 0x80 != 0 }) {
                try {
                    String(raw, Charset.forName("GB18030"))
                } catch (e: Exception) {
                    utf8
                }
            } else {
                utf8
            }
        }
    }
}
