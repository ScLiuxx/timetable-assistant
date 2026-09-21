package com.kyant.backdrop.catalog.schedule

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import org.json.JSONArray
import org.json.JSONObject

data class Course(
    val id: Long = System.nanoTime(),
    val name: String = "",
    val teacher: String = "",
    val location: String = "",
    val note: String = "",
    val colorIndex: Int = 0,
    val dayOfWeek: Int = 1,
    val startPeriod: Int = 1,
    val endPeriod: Int = 1,
    val startWeek: Int = 1,
    val endWeek: Int = 20,
    val parity: Int = PARITY_ALL
) {
    fun weekRangeLabel(): String =
        if (startWeek == endWeek) "第${startWeek}周" else "第${startWeek}-${endWeek}周"

    fun parityLabel(): String =
        when (parity) {
            PARITY_ODD -> "单周"
            PARITY_EVEN -> "双周"
            else -> ""
        }

    fun weeksLabel(): String {
        val parityText = parityLabel()
        return if (parityText.isEmpty()) weekRangeLabel() else "${weekRangeLabel()} $parityText"
    }

    fun periodLabel(): String =
        if (startPeriod == endPeriod) "第${startPeriod}节" else "第${startPeriod}-${endPeriod}节"

    fun matchesWeek(week: Int): Boolean {
        if (week < startWeek || week > endWeek) return false
        return when (parity) {
            PARITY_ODD -> week % 2 == 1
            PARITY_EVEN -> week % 2 == 0
            else -> true
        }
    }

    companion object {
        const val PARITY_ALL = 0
        const val PARITY_ODD = 1
        const val PARITY_EVEN = 2
    }
}

data class Holiday(
    val epochDay: Long,
    val name: String = "节假日"
)

/**
 * 调休补课：在 [epochDay] 这一天，按 [targetDayOfWeek]（1=周一 ... 7=周日）的课表上课。
 */
data class Makeup(
    val epochDay: Long,
    val targetDayOfWeek: Int,
    val name: String = "调休补课"
)

data class Semester(
    val name: String = "我的课表",
    val startEpochDay: Long = mondayOfWeek(todayEpochDay()),
    val totalWeeks: Int = 20,
    val periodsPerDay: Int = 12
)

data class PeriodTime(
    val start: String = "08:00",
    val end: String = "08:45"
)

val CourseColors: List<Color> = listOf(
    Color(0xFF4C8DFF),
    Color(0xFF34C759),
    Color(0xFFAF52DE),
    Color(0xFFFF9500),
    Color(0xFFFF3B30),
    Color(0xFF00C7BE),
    Color(0xFFFF2D55),
    Color(0xFF5856D6),
    Color(0xFF32ADE6),
    Color(0xFF8E8E93)
)

val CourseColorNames: List<String> = listOf(
    "蓝", "绿", "紫", "橙", "红", "青", "粉", "靛", "天蓝", "灰"
)

fun courseColor(index: Int): Color = CourseColors[((index % CourseColors.size) + CourseColors.size) % CourseColors.size]

/** 可自定义的强调/主题色预设。 */
val AccentColorPresets: List<Color> = listOf(
    Color(0xFF0A84FF),
    Color(0xFF34C759),
    Color(0xFFAF52DE),
    Color(0xFFFF9500),
    Color(0xFFFF3B30),
    Color(0xFF00C7BE),
    Color(0xFFFF2D55),
    Color(0xFF5856D6)
)

val AccentColorPresetNames: List<String> = listOf(
    "蓝", "绿", "紫", "橙", "红", "青", "粉", "靛"
)

fun accentColorPreset(index: Int): Color =
    AccentColorPresets[((index % AccentColorPresets.size) + AccentColorPresets.size) % AccentColorPresets.size]

fun defaultPeriods(): List<PeriodTime> = listOf(
    PeriodTime("08:00", "08:45"),
    PeriodTime("08:55", "09:40"),
    PeriodTime("10:00", "10:45"),
    PeriodTime("10:55", "11:40"),
    PeriodTime("14:00", "14:45"),
    PeriodTime("14:55", "15:40"),
    PeriodTime("16:00", "16:45"),
    PeriodTime("16:55", "17:40"),
    PeriodTime("19:00", "19:45"),
    PeriodTime("19:55", "20:40"),
    PeriodTime("20:50", "21:35"),
    PeriodTime("21:45", "22:30")
)

enum class DayType { NORMAL, HOLIDAY, MAKEUP }

data class DayResolution(
    val epochDay: Long,
    val type: DayType,
    /** 1=周一 ... 7=周日；对于调休日表示实际使用的课表星期。 */
    val courseDayOfWeek: Int,
    val note: String = ""
)

