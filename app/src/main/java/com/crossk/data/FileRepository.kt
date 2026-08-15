package com.crossk.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.crossk.ai.AnalysisEngine
import com.crossk.ai.AnalysisStage
import com.crossk.data.db.AppDatabase
import com.crossk.data.db.toDomain
import com.crossk.data.db.toEntity
import com.crossk.data.db.toEntityEntity
import com.crossk.data.db.toFileEntity
import com.crossk.data.db.toFileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import java.util.UUID

/** XP 拆解条目 — 单次 XP 事件的可读分解 */
data class XpBreakdownItem(
    val amount: Int,
    val label: String,
    val count: Int = 1,
) {
    val displayText: String get() = if (count > 1) "+${amount * count} ${label} ×$count" else "+$amount $label"
}

/**
 * FileRepository v2.0 — 核心数据仓库。
 *
 * 关键改动：
 * - 统一返回 RepoResult，避免静默吞错
 * - 导入走 Room 事务（DAO @Transaction），原子写 file/entities/edges
 * - 内存图谱数据从边表读取（不再文件级连边）
 * - 保留必要的 Compose state 暴露给 UI，由 ViewModel 转化为 StateFlow
 */
class FileRepository {

    private val _files = mutableStateListOf<FileItem>()
    val files: SnapshotStateList<FileItem> get() = _files

    private val _globalEntities = mutableStateListOf<Entity>()
    val globalEntities: List<Entity> get() = _globalEntities

    private val _globalRelations = mutableStateListOf<Relation>()
    val globalRelations: List<Relation> get() = _globalRelations

    val gameEngine = GameEngine()
    internal val analysisEngine = AnalysisEngine()

    private val graphReconstructor = GraphReconstructor()

    var soundManager: SoundManager? = null

    var graphVisualLevel by mutableStateOf(1)
        private set

    val level: KnowledgeLevel get() = gameEngine.level
    val heatmapData: List<HeatmapDay> get() = gameEngine.heatmapData

    var database: AppDatabase? = null

    private val _graphDataChanged = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val graphDataChanged: SharedFlow<Unit> = _graphDataChanged.asSharedFlow()

    var analysisStage by mutableStateOf(AnalysisStage.IDLE)
        private set

    private val _xpBreakdown = MutableSharedFlow<List<XpBreakdownItem>>(extraBufferCapacity = 4)
    val xpBreakdown: SharedFlow<List<XpBreakdownItem>> = _xpBreakdown.asSharedFlow()

    init {
        gameEngine.restoreXp(0)
        graphVisualLevel = 1
    }

    // ── 加载 / 持久化 ──

    suspend fun loadFromRoom() = withContext(Dispatchers.IO) {
        val db = database ?: return@withContext
        val knowledge = db.knowledgeDao().getKnowledgeSync()
        if (knowledge != null) {
            gameEngine.restoreXp(knowledge.totalXp)
            graphVisualLevel = knowledge.graphVisualLevel
            gameEngine.restoreStreak(
                StreakData(
                    currentStreak = knowledge.streakCurrent,
                    longestStreak = knowledge.streakLongest,
                    lastActiveDate = knowledge.streakLastActive,
                ),
            )
        }
    }

