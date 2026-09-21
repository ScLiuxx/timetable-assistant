package com.kyant.backdrop.catalog.schedule

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import androidx.compose.ui.graphics.toArgb
import com.kyant.backdrop.catalog.R

/**
 * 本周课表周视图款（4x4）：按当前周展示周一至周日的课程名称概览，支持左右滑动翻周。
 */
class ScheduleWidgetWeekProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            val store = ScheduleStore(context)
            val views = RemoteViews(context.packageName, R.layout.schedule_widget_week_layout)
            val today = todayEpochDay()
            val currentWeek = store.weekOf(today).coerceAtLeast(1)
            val accent = accentColorPreset(store.accentColorIndex).toArgb()

            views.setTextViewText(R.id.widget_title, "第 $currentWeek 周课表")
            views.setTextColor(R.id.widget_title, accent)

            val cellLabels = listOf(
                R.id.widget_day1_label, R.id.widget_day2_label, R.id.widget_day3_label,
                R.id.widget_day4_label, R.id.widget_day5_label, R.id.widget_day6_label,
                R.id.widget_day7_label
            )
            val cellTexts = listOf(
                R.id.widget_day1_a, R.id.widget_day2_a, R.id.widget_day3_a,
                R.id.widget_day4_a, R.id.widget_day5_a, R.id.widget_day6_a,
                R.id.widget_day7_a
            )

            for (day in 1..7) {
                val label = weekdayShortName(day)
                views.setTextViewText(cellLabels[day - 1], label)
            }

            // 当前周每天课程的课程名（去重保留前两个，用空格分隔，长名取前两字）。
            val coursesByDay = store.coursesForWeek(currentWeek)
            for (day in 1..7) {
                val courses = coursesByDay[day] ?: emptyList()
                val text = if (courses.isEmpty()) {
                    if (day >= 6) "休" else "无课"
                } else {
                    courses.take(2).joinToString(" ") { c ->
                        val name = c.name.ifBlank { "课" }
                        if (name.length <= 2) name else name.take(2)
                    }
                }
                views.setTextViewText(cellTexts[day - 1], text)
            }

            WidgetCore.applyOpenClick(context, views, widgetId)
            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}