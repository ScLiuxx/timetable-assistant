package com.kyant.backdrop.catalog.schedule

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import androidx.core.graphics.ColorUtils
import androidx.compose.ui.graphics.toArgb
import com.kyant.backdrop.catalog.R

/**
 * 下一节大字倒计时款（4x2）：超大字号展示下一节/进行中课程，并给出开始/结束倒计时。
 */
class ScheduleWidgetCountdownProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            val store = ScheduleStore(context)
            val views = RemoteViews(context.packageName, R.layout.schedule_widget_countdown_layout)
            val now = WidgetCore.session().nowMinutes
            val items = WidgetCore.dayItems(store)
            val ongoing = WidgetCore.ongoing(items, now)
            val upcoming = WidgetCore.upcoming(items, now)
            val next = ongoing ?: upcoming
            val today = todayEpochDay()
            val week = store.weekOf(today).coerceAtLeast(1)
            val resolution = store.resolveDay(today)
            val accent = accentColorPreset(store.accentColorIndex).toArgb()

            val caption = when {
                resolution.type == DayType.HOLIDAY -> "今日放假 · ${formatMonthDay(today)}"
                resolution.type == DayType.MAKEUP ->
                    "今日调休 · 按${weekdayShortName(resolution.courseDayOfWeek)}"
                else -> "第 $week 周 ${weekdayShortName(resolution.courseDayOfWeek)} · ${formatMonthDay(today)}"
            }
            views.setTextViewText(R.id.widget_caption, caption)
            views.setTextColor(R.id.widget_caption, ColorUtils.setAlphaComponent(accent, 160))

            if (next == null) {
                views.setTextViewText(R.id.widget_course, if (items.isEmpty()) "今天没课啦" else "今日课程已结束")
                views.setTextViewText(R.id.widget_countdown, if (items.isEmpty()) "休息" else "结束")
                views.setTextViewText(R.id.widget_meta, if (items.isEmpty()) "去添加课程或安排自习吧" else "明天见")
                WidgetCore.applyOpenClick(context, views, widgetId)
                appWidgetManager.updateAppWidget(widgetId, views)
                continue
            }

            val course = next.course
            views.setTextViewText(R.id.widget_course, course.name.ifBlank { "未命名课程" })

            val meta = buildString {
                val s = store.periods.getOrNull(course.startPeriod - 1)?.start ?: ""
                val e = store.periods.getOrNull(course.endPeriod - 1)?.end ?: ""
                if (s.isNotBlank() && e.isNotBlank()) append("$s–$e")
                if (course.location.isNotBlank()) append(" · ${course.location}")
                append(" · ${course.weeksLabel()}")
            }
            views.setTextViewText(R.id.widget_meta, meta)
            views.setTextColor(R.id.widget_meta, ColorUtils.setAlphaComponent(accent, 150))

            val countdown: String
            val countdownColor: Int
            if (ongoing != null) {
                val remain = (ongoing.endMin ?: 0) - now
                countdown = if (remain > 0) "还有 $remain 分" else "进行中"
                countdownColor = accent
            } else {
                val diff = (upcoming?.startMin ?: 0) - now
                countdown = if (diff > 0) "还有 $diff 分" else "即将开始"
                countdownColor = accent
            }
            views.setTextViewText(R.id.widget_countdown, countdown)
            views.setTextColor(R.id.widget_countdown, countdownColor)

            WidgetCore.applyOpenClick(context, views, widgetId)
            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}