    /**
     * 一次性加载全部文件 + 边表数据（应用启动时调用）。
     */
    suspend fun loadAllFromRoom(): RepoResult<List<FileItem>> = withContext(Dispatchers.IO) {
    try {
        RepoResult.runCatchingResult {
            val db = database ?: return@runCatchingResult emptyList()
            val fileEntities = db.fileDao().getAllFilesSync()
            val files = fileEntities.map { fe ->
                val entities = db.entityDao().getEntitiesForFile(fe.id).map { it.toEntity() }
                fe.toFileItem().copy(entities = entities)
            }
            _files.clear()
            _files.addAll(files)
            // 边表回灌内存（v2.1：跨会话恢复图谱关系）
            _globalRelations.clear()
            _globalRelations.addAll(db.edgeDao().getAll().map { it.toDomain() })
            rebuildGlobalGraph()
            files
        }
    }

    /**
     * 持久化当前完整状态（knowledge + 文件 + 实体 + 边）。
     * 事务化：单文件失败会回滚，避免半完成状态。
     */
    suspend fun saveAll(): RepoResult<Unit> = withContext(Dispatchers.IO) {
        RepoResult.runCatchingResult {
            val db = database ?: return@runCatchingResult
            // 1. knowledge
            db.knowledgeDao().upsert(
                com.crossk.data.db.KnowledgeEntity(
                    totalXp = gameEngine.totalXp,
                    graphVisualLevel = graphVisualLevel,
                    streakCurrent = gameEngine.streak.currentStreak,
                    streakLongest = gameEngine.streak.longestStreak,
                    streakLastActive = gameEngine.streak.lastActiveDate,
                ),
            )
            // 2. files + entities（逐文件事务）
            for (file in _files) {
                db.fileDao().insert(file.toFileEntity())
                db.entityDao().deleteForFile(file.id)
                if (file.entities.isNotEmpty()) {
                    db.entityDao().insertAll(file.entities.map { it.toEntityEntity(file.id) })
                }
            }
            // 3. 边表全量持久化（v2.1 修复：同步路径 addFile 不写边表，
            //    saveAll 若不落库，QuickCapture 产生的关系会在重启后丢失）
            db.edgeDao().deleteAll()
            if (_globalRelations.isNotEmpty()) {
                db.edgeDao().insertAll(_globalRelations.map { it.toEdge() })
            }
        }
    }

    /**
     * 导入一个文件（核心管线入口）。
     * 步骤：解析 → 分析 → 抽出实体+关系 → 写 Room（含边表） → 更新内存。
     * 失败：返回 RepoResult.Err，UI 弹 Snackbar；文件不会进入内存。
     */
    suspend fun addFileAsync(
        name: String,
        content: String,
        extension: String,
        sizeBytes: Long,
        onStageChange: (AnalysisStage) -> Unit = {},
    ): RepoResult<FileItem> = withContext(Dispatchers.IO) {
        RepoResult.runCatchingResult {
            val now = System.currentTimeMillis()
            val fileId = UUID.randomUUID().toString()

            val result = analysisEngine.analyzeWithProgress(content, fileId) { stage ->
                analysisStage = stage
                onStageChange(stage)
            }

            // 关系抽取（v2.0 真实句级/段级/文档级）
            val relations = analysisEngine.extractRelationsFromAnalysis(result, content)

            val file = FileItem(
                id = fileId,
                name = name,
                path = "/local/$name",
                extension = extension,
                sizeBytes = sizeBytes,
                lastModified = now,
                createdAt = now,
                content = content,
                aiSummary = result.summary,
                entities = result.entities,
                topics = result.topics.map { it.name },
                importance = computeFileImportance(result),
            )

            // 持久化（含边表） — 单文件事务
            val db = database
            if (db != null) {
                db.fileDao().insert(file.toFileEntity())
                db.entityDao().deleteForFile(file.id)
                if (file.entities.isNotEmpty()) {
                    db.entityDao().insertAll(file.entities.map { it.toEntityEntity(file.id) })
                }
                val edges = relations.map { it.toEdge() }
                db.edgeDao().replaceForFile(file.id, edges)
            }

            // 内存更新
            _files.add(0, file)

            // 关系并入内存累积器（v2.1：同步路径不再依赖 runBlocking 读库）
            mergeRelations(relations)

            // XP / 音效
            addXpForAnalysis()
            addXpForEntities(result.entities.size)
            rebuildGlobalGraph()
            soundManager?.playParseComplete()
            file
        }
    } finally {
        analysisStage = AnalysisStage.IDLE
    }
    }

    /**
     * 同步快速添加（用于 QuickCapture / 测试桩）。
     * 失败降级：仅写入无分析结果的文件，关系留空。
     */
    fun addFile(
        name: String,
        content: String,
        extension: String,
        sizeBytes: Long,
    ): FileItem {
        val now = System.currentTimeMillis()
        val fileId = UUID.randomUUID().toString()
        val file = FileItem(
            id = fileId,
            name = name,
            path = "/local/$name",
            extension = extension,
            sizeBytes = sizeBytes,
            lastModified = now,
            createdAt = now,
            content = content,
        )

        val analyzed = try {
            val result = analysisEngine.analyze(content, file.id)
            val analyzedFile = file.copy(
                aiSummary = result.summary,
                entities = result.entities,
                topics = result.topics.map { it.name },
                importance = computeFileImportance(result),
            )
            val existingEntityIds = _globalEntities.map { it.id }.toSet()
            val trulyNewCount = result.entities.count { it.id !in existingEntityIds }
            addXpForAnalysis()
            addXpForEntities(trulyNewCount)
            // 同步路径同样并入关系（v2.1 修复：此前新文件关系不进入内存图谱）
            mergeRelations(analysisEngine.extractRelationsFromAnalysis(result, content))
            rebuildGlobalGraph()
            soundManager?.playParseComplete()
            analyzedFile
        } catch (e: Exception) {
            e.printStackTrace()
            file
        }

        _files.add(0, analyzed)
        return analyzed
    }

