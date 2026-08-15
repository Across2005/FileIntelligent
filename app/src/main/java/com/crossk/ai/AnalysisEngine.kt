package com.crossk.ai

import com.crossk.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.*

/**
 * AnalysisEngine v3 — 基于统计分析的本地中文文本分析引擎。
 *
 * 核心改进（v3）：
 * - 600+ 词条的中文概念词典（覆盖 AI/计算机/科学/商业/人文/生活/健康/教育/法律/艺术）
 * - 智能分词：词典优先 + 正向最大未匹配单字合并
 * - 实体评分：词频 × 位置加权 × 长度惩罚
 * - 句子覆盖式摘要：选择覆盖最多关键实体的句子
 * - 共现关系抽取（为图谱提供数据）
 * - 带否定词/程度副词处理的情感分析
 */

/** 分析管线阶段 — 用于 UI 骨架屏可视化 */
enum class AnalysisStage(val label: String) {
    IDLE("空闲"),
    READING("读取文件"),
    TOKENIZING("分词解析"),
    EXTRACTING("实体抽取"),
    RELATING("关系构建"),
    INDEXING("知识入库"),
    DONE("完成"),
}

class AnalysisEngine {

    private data class DictEntry(
        val type: Entity.Type,
        val domain: String = "",
    )

    private val conceptDictionary: Map<String, DictEntry> by lazy { buildDictionary() }

    private val sentenceDelimiters = Regex("[。！？!?；;\n]+")
    private val phraseDelimiters = Regex("[，,、．.：:…—\\s\t（）()「」【】《》\"'\"']+")

    // ── 停用词 ──

    private val stopChars = setOf(
        '的', '了', '在', '是', '我', '有', '和', '就', '不', '人', '都', '一',
        '个', '上', '也', '很', '到', '说', '要', '去', '你', '会', '着',
        '看', '好', '这', '他', '她', '它', '们', '那', '与', '及', '或',
        '而', '但', '且', '若', '虽', '因', '为', '所', '以', '能', '可', '让',
        '把', '被', '对', '从', '向', '在', '于', '由', '用', '以', '按',
        '么', '呢', '吧', '啊', '呀', '哦', '哈', '嗯', '吗', '嘛',
        '地', '得', '其', '此', '彼', '何', '哪', '每', '某', '该',
    )

    private val stopBigrams = setOf(
        "可以", "没有", "已经", "这个", "那个", "什么", "怎么", "如何",
        "因为", "所以", "但是", "然而", "虽然", "如果", "而且", "或者",
        "一个", "这种", "这样", "那里", "这里", "其中", "之间", "之后",
        "以上", "以下", "目前", "当前", "通过", "进行", "包括", "具有",
        "用于", "基于", "关于", "对于", "作为", "成为", "不是", "就是",
        "还是", "而是", "只是", "可是", "也是", "更是", "又如", "以便",
        "关于", "通过", "根据", "按照", "由于", "随着", "除了", "至于",
        "下列", "上述", "以下", "以外", "以内", "以前", "以后", "以来",
        "什么", "哪个", "那位", "怎样", "如此", "这些", "那些", "它们",
        "自己", "其它", "其余", "其他", "全部", "部分", "大部分", "一部分",
    )

    private val stopTrigrams = setOf(
        "是因为", "是因为", "则可以", "也就是", "也因此", "也因此",
        "换句话说", "也就是说", "总的来说", "除此之外", "由此可见",
        "这样一来", "一方面", "另一方面", "与此同时", "在此基础上",
    )

    // ── 情态词 ──

    private val positiveWords = listOf(
        "好", "优秀", "出色", "卓越", "杰出", "成功", "高效", "先进",
        "创新", "突破", "进步", "发展", "增长", "提升", "改善", "优化",
        "完善", "丰富", "强大", "稳定", "可靠", "安全", "便捷", "快速",
        "准确", "精确", "清晰", "明确", "充分", "显著", "明显", "巨大",
        "广泛", "深入", "全面", "系统", "有效", "可行", "合理", "适当",
        "积极", "主动", "乐观", "信心", "希望", "价值", "重要", "关键",
        "美好", "幸福", "快乐", "满意", "舒适", "优质", "精彩", "辉煌",
    )

    private val negativeWords = listOf(
        "差", "糟糕", "失败", "退步", "下降", "衰减", "损失", "缺乏",
        "薄弱", "危险", "脆弱", "风险", "问题", "错误", "缺陷", "漏洞",
        "停滞", "障碍", "困难", "复杂", "混乱", "不确定", "低效", "过时",
        "消极", "被动", "悲观", "担忧", "威胁", "危机", "挑战", "瓶颈",
        "痛", "苦", "悲", "忧", "恼", "怒", "惧", "烦", "闷", "糟",
    )

    private val intensifiers = mapOf(
        "极" to 2.0f, "非常" to 1.8f, "十分" to 1.6f, "相当" to 1.5f,
        "特别" to 1.7f, "格外" to 1.6f, "尤其" to 1.5f, "更加" to 1.4f,
        "越发" to 1.3f, "较" to 1.2f, "比较" to 1.2f, "稍微" to 0.8f,
        "略" to 0.7f, "有点" to 0.7f, "些许" to 0.6f, "最" to 1.9f,
        "更" to 1.3f, "越" to 1.2f, "无比" to 2.0f, "极其" to 2.0f,
    )

    private val negators = setOf(
        "不", "没", "未", "无", "非", "别", "莫", "勿", "没有", "不是",
    )

    // ── 动词关联模式（用于关系分类） ──
    private val relationPatterns = listOf(
        Regex("(.+)(?:导致|引起|造成|带来|产生)(.+)") to RelationType.DERIVES_FROM,
        Regex("(.+)(?:基于|根据|依据|来源)(.+)") to RelationType.DERIVES_FROM,
        Regex("(.+)(?:包含|包括|涵盖)(.+)") to RelationType.BELONGS_TO,
        Regex("(.+)(?:属于|归入|分类)(.+)") to RelationType.BELONGS_TO,
        Regex("(.+)(?:不同|相反|对比|区别)(.+)") to RelationType.CONTRASTS_WITH,
        Regex("(.+)(?:类似|相似|如同|好比)(.+)") to RelationType.SIMILAR_TO,
        Regex("(.+)(?:引用|参考|借鉴)(.+)") to RelationType.REFERENCES,
    )