class ScheduleStore(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var semester by mutableStateOf(Semester())
        private set

    var periods by mutableStateOf(defaultPeriods())
        private set

    var reminderEnabled by mutableStateOf(false)
        private set

    var reminderLeadMinutes by mutableIntStateOf(10)
        private set

    var accentColorIndex by mutableIntStateOf(0)
        private set

    /** 玻璃表面透明度（0.2 ~ 0.8），越高玻璃越"实"。 */
    var glassOpacity by mutableFloatStateOf(0.42f)
        private set

    val courses = mutableStateListOf<Course>()
    val holidays = mutableStateListOf<Holiday>()
    val makeups = mutableStateListOf<Makeup>()

    init {
        load()
    }

    fun persist() {
        val root = JSONObject()
        root.put("version", 1)
        root.put("reminderEnabled", reminderEnabled)
        root.put("reminderLeadMinutes", reminderLeadMinutes)
        root.put("accentColorIndex", accentColorIndex)
        root.put("glassOpacity", glassOpacity.toDouble())
        root.put("semester", JSONObject().apply {
            put("name", semester.name)
            put("startEpochDay", semester.startEpochDay)
            put("totalWeeks", semester.totalWeeks)
            put("periodsPerDay", semester.periodsPerDay)
        })
        root.put("periods", JSONArray().apply {
            periods.forEach { period ->
                put(JSONObject().apply {
                    put("start", period.start)
                    put("end", period.end)
                })
            }
        })
        root.put("courses", JSONArray().apply {
            courses.forEach { course ->
                put(JSONObject().apply {
                    put("id", course.id)
                    put("name", course.name)
                    put("teacher", course.teacher)
                    put("location", course.location)
                    put("note", course.note)
                    put("colorIndex", course.colorIndex)
                    put("dayOfWeek", course.dayOfWeek)
                    put("startPeriod", course.startPeriod)
                    put("endPeriod", course.endPeriod)
                    put("startWeek", course.startWeek)
                    put("endWeek", course.endWeek)
                    put("parity", course.parity)
                })
            }
        })
        root.put("holidays", JSONArray().apply {
            holidays.forEach { holiday ->
                put(JSONObject().apply {
                    put("epochDay", holiday.epochDay)
                    put("name", holiday.name)
                })
            }
        })
        root.put("makeups", JSONArray().apply {
            makeups.forEach { makeup ->
                put(JSONObject().apply {
                    put("epochDay", makeup.epochDay)
                    put("targetDayOfWeek", makeup.targetDayOfWeek)
                    put("name", makeup.name)
                })
            }
        })
        prefs.edit().putString(KEY_DATA, root.toString()).apply()
        // 课表数据变化后刷新桌面小组件与 Live Updates。
        runCatching { ScheduleWidgetProvider.refreshAll(context) }
        runCatching { LiveUpdateManager.sync(context, this) }
    }

    fun load() {
        val raw = prefs.getString(KEY_DATA, null) ?: return
        runCatching {
            val root = JSONObject(raw)
            reminderEnabled = root.optBoolean("reminderEnabled", false)
            reminderLeadMinutes = root.optInt("reminderLeadMinutes", 10).coerceIn(1, 60)
            accentColorIndex = root.optInt("accentColorIndex", 0)
                .coerceIn(0, AccentColorPresets.size - 1)
            glassOpacity = root.optDouble("glassOpacity", 0.42).toFloat().coerceIn(0.2f, 0.8f)
            root.optJSONObject("semester")?.let {
                semester = Semester(
                    name = it.optString("name", semester.name),
                    startEpochDay = it.optLong("startEpochDay", semester.startEpochDay),
                    totalWeeks = it.optInt("totalWeeks", semester.totalWeeks).coerceIn(1, 60),
                    periodsPerDay = it.optInt("periodsPerDay", semester.periodsPerDay).coerceIn(1, 20)
                )
            }
            root.optJSONArray("periods")?.let { array ->
                val list = ArrayList<PeriodTime>()
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    list.add(PeriodTime(item.optString("start", "08:00"), item.optString("end", "08:45")))
                }
                if (list.isNotEmpty()) periods = list
            }
            courses.clear()
            root.optJSONArray("courses")?.let { array ->
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    courses.add(
                        Course(
                            id = item.optLong("id", System.nanoTime()),
                            name = item.optString("name", ""),
                            teacher = item.optString("teacher", ""),
                            location = item.optString("location", ""),
                            note = item.optString("note", ""),
                            colorIndex = item.optInt("colorIndex", 0),
                            dayOfWeek = item.optInt("dayOfWeek", 1).coerceIn(1, 7),
                            startPeriod = item.optInt("startPeriod", 1),
                            endPeriod = item.optInt("endPeriod", 1),
                            startWeek = item.optInt("startWeek", 1),
                            endWeek = item.optInt("endWeek", 20),
                            parity = item.optInt("parity", 0)
                        )
                    )
                }
            }
            holidays.clear()
            root.optJSONArray("holidays")?.let { array ->
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    holidays.add(Holiday(item.optLong("epochDay"), item.optString("name", "节假日")))
                }
            }
            makeups.clear()
            root.optJSONArray("makeups")?.let { array ->
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    makeups.add(
                        Makeup(
                            epochDay = item.optLong("epochDay"),
                            targetDayOfWeek = item.optInt("targetDayOfWeek", 1).coerceIn(1, 7),
                            name = item.optString("name", "调休补课")
                        )
                    )
                }
            }
        }
    }

    fun exportJson(): String {
        persist()
        return prefs.getString(KEY_DATA, "{}") ?: "{}"
    }

    fun importJson(json: String): Boolean {
        return runCatching {
            val root = JSONObject(json)
            // 校验这是本应用的课表数据，避免把任意 JSON 当成课表导入从而清空现有数据。
            if (!root.has("version") && !root.has("courses") && !root.has("semester")) {
                return@runCatching false
            }
            prefs.edit().putString(KEY_DATA, json).apply()
            resetInMemory()
            load()
            true
        }.getOrDefault(false)
    }

    private fun resetInMemory() {
        semester = Semester()
        periods = defaultPeriods()
        reminderEnabled = false
        reminderLeadMinutes = 10
        accentColorIndex = 0
        glassOpacity = 0.42f
        courses.clear()
        holidays.clear()
        makeups.clear()
    }

    fun updateSemester(value: Semester) {
        semester = value.copy(
            totalWeeks = value.totalWeeks.coerceIn(1, 60),
            periodsPerDay = value.periodsPerDay.coerceIn(1, 20)
        )
        verticalAlignmentPeriods()
        persist()
    }

    fun updatePeriods(value: List<PeriodTime>) {
        periods = value
        persist()
    }

    fun updateReminderEnabled(enabled: Boolean) {
        reminderEnabled = enabled
        persist()
    }

    fun updateReminderLeadMinutes(minutes: Int) {
        reminderLeadMinutes = minutes.coerceIn(1, 60)
        persist()
    }

    fun updateAccentColorIndex(index: Int) {
        if (index < 0 || index >= AccentColorPresets.size) return
        accentColorIndex = index
        persist()
    }

    fun updateGlassOpacity(value: Float) {
        glassOpacity = value.coerceIn(0.2f, 0.8f)
        persist()
    }

    fun addCourse(course: Course) {
        courses.add(course)
        persist()
    }

    fun updateCourse(course: Course) {
        val index = courses.indexOfFirst { it.id == course.id }
        if (index >= 0) courses[index] = course else courses.add(course)
        persist()
    }

    fun removeCourse(id: Long) {
        courses.removeAll { it.id == id }
        persist()
    }

    fun clearCourses() {
        courses.clear()
        persist()
    }

    fun addHoliday(holiday: Holiday) {
        holidays.removeAll { it.epochDay == holiday.epochDay }
        holidays.add(holiday)
        holidays.sortBy { it.epochDay }
        persist()
    }

    fun removeHoliday(epochDay: Long) {
        holidays.removeAll { it.epochDay == epochDay }
        persist()
    }

    fun addMakeup(makeup: Makeup) {
        makeups.removeAll { it.epochDay == makeup.epochDay }
        makeups.add(makeup)
        makeups.sortBy { it.epochDay }
        persist()
    }

    fun removeMakeup(epochDay: Long) {
        makeups.removeAll { it.epochDay == epochDay }
        persist()
    }

    fun resetAll() {
        resetInMemory()
        persist()
    }

    private fun verticalAlignmentPeriods() {
        val count = semester.periodsPerDay
        if (periods.size == count) return
        periods = if (count <= periods.size) periods.take(count)
        else ArrayList(periods).apply {
            while (size < count) add(PeriodTime("08:00", "08:45"))
        }
    }

    fun resolveDay(epochDay: Long): DayResolution {
        holidays.firstOrNull { it.epochDay == epochDay }?.let {
            return DayResolution(epochDay, DayType.HOLIDAY, 0, it.name)
        }
        makeups.firstOrNull { it.epochDay == epochDay }?.let {
            return DayResolution(epochDay, DayType.MAKEUP, it.targetDayOfWeek, it.name)
        }
        return DayResolution(epochDay, DayType.NORMAL, isoDayOfWeek(epochDay))
    }

    fun weekOf(epochDay: Long): Int {
        val diff = epochDay - semester.startEpochDay
        val week = Math.floorDiv(diff, 7L).toInt() + 1
        return week
    }

    fun dateOf(week: Int, dayOfWeek: Int): Long =
        semester.startEpochDay + (week - 1).toLong() * 7L + (dayOfWeek - 1).toLong()

    fun coursesFor(epochDay: Long): List<Course> {
        val resolution = resolveDay(epochDay)
        if (resolution.type == DayType.HOLIDAY) return emptyList()
        val week = weekOf(epochDay)
        return courses
            .filter { it.dayOfWeek == resolution.courseDayOfWeek && it.matchesWeek(week) }
            .sortedBy { it.startPeriod }
    }

    fun coursesForWeek(week: Int): Map<Int, List<Course>> {
        val result = LinkedHashMap<Int, List<Course>>()
        for (day in 1..7) {
            val epochDay = dateOf(week, day)
            result[day] = coursesFor(epochDay)
        }
        return result
    }

    companion object {
        private const val PREFS_NAME = "course_schedule_store"
        private const val KEY_DATA = "schedule_data"
    }
}
