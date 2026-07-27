package com.fileintelligence.data

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.fileintelligence.ai.AnalysisEngine
import java.util.UUID

class FileRepository {

    private val _files = mutableStateListOf<FileItem>()
    val files: SnapshotStateList<FileItem> get() = _files

    private val analysisEngine = AnalysisEngine()

    init {
        loadMockData()
    }

    private fun loadMockData() {
        val mocks = generateMockFilesRaw()
        _files.addAll(mocks)
    }

    fun addFile(
        name: String,
        content: String,
        extension: String,
        sizeBytes: Long,
    ): FileItem {
        val now = System.currentTimeMillis()
        val file = FileItem(
            id = UUID.randomUUID().toString(),
            name = name,
            path = "/local/$name",
            extension = extension,
            sizeBytes = sizeBytes,
            lastModified = now,
            createdAt = now,
            content = content,
        )

        // Run AI analysis
        val result = analysisEngine.analyze(content)
        val analyzed = file.copy(
            aiSummary = result.summary,
            entities = result.entities,
            topics = result.topics.map { it.name },
            importance = result.topics.firstOrNull()?.weight ?: 0.5f,
        )

        _files.add(0, analyzed)
        return analyzed
    }

    fun deleteFile(id: String) {
        _files.removeAll { it.id == id }
    }

    fun getFile(id: String): FileItem? = _files.find { it.id == id }

    fun getStats() = FileStats(
        totalFiles = _files.size,
        totalEntities = _files.sumOf { it.entities.size },
        totalConnections = _files.size * 3, // simplified
        topicsCovered = _files.flatMap { it.topics }.distinct().size,
    )
}

data class FileStats(
    val totalFiles: Int,
    val totalEntities: Int,
    val totalConnections: Int,
    val topicsCovered: Int,
)
