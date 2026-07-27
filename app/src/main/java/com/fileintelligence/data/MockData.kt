package com.fileintelligence.data

import androidx.compose.ui.graphics.Color
import com.fileintelligence.ai.AnalysisEngine

// Raw mock data without AnalysisEngine dependency (used by FileRepository)
fun generateMockFilesRaw(): List<FileItem> {
    return listOf(
        FileItem(
            id = "1", name = "认知架构笔记.md", path = "/docs/cognitive-architecture.md",
            extension = "md", sizeBytes = 3270, lastModified = System.currentTimeMillis() - 7200000,
            tags = listOf("认知", "AI"),
            content = "这是一个包含认知架构和Transformer模型讨论的文本文件。内容覆盖了注意力机制、深度学习方法和AI基础概念。",
            entities = listOf(
                Entity("e1", "认知", Entity.Type.CONCEPT, mentions = 12),
                Entity("e2", "Transformer", Entity.Type.TOOL, mentions = 8),
                Entity("e3", "注意力", Entity.Type.CONCEPT, mentions = 6),
            ),
            topics = listOf("AI 基础", "研究活动"), importance = 0.9f,
        ),
        FileItem(
            id = "2", name = "Transformer 综述.txt", path = "/docs/transformer-survey.txt",
            extension = "txt", sizeBytes = 8300, lastModified = System.currentTimeMillis() - 18000000,
            tags = listOf("ML", "深度学习"),
            content = "Transformer架构彻底改变了自然语言处理领域。BERT和GPT等模型。",
            entities = listOf(
                Entity("e1", "Transformer", Entity.Type.TOOL, mentions = 15),
                Entity("e2", "BERT", Entity.Type.TOOL, mentions = 10),
                Entity("e3", "GPT", Entity.Type.TOOL, mentions = 8),
                Entity("e4", "注意力机制", Entity.Type.CONCEPT, mentions = 7),
            ),
            topics = listOf("深度学习", "技术栈"), importance = 0.85f,
        ),
        FileItem(
            id = "3", name = "读书札记_原则.pdf", path = "/books/principles-notes.pdf",
            extension = "pdf", sizeBytes = 5840, lastModified = System.currentTimeMillis() - 86400000L,
            tags = listOf("管理", "方法"),
            content = "这是一本关于管理原则的读书笔记。讨论了优化方法和工作效率。",
            entities = listOf(
                Entity("e1", "管理", Entity.Type.CONCEPT, mentions = 9),
                Entity("e2", "方法", Entity.Type.METHOD, mentions = 6),
                Entity("e3", "优化", Entity.Type.METHOD, mentions = 4),
            ),
            topics = listOf("方法论"), importance = 0.7f,
        ),
    )
}

fun generateMockFiles(): List<FileItem> = generateMockFilesRaw()

// Mock graph nodes for visualization
fun generateMockGraphNodes(): List<GraphNode> {
    return listOf(
        GraphNode("n1", "认知架构", NodeType.CONCEPT, x = 180f, y = 300f, size = 3f),
        GraphNode("n2", "Transformer", NodeType.ENTITY, x = 100f, y = 180f, size = 2f),
        GraphNode("n3", "深度学习", NodeType.TOPIC, x = 280f, y = 200f, size = 2.2f),
        GraphNode("n4", "注意力", NodeType.CONCEPT, x = 160f, y = 420f, size = 1.8f),
        GraphNode("n5", "BERT", NodeType.ENTITY, x = 260f, y = 380f, size = 1.5f),
        GraphNode("n6", "向量化", NodeType.METHOD, x = 100f, y = 360f, size = 1.7f),
        GraphNode("n7", "嵌入", NodeType.CONCEPT, x = 100f, y = 500f, size = 1.2f),
        GraphNode("n8", "相似性", NodeType.METHOD, x = 220f, y = 520f, size = 1f),
    )
}

fun generateMockGraphEdges(): List<GraphEdge> {
    return listOf(
        GraphEdge("n1", "n2", EdgeType.REFERENCES),
        GraphEdge("n1", "n3", EdgeType.BELONGS_TO),
        GraphEdge("n1", "n4", EdgeType.DERIVES),
        GraphEdge("n2", "n5", EdgeType.DERIVES),
        GraphEdge("n2", "n6", EdgeType.REFERENCES),
        GraphEdge("n1", "n6", EdgeType.BELONGS_TO),
        GraphEdge("n4", "n7", EdgeType.DERIVES),
        GraphEdge("n6", "n7", EdgeType.REFERENCES),
        GraphEdge("n7", "n8", EdgeType.REFERENCES),
        GraphEdge("n5", "n3", EdgeType.BELONGS_TO),
    )
}
