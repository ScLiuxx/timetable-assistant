package com.kyant.backdrop.catalog.schedule

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.kyant.backdrop.catalog.MainActivity
import com.kyant.backdrop.catalog.R
import java.util.Calendar

private const val ACTION_CLASS_REMIND = "com.kyant.backdrop.catalog.action.CLASS_REMIND"
private const val REMINDER_PREFS = "reminder_prefs"
private const val KEY_SCHEDULED_CODES = "scheduled_codes"
private const val DAYS_AHEAD = 8

object ReminderScheduler {

    const val CHANNEL_ID = "class_reminder"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.reminder_channel_description)
        }
        manager.createNotificationChannel(channel)
    }

    fun reschedule(context: Context, store: ScheduleStore) {
        ensureChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        cancelScheduled(context, alarmManager)
        if (!store.reminderEnabled) return

        val now = System.currentTimeMillis()
        val leadMillis = store.reminderLeadMinutes * 60_000L
        val today = todayEpochDay()
        val codes = ArrayList<Int>()

        for (offset in 0 until DAYS_AHEAD) {
            val epochDay = today + offset
            val resolution = store.resolveDay(epochDay)
            if (resolution.type == DayType.HOLIDAY) continue
            for (course in store.coursesFor(epochDay)) {
                val period = store.periods.getOrNull(course.startPeriod - 1) ?: continue
                val minutes = minutesOf(period.start) ?: continue
                val triggerAt = localMillis(epochDay, minutes) - leadMillis
                if (triggerAt <= now) continue

                val requestCode = requestCodeFor(course.id, offset)
                val intent = Intent(context, ReminderReceiver::class.java).apply {
                    action = ACTION_CLASS_REMIND
                    putExtra(ReminderReceiver.EXTRA_COURSE, course.name)
                    putExtra(ReminderReceiver.EXTRA_LOCATION, course.location)
                    putExtra(ReminderReceiver.EXTRA_TIME, period.start)
                    putExtra(ReminderReceiver.EXTRA_PERIOD, course.startPeriod)
                    putExtra(
                        ReminderReceiver.EXTRA_DAY_LABEL,
                        weekdayShortName(resolution.courseDayOfWeek)
                    )
                }
                val pending = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                if (canScheduleExactAlarms(context)) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAt,
                        pending
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
                }
                codes.add(requestCode)
            }
        }

        context.getSharedPreferences(REMINDER_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SCHEDULED_CODES, codes.joinToString(","))
            .apply()
    }

    fun cancelAll(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        cancelScheduled(context, alarmManager)
    }

    private fun cancelScheduled(context: Context, alarmManager: AlarmManager) {
        val prefs = context.getSharedPreferences(REMINDER_PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_SCHEDULED_CODES, null) ?: return
        for (part in raw.split(",")) {
            val code = part.toIntOrNull() ?: continue
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                action = ACTION_CLASS_REMIND
            }
            val pending = PendingIntent.getBroadcast(
                context,
                code,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pending)
            pending.cancel()
        }
        prefs.edit().remove(KEY_SCHEDULED_CODES).apply()
    }

    private fun requestCodeFor(courseId: Long, offset: Int): Int {
        val base = (courseId % 1_000_000L).toInt() * 10 + offset
        return base and 0x7FFFFFFF
    }

    private fun localMillis(epochDay: Long, minutes: Int): Long {
        val (year, month, day) = epochDayToCivil(epochDay)
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, minutes / 60)
            set(Calendar.MINUTE, minutes % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    @SuppressLint("MissingPermission")
    fun showReminder(
        context: Context,
        course: String,
        location: String,
        time: String,
        period: Int,
        dayLabel: String
    ) {
        ensureChannel(context)
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return

        val tapIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val detail = buildString {
            append(dayLabel)
            append(" 第 ")
            append(period)
            append(" 节 ")
            append(time)
            if (location.isNotBlank()) {
                append(" · ")
                append(location)
            }
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("即将上课：$course")
            .setContentText(detail)
            .setStyle(NotificationCompat.BigTextStyle().bigText(detail))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(tapIntent)
            .build()
        manager.notify((System.currentTimeMillis() and 0xFFFFFF).toInt(), notification)
    }
}

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_CLASS_REMIND) return
        val course = intent.getStringExtra(EXTRA_COURSE) ?: return
        ReminderScheduler.showReminder(
            context,
            course,
            intent.getStringExtra(EXTRA_LOCATION) ?: "",
            intent.getStringExtra(EXTRA_TIME) ?: "",
            intent.getIntExtra(EXTRA_PERIOD, 0),
            intent.getStringExtra(EXTRA_DAY_LABEL) ?: ""
        )
    }

    companion object {
        const val EXTRA_COURSE = "extra_course"
        const val EXTRA_LOCATION = "extra_location"
        const val EXTRA_TIME = "extra_time"
        const val EXTRA_PERIOD = "extra_period"
        const val EXTRA_DAY_LABEL = "extra_day_label"
    }
}

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                val store = ScheduleStore(context)
                if (store.reminderEnabled) {
                    ReminderScheduler.reschedule(context, store)
                }
            }
        }
    }
}

fun canScheduleExactAlarms(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    return alarmManager.canScheduleExactAlarms()
}

fun openExactAlarmSettings(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
        data = Uri.fromParts("package", context.packageName, null)
    }
    runCatching { context.startActivity(intent) }
}