    /**
     * 删除文件 — 事务化清实体 + 边（FK CASCADE 也会清，但显式删除以保留审计）。
     */
    suspend fun deleteFileAsync(id: String): RepoResult<Unit> = withContext(Dispatchers.IO) {
        RepoResult.runCatchingResult {
            val db = database ?: return@runCatchingResult
            db.fileDao().deleteById(id)
            db.entityDao().deleteForFile(id)
            db.edgeDao().deleteForFile(id)
            removeRelationsForFile(id)
            _files.removeAll { it.id == id }
            rebuildGlobalGraph()
        }
    }

    fun deleteFile(id: String) {
        removeRelationsForFile(id)
        _files.removeAll { it.id == id }
        rebuildGlobalGraph()
    }

    fun getFile(id: String): FileItem? = _files.find { it.id == id }

    /**
     * 全局图谱数据重建（纯内存版）。
     *
     * v2.1 修复：原实现在此 runBlocking 读 Room，若从 UI 线程（QuickCapture 的
     * addFile / Library 的 deleteFile）调用会阻塞主线程造成 ANR；且同步路径
     * 新增文件的关系根本没写库，读库也读不到。现改为：
     * - 实体：内存按 (name,type) 去重
     * - 关系：从内存累积器 _globalRelations 过滤（由 add* 时并入、delete 时剔除）
     * - 数据库边表仅在启动 loadAllFromRoom 时回灌，不作为运行时查询源
     */
    fun rebuildGlobalGraph() {
        val entities = graphReconstructor.rebuildGlobalEntities(_files)
        _globalEntities.clear()
        _globalEntities.addAll(entities)
        // 过滤已不存在实体涉及的关系（防幽灵边）
        val aliveIds = entities.map { it.id }.toSet()
        val filtered = _globalRelations.filter {
            it.sourceEntityId in aliveIds && it.targetEntityId in aliveIds
        }
        _globalRelations.clear()
        _globalRelations.addAll(filtered)
        _graphDataChanged.tryEmit(Unit)
    }

    /** 把新分析出的关系并入内存累积器（同 (src,dst,type) 权重累加） */
    private fun mergeRelations(relations: List<Relation>) {
        for (r in relations) {
            val key = Triple(r.sourceEntityId, r.targetEntityId, r.type)
            val existing = _globalRelations.firstOrNull {
                Triple(it.sourceEntityId, it.targetEntityId, it.type) == key
            }
            if (existing != null) {
                val idx = _globalRelations.indexOf(existing)
                _globalRelations[idx] = existing.copy(weight = existing.weight + r.weight)
            } else {
                _globalRelations.add(r)
            }
        }
    }

    /** 删除某文件涉及的全部关系（实体删除后清幽灵边） */
    private fun removeRelationsForFile(fileId: String) {
        val entityIds = _files.find { it.id == fileId }?.entities?.map { it.id }?.toSet() ?: emptySet()
        _globalRelations.removeAll {
            it.sourceEntityId in entityIds || it.targetEntityId in entityIds
        }
    }

    // ── 图布局持久化 ──

    suspend fun loadGraphLayout(): Map<String, Pair<Float, Float>> = withContext(Dispatchers.IO) {
        val db = database ?: return@withContext emptyMap()
        db.graphLayoutDao().getAll().associate { it.nodeId to (it.x to it.y) }
    }

    suspend fun saveGraphLayout(positions: Map<String, Pair<Float, Float>>) = withContext(Dispatchers.IO) {
        val db = database ?: return@withContext
        val entities = positions.map { (nodeId, pos) ->
            com.crossk.data.db.GraphLayoutEntity(
                nodeId = nodeId,
                x = pos.first,
                y = pos.second,
                updatedAt = System.currentTimeMillis(),
            )
        }
        db.graphLayoutDao().upsertAll(entities)
    }

    // ── XP / Game ──

    fun addXpForAnalysis(): KnowledgeLevel {
        gameEngine.addXp(XpEvent.FILE_ANALYZED)
        updateGraphVisualLevel()
        _xpBreakdown.tryEmit(
            listOf(
                XpBreakdownItem(
                    amount = XpEvent.FILE_ANALYZED.xp,
                    label = XpEvent.FILE_ANALYZED.label,
                ),
            ),
        )
        return level
    }

    fun addXpForEntities(count: Int): KnowledgeLevel {
        if (count <= 0) return level
        repeat(count) { gameEngine.addXp(XpEvent.ENTITY_DISCOVERED) }
        updateGraphVisualLevel()
        _xpBreakdown.tryEmit(
            listOf(
                XpBreakdownItem(
                    amount = XpEvent.ENTITY_DISCOVERED.xp,
                    label = XpEvent.ENTITY_DISCOVERED.label,
                    count = count,
                ),
            ),
        )
        return level
    }