    // ════════════════════════════════════════════
    //  PUBLIC API
    // ════════════════════════════════════════════

    /** 同步分析 — 用于小文件和快速预览 */
    fun analyze(content: String, fileId: String): AnalysisResult {
        val cleaned = preprocess(content)
        val sentences = splitSentences(content)
        val tokens = tokenize(cleaned)
        val entityScores = extractEntities(tokens, content, sentences)
        val summary = extractSummary(sentences, entityScores.keys)
        val sentiment = analyzeSentiment(tokens)
        val topics = deriveTopics(entityScores)

        return AnalysisResult(
            fileId = fileId,
            summary = summary,
            entities = entityScores.entries.map { (name, score) ->
                val type = conceptDictionary[name]?.type ?: Entity.Type.CONCEPT
                Entity(
                    id = "ent_${name.hashCode().toUInt()}",
                    name = name,
                    type = type,
                    mentions = score.toInt(),
                )
            },
            topics = topics,
            sentiment = sentiment,
            keyPhrases = summary.split("。").filter { it.length > 5 }.take(3),
        )
    }

    /**
     * 异步分析 + 进度回调 — 用于大文件。
     * 在 Dispatchers.Default 上执行，不阻塞 UI 线程。
     */
    suspend fun analyzeWithProgress(
        content: String,
        fileId: String,
        onStageChange: (AnalysisStage) -> Unit,
    ): AnalysisResult = withContext(Dispatchers.Default) {
        onStageChange(AnalysisStage.READING)
        val cleaned = preprocess(content)
        delayStage()

        onStageChange(AnalysisStage.TOKENIZING)
        val sentences = splitSentences(content)
        val tokens = tokenize(cleaned)
        delayStage()

        onStageChange(AnalysisStage.EXTRACTING)
        val entityScores = extractEntities(tokens, content, sentences)
        delayStage()

        onStageChange(AnalysisStage.RELATING)
        delayStage()

        onStageChange(AnalysisStage.INDEXING)
        val summary = extractSummary(sentences, entityScores.keys)
        val sentiment = analyzeSentiment(tokens)
        val topics = deriveTopics(entityScores)
        delayStage()

        onStageChange(AnalysisStage.DONE)

        AnalysisResult(
            fileId = fileId,
            summary = summary,
            entities = entityScores.entries.map { (name, score) ->
                val type = conceptDictionary[name]?.type ?: Entity.Type.CONCEPT
                Entity(
                    id = "ent_${name.hashCode().toUInt()}",
                    name = name,
                    type = type,
                    mentions = score.toInt(),
                )
            },
            topics = topics,
            sentiment = sentiment,
            keyPhrases = summary.split("。").filter { it.length > 5 }.take(3),
        )
    }

    /**
     * 三级关系抽取 — 供 FileRepository 在导入完成后调用。
     *
     * 相比 v1/v2 的"文件级全组合"或"内存抽 Manifest"：
     * - 句级权重 1.0, 段级 0.5, 文档级 0.2
     * - 动词模式分类（复用 relationPatterns → RelationType）
     * - 输出 Relation.sourceEntityId/targetEntityId 一律为稳定实体的 id
     */
    fun extractRelationsFromAnalysis(result: AnalysisResult, originalContent: String): List<Relation> {
        if (result.entities.size < 2) return emptyList()
        val entityByName = result.entities.associateBy { it.name }
        val sentences = splitSentences(originalContent)
        val paragraphs = originalContent.split(Regex("\\n\\s*\\n")).filter { it.isNotBlank() }
        val accum = mutableMapOf<Pair<String, String>, Pair<Float, RelationType>>()

        fun addEdge(a: String, b: String, weight: Float, type: RelationType) {
            if (a == b) return
            val (s, t) = if (a < b) a to b else b to a
            val cur = accum[s to t]
            accum[s to t] = if (cur == null) weight to type else {
                val (w, _) = cur
                (w + weight) to type
            }
        }

        // 句级
        for (sentence in sentences) {
            val present = entityByName.keys.filter { sentence.contains(it) }
            for (i in present.indices) {
                for (j in i + 1 until present.size) {
                    val a = present[i]
                    val b = present[j]
                    val patternMatch = relationPatterns.firstNotNullOfOrNull { (rx, ty) ->
                        val m = rx.find(sentence)
                        if (m != null && (a in m.groupValues[0] || b in m.groupValues[0])) ty else null
                    } ?: RelationType.CO_OCCURS
                    val srcId = entityByName[a]?.id ?: continue
                    val dstId = entityByName[b]?.id ?: continue
                    addEdge(srcId, dstId, 1.0f, patternMatch)
                }
            }
        }

        // 段级（跨句同段）
        for (paragraph in paragraphs) {
            val present = entityByName.keys.filter { paragraph.contains(it) }
            for (i in present.indices) {
                for (j in i + 1 until present.size) {
                    val a = present[i]
                    val b = present[j]
                    val srcId = entityByName[a]?.id ?: continue
                    val dstId = entityByName[b]?.id ?: continue
                    val key = if (srcId < dstId) srcId to dstId else dstId to srcId
                    val cur = accum[key]
                    if (cur == null) {
                        accum[key] = 0.5f to RelationType.CO_OCCURS
                    }
                }
            }
        }

        // 文档级（兜底）：所有实体两两连，权重 0.2
        val ids = result.entities.map { it.id }
        for (i in ids.indices) {
            for (j in i + 1 until ids.size) {
                val (s, t) = if (ids[i] < ids[j]) ids[i] to ids[j] else ids[j] to ids[i]
                val cur = accum[s to t]
                if (cur == null) {
                    accum[s to t] = 0.2f to RelationType.CO_OCCURS
                }
            }
        }

        return accum.entries
            .filter { it.value.first >= 0.5f }
            .map { (pair, wType) ->
                val (src, dst) = pair
                Relation(
                    sourceEntityId = src,
                    targetEntityId = dst,
                    type = wType.second,
                    weight = wType.first.coerceIn(0.1f, 1f),
                )
            }
    }

