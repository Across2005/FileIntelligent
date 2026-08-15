package com.crossk.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Manages full backup and restore of all app data.
 * Export format: .fiba (CrossKBackup) — a zip containing:
 *   - manifest.json (metadata: version, date, stats)
 *   - files.json (all FileItems)
 *   - knowledge.json (XP, levels, quests, streaks)
 */
class BackupManager(private val context: Context) {

    companion object {
        private const val BACKUP_VERSION = 1
        private const val MANIFEST_FILE = "manifest.json"
        private const val FILES_FILE = "files.json"
        private const val KNOWLEDGE_FILE = "knowledge.json"
    }

    data class BackupManifest(
        val version: Int,
        val createdAt: Long,
        val fileCount: Int,
        val totalXp: Int,
    )

    /**
     * Export full backup to an output stream (for SAF / file picker).
     */
    suspend fun exportBackup(
        repository: FileRepository,
        outputStream: OutputStream,
    ): Result<BackupManifest> = withContext(Dispatchers.IO) {
        try {
            val manifest = BackupManifest(
                version = BACKUP_VERSION,
                createdAt = System.currentTimeMillis(),
                fileCount = repository.files.size,
                totalXp = repository.gameEngine.totalXp,
            )
            ZipOutputStream(outputStream.buffered()).use { zos ->
                // 1. Manifest
                val manifestJson = JSONObject().apply {
                    put("version", manifest.version)
                    put("createdAt", manifest.createdAt)
                    put("fileCount", manifest.fileCount)
                    put("totalXp", manifest.totalXp)
                }
                zos.putNextEntry(ZipEntry(MANIFEST_FILE))
                zos.write(manifestJson.toString(2).toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                // 2. Files
                val filesArray = JSONArray()
                repository.files.forEach { file ->
                    filesArray.put(JSONObject().apply {
                        put("id", file.id)
                        put("name", file.name)
                        put("path", file.path)
                        put("extension", file.extension)
                        put("sizeBytes", file.sizeBytes)
                        put("lastModified", file.lastModified)
                        put("createdAt", file.createdAt)
                        put("content", file.content)
                        put("aiSummary", file.aiSummary ?: "")
                        put("importance", file.importance.toDouble())
                        put("topics", JSONArray(file.topics))
                        put("tags", JSONArray(file.tags))
                        // Entities
                        val entitiesArray = JSONArray()
                        file.entities.forEach { entity ->
                            entitiesArray.put(JSONObject().apply {
                                put("id", entity.id)
                                put("name", entity.name)
                                put("type", entity.type.name)
                                put("mentions", entity.mentions)
                                put("firstSeen", entity.firstSeen)
                                put("lastSeen", entity.lastSeen)
                            })
                        }
                        put("entities", entitiesArray)
                    })
                }
                zos.putNextEntry(ZipEntry(FILES_FILE))
                zos.write(filesArray.toString(2).toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                // 3. Knowledge data
                val knowledgeJson = JSONObject().apply {
                    put("totalXp", repository.gameEngine.totalXp)
                    put("streakCurrent", repository.gameEngine.streak.currentStreak)
                    put("streakLongest", repository.gameEngine.streak.longestStreak)
                    put("streakLastActive", repository.gameEngine.streak.lastActiveDate)
                }
                zos.putNextEntry(ZipEntry(KNOWLEDGE_FILE))
                zos.write(knowledgeJson.toString(2).toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
            Result.success(manifest)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Import backup from an input stream.
     */
    suspend fun importBackup(
        repository: FileRepository,
        inputStream: InputStream,
    ): Result<BackupManifest> = withContext(Dispatchers.IO) {
        try {
            var manifest: BackupManifest? = null
            var filesJson: String? = null
            var knowledgeJson: String? = null

            // Max 50MB per backup entry to prevent Zip Bomb / OOM
            val maxEntrySize = 50 * 1024 * 1024

            ZipInputStream(inputStream.buffered()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    // Validate entry name — no path traversal
                    val name = entry.name
                    if (name.contains("..") || name.contains("/") || name.contains("\\")) {
                        zis.closeEntry()
                        entry = zis.nextEntry
                        continue
                    }

                    // Read with size limit to prevent Zip Bomb
                    val limitedStream = object : java.io.FilterInputStream(zis) {
                        private var bytesRead = 0L
                        override fun read(): Int {
                            val b = super.read()
                            if (b != -1) bytesRead++
                            if (bytesRead > maxEntrySize) throw java.io.IOException("Backup entry too large: $name")
                            return b
                        }
                        override fun read(b: ByteArray, off: Int, len: Int): Int {
                            val n = super.read(b, off, len)
                            if (n > 0) bytesRead += n
                            if (bytesRead > maxEntrySize) throw java.io.IOException("Backup entry too large: $name")
                            return n
                        }
                    }

                    val content = limitedStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    when (name) {
                        MANIFEST_FILE -> {
                            val obj = JSONObject(content)
                            manifest = BackupManifest(
                                version = obj.getInt("version"),
                                createdAt = obj.getLong("createdAt"),
                                fileCount = obj.getInt("fileCount"),
                                totalXp = obj.getInt("totalXp").coerceIn(0, 25000),
                            )
                        }
                        FILES_FILE -> filesJson = content
                        KNOWLEDGE_FILE -> knowledgeJson = content
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            val m = manifest ?: return@withContext Result.failure(Exception("Invalid backup: missing manifest"))

            // Restore files
            if (filesJson != null) {
                val filesArray = JSONArray(filesJson)

                // Validate file count to prevent abuse
                if (filesArray.length() > 10000) {
                    return@withContext Result.failure(Exception("Invalid backup: too many files"))
                }

                // Clear existing and add restored files
                repository.files.clear()
                for (i in 0 until filesArray.length()) {
                    val obj = filesArray.getJSONObject(i)
                    val entitiesArray = obj.optJSONArray("entities") ?: JSONArray()
                    val entities = mutableListOf<Entity>()
                    for (j in 0 until entitiesArray.length()) {
                        val e = entitiesArray.getJSONObject(j)
                        entities.add(Entity(
                            id = e.getString("id"),
                            name = e.getString("name"),
                            type = Entity.Type.valueOf(e.getString("type")),
                            mentions = e.getInt("mentions"),
                            firstSeen = e.getLong("firstSeen"),
                            lastSeen = e.getLong("lastSeen"),
                        ))
                    }
                    val topicsArray = obj.optJSONArray("topics") ?: JSONArray()
                    val topics = mutableListOf<String>()
                    for (j in 0 until topicsArray.length()) {
                        topics.add(topicsArray.getString(j))
                    }
                    val tagsArray = obj.optJSONArray("tags") ?: JSONArray()
                    val tags = mutableListOf<String>()
                    for (j in 0 until tagsArray.length()) {
                        tags.add(tagsArray.getString(j))
                    }
                    repository.files.add(FileItem(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        path = obj.getString("path"),
                        extension = obj.getString("extension"),
                        sizeBytes = obj.getLong("sizeBytes"),
                        lastModified = obj.getLong("lastModified"),
                        createdAt = obj.getLong("createdAt"),
                        content = obj.getString("content"),
                        aiSummary = obj.optString("aiSummary", "").ifBlank { null },
                        importance = obj.optDouble("importance", 0.0).toFloat(),
                        topics = topics,
                        tags = tags,
                        entities = entities,
                    ))
                }
            }

            // Restore knowledge data
            val kj = knowledgeJson
            if (kj != null) {
                val obj = JSONObject(kj)
                        repository.gameEngine.restoreXp(obj.optInt("totalXp", 0))
            }

            Result.success(m)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Generate a default backup filename with timestamp.
     */
    fun generateBackupFilename(): String {
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            .format(Date())
        return "文件智析_备份_$dateStr.fiba"
    }
}
