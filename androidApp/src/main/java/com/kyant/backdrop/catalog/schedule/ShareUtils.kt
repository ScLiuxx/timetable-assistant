package com.kyant.backdrop.catalog.schedule

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/**
 * 文件分享与导出工具。通过 FileProvider 将内容写入临时文件并调用系统分享面板。
 */
object ShareUtils {

    /**
     * 将 [content] 写入临时文件并通过系统分享面板分享。
     */
    fun shareFile(
        context: Context,
        fileName: String,
        content: String,
        mimeType: String
    ) {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, fileName)
        file.writeText(content)
        val providerUri = FileProvider.getUriForFile(
            context,
            context.packageName + AUTHORITY_SUFFIX,
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, providerUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(intent, "分享到")
        )
    }

    /**
     * 生成包含全部课程事件的 ICS 日历文本。
     * 逐周展开课程（含单双周），跳过节假日当天。
     */
    fun buildIcs(store: ScheduleStore): String {
        val sb = StringBuilder()
        sb.appendLine("BEGIN:VCALENDAR")
        sb.appendLine("VERSION:2.0")
        sb.appendLine("PRODID:-//Timetable Assistant//CN")
        for (course in store.courses) {
            for (week in course.startWeek..course.endWeek) {
                if (!course.matchesWeek(week)) continue
                val epochDay = store.dateOf(week, course.dayOfWeek)
                if (store.resolveDay(epochDay).type == DayType.HOLIDAY) continue
                val start = store.periods.getOrNull(course.startPeriod - 1)
                val end = store.periods.getOrNull(course.endPeriod - 1)
                if (start == null) continue
                val (y, m, d) = epochDayToCivil(epochDay)
                val dtStart = "%04d%02d%02dT%s00".format(y, m, d, start.start.replace(":", ""))
                val dtEndText = if (end != null) {
                    "%04d%02d%02dT%s00".format(y, m, d, end.end.replace(":", ""))
                } else {
                    "%04d%02d%02dT%s00".format(y, m, d, start.end.replace(":", ""))
                }
                sb.appendLine("BEGIN:VEVENT")
                sb.appendLine("UID:${course.id}-$epochDay@timetable")
                sb.appendLine("DTSTAMP:${icsStamp()}")
                sb.appendLine("DTSTART:$dtStart")
                sb.appendLine("DTEND:$dtEndText")
                sb.appendLine("SUMMARY:${course.name.ifBlank { "未命名课程" }}")
                if (course.location.isNotBlank()) sb.appendLine("LOCATION:${course.location}")
                if (course.teacher.isNotBlank()) sb.appendLine("DESCRIPTION:教师 ${course.teacher}；${course.weeksLabel()}")
                sb.appendLine("END:VEVENT")
            }
        }
        sb.appendLine("END:VCALENDAR")
        return sb.toString()
    }

    private fun icsStamp(): String {
        val now = java.util.Calendar.getInstance()
        return "%04d%02d%02dT%02d%02d%02dZ".format(
            now.get(java.util.Calendar.YEAR),
            now.get(java.util.Calendar.MONTH) + 1,
            now.get(java.util.Calendar.DAY_OF_MONTH),
            now.get(java.util.Calendar.HOUR_OF_DAY),
            now.get(java.util.Calendar.MINUTE),
            now.get(java.util.Calendar.SECOND)
        )
    }

    private const val AUTHORITY_SUFFIX = ".fileprovider"
}