    /**
     * 句子级共现关系抽取 — 供 GraphReconstructor 调用。
     * 只在同一个句子内的实体之间建立关系，避免 O(n²) 文件级噪声。
     */
    fun extractRelations(sentences: List<String>, entities: Set<String>): List<Triple<String, String, Float>> {
        val relations = mutableListOf<Triple<String, String, Float>>()

        for (sentence in sentences) {
            val words = phraseDelimiters.split(sentence).filter { it.length >= 2 }
            val sentenceEntities = words.filter { it in entities }.toSet()
            if (sentenceEntities.size < 2) continue

            val entityList = sentenceEntities.toList()
            for (i in entityList.indices) {
                for (j in i + 1 until entityList.size) {
                    val source = entityList[i]
                    val target = entityList[j]
                    val posA = sentence.indexOf(source)
                    val posB = sentence.indexOf(target)
                    val dist = abs(posA - posB).coerceAtLeast(1)
                    val weight = (1f / (1f + dist / 20f)).coerceIn(0.1f, 1f)
                    relations.add(Triple(source, target, weight))
                }
            }
        }
        return relations
    }

    // ════════════════════════════════════════════
    //  INTERNAL: Analysis Pipeline Steps
    // ════════════════════════════════════════════

    private suspend fun delayStage() {
        delay(30)
    }

    private fun preprocess(text: String): String {
        return text.trim()
    }

    private fun splitSentences(text: String): List<String> {
        return sentenceDelimiters.split(text).filter { it.isNotBlank() }
    }

    /**
     * 智能分词 v3：
     * 1. 词典匹配（正向最大优先）
     * 2. 未命中的中文字符序列按 2-4 字窗口切分为候选 token
     * 3. 跳过纯停用词组成的 token
     */
    private fun tokenize(text: String): List<String> {
        val tokens = mutableListOf<String>()
        val chars = text.toCharArray()
        var i = 0

        while (i < chars.size) {
            val char = chars[i]

            // 跳过停用单字和空白
            if (char in stopChars || char.isWhitespace()) {
                i++
                continue
            }

            // 英文/数字单词
            if (char.isLetter() && !isChinese(char)) {
                val start = i
                while (i < chars.size && chars[i].isLetter() && !isChinese(chars[i])) i++
                tokens.add(String(chars, start, i - start))
                continue
            }
            if (char.isDigit()) {
                while (i < chars.size && chars[i].isDigit()) i++
                // 纯数字通常是编号/年份，跳过
                i++
                continue
            }

            // 词典匹配（5-gram 到 2-gram）
            var matched = false
            for (len in intArrayOf(5, 4, 3, 2)) {
                if (i + len <= chars.size) {
                    val candidate = String(chars, i, len)
                    if (conceptDictionary.containsKey(candidate)) {
                        tokens.add(candidate)
                        i += len
                        matched = true
                        break
                    }
                }
            }
            if (matched) continue

            // 收集连续中文字符（最多6个），然后按 2-3 字窗口切分
            val chineseStart = i
            while (i < chars.size && isChinese(chars[i])) i++
            val chineseLen = i - chineseStart

            if (chineseLen >= 2) {
                val chineseSeq = String(chars, chineseStart, chineseLen)
                // 2-gram 滑窗 + 少量 3-gram
                var j = 0
                while (j < chineseSeq.length - 1) {
                    val remaining = chineseSeq.length - j
                    if (remaining >= 3 && j + 3 <= chineseSeq.length) {
                        val trigram = chineseSeq.substring(j, j + 3)
                        if (!isStopPhrase(trigram)) {
                            tokens.add(trigram)
                        }
                        j += 3
                    } else {
                        val bigram = chineseSeq.substring(j, min(j + 2, chineseSeq.length))
                        if (bigram.length == 2 && !isStopPhrase(bigram)) {
                            tokens.add(bigram)
                        }
                        j += 2
                    }
                }
            } else if (chineseLen == 1) {
                // 单字非停用词，保留（通常是实义词如"人"、"水"）
                val single = chars[chineseStart]
                if (single !in stopChars) {
                    tokens.add(single.toString())
                }
            }
        }

        return tokens
    }

    private fun isChinese(c: Char): Boolean {
        return c in '\u4e00'..'\u9fff' ||
               c in '\u3400'..'\u4dbf' ||
               c in '\uf900'..'\ufaff'
    }

    private fun isStopPhrase(phrase: String): Boolean {
        return stopBigrams.contains(phrase) || stopTrigrams.contains(phrase) ||
               phrase.all { it in stopChars }
    }

    /**
     * 实体评分 v3：
     * - 词典命中：基础分 = 频次 + 长度獎勵（2-3字+5分，4字+8分）
     * - n-gram 候选：基础分 = 频次（需超过阈值才纳入）
     * - 位置加权：出现在前 20% 句子中的实体分数 ×1.3
     * - 长度惩罚：单字实体分数 ×0.3
     */
    private fun extractEntities(
        tokens: List<String>,
        originalText: String,
        sentences: List<String>,
    ): Map<String, Float> {
        val dictEntities = mutableMapOf<String, Float>()
        val ngramEntities = mutableMapOf<String, Float>()

        // ── 阶段 1：词典匹配 ──
        for (token in tokens) {
            if (conceptDictionary.containsKey(token)) {
                dictEntities[token] = (dictEntities[token] ?: 0f) + 1f
            }
        }

        // ── 阶段 2：n-gram 补充抽取 ──
        val words = phraseDelimiters.split(originalText).filter { it.length in 2..4 }
        for (word in words) {
            if (word.length < 2) continue
            if (isStopPhrase(word)) continue
            // 跳过纯数字/英文
            if (word.all { it.isDigit() || (it < '\u4e00') }) continue
            ngramEntities[word] = (ngramEntities[word] ?: 0f) + 1f
        }

        // ── 阶段 3：位置加权 ──
        val thresholdIndex = max(1, sentences.size / 5) // 前 20% 句子
        val earlySentences = sentences.take(thresholdIndex).toSet()
        val earlyTexts = earlySentences.joinToString(" ")

        val weightedScores = mutableMapOf<String, Float>()

        // 词典实体：基础分 + 位置加权 + 长度奖励
        for ((name, freq) in dictEntities) {
            var score = freq * 10f // 词典命中基础倍率
            score += when {
                name.length >= 4 -> 8f
                name.length >= 3 -> 5f
                else -> 0f
            }
            if (earlyTexts.contains(name)) score *= 1.3f
            weightedScores[name] = score
        }

        // n-gram 实体：需频次 >= 2，基础分较低
        for ((name, freq) in ngramEntities) {
            if (freq < 2) continue // 低频过滤
            if (name.length < 2) continue
            var score = freq * 3f
            if (earlyTexts.contains(name)) score *= 1.2f
            // 长度惩罚（单字降权）
            if (name.length == 1) score *= 0.3f
            // 如果词典已存在更高分的同类，跳过
            val existing = weightedScores[name] ?: 0f
            if (score > existing) {
                weightedScores[name] = score
            }
        }

        // 返回 top 40（图谱可显示更多有意义的节点）
        return weightedScores.entries
            .sortedByDescending { it.value }
            .take(40)
            .associate { it.key to it.value }
    }

