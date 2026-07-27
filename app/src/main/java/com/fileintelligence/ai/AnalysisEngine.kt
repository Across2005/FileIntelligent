package com.fileintelligence.ai

import com.fileintelligence.data.*

class AnalysisEngine {

    private val conceptKeywords = mapOf(
        "认知" to "CONCEPT", "架构" to "CONCEPT", "模型" to "CONCEPT",
        "注意力" to "CONCEPT", "注意力机制" to "CONCEPT", "神经网络" to "CONCEPT",
        "深度学习" to "CONCEPT", "特征" to "CONCEPT", "嵌入" to "CONCEPT",
        "向量化" to "CONCEPT", "相似度" to "CONCEPT", "检索" to "CONCEPT",
        "微调" to "METHOD", "GPT" to "TOOL", "BERT" to "TOOL",
        "Transformer" to "TOOL", "自" to "CONCEPT", "编解码" to "METHOD",
        "研究" to "CONCEPT", "方法" to "METHOD", "实验" to "EVENT",
        "论文" to "EVENT", "数据" to "CONCEPT", "算法" to "CONCEPT",
        "优化" to "METHOD", "训练" to "METHOD", "推理" to "METHOD",
    )

    fun analyze(text: String): AnalysisResult {
        val entities = extractEntities(text)
        val topics = extractTopics(text, entities)
        val keyPhrases = extractKeyPhrases(text)
        val summary = generateSummary(text, entities, topics)
        val sentiment = computeSentiment(text)

        return AnalysisResult(
            fileId = "",
            summary = summary,
            entities = entities,
            topics = topics,
            sentiment = sentiment,
            keyPhrases = keyPhrases,
        )
    }

    private fun extractEntities(text: String): List<Entity> {
        val found = mutableListOf<Entity>()
        conceptKeywords.forEach { (word, type) ->
            if (text.contains(word)) {
                found.add(
                    Entity(
                        id = word,
                        name = word,
                        type = Entity.Type.entries.first { it.name == type },
                        mentions = text.split(word).size - 1,
                    )
                )
            }
        }
        // Keep top entities by mention count
        return found.sortedByDescending { it.mentions }.take(20)
    }

    private fun extractTopics(text: String, entities: List<Entity>): List<Topic> {
        val topicNames = entities
            .groupBy { it.type }
            .map { (type, ents) ->
                val name = when (type) {
                    Entity.Type.CONCEPT -> "AI 基础"
                    Entity.Type.METHOD -> "方法论"
                    Entity.Type.TOOL -> "技术栈"
                    Entity.Type.EVENT -> "研究活动"
                    Entity.Type.PERSON -> "人物"
                    Entity.Type.PLACE -> "场景"
                }
                Topic(
                    name = name,
                    weight = ents.size.toFloat().coerceAtMost(1f),
                    color = type.color,
                )
            }
        return topicNames.sortedByDescending { it.weight }
    }

    private fun extractKeyPhrases(text: String): List<String> {
        // Simple: take sentences or segments that contain key concepts
        return text.split(Regex("[，。！？；\n]"))
            .filter { it.length > 4 && it.length < 60 }
            .take(5)
    }

    private fun generateSummary(text: String, entities: List<Entity>, topics: List<Topic>): String {
        val topEntities = entities.take(5).joinToString("、") { it.name }
        val topTopics = topics.take(3).joinToString(" · ") { it.name }
        return "提取了 ${entities.size} 个核心概念，覆盖 $topTopics，主要涉及：$topEntities。"
    }

    private fun computeSentiment(text: String): Float {
        val positive = listOf("好", "高", "强", "优", "进步", "提升", "增加", "成功")
        val negative = listOf("差", "低", "弱", "不足", "下降", "减少", "失败")
        var score = 0f
        positive.forEach { score += text.split(it).size - 1f }
        negative.forEach { score -= text.split(it).size - 1f }
        return score.coerceIn(-1f, 1f)
    }
}
