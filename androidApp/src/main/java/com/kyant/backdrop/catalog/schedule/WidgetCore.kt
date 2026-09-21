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
import java.util.Calendar

/**
 * 小组件公有数据与辅助函数：供各款式 Provider 复用，避免重复实现。
 */
object WidgetCore {

    data class Session(val nowMinutes: Int)

    fun session(): Session {
        val c = Calendar.getInstance()
        return Session(c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE))
    }

    data class Item(val course: Course, val startMin: Int?, val endMin: Int?)

    /** 当日全部课程，按时段升序。 */
    fun dayItems(store: ScheduleStore): List<Item> {
        val today = todayEpochDay()
        return store.coursesFor(today).mapNotNull { course ->
            val start = store.periods.getOrNull(course.startPeriod - 1)
            val end = store.periods.getOrNull(course.endPeriod - 1)
            Item(
                course,
                start?.let { minutesOf(it.start) },
                end?.let { minutesOf(it.end) }
            )
        }.sortedBy { it.startMin ?: Int.MAX_VALUE }
    }

    fun ongoing(items: List<Item>, now: Int): Item? =
        items.firstOrNull {
            it.startMin != null && it.endMin != null && now in it.startMin..it.endMin
        }

    fun upcoming(items: List<Item>, now: Int): Item? =
        items.firstOrNull { it.startMin != null && it.startMin > now }

    /** 对整卡点击跳转到应用。 */
    fun applyOpenClick(context: Context, views: RemoteViews, widgetId: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context,
            widgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, pending)
    }

    /** 全部小组件款式 Provider 的类，供全局刷新使用。 */
    private fun providerClasses(): List<Class<out AppWidgetProvider>> = listOf(
        ScheduleWidgetProvider::class.java,
        ScheduleWidgetListProvider::class.java,
        ScheduleWidgetCountdownProvider::class.java,
        ScheduleWidgetWeekProvider::class.java
    )

    /** 数据变化时刷新所有已放置的小组件。 */
    fun refreshAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        for (clazz in providerClasses()) {
            val component = ComponentName(context, clazz)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isEmpty()) continue
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
                this.component = component
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
    }
}