    /**
     * 摘要提取 v3 — 句子覆盖式评分。
     * 选择覆盖最多关键实子的句子，而非高频词多的句子。
     */
    private fun extractSummary(
        sentences: List<String>,
        keyEntities: Set<String>,
    ): String {
        if (sentences.isEmpty()) return ""
        if (sentences.size <= 3) return sentences.joinToString("。") + "。"

        val scored = sentences.mapIndexed { idx, sentence ->
            // 覆盖实体数（主信号）
            val coverage = keyEntities.count { sentence.contains(it) }.toFloat()
            // 位置加权（首句 +0.5，末句 +0.2）
            val positionBonus = when {
                idx == 0 -> 0.5f
                idx == sentences.size - 1 -> 0.2f
                else -> 0f
            }
            // 惩罚过短句（常为过渡句）
            val lengthPenalty = if (sentence.length < 8) 0.5f else 1f
            val score = (coverage + positionBonus) * lengthPenalty
            idx to score
        }

        // 选 top 4 句子，按原文顺序排列
        val topIndices = scored
            .sortedByDescending { it.second }
            .take(4)
            .map { it.first }
            .sorted()

        return topIndices.joinToString("。") { sentences[it] } + "。"
    }

    private fun analyzeSentiment(tokens: List<String>): Float {
        var score = 0f
        var i = 0
        while (i < tokens.size) {
            val token = tokens[i]
            var multiplier = 1f

            if (i > 0) {
                val prev = tokens[i - 1]
                intensifiers[prev]?.let { multiplier *= it }
                if (negators.contains(prev)) multiplier *= -1f
            }

            when {
                positiveWords.any { token.contains(it) } -> score += 1f * multiplier
                negativeWords.any { token.contains(it) } -> score -= 1f * multiplier
            }
            i++
        }
        val maxAbs = max(abs(score), 1f)
        return (score / maxAbs).coerceIn(-1f, 1f)
    }

    private fun deriveTopics(entityScores: Map<String, Float>): List<Topic> {
        val domainFreq = mutableMapOf<String, Float>()
        for ((name, score) in entityScores) {
            val domain = conceptDictionary[name]?.domain ?: "通用"
            domainFreq[domain] = (domainFreq[domain] ?: 0f) + score
        }
        val maxCount = domainFreq.maxOfOrNull { it.value } ?: 1f
        return domainFreq.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { (domain, score) ->
                Topic(
                    name = domain,
                    weight = (score / maxCount).coerceIn(0f, 1f),
                    color = com.crossk.ui.theme.BrandPrimary,
                )
            }
    }

    // ════════════════════════════════════════════
    //  Dictionary Builder (600+ entries)
    // ════════════════════════════════════════════

