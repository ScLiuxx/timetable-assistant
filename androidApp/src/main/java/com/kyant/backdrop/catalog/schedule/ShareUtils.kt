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
     * 按学期逐日扫描实际课程（含单双周、节假日跳过、调休补课按目标星期排课）。
     */
    fun buildIcs(store: ScheduleStore): String {
        val sb = StringBuilder()
        sb.appendLine("BEGIN:VCALENDAR")
        sb.appendLine("VERSION:2.0")
        sb.appendLine("PRODID:-//Timetable Assistant//CN")
        sb.appendLine("CALSCALE:GREGORIAN")
        val semesterStart = store.semester.startEpochDay
        val semesterEnd = semesterStart + store.semester.totalWeeks * 7L - 1L
        var day = semesterStart
        while (day <= semesterEnd) {
            val resolution = store.resolveDay(day)
            if (resolution.type != DayType.HOLIDAY) {
                for (course in store.coursesFor(day)) {
                    val start = store.periods.getOrNull(course.startPeriod - 1)
                    val end = store.periods.getOrNull(course.endPeriod - 1)
                    if (start == null) continue
                    val (y, m, d) = epochDayToCivil(day)
                    val dtStart = "%04d%02d%02dT%s00".format(y, m, d, digits(start.start))
                    val dtEndText = if (end != null) {
                        "%04d%02d%02dT%s00".format(y, m, d, digits(end.end))
                    } else {
                        "%04d%02d%02dT%s00".format(y, m, d, digits(start.end))
                    }
                    val desc = buildString {
                        if (course.teacher.isNotBlank()) append("教师 ${course.teacher}；")
                        append(course.weeksLabel())
                        if (resolution.type == DayType.MAKEUP) {
                            append("；${resolution.note}")
                        }
                        if (course.note.isNotBlank()) append("；${course.note}")
                    }
                    sb.appendLine("BEGIN:VEVENT")
                    sb.appendLine("UID:${course.id}-$day@timetable")
                    sb.appendLine("DTSTAMP:${icsStamp()}")
                    sb.appendLine("DTSTART:$dtStart")
                    sb.appendLine("DTEND:$dtEndText")
                    sb.appendLine("SUMMARY:${icsEscape(course.name.ifBlank { "未命名课程" })}")
                    if (course.location.isNotBlank()) {
                        sb.appendLine("LOCATION:${icsEscape(course.location)}")
                    }
                    if (desc.isNotBlank()) {
                        sb.appendLine("DESCRIPTION:${icsEscape(desc)}")
                    }
                    sb.appendLine("END:VEVENT")
                }
            }
            day++
        }
        sb.appendLine("END:VCALENDAR")
        return sb.toString()
    }

    private fun digits(time: String): String = time.replace(":", "")

    /** 对 ICS 文本字段中的换行、分号、逗号、反斜杠进行转义。 */
    private fun icsEscape(text: String): String =
        text
            .replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace("\r\n", "\\n")
            .replace("\n", "\\n")

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