package com.kyant.backdrop.catalog.schedule

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.kyant.backdrop.catalog.MainActivity
import com.kyant.backdrop.catalog.R

/**
 * Android 16 Live Updates：把「正在进行的课程」展示为系统实时活动。
 *
 * 依据官方规范：
 *  - 只有"进行中、用户主动触发、时间敏感"的活动适合 Live Updates，
 *    因此本管理器仅在当前有进行中的课时发布，下课/不在上课时自动撤销。
 *  - 使用 NotificationCompat.ProgressStyle + POST_PROMOTED_NOTIFICATIONS 权限
 *    + setRequestPromotedOngoing(true)，在支持状态栏 chip 的 Android 16 QPR1+
 *    上可上状态栏/灵动区（OPPO ColorOS 16 已确认原生兼容）。
 */
object LiveUpdateManager {

    private const val CHANNEL_ID = "live_class_update"
    private const val LIVE_NOTIFICATION_ID = 0x5A11

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "上课实时活动",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "上课中显示课程进行状态"
                setShowBadge(false)
            }
        )
    }

    /** 根据当前课程状态发布或撤销 Live Updates。 */
    fun sync(context: Context, store: ScheduleStore) {
        ensureChannel(context)
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) {
            manager.cancel(LIVE_NOTIFICATION_ID)
            return
        }

        val today = todayEpochDay()
        val resolution = store.resolveDay(today)
        if (resolution.type == DayType.HOLIDAY) {
            cancel(manager)
            return
        }
        val now = currentHourNow()

        // 找到正在进行的课程。
        val ongoing = store.coursesFor(today).mapNotNull { course ->
            val s = store.periods.getOrNull(course.startPeriod - 1)?.let { minutesOf(it.start) }
            val e = store.periods.getOrNull(course.endPeriod - 1)?.let { minutesOf(it.end) }
            if (s != null && e != null && now in s..e) Triple(course, s, e) else null
        }.firstOrNull()

        if (ongoing == null) {
            cancel(manager)
            return
        }

        val (course, startMin, endMin) = ongoing
        val total = endMin - startMin
        val elapsed = now - startMin
        val remain = endMin - now
        val fraction = (elapsed.toFloat() / total.toFloat()).coerceIn(0f, 1f)

        val openIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(course.name.ifBlank { "未命名课程" })
            .setContentText(
                "${course.periodLabel()} · ${course.location.ifBlank { "上课中" }} · 还剩 $remain 分钟"
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setAutoCancel(false)
            .setContentIntent(openIntent)
            .setProgress(100, (fraction * 100).toInt(), false)
            // Android 16 Live Updates：进度型 + 请求提升。
            .setStyle(NotificationCompat.ProgressStyle())
            .setShortCriticalText(chronoText(remain))

        // Android 16 Live Updates：请求把该通知提升为"常驻/状态栏 chip"。
        // NotificationCompat 通过 EXTRA_REQUEST_PROMOTED_ONGOING 向下兼容，
        // 在不支持提升的系统上会降级为普通常驻通知。
        builder.setRequestPromotedOngoing(true)

        @SuppressLint("MissingPermission")
        manager.notify(LIVE_NOTIFICATION_ID, builder.build())
    }

    private fun cancel(manager: NotificationManagerCompat) {
        manager.cancel(LIVE_NOTIFICATION_ID)
    }

    /** 状态栏倒计时文字，如「25 分」「1 时」。 */
    private fun chronoText(remainMinutes: Int): String =
        if (remainMinutes >= 60) "${remainMinutes / 60}小时${remainMinutes % 60}分" else "$remainMinutes 分"
}

/** 当前时刻的绝对分钟（0..1439）。 */
private fun currentHourNow(): Int = currentHourMinute().let { it.first * 60 + it.second }