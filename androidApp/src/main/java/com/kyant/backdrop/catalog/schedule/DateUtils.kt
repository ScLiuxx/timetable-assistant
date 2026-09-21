package com.kyant.backdrop.catalog.schedule

import java.util.Calendar

private const val MS_PER_DAY = 86_400_000L

fun civilToEpochDay(year: Int, month: Int, day: Int): Long {
    val y = if (month <= 2) year - 1 else year
    val era = (if (y >= 0) y else y - 399) / 400
    val yoe = y - era * 400
    val doy = (153 * (if (month > 2) month - 3 else month + 9) + 2) / 5 + day - 1
    val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
    return era * 146097L + doe - 719468L
}

fun epochDayToCivil(epochDay: Long): Triple<Int, Int, Int> {
    val z = epochDay + 719468L
    val era = (if (z >= 0) z else z - 146096L) / 146097L
    val doe = z - era * 146097L
    val yoe = (doe - doe / 1460L + doe / 36524L - doe / 146096L) / 365L
    val y = yoe + era * 400L
    val doy = doe - (365L * yoe + yoe / 4L - yoe / 100L)
    val mp = (5L * doy + 2L) / 153L
    val d = doy - (153L * mp + 2L) / 5L + 1L
    val m = if (mp < 10L) mp + 3L else mp - 9L
    return Triple((if (m <= 2L) y + 1L else y).toInt(), m.toInt(), d.toInt())
}

fun todayEpochDay(): Long {
    val calendar = Calendar.getInstance()
    return civilToEpochDay(
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH) + 1,
        calendar.get(Calendar.DAY_OF_MONTH)
    )
}

fun epochDayYear(epochDay: Long): Int = epochDayToCivil(epochDay).first
fun epochDayMonth(epochDay: Long): Int = epochDayToCivil(epochDay).second
fun epochDayDayOfMonth(epochDay: Long): Int = epochDayToCivil(epochDay).third

/** ISO weekday: Monday = 1 ... Sunday = 7. */
fun isoDayOfWeek(epochDay: Long): Int {
    val mod = ((epochDay + 3L) % 7L + 7L) % 7L
    return mod.toInt() + 1
}

/** Monday-based index: Monday = 0 ... Sunday = 6. */
fun mondayIndex(epochDay: Long): Int = isoDayOfWeek(epochDay) - 1

fun mondayOfWeek(epochDay: Long): Long = epochDay - mondayIndex(epochDay)

fun weekdayShortName(iso: Int): String =
    when (iso) {
        1 -> "周一"
        2 -> "周二"
        3 -> "周三"
        4 -> "周四"
        5 -> "周五"
        6 -> "周六"
        7 -> "周日"
        else -> ""
    }

fun formatMonthDay(epochDay: Long): String =
    "${epochDayMonth(epochDay)}/${epochDayDayOfMonth(epochDay)}"

fun formatFullDate(epochDay: Long): String {
    val (y, m, d) = epochDayToCivil(epochDay)
    return "${y}年${m}月${d}日 ${weekdayShortName(isoDayOfWeek(epochDay))}"
}

fun formatYearMonthDay(epochDay: Long): String {
    val (y, m, d) = epochDayToCivil(epochDay)
    return "%04d-%02d-%02d".format(y, m, d)
}

fun parseDate(text: String): Long? {
    val parts = text.trim().split("-", "/", ".")
    if (parts.size != 3) return null
    val y = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    val d = parts[2].toIntOrNull() ?: return null
    if (m !in 1..12) return null
    if (d !in 1..daysInMonth(y, m)) return null
    return civilToEpochDay(y, m, d)
}

/** 该年该月的实际天数；month 非法时返回 0。 */
fun daysInMonth(year: Int, month: Int): Int = when (month) {
    1, 3, 5, 7, 8, 10, 12 -> 31
    4, 6, 9, 11 -> 30
    2 -> if (isLeapYear(year)) 29 else 28
    else -> 0
}

private fun isLeapYear(year: Int): Boolean =
    year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)

fun currentHourMinute(): Pair<Int, Int> {
    val calendar = Calendar.getInstance()
    return calendar.get(Calendar.HOUR_OF_DAY) to calendar.get(Calendar.MINUTE)
}

fun minutesOf(time: String): Int? {
    val parts = time.trim().split(":")
    if (parts.size != 2) return null
    val h = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    if (h !in 0..23 || m !in 0..59) return null
    return h * 60 + m
}

fun isValidTime(time: String): Boolean = minutesOf(time) != null

fun normalizeTime(time: String): String {
    val minutes = minutesOf(time) ?: return time
    return "%02d:%02d".format(minutes / 60, minutes % 60)
}