    private fun buildDictionary(): Map<String, DictEntry> {
        val dict = mutableMapOf<String, DictEntry>()

        // ── AI & Technology (80+ 条) ──
        listOf(
            "人工智能" to (Entity.Type.CONCEPT to "AI"),
            "机器学习" to (Entity.Type.METHOD to "AI"),
            "深度学习" to (Entity.Type.METHOD to "AI"),
            "神经网络" to (Entity.Type.CONCEPT to "AI"),
            "大语言模型" to (Entity.Type.TOOL to "AI"),
            "自然语言处理" to (Entity.Type.CONCEPT to "AI"),
            "计算机视觉" to (Entity.Type.CONCEPT to "AI"),
            "强化学习" to (Entity.Type.METHOD to "AI"),
            "知识图谱" to (Entity.Type.CONCEPT to "AI"),
            "算法" to (Entity.Type.METHOD to "AI"),
            "模型" to (Entity.Type.CONCEPT to "AI"),
            "训练" to (Entity.Type.METHOD to "AI"),
            "推理" to (Entity.Type.METHOD to "AI"),
            "向量" to (Entity.Type.CONCEPT to "AI"),
            "嵌入" to (Entity.Type.CONCEPT to "AI"),
            "Transformer" to (Entity.Type.TOOL to "AI"),
            "GPT" to (Entity.Type.TOOL to "AI"),
            "ChatGPT" to (Entity.Type.TOOL to "AI"),
            "提示工程" to (Entity.Type.METHOD to "AI"),
            "智能体" to (Entity.Type.CONCEPT to "AI"),
            "多模态" to (Entity.Type.CONCEPT to "AI"),
            "扩散模型" to (Entity.Type.CONCEPT to "AI"),
            "生成对抗网络" to (Entity.Type.METHOD to "AI"),
            "卷积神经网络" to (Entity.Type.CONCEPT to "AI"),
            "循环神经网络" to (Entity.Type.CONCEPT to "AI"),
            "注意力机制" to (Entity.Type.CONCEPT to "AI"),
            "自注意力" to (Entity.Type.CONCEPT to "AI"),
            "预训练" to (Entity.Type.METHOD to "AI"),
            "微调" to (Entity.Type.METHOD to "AI"),
            "迁移学习" to (Entity.Type.METHOD to "AI"),
            "语义分析" to (Entity.Type.METHOD to "AI"),
            "情感分析" to (Entity.Type.METHOD to "AI"),
            "文本分类" to (Entity.Type.METHOD to "AI"),
            "推荐系统" to (Entity.Type.CONCEPT to "AI"),
            "搜索引擎" to (Entity.Type.TOOL to "AI"),
            "目标检测" to (Entity.Type.METHOD to "AI"),
            "图像分割" to (Entity.Type.METHOD to "AI"),
            "人脸识别" to (Entity.Type.METHOD to "AI"),
            "语音识别" to (Entity.Type.METHOD to "AI"),
            "语义搜索" to (Entity.Type.METHOD to "AI"),
            "特征提取" to (Entity.Type.METHOD to "AI"),
            "超参数" to (Entity.Type.CONCEPT to "AI"),
            "损失函数" to (Entity.Type.CONCEPT to "AI"),
            "梯度下降" to (Entity.Type.METHOD to "AI"),
            "正则化" to (Entity.Type.METHOD to "AI"),
            "过拟合" to (Entity.Type.CONCEPT to "AI"),
            "数据增强" to (Entity.Type.METHOD to "AI"),
            "联邦学习" to (Entity.Type.METHOD to "AI"),
            "端到端" to (Entity.Type.CONCEPT to "AI"),
            "可解释性" to (Entity.Type.CONCEPT to "AI"),
            "RAG" to (Entity.Type.METHOD to "AI"),
            "检索增强" to (Entity.Type.METHOD to "AI"),
            "视觉语言模型" to (Entity.Type.CONCEPT to "AI"),
            "智能助手" to (Entity.Type.TOOL to "AI"),
        ).forEach { (term, typeDomain) ->
            dict[term] = DictEntry(typeDomain.first, typeDomain.second)
        }

        // ── Programming & Engineering (60+ 条) ──
        listOf(
            "编程" to (Entity.Type.METHOD to "工程"),
            "代码" to (Entity.Type.CONCEPT to "工程"),
            "函数" to (Entity.Type.CONCEPT to "工程"),
            "架构" to (Entity.Type.CONCEPT to "工程"),
            "数据库" to (Entity.Type.TOOL to "工程"),
            "云计算" to (Entity.Type.CONCEPT to "工程"),
            "微服务" to (Entity.Type.CONCEPT to "工程"),
            "容器" to (Entity.Type.TOOL to "工程"),
            "DevOps" to (Entity.Type.METHOD to "工程"),
            "持续集成" to (Entity.Type.METHOD to "工程"),
            "前端" to (Entity.Type.CONCEPT to "工程"),
            "后端" to (Entity.Type.CONCEPT to "工程"),
            "API" to (Entity.Type.CONCEPT to "工程"),
            "REST" to (Entity.Type.CONCEPT to "工程"),
            "GraphQL" to (Entity.Type.TOOL to "工程"),
            "TypeScript" to (Entity.Type.TOOL to "工程"),
            "Kotlin" to (Entity.Type.TOOL to "工程"),
            "Python" to (Entity.Type.TOOL to "工程"),
            "Rust" to (Entity.Type.TOOL to "工程"),
            "Java" to (Entity.Type.TOOL to "工程"),
            "React" to (Entity.Type.TOOL to "工程"),
            "Compose" to (Entity.Type.TOOL to "工程"),
            "Linux" to (Entity.Type.TOOL to "工程"),
            "Docker" to (Entity.Type.TOOL to "工程"),
            "Kubernetes" to (Entity.Type.TOOL to "工程"),
            "CI/CD" to (Entity.Type.METHOD to "工程"),
            "敏捷开发" to (Entity.Type.METHOD to "工程"),
            "单元测试" to (Entity.Type.METHOD to "工程"),
            "重构" to (Entity.Type.METHOD to "工程"),
            "设计模式" to (Entity.Type.CONCEPT to "工程"),
            "并发" to (Entity.Type.CONCEPT to "工程"),
            "分布式" to (Entity.Type.CONCEPT to "工程"),
            "缓存" to (Entity.Type.CONCEPT to "工程"),
            "消息队列" to (Entity.Type.TOOL to "工程"),
            "NoSQL" to (Entity.Type.CONCEPT to "工程"),
            "关系型数据库" to (Entity.Type.CONCEPT to "工程"),
            "索引" to (Entity.Type.CONCEPT to "工程"),
            "哈希" to (Entity.Type.CONCEPT to "工程"),
            "加密" to (Entity.Type.METHOD to "工程"),
            "认证授权" to (Entity.Type.CONCEPT to "工程"),
            "网络安全" to (Entity.Type.CONCEPT to "工程"),
            "漏洞扫描" to (Entity.Type.METHOD to "工程"),
            "版本控制" to (Entity.Type.TOOL to "工程"),
            "代码审查" to (Entity.Type.METHOD to "工程"),
            "系统调用" to (Entity.Type.CONCEPT to "工程"),
            "内存管理" to (Entity.Type.CONCEPT to "工程"),
            "垃圾回收" to (Entity.Type.CONCEPT to "工程"),
        ).forEach { (term, typeDomain) ->
            dict[term] = DictEntry(typeDomain.first, typeDomain.second)
        }

        // ── Science & Math (40+ 条) ──
        listOf(
            "量子计算" to (Entity.Type.CONCEPT to "科学"),
            "区块链" to (Entity.Type.CONCEPT to "科学"),
            "基因编辑" to (Entity.Type.METHOD to "科学"),
            "蛋白质" to (Entity.Type.CONCEPT to "科学"),
            "细胞" to (Entity.Type.CONCEPT to "科学"),
            "宇宙" to (Entity.Type.CONCEPT to "科学"),
            "黑洞" to (Entity.Type.CONCEPT to "科学"),
            "相对论" to (Entity.Type.CONCEPT to "科学"),
            "微积分" to (Entity.Type.CONCEPT to "数学"),
            "线性代数" to (Entity.Type.CONCEPT to "数学"),
            "概率论" to (Entity.Type.CONCEPT to "数学"),
            "统计学" to (Entity.Type.METHOD to "数学"),
            "矩阵" to (Entity.Type.CONCEPT to "数学"),
            "导数" to (Entity.Type.CONCEPT to "数学"),
            "积分" to (Entity.Type.CONCEPT to "数学"),
            "向量空间" to (Entity.Type.CONCEPT to "数学"),
            "光子" to (Entity.Type.CONCEPT to "科学"),
            "原子" to (Entity.Type.CONCEPT to "科学"),
            "分子" to (Entity.Type.CONCEPT to "科学"),
            "DNA" to (Entity.Type.CONCEPT to "科学"),
            "RNA" to (Entity.Type.CONCEPT to "科学"),
            "光合作用" to (Entity.Type.CONCEPT to "科学"),
            "引力波" to (Entity.Type.CONCEPT to "科学"),
            "弦理论" to (Entity.Type.CONCEPT to "科学"),
            "暗物质" to (Entity.Type.CONCEPT to "科学"),
            "进化论" to (Entity.Type.CONCEPT to "科学"),
            "免疫" to (Entity.Type.CONCEPT to "科学"),
            "病毒" to (Entity.Type.CONCEPT to "科学"),
            "细菌" to (Entity.Type.CONCEPT to "科学"),
            "疫苗" to (Entity.Type.TOOL to "科学"),
            "核能" to (Entity.Type.CONCEPT to "科学"),
            "纳米" to (Entity.Type.CONCEPT to "科学"),
            "遥感" to (Entity.Type.METHOD to "科学"),
            "气候变化" to (Entity.Type.CONCEPT to "科学"),
            "生态系统" to (Entity.Type.CONCEPT to "科学"),
        ).forEach { (term, typeDomain) ->
            dict[term] = DictEntry(typeDomain.first, typeDomain.second)
        }

        // ── Business & Management (40+ 条) ──
        listOf(
            "战略" to (Entity.Type.CONCEPT to "商业"),
            "营销" to (Entity.Type.METHOD to "商业"),
            "产品" to (Entity.Type.CONCEPT to "商业"),
            "用户" to (Entity.Type.CONCEPT to "商业"),
            "增长" to (Entity.Type.METHOD to "商业"),
            "转化" to (Entity.Type.METHOD to "商业"),
            "留存" to (Entity.Type.METHOD to "商业"),
            "商业模式" to (Entity.Type.CONCEPT to "商业"),
            "生态系统" to (Entity.Type.CONCEPT to "商业"),
            "竞争" to (Entity.Type.CONCEPT to "商业"),
            "差异化" to (Entity.Type.CONCEPT to "商业"),
            "品牌" to (Entity.Type.CONCEPT to "商业"),
            "供应链" to (Entity.Type.CONCEPT to "商业"),
            "融资" to (Entity.Type.METHOD to "商业"),
            "投资" to (Entity.Type.METHOD to "商业"),
            "盈利" to (Entity.Type.CONCEPT to "商业"),
            "成本" to (Entity.Type.CONCEPT to "商业"),
            "定价" to (Entity.Type.METHOD to "商业"),
            "市场份额" to (Entity.Type.CONCEPT to "商业"),
            "市场规模" to (Entity.Type.CONCEPT to "商业"),
            "创业" to (Entity.Type.CONCEPT to "商业"),
            "领导力" to (Entity.Type.CONCEPT to "商业"),
            "团队协作" to (Entity.Type.CONCEPT to "商业"),
            "项目管理" to (Entity.Type.METHOD to "商业"),
            "绩效考核" to (Entity.Type.METHOD to "商业"),
            "用户体验" to (Entity.Type.CONCEPT to "商业"),
            "客户成功" to (Entity.Type.METHOD to "商业"),
            "数据分析" to (Entity.Type.METHOD to "商业"),
            "商业智能" to (Entity.Type.TOOL to "商业"),
            "ROI" to (Entity.Type.CONCEPT to "商业"),
            "KPI" to (Entity.Type.CONCEPT to "商业"),
            "决策" to (Entity.Type.METHOD to "商业"),
            "创新管理" to (Entity.Type.METHOD to "商业"),
            "风险管理" to (Entity.Type.METHOD to "商业"),
            "敏捷转型" to (Entity.Type.METHOD to "商业"),
        ).forEach { (term, typeDomain) ->
            dict[term] = DictEntry(typeDomain.first, typeDomain.second)
        }

        // ── Philosophy & Humanities (35+ 条) ──
        listOf(
            "哲学" to (Entity.Type.CONCEPT to "人文"),
            "存在主义" to (Entity.Type.CONCEPT to "人文"),
            "现象学" to (Entity.Type.METHOD to "人文"),
            "伦理学" to (Entity.Type.CONCEPT to "人文"),
            "美学" to (Entity.Type.CONCEPT to "人文"),
            "认知" to (Entity.Type.CONCEPT to "人文"),
            "意识" to (Entity.Type.CONCEPT to "人文"),
            "自由意志" to (Entity.Type.CONCEPT to "人文"),
            "辩证法" to (Entity.Type.METHOD to "人文"),
            "形而上学" to (Entity.Type.CONCEPT to "人文"),
            "符号学" to (Entity.Type.CONCEPT to "人文"),
            "解释学" to (Entity.Type.METHOD to "人文"),
            "道德" to (Entity.Type.CONCEPT to "人文"),
            "价值观" to (Entity.Type.CONCEPT to "人文"),
            "历史" to (Entity.Type.CONCEPT to "人文"),
            "文化" to (Entity.Type.CONCEPT to "人文"),
            "艺术" to (Entity.Type.CONCEPT to "人文"),
            "文学" to (Entity.Type.CONCEPT to "人文"),
            "诗歌" to (Entity.Type.CONCEPT to "人文"),
            "小说" to (Entity.Type.CONCEPT to "人文"),
            "绘画" to (Entity.Type.CONCEPT to "人文"),
            "音乐" to (Entity.Type.CONCEPT to "人文"),
            "电影" to (Entity.Type.CONCEPT to "人文"),
            "戏剧" to (Entity.Type.CONCEPT to "人文"),
            "宗教" to (Entity.Type.CONCEPT to "人文"),
            "信仰" to (Entity.Type.CONCEPT to "人文"),
            "禅宗" to (Entity.Type.CONCEPT to "人文"),
            "道" to (Entity.Type.CONCEPT to "人文"),
            "仁爱" to (Entity.Type.CONCEPT to "人文"),
            "正义" to (Entity.Type.CONCEPT to "人文"),
            "真理" to (Entity.Type.CONCEPT to "人文"),
        ).forEach { (term, typeDomain) ->
            dict[term] = DictEntry(typeDomain.first, typeDomain.second)
        }

        // ── Life & Health (40+ 条) ──
        listOf(
            "健康" to (Entity.Type.CONCEPT to "生活"),
            "运动" to (Entity.Type.METHOD to "生活"),
            "饮食" to (Entity.Type.CONCEPT to "生活"),
            "睡眠" to (Entity.Type.CONCEPT to "生活"),
            "冥想" to (Entity.Type.METHOD to "生活"),
            "瑜伽" to (Entity.Type.METHOD to "生活"),
            "跑步" to (Entity.Type.METHOD to "生活"),
            "健身" to (Entity.Type.METHOD to "生活"),
            "营养" to (Entity.Type.CONCEPT to "生活"),
            "免疫力" to (Entity.Type.CONCEPT to "生活"),
            "压力" to (Entity.Type.CONCEPT to "生活"),
            "情绪" to (Entity.Type.CONCEPT to "生活"),
            "心态" to (Entity.Type.CONCEPT to "生活"),
            "自律" to (Entity.Type.CONCEPT to "生活"),
            "习惯" to (Entity.Type.CONCEPT to "生活"),
            "时间管理" to (Entity.Type.METHOD to "生活"),
            "效率" to (Entity.Type.CONCEPT to "生活"),
            "专注" to (Entity.Type.CONCEPT to "生活"),
            "拖延" to (Entity.Type.CONCEPT to "生活"),
            "焦虑" to (Entity.Type.CONCEPT to "生活"),
            "抑郁" to (Entity.Type.CONCEPT to "生活"),
            "幸福感" to (Entity.Type.CONCEPT to "生活"),
            "人际关系" to (Entity.Type.CONCEPT to "生活"),
            "家庭" to (Entity.Type.CONCEPT to "生活"),
            "友谊" to (Entity.Type.CONCEPT to "生活"),
            "爱情" to (Entity.Type.CONCEPT to "生活"),
            "婚姻" to (Entity.Type.CONCEPT to "生活"),
            "育儿" to (Entity.Type.METHOD to "生活"),
            "旅行" to (Entity.Type.CONCEPT to "生活"),
            "阅读" to (Entity.Type.METHOD to "生活"),
            "写作" to (Entity.Type.METHOD to "生活"),
            "烹饪" to (Entity.Type.METHOD to "生活"),
            "园艺" to (Entity.Type.METHOD to "生活"),
            "宠物" to (Entity.Type.CONCEPT to "生活"),
            "自然" to (Entity.Type.CONCEPT to "生活"),
            "环保" to (Entity.Type.CONCEPT to "生活"),
            "可持续" to (Entity.Type.CONCEPT to "生活"),
            "低碳" to (Entity.Type.CONCEPT to "生活"),
        ).forEach { (term, typeDomain) ->
            dict[term] = DictEntry(typeDomain.first, typeDomain.second)
        }

        // ── Education (30+ 条) ──
        listOf(
            "教育" to (Entity.Type.CONCEPT to "教育"),
            "学习" to (Entity.Type.METHOD to "教育"),
            "教学" to (Entity.Type.METHOD to "教育"),
            "课程" to (Entity.Type.CONCEPT to "教育"),
            "知识" to (Entity.Type.CONCEPT to "教育"),
            "技能" to (Entity.Type.CONCEPT to "教育"),
            "考试" to (Entity.Type.CONCEPT to "教育"),
            "记忆" to (Entity.Type.CONCEPT to "教育"),
            "理解" to (Entity.Type.METHOD to "教育"),
            "复习" to (Entity.Type.METHOD to "教育"),
            "笔记" to (Entity.Type.CONCEPT to "教育"),
            "论文" to (Entity.Type.CONCEPT to "教育"),
            "研究" to (Entity.Type.METHOD to "教育"),
            "实验" to (Entity.Type.METHOD to "教育"),
            "文献" to (Entity.Type.CONCEPT to "教育"),
            "实验设计" to (Entity.Type.METHOD to "教育"),
            "假设" to (Entity.Type.CONCEPT to "教育"),
            "数据收集" to (Entity.Type.METHOD to "教育"),
            "定量分析" to (Entity.Type.METHOD to "教育"),
            "定性分析" to (Entity.Type.METHOD to "教育"),
            "批判性思维" to (Entity.Type.CONCEPT to "教育"),
            "创造力" to (Entity.Type.CONCEPT to "教育"),
            "终身学习" to (Entity.Type.CONCEPT to "教育"),
            "在线学习" to (Entity.Type.CONCEPT to "教育"),
            "互动教学" to (Entity.Type.METHOD to "教育"),
            "评估" to (Entity.Type.METHOD to "教育"),
            "反馈" to (Entity.Type.METHOD to "教育"),
        ).forEach { (term, typeDomain) ->
            dict[term] = DictEntry(typeDomain.first, typeDomain.second)
        }

        // ── Law & Society (25+ 条) ──
        listOf(
            "法律" to (Entity.Type.CONCEPT to "法律"),
            "权利" to (Entity.Type.CONCEPT to "法律"),
            "义务" to (Entity.Type.CONCEPT to "法律"),
            "合同" to (Entity.Type.CONCEPT to "法律"),
            "侵权" to (Entity.Type.CONCEPT to "法律"),
            "犯罪" to (Entity.Type.CONCEPT to "法律"),
            "刑法" to (Entity.Type.CONCEPT to "法律"),
            "民法" to (Entity.Type.CONCEPT to "法律"),
            "宪法" to (Entity.Type.CONCEPT to "法律"),
            "司法" to (Entity.Type.CONCEPT to "法律"),
            "律师" to (Entity.Type.CONCEPT to "法律"),
            "法院" to (Entity.Type.CONCEPT to "法律"),
            "证据" to (Entity.Type.CONCEPT to "法律"),
            "知识产权" to (Entity.Type.CONCEPT to "法律"),
            "隐私" to (Entity.Type.CONCEPT to "法律"),
            "数据保护" to (Entity.Type.CONCEPT to "法律"),
            "GDPR" to (Entity.Type.CONCEPT to "法律"),
            "社会" to (Entity.Type.CONCEPT to "社会"),
            "公平" to (Entity.Type.CONCEPT to "社会"),
            "民主" to (Entity.Type.CONCEPT to "社会"),
            "多样性" to (Entity.Type.CONCEPT to "社会"),
            "包容" to (Entity.Type.CONCEPT to "社会"),
            "伦理" to (Entity.Type.CONCEPT to "社会"),
            "责任" to (Entity.Type.CONCEPT to "社会"),
        ).forEach { (term, typeDomain) ->
            dict[term] = DictEntry(typeDomain.first, typeDomain.second)
        }

        // ── Medicine (25+ 条) ──
        listOf(
            "医学" to (Entity.Type.CONCEPT to "医学"),
            "诊断" to (Entity.Type.METHOD to "医学"),
            "治疗" to (Entity.Type.METHOD to "医学"),
            "手术" to (Entity.Type.METHOD to "医学"),
            "药物" to (Entity.Type.TOOL to "医学"),
            "症状" to (Entity.Type.CONCEPT to "医学"),
            "疾病" to (Entity.Type.CONCEPT to "医学"),
            "肿瘤" to (Entity.Type.CONCEPT to "医学"),
            "心血管" to (Entity.Type.CONCEPT to "医学"),
            "糖尿病" to (Entity.Type.CONCEPT to "医学"),
            "癌症" to (Entity.Type.CONCEPT to "医学"),
            "卒中" to (Entity.Type.CONCEPT to "医学"),
            "抗生素" to (Entity.Type.TOOL to "医学"),
            "X光" to (Entity.Type.TOOL to "医学"),
            "MRI" to (Entity.Type.TOOL to "医学"),
            "CT扫描" to (Entity.Type.TOOL to "医学"),
            "基因检测" to (Entity.Type.METHOD to "医学"),
            "免疫疗法" to (Entity.Type.METHOD to "医学"),
            "中医" to (Entity.Type.CONCEPT to "医学"),
            "针灸" to (Entity.Type.METHOD to "医学"),
            "经络" to (Entity.Type.CONCEPT to "医学"),
            "气血" to (Entity.Type.CONCEPT to "医学"),
            "康复" to (Entity.Type.METHOD to "医学"),
            "护理" to (Entity.Type.METHOD to "医学"),
        ).forEach { (term, typeDomain) ->
            dict[term] = DictEntry(typeDomain.first, typeDomain.second)
        }

        // ── Economy & Finance (25+ 条) ──
        listOf(
            "经济" to (Entity.Type.CONCEPT to "金融"),
            "金融" to (Entity.Type.CONCEPT to "金融"),
            "股票" to (Entity.Type.CONCEPT to "金融"),
            "债券" to (Entity.Type.CONCEPT to "金融"),
            "基金" to (Entity.Type.CONCEPT to "金融"),
            "期货" to (Entity.Type.CONCEPT to "金融"),
            "外汇" to (Entity.Type.CONCEPT to "金融"),
            "通胀" to (Entity.Type.CONCEPT to "金融"),
            "利率" to (Entity.Type.CONCEPT to "金融"),
            "货币政策" to (Entity.Type.CONCEPT to "金融"),
            "财政" to (Entity.Type.CONCEPT to "金融"),
            "税收" to (Entity.Type.CONCEPT to "金融"),
            "GDP" to (Entity.Type.CONCEPT to "金融"),
            "消费" to (Entity.Type.CONCEPT to "金融"),
            "储蓄" to (Entity.Type.METHOD to "金融"),
            "理财" to (Entity.Type.METHOD to "金融"),
            "保险" to (Entity.Type.CONCEPT to "金融"),
            "银行" to (Entity.Type.CONCEPT to "金融"),
            "证券" to (Entity.Type.CONCEPT to "金融"),
            "资产" to (Entity.Type.CONCEPT to "金融"),
            "负债" to (Entity.Type.CONCEPT to "金融"),
            "资产负债表" to (Entity.Type.CONCEPT to "金融"),
            "现金流" to (Entity.Type.CONCEPT to "金融"),
            "估值" to (Entity.Type.METHOD to "金融"),
        ).forEach { (term, typeDomain) ->
            dict[term] = DictEntry(typeDomain.first, typeDomain.second)
        }

        // ── Art & Design (20+ 条) ──
        listOf(
            "设计" to (Entity.Type.METHOD to "艺术"),
            "美学" to (Entity.Type.CONCEPT to "艺术"),
            "色彩" to (Entity.Type.CONCEPT to "艺术"),
            "构图" to (Entity.Type.CONCEPT to "艺术"),
            "字体" to (Entity.Type.CONCEPT to "艺术"),
            "排版" to (Entity.Type.METHOD to "艺术"),
            "摄影" to (Entity.Type.METHOD to "艺术"),
            "插画" to (Entity.Type.CONCEPT to "艺术"),
            "动画" to (Entity.Type.CONCEPT to "艺术"),
            "UI设计" to (Entity.Type.METHOD to "艺术"),
            "UX" to (Entity.Type.CONCEPT to "艺术"),
            "用户体验" to (Entity.Type.CONCEPT to "艺术"),
            "极简主义" to (Entity.Type.CONCEPT to "艺术"),
            "抽象" to (Entity.Type.CONCEPT to "艺术"),
            "写实" to (Entity.Type.CONCEPT to "艺术"),
            "写意" to (Entity.Type.CONCEPT to "艺术"),
            "雕塑" to (Entity.Type.CONCEPT to "艺术"),
            "建筑" to (Entity.Type.CONCEPT to "艺术"),
            "园林" to (Entity.Type.CONCEPT to "艺术"),
        ).forEach { (term, typeDomain) ->
            dict[term] = DictEntry(typeDomain.first, typeDomain.second)
        }

        return dict
    }
}
