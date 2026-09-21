package com.kyant.backdrop.catalog.schedule

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.kyant.backdrop.catalog.MainActivity
import com.kyant.backdrop.catalog.R

/**
 * 跨厂商通用的桌面小组件：展示「今日 / 下一节课」。
 * 基于 Android 标准 AppWidget（AppWidgetProvider + RemoteViews），
 * 小米 / 荣耀 / OPPO / vivo 的桌面均原生支持，无需厂商 SDK 或审核。
 */
class ScheduleWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId)
        }
    }

    companion object {

        /** 广播通知：课表数据变化后强制刷新桌面小组件。 */
        const val ACTION_REFRESH = "com.kyant.backdrop.catalog.action.REFRESH_WIDGET"

        /** 在应用内数据持久化后调用，刷新所有同应用的小组件。 */
        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, ScheduleWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            for (id in ids) {
                updateWidget(context, manager, id)
            }
        }

        private fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.schedule_widget_layout)

            val store = ScheduleStore(context)
            val today = todayEpochDay()
            val resolution = store.resolveDay(today)
            val week = store.weekOf(today).coerceAtLeast(1)
            val courses = store.coursesFor(today)
            val (nowHour, nowMinute) = currentHourMinute()
            val nowMinutes = nowHour * 60 + nowMinute

            data class Item(val course: Course, val startMin: Int?, val endMin: Int?)

            val items = courses.mapNotNull { course ->
                val s = store.periods.getOrNull(course.startPeriod - 1)
                val e = store.periods.getOrNull(course.endPeriod - 1)
                Item(course, s?.let { minutesOf(it.start) }, e?.let { minutesOf(it.end) })
            }
            val ongoing = items.firstOrNull {
                it.startMin != null && it.endMin != null && nowMinutes in it.startMin..it.endMin
            }
            val upcoming = items.firstOrNull { it.startMin != null && it.startMin > nowMinutes }
            val next = ongoing ?: upcoming

            val title = when {
                resolution.type == DayType.HOLIDAY -> "今日放假"
                resolution.type == DayType.MAKEUP ->
                    "今日调休 · 按${weekdayShortName(resolution.courseDayOfWeek)}"
                else -> "第 $week 周 ${weekdayShortName(resolution.courseDayOfWeek)}"
            }

            var subtitle = ""
            when {
                resolution.type == DayType.HOLIDAY -> subtitle = resolution.note
                courses.isEmpty() -> subtitle = "今天没有课"
                next == null -> subtitle = "今日课程已结束"
                next.startMin != null && next.endMin != null &&
                    nowMinutes in next.startMin..next.endMin -> {
                    subtitle = "正在上课，还剩 ${next.endMin - nowMinutes} 分钟"
                }
                next.startMin != null -> {
                    val diff = next.startMin - nowMinutes
                    subtitle = if (diff <= 0) "即将开始" else "还有 $diff 分钟开始"
                }
            }

            val courseText = when {
                next == null -> if (courses.isEmpty()) "去添加课程吧" else ""
                else -> next.course.name.ifBlank { "未命名课程" }
            }
            val meta = if (next == null) {
                ""
            } else {
                buildString {
                    append(next.course.periodLabel())
                    val s = store.periods.getOrNull(next.course.startPeriod - 1)?.start ?: ""
                    val e = store.periods.getOrNull(next.course.endPeriod - 1)?.end ?: ""
                    if (s.isNotBlank()) append("  $s–$e")
                    if (next.course.location.isNotBlank()) append("  @${next.course.location}")
                    if (next.course.teacher.isNotBlank()) append("  ${next.course.teacher}")
                    append("  ${next.course.weeksLabel()}")
                }
            }

            views.setTextViewText(R.id.widget_title, title)
            views.setTextViewText(R.id.widget_subtitle, subtitle)
            views.setTextViewText(R.id.widget_course, courseText)
            views.setTextViewText(R.id.widget_meta, meta)

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pending = PendingIntent.getActivity(
                context,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pending)

            manager.updateAppWidget(widgetId, views)
        }
    }
}