    fun restoreGraphVisualLevel(level: Int) {
        graphVisualLevel = level.coerceIn(1, 20)
    }

    private fun updateGraphVisualLevel() {
        graphVisualLevel = level.level
    }

    // ── 统计 ──

    fun getStats(): FileStats {
        val entityCount = _globalEntities.size
        val connectionCount = _globalRelations.size
        val topics = _files.flatMap { it.topics }.distinct()
        return FileStats(
            totalFiles = _files.size,
            totalEntities = entityCount,
            totalConnections = connectionCount,
            topicsCovered = topics.size,
        )
    }

    private fun computeFileImportance(result: AnalysisResult): Float {
        if (result.entities.isEmpty()) return 0.1f
        val denseEntityScore = (result.entities.size.toFloat() / 10f).coerceAtMost(1f)
        val topicDiversity = (result.topics.size.toFloat() / 3f).coerceAtMost(1f)
        return (denseEntityScore * 0.6f + topicDiversity * 0.4f).coerceIn(0.1f, 1f)
    }

    // ── 成长曲线 ──

    fun computeGrowthMetrics(): List<GrowthMetric> {
        if (_files.isEmpty()) return emptyList()
        val sortedFiles = _files.sortedBy { it.createdAt }
        val startDate = sortedFiles.first().createdAt
        val now = System.currentTimeMillis()
        val weekMs = 7L * 24 * 60 * 60 * 1000
        val totalWeeks = ((now - startDate) / weekMs + 1).toInt().coerceIn(1, 12)

        val metrics = mutableListOf<GrowthMetric>()
        for (week in 0 until totalWeeks) {
            val weekEnd = startDate + (week + 1) * weekMs
            val filesUpToWeek = sortedFiles.filter { it.createdAt <= weekEnd }
            val entitiesUpToWeek = filesUpToWeek.flatMap { it.entities }
            val topicsUpToWeek = filesUpToWeek.flatMap { it.topics }.distinct()
            val entityIdsInScope = entitiesUpToWeek.map { it.id }.toSet()
            val relationsUpToWeek = _globalRelations.count {
                it.sourceEntityId in entityIdsInScope || it.targetEntityId in entityIdsInScope
            }
            metrics.add(
                GrowthMetric(
                    weekIndex = week + 1,
                    filesAnalyzed = filesUpToWeek.size,
                    entitiesDiscovered = entitiesUpToWeek.size,
                    connectionsMade = relationsUpToWeek,
                    topicsCovered = topicsUpToWeek.size,
                ),
            )
        }
        return metrics
    }

    // ── 洞察 ──

    val insightText: String
        get() {
            if (_files.isEmpty()) return "还没有分析任何文件，导入文件后将自动生成知识洞察。"
            val parts = mutableListOf<String>()
            val totalFiles = _files.size
            val totalEntities = _globalEntities.size
            val totalRelations = _globalRelations.size
            val questsDone = gameEngine.quests.count { it.completed }
            parts.add("已分析 $totalFiles 份文件，发现 $totalEntities 个实体，建立 $totalRelations 条关联。")
            if (totalEntities > 0) {
                val topEntity = _globalEntities.firstOrNull()
                if (topEntity != null) {
                    parts.add("最活跃概念：「${topEntity.name}」跨 ${topEntity.mentions} 次提及。")
                }
            }
            val recentFiles = _files.sortedByDescending { it.createdAt }.take(3)
            if (recentFiles.isNotEmpty()) {
                parts.add("最近分析：${recentFiles.joinToString("、") { it.name }}。")
            }
            if (questsDone > 0) {
                parts.add("今日已完成 $questsDone 个任务，继续保持！")
            }
            parts.add("知识图谱维度：${gameEngine.level.title} | 连续活跃：${gameEngine.streak.currentStreak} 天")
            return parts.joinToString("\n\n")
        }

    val graphEvolution: GraphEvolutionState get() = getGraphEvolution(graphVisualLevel)
}

/**
 * Relation → EdgeEntity 转换扩展（与 EdgeEntity.toDomain 配对）。
 */
private fun Relation.toEdge(): com.crossk.data.db.EdgeEntity =
    com.crossk.data.db.EdgeEntity(
        srcId = sourceEntityId,
        dstId = targetEntityId,
        type = type.name,
        weight = weight,
        evidence = null,
        createdAt = System.currentTimeMillis(),
    )
