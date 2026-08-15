package com.crossk.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.crossk.CrossKApp
import com.crossk.MainActivity
import com.crossk.R

/**
 * App Widget showing quick stats and a launch button.
 */
class CrossKWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val app = try {
            context.applicationContext as? CrossKApp
        } catch (_: ClassCastException) {
            null
        }

        val stats = app?.fileRepository?.getStats()
        val level = app?.fileRepository?.level

        appWidgetIds.forEach { widgetId ->
            val views = RemoteViews(context.packageName, R.layout.widget_file_intelligence)

            if (stats != null && level != null) {
                // Set stats
                views.setTextViewText(R.id.widget_stats, buildString {
                    append("📂 ${stats.totalFiles} 文件")
                    append("  ·  ")
                    append("🧩 ${stats.totalEntities} 实体")
                    append("  ·  ")
                    append("🏷️ ${stats.topicsCovered} 主题")
                })
                views.setTextViewText(R.id.widget_level, "Lv.${level.level} ${level.title}")
            } else {
                views.setTextViewText(R.id.widget_stats, "📂 数据加载中…")
                views.setTextViewText(R.id.widget_level, "文件智析")
            }

            // Open app on tap
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}
