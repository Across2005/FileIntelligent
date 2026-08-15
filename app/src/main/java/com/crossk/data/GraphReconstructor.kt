package com.crossk.data

/**
 * GraphReconstructor v3 — 负责从文件数据重建全局实体（去重 + 合并 mentions）。
 *
 * 关系数据 v2.1 起由 FileRepository 在内存累积（分析时并入、删除时剔除），
 * 本类不再承担关系重建职责，避免 runBlocking 主线程查库。
 */
class GraphReconstructor {

    /**
     * 跨文件全局实体（按 (name, type) 去重 + 合并 mentions）。
     */
    fun rebuildGlobalEntities(files: List<FileItem>): List<Entity> {
        val allEntities = files.flatMap { it.entities }
        val merged = mutableMapOf<Pair<String, Entity.Type>, Entity>()
        for (e in allEntities) {
            val key = e.name to e.type
            val existing = merged[key]
            merged[key] = if (existing != null) {
                existing.copy(mentions = existing.mentions + e.mentions)
            } else {
                e
            }
        }
        return merged.values.sortedByDescending { it.mentions }
    }
}
