package com.crossk.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Handles saving/loading all app state to internal storage as JSON.
 * No external dependencies — uses org.json (Android built-in).
 */
class PersistenceManager(context: Context) {

    private val filesDir = context.filesDir
    private val dataFile = File(filesDir, "repository_state.json")

    // ── Save ──

    suspend fun save(repository: FileRepository) = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("totalXp", repository.gameEngine.totalXp)
                put("graphVisualLevel", repository.graphVisualLevel)
                put("streakCurrent", repository.gameEngine.streak.currentStreak)
                put("streakLongest", repository.gameEngine.streak.longestStreak)
                put("streakLastActive", repository.gameEngine.streak.lastActiveDate)

                // Serialize files
                val filesArray = JSONArray()
                repository.files.forEach { file ->
                    filesArray.put(serializeFileItem(file))
                }
                put("files", filesArray)
            }
            dataFile.writeText(json.toString(), Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ── Load ──

    suspend fun load(repository: FileRepository) = withContext(Dispatchers.IO) {
        try {
            if (!dataFile.exists()) return@withContext

            val json = JSONObject(dataFile.readText(Charsets.UTF_8))

            // Restore XP
            val xp = json.optInt("totalXp", 0)
            repository.gameEngine.restoreXp(xp)
            repository.restoreGraphVisualLevel(json.optInt("graphVisualLevel", 1))

            // Restore streak
            repository.gameEngine.restoreStreak(
                StreakData(
                    currentStreak = json.optInt("streakCurrent", 0),
                    longestStreak = json.optInt("streakLongest", 0),
                    lastActiveDate = json.optLong("streakLastActive", 0L),
                )
            )

            // Restore files
            val filesArray = json.optJSONArray("files") ?: return@withContext
            repository.files.clear()
            for (i in 0 until filesArray.length()) {
                val obj = filesArray.getJSONObject(i)
                repository.files.add(deserializeFileItem(obj))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ── Serialization helpers ──

    private fun serializeFileItem(file: FileItem): JSONObject {
        return JSONObject().apply {
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
        }
    }

    private fun deserializeFileItem(obj: JSONObject): FileItem {
        val entitiesArray = obj.optJSONArray("entities") ?: JSONArray()
        val entities = mutableListOf<Entity>()
        for (j in 0 until entitiesArray.length()) {
            val e = entitiesArray.getJSONObject(j)
            entities.add(
                Entity(
                    id = e.getString("id"),
                    name = e.getString("name"),
                    type = try {
                        Entity.Type.valueOf(e.getString("type"))
                    } catch (_: Exception) {
                        Entity.Type.CONCEPT
                    },
                    mentions = e.getInt("mentions"),
                    firstSeen = e.getLong("firstSeen"),
                    lastSeen = e.getLong("lastSeen"),
                )
            )
        }

        val topicsArray = obj.optJSONArray("topics") ?: JSONArray()
        val topics = (0 until topicsArray.length()).map { topicsArray.getString(it) }

        val tagsArray = obj.optJSONArray("tags") ?: JSONArray()
        val tags = (0 until tagsArray.length()).map { tagsArray.getString(it) }

        return FileItem(
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
        )
    }

    fun hasData(): Boolean = dataFile.exists() && dataFile.length() > 0
}
