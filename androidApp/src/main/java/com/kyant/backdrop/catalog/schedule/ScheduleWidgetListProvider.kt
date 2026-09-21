package com.kyant.backdrop.catalog.schedule

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.view.View
import android.widget.RemoteViews
import androidx.compose.ui.graphics.toArgb
import com.kyant.backdrop.catalog.R

/**
 * 今日课程时间表列表款（4x3）：把今天每门课的节次时间、名称、地点逐行列出。
 */
class ScheduleWidgetListProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            val store = ScheduleStore(context)
            val views = RemoteViews(context.packageName, R.layout.schedule_widget_list_layout)
            val items = WidgetCore.dayItems(store)
            val today = todayEpochDay()
            val week = store.weekOf(today).coerceAtLeast(1)
            val resolution = store.resolveDay(today)
            val accent = accentColorPreset(store.accentColorIndex).toArgb()

            val title = when {
                resolution.type == DayType.HOLIDAY -> "今日放假"
                resolution.type == DayType.MAKEUP ->
                    "今日调休 · 按${weekdayShortName(resolution.courseDayOfWeek)}"
                else -> "今日 · 第 $week 周 ${weekdayShortName(resolution.courseDayOfWeek)}"
            }
            views.setTextViewText(R.id.widget_title, title)
            views.setTextColor(R.id.widget_title, accent)

            val subtitle = when {
                resolution.type == DayType.HOLIDAY -> resolution.note
                items.isEmpty() -> "今天没有课 · ${formatMonthDay(today)}"
                else -> "共 ${items.size} 节课 · ${formatMonthDay(today)}"
            }
            views.setTextViewText(R.id.widget_subtitle, subtitle)

            val rowIds = listOf(
                R.id.widget_row1, R.id.widget_row2, R.id.widget_row3, R.id.widget_row4
            )
            val timeIds = listOf(
                R.id.widget_row1_time, R.id.widget_row2_time,
                R.id.widget_row3_time, R.id.widget_row4_time
            )
            val nameIds = listOf(
                R.id.widget_row1_name, R.id.widget_row2_name,
                R.id.widget_row3_name, R.id.widget_row4_name
            )
            val metaIds = listOf(
                R.id.widget_row1_meta, R.id.widget_row2_meta,
                R.id.widget_row3_meta, R.id.widget_row4_meta
            )

            items.take(4).forEachIndexed { idx, item ->
                views.setViewVisibility(rowIds[idx], View.VISIBLE)
                val period = store.periods.getOrNull(item.course.startPeriod - 1)
                views.setTextViewText(timeIds[idx], period?.start ?: "")
                views.setTextViewText(nameIds[idx], item.course.name.ifBlank { "未命名课程" })
                val metaText = listOfNotNull(
                    item.course.location.takeIf { it.isNotBlank() },
                    item.course.teacher.takeIf { it.isNotBlank() }
                ).joinToString(" ")
                views.setTextViewText(metaIds[idx], metaText)
            }
            for (i in items.size until 4) {
                views.setViewVisibility(rowIds[i], View.GONE)
            }
            if (items.size > 4) {
                views.setTextViewText(R.id.widget_subtitle, "$subtitle · +${items.size - 4} 节")
            }

            WidgetCore.applyOpenClick(context, views, widgetId)
            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}