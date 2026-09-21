package com.kyant.backdrop.catalog.schedule

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.graphics.ColorUtils
import com.kyant.backdrop.catalog.MainActivity
import com.kyant.backdrop.catalog.R
import androidx.compose.ui.graphics.toArgb

/**
 * 跨厂商通用的桌面小组件：展示「今日 / 下一节课」。
 * 基于 Android 标准 AppWidget（AppWidgetProvider + RemoteViews），
 * 小米 / 荣耀 / OPPO / vivo 桌面原生支持，无需厂商 SDK 或审核。
 *
 * 支持两种尺寸模板（紧凑 2x2 / 宽 4x2）并跟随应用主题色（accentColor）。
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

        const val ACTION_REFRESH = "com.kyant.backdrop.catalog.action.REFRESH_WIDGET"

        /** 数据构建结果，供不同尺寸共用。 */
        private data class BindData(
            val layoutId: Int,
            val title: String,
            val subtitle: String,
            val course: String,
            val meta: String,
            val badge: String,
            val isOngoing: Boolean
        )

        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, ScheduleWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            for (id in ids) {
                updateWidget(context, manager, id)
            }
        }

        private fun bindData(
            context: Context,
            manager: AppWidgetManager,
            widgetId: Int
        ): BindData {
            val options = manager.getAppWidgetOptions(widgetId)
            val widthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 200)
            // 大多数 Launcher 一个 appwidget cell ≈ 63-70dp；宽模板需要能呈现更多信息。
            val useLarge = widthDp >= 260

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
            var badge = "今日"
            when {
                resolution.type == DayType.HOLIDAY -> {
                    subtitle = resolution.note
                    badge = "放假"
                }
                courses.isEmpty() -> {
                    subtitle = "今天没有课"
                    badge = "休息"
                }
                next == null -> {
                    subtitle = "今日课程已结束"
                    badge = "休息"
                }
                next.startMin != null && next.endMin != null &&
                    nowMinutes in next.startMin..next.endMin -> {
                    badge = "进行中"
                    subtitle = "还有 ${next.endMin - nowMinutes} 分钟结束 · ${next.course.periodLabel()}"
                }
                next.startMin != null -> {
                    val diff = next.startMin - nowMinutes
                    badge = "下一节"
                    subtitle = if (diff <= 0) "即将开始" else "还有 $diff 分钟开始"
                }
            }

            val course = when {
                next == null -> if (courses.isEmpty()) "去添加课程吧" else ""
                else -> next.course.name.ifBlank { "未命名课程" }
            }
            val meta = if (next == null) {
                ""
            } else {
                buildString {
                    val s = store.periods.getOrNull(next.course.startPeriod - 1)?.start ?: ""
                    val e = store.periods.getOrNull(next.course.endPeriod - 1)?.end ?: ""
                    if (s.isNotBlank()) append("$s–$e")
                    if (next.course.location.isNotBlank()) append("  @${next.course.location}")
                    if (next.course.teacher.isNotBlank()) append("  ${next.course.teacher}")
                    append("  ${next.course.weeksLabel()}")
                }
            }

            return BindData(
                layoutId = if (useLarge) R.layout.schedule_widget_large_layout else R.layout.schedule_widget_layout,
                title = title,
                subtitle = subtitle,
                course = course,
                meta = meta,
                badge = badge,
                isOngoing = ongoing != null
            )
        }

        private fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val data = bindData(context, manager, widgetId)
            val views = RemoteViews(context.packageName, data.layoutId)

            views.setTextViewText(R.id.widget_title, data.title)
            views.setTextViewText(R.id.widget_subtitle, data.subtitle)
            views.setTextViewText(R.id.widget_course, data.course)
            views.setTextViewText(R.id.widget_meta, data.meta)

            // 主题色：读取应用的 accentColorIndex，动态应用到标题与标签。
            val store = ScheduleStore(context)
            val accent = accentColorPreset(store.accentColorIndex).toArgb()
            views.setTextColor(R.id.widget_title, accent)
            if (data.layoutId == R.layout.schedule_widget_large_layout) {
                views.setTextViewText(R.id.widget_badge, data.badge)
                val badgeColor = if (data.isOngoing) accent else ColorUtils.setAlphaComponent(accent, 190)
                views.setInt(R.id.widget_badge, "setBackgroundColor", badgeColor)
            } else {
                // 紧凑布局的副标题使用主题微调色。
                val subdued = ColorUtils.setAlphaComponent(accent, 120)
                views.setTextColor(R.id.widget_subtitle, subdued)
            }

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