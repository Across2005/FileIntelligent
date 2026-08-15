package com.crossk.data

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Creative export engine — exports knowledge graph snapshots and postcards
 * as HTML, Markdown, or image files.
 */
class PostcardExporter(private val context: Context) {

    /**
     * Export postcard as a self-contained HTML file.
     */
    fun exportHtml(project: PostcardProject, nodes: List<GraphNode>): File {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val timestamp = dateFormat.format(Date(project.createdAt))

        val bgStyle = when (val bg = project.backgroundType) {
            is PostcardBackground.Gradient -> buildGradientCss(bg.startColor, bg.endColor)
            is PostcardBackground.Solid -> bgColorToHex(bg.color)
        }

        val layersHtml = buildLayerHtml(project.layers)

        val html = """
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${escapeHtml(project.title)}</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            font-family: -apple-system, 'Noto Sans SC', 'PingFang SC', sans-serif;
            background: #0F0F1A;
            padding: 20px;
        }
        .postcard {
            width: 1080px;
            max-width: 100%;
            min-height: 720px;
            background: $bgStyle;
            border-radius: 16px;
            padding: 40px;
            position: relative;
            overflow: hidden;
            box-shadow: 0 20px 60px rgba(0,0,0,0.5);
        }
        .postcard .layer-text {
            position: absolute;
            color: #F0EAEC;
            font-size: 18px;
            line-height: 1.6;
            text-shadow: 0 2px 4px rgba(0,0,0,0.3);
        }
        .postcard .layer-graph {
            position: absolute;
            background: rgba(255,255,255,0.05);
            border-radius: 12px;
            padding: 20px;
            backdrop-filter: blur(8px);
            border: 1px solid rgba(255,255,255,0.1);
        }
        .postcard .layer-graph h3 {
            color: #E8A0BF;
            font-size: 14px;
            margin-bottom: 8px;
        }
        .postcard .layer-graph .stat {
            display: inline-block;
            margin-right: 16px;
            color: #B5C9B7;
            font-size: 12px;
        }
        .postcard .footer {
            position: absolute;
            bottom: 20px;
            right: 30px;
            color: rgba(255,255,255,0.3);
            font-size: 11px;
        }
        .postcard .node-list {
            display: flex;
            flex-wrap: wrap;
            gap: 6px;
            margin-top: 8px;
        }
        .postcard .node-tag {
            padding: 2px 10px;
            border-radius: 999px;
            font-size: 11px;
            background: rgba(232,160,191,0.15);
            color: #E8A0BF;
        }
        .watermark {
            position: absolute;
            bottom: 20px;
            left: 30px;
            color: rgba(255,255,255,0.15);
            font-size: 10px;
            letter-spacing: 2px;
        }
    </style>
</head>
<body>
    <div class="postcard">
        <h1 style="color:#E8A0BF;font-size:28px;margin-bottom:8px;">
            ${escapeHtml(project.title)}
        </h1>
        <p style="color:rgba(255,255,255,0.4);font-size:12px;margin-bottom:24px;">
            $timestamp · 文件智析
        </p>
        $layersHtml
        <div class="node-list" style="margin-top: 40px;">
            ${nodes.take(12).joinToString("") { "<span class=\"node-tag\">${escapeHtml(it.label)}</span>" }}
        </div>
        <div class="watermark">✦ 文件智析 · File Intelligence</div>
    </div>
</body>
</html>
        """.trimIndent()

        val file = File(context.cacheDir, "postcard_${project.id.take(8)}.html")
        file.writeText(html)
        return file
    }

    /**
     * Export as Markdown summary.
     */
    fun exportMarkdown(project: PostcardProject, nodes: List<GraphNode>): File {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val timestamp = dateFormat.format(Date(project.createdAt))

        val textLayers = project.layers
            .filter { it.content is LayerContent.TextContent }
            .map { (it.content as LayerContent.TextContent).text }
            .joinToString("\n\n")

        val md = """
# ${project.title}

> 生成时间：$timestamp · 来源：文件智析

## 知识节点

${nodes.joinToString("\n") { "- **${it.label}** (${it.type.name})" }}

## 明信片笔记

$textLayers

---

*由 文件智析 (File Intelligence) 生成 — 本地私有知识图谱与成长记录*
        """.trimIndent()

        val file = File(context.cacheDir, "postcard_${project.id.take(8)}.md")
        file.writeText(md)
        return file
    }

    private fun buildLayerHtml(layers: List<PostcardLayer>): String {
        return layers.sortedBy { it.zIndex }.joinToString("\n") { layer ->
            val style = "left:${layer.translation.x}px;top:${layer.translation.y}px;"
            when (val content = layer.content) {
                is LayerContent.TextContent -> {
                    val colorHex = bgColorToHex(content.color)
                    """<div class="layer-text" style="$style color:$colorHex;font-size:${content.fontSize}px;font-weight:${content.fontWeight.weight};">${escapeHtml(content.text)}</div>"""
                }
                is LayerContent.GraphSnapshot -> {
                    """<div class="layer-graph" style="$style">
                        <h3>${escapeHtml(content.title)}</h3>
                        <span class="stat">节点 ${content.nodeCount}</span>
                        <span class="stat">关系 ${content.edgeCount}</span>
                    </div>"""
                }
                is LayerContent.StatBadge -> {
                    val colorHex = bgColorToHex(content.color)
                    """<div class="layer-text" style="$style font-size:14px;background:${colorHex}15;padding:6px 14px;border-radius:8px;">
                        ${escapeHtml(content.label)}: <strong>${escapeHtml(content.value)}</strong>
                    </div>"""
                }
            }
        }
    }

    private fun buildGradientCss(start: Color, end: Color): String {
        return "linear-gradient(135deg, ${bgColorToHex(start)}, ${bgColorToHex(end)})"
    }

    companion object {
        fun bgColorToHex(color: Color): String {
            return String.format("#%06X", 0xFFFFFF and color.toArgb())
        }

        fun escapeHtml(text: String): String {
            return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
        }
    }
}
