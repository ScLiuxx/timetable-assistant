package com.kyant.backdrop.catalog.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CourseEditorDialog(
    store: ScheduleStore,
    existing: Course?,
    defaultDay: Int,
    defaultPeriod: Int,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var teacher by remember { mutableStateOf(existing?.teacher ?: "") }
    var location by remember { mutableStateOf(existing?.location ?: "") }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var colorIndex by remember { mutableIntStateOf(existing?.colorIndex ?: 0) }
    var day by remember { mutableIntStateOf(existing?.dayOfWeek ?: defaultDay) }
    var startPeriod by remember { mutableIntStateOf(existing?.startPeriod ?: defaultPeriod) }
    var endPeriod by remember { mutableIntStateOf(existing?.endPeriod ?: defaultPeriod) }
    val totalWeeks = store.semester.totalWeeks.coerceAtLeast(1)
    val allWeeks = remember(totalWeeks) { (1..totalWeeks).toSet() }
    val allOddWeeks = remember(allWeeks) { allWeeks.filter { it % 2 == 1 }.toSet() }
    val allEvenWeeks = remember(allWeeks) { allWeeks.filter { it % 2 == 0 }.toSet() }
    val selectedWeeks = remember {
        val weeks = if (existing != null) {
            existing.weekRanges().flatMap { it }.toCollection(mutableSetOf())
        } else {
            (1..totalWeeks).toCollection(mutableSetOf())
        }
        // 保证至少有一周被选。
        if (weeks.isEmpty()) weeks.add(1)
        mutableStateOf(weeks)
    }

    val toggleWeek: (Int) -> Unit = { week ->
        val current = HashSet(selectedWeeks.value)
        if (!current.remove(week)) current.add(week)
        if (current.isEmpty()) current.add(1)
        selectedWeeks.value = current
    }
    val setWeeks: (Set<Int>) -> Unit = { weeks ->
        val clean = weeks.filter { it in 1..totalWeeks }.toMutableSet()
        selectedWeeks.value = if (clean.isEmpty()) mutableSetOf(1) else clean
    }

    var conflictCourse by remember { mutableStateOf<Course?>(null) }

    /**
     * 查找与当前编辑内容在同一时段存在冲突的已有课程。
     * 判断依据：星期相同、节次区间重叠、任一周段重叠、单双周可同时生效。
     */
    val findConflict: () -> Course? = {
        store.courses.firstOrNull { other ->
            other.id != existing?.id &&
                other.dayOfWeek == day &&
                maxOf(other.startPeriod, startPeriod) <= minOf(other.endPeriod, endPeriod) &&
                // 草稿周集合与其他课程（含单双周标记）在可同时开课的周上存在重叠才算冲突。
                overlapsDraft(draftWeeks = selectedWeeks.value, other = other)
        }
    }

    val saveCourse: () -> Unit = {
        val ranges = compressRanges(selectedWeeks.value.sorted())
        val first = ranges.first().first
        val last = ranges.last().last
        val course = Course(
            id = existing?.id ?: System.nanoTime(),
            name = name.ifBlank { "未命名课程" },
            teacher = teacher,
            location = location,
            note = note,
            colorIndex = colorIndex,
            dayOfWeek = day,
            startPeriod = startPeriod,
            endPeriod = endPeriod,
            startWeek = first,
            endWeek = last,
            weekSegments = ranges,
            parity = Course.PARITY_ALL
        )
        if (existing == null) store.addCourse(course) else store.updateCourse(course)
        onDismiss()
    }

    GlassDialog(onDismiss = onDismiss) {
        GlassTitle(if (existing == null) "添加课程" else "编辑课程")
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 440.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            EditorField("课程名称", name) { name = it }
            EditorField("任课教师", teacher) { teacher = it }
            EditorField("上课地点", location) { location = it }
            EditorField("备注", note) { note = it }

            EditorLabel("星期")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (d in 1..7) {
                    GlassChip(
                        weekdayShortName(d).removePrefix("周"),
                        selected = day == d,
                        onClick = { day = d },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            EditorLabel("节次")
            StepperRow("开始", startPeriod, 1..store.semester.periodsPerDay) {
                startPeriod = it
                if (endPeriod < it) endPeriod = it
            }
            StepperRow("结束", endPeriod, startPeriod..store.semester.periodsPerDay) {
                endPeriod = it
            }

            EditorLabel("开课周次（点击切换，可多段）")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                GlassChip(
                    "每周",
                    selected = allWeeks == selectedWeeks.value,
                    onClick = { setWeeks(allWeeks) }
                )
                GlassChip(
                    "单周",
                    selected = allOddWeeks == selectedWeeks.value,
                    onClick = { setWeeks(allOddWeeks) }
                )
                GlassChip(
                    "双周",
                    selected = allEvenWeeks == selectedWeeks.value,
                    onClick = { setWeeks(allEvenWeeks) }
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                GlassPillButton(
                    "全选",
                    onClick = { setWeeks(allWeeks) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                )
                GlassPillButton(
                    "清空",
                    onClick = { setWeeks(emptySet()) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                )
                GlassLabel(
                    "已选 ${selectedWeeks.value.size} / $totalWeeks 周",
                    fontSize = 12
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                (1..totalWeeks).chunked(6).forEachIndexed { rowIndex, rowWeeks ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        rowWeeks.forEach { week ->
                            GlassChip(
                                "$week",
                                selected = week in selectedWeeks.value,
                                onClick = { toggleWeek(week) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(6 - rowWeeks.size) {
                            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            EditorLabel("颜色")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CourseColors.forEachIndexed { index, color ->
                    Box(
                        Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(color)
                            .then(
                                if (index == colorIndex) {
                                    Modifier.border(2.5.dp, contentColor(), CircleShape)
                                } else {
                                    Modifier
                                }
                            )
                            .clickable { colorIndex = index }
                    )
                }
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (existing != null) {
                GlassPillButton("删除", onClick = {
                    store.removeCourse(existing.id)
                    onDismiss()
                })
            }
            FlexSpacer()
            GlassPillButton("取消", onClick = onDismiss)
            GlassPillButton(
                "保存",
                onClick = {
                    val conflict = findConflict()
                    conflictCourse = conflict
                    if (conflict == null) saveCourse()
                },
                tint = accentColor(),
                contentColorOverride = Color.White
            )
        }
    }

    conflictCourse?.let { conflict ->
        GlassDialog(onDismiss = { conflictCourse = null }) {
            GlassTitle("时间冲突提示")
            GlassLabel(
                "「${conflict.name}」已在${weekdayShortName(conflict.dayOfWeek)}" +
                    "${conflict.periodLabel()}${conflict.weeksLabel()}上课，与当前课程时间重叠。",
                fontSize = 13
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FlexSpacer()
                GlassPillButton("再想想", onClick = { conflictCourse = null })
                GlassPillButton(
                    "仍然保存",
                    onClick = {
                        conflictCourse = null
                        saveCourse()
                    },
                    tint = accentColor(),
                    contentColorOverride = Color.White
                )
            }
        }
    }
}

@Composable
private fun EditorField(label: String, value: String, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        EditorLabel(label)
        GlassTextField(value = value, onValueChange = onChange, placeholder = "请输入$label")
    }
}

@Composable
private fun EditorLabel(text: String) {
    BasicText(text, style = TextStyle(secondaryContentColor(), 13.sp))
}

@Composable
private fun StepperRow(
    label: String,
    value: Int,
    range: IntRange,
    onChange: (Int) -> Unit
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        BasicText(label, style = TextStyle(contentColor(), 14.sp))
        FlexSpacer()
        GlassPillButton(
            "−",
            onClick = { if (value > range.first) onChange(value - 1) },
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
        )
        BasicText(
            value.toString(),
            Modifier.padding(horizontal = 14.dp),
            style = TextStyle(contentColor(), 16.sp, FontWeight.SemiBold)
        )
        GlassPillButton(
            "+",
            onClick = { if (value < range.last) onChange(value + 1) },
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
        )
    }
}

/**
 * 判断草稿（已选周集合 [draftWeeks]）与其他课程是否存在可同时开课的周次重叠。
 *
 * 其他课程可能是单周/双周课程（[Course.parity] 非 ALL），其 [Course.weekRanges] 只是
 * 区间描述，实际仅有奇/偶周开课。若只对区间做重叠判断，会把"单周课程"与"每周课程"
 * 误判为冲突。正确做法是：存在某一周 w，既在草稿中、也在 other 的区间内，且
 * w 满足 other 的单双周奇偶属性。
 */
private fun overlapsDraft(draftWeeks: Set<Int>, other: Course): Boolean {
    val ranges = other.weekRanges()
    for (w in draftWeeks) {
        val inRange = ranges.any { w in it }
        if (!inRange) continue
        val parityHit = when (other.parity) {
            Course.PARITY_ODD -> w % 2 == 1
            Course.PARITY_EVEN -> w % 2 == 0
            else -> true
        }
        if (parityHit) return true
    }
    return false
}

/** 将一组升序的周号压缩为连续周段，如 [1,2,3,6,7] -> [1..3, 6..7]。 */
private fun compressRanges(sortedWeeks: List<Int>): List<IntRange> {
    if (sortedWeeks.isEmpty()) return listOf(1..1)
    val result = ArrayList<IntRange>()
    var start = sortedWeeks.first()
    var prev = start
    for (week in sortedWeeks.drop(1)) {
        if (week == prev + 1) {
            prev = week
        } else {
            result.add(start..prev)
            start = week
            prev = week
        }
    }
    result.add(start..prev)
    return result
}
