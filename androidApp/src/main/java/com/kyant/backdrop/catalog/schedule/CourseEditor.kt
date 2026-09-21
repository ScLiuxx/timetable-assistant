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
import androidx.compose.runtime.mutableStateListOf
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
    var draftStartWeek by remember { mutableIntStateOf(1) }
    var draftEndWeek by remember { mutableIntStateOf(store.semester.totalWeeks) }
    val segments = remember {
        val initial = existing?.weekRanges() ?: listOf(1..store.semester.totalWeeks)
        mutableStateListOf<IntRange>().apply { addAll(initial) }
    }
    var parity by remember { mutableIntStateOf(existing?.parity ?: Course.PARITY_ALL) }

    val addSegment: (Int, Int) -> Unit = { s, e ->
        val start = s.coerceAtLeast(1).coerceAtMost(e)
        val end = e.coerceAtLeast(start)
        // 与被已有段合并，避免重叠段重复显示。
        segments.removeAll { pairwiseOverlap(it, start..end) }
        segments.add(start..end)
        segments.sortBy { it.first }
    }
    val removeSegment: (IntRange) -> Unit = { seg ->
        segments.remove(seg)
        if (segments.isEmpty()) segments.add(1..store.semester.totalWeeks)
    }

    var conflictCourse by remember { mutableStateOf<Course?>(null) }

    /**
     * 查找与当前编辑内容在同一时段存在冲突的已有课程。
     * 判断依据：星期相同、节次区间重叠、任一周段重叠、单双周可同时生效。
     */
    val findConflict: () -> Course? = {
        val draftWeekRanges = segments.toList()
        store.courses.firstOrNull { other ->
            other.id != existing?.id &&
                other.dayOfWeek == day &&
                maxOf(other.startPeriod, startPeriod) <= minOf(other.endPeriod, endPeriod) &&
                rangesOverlap(draftWeekRanges, other.weekRanges()) &&
                (other.parity == Course.PARITY_ALL ||
                    parity == Course.PARITY_ALL ||
                    other.parity == parity)
        }
    }

    val saveCourse: () -> Unit = {
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
            startWeek = segments.first().first,
            endWeek = segments.first().last,
            weekSegments = segments.toList(),
            parity = parity
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

            EditorLabel("开课周次（可多段，如 1-4 周 + 6-8 周）")
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                segments.forEach { seg ->
                    GlassPillButton(
                        if (seg.first == seg.last) "第${seg.first}周" else "第${seg.first}-${seg.last}周",
                        onClick = { removeSegment(seg) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            StepperRow("起始周", draftStartWeek, 1..store.semester.totalWeeks) {
                draftStartWeek = it
                if (draftEndWeek < it) draftEndWeek = it
            }
            StepperRow("结束周", draftEndWeek, draftStartWeek..store.semester.totalWeeks) {
                draftEndWeek = it
            }
            GlassPillButton(
                "＋ 添加周段",
                onClick = { addSegment(draftStartWeek, draftEndWeek) },
                contentPadding = PaddingValues(vertical = 8.dp)
            )

            EditorLabel("单双周")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassChip("每周", parity == Course.PARITY_ALL, onClick = { parity = Course.PARITY_ALL })
                GlassChip("单周", parity == Course.PARITY_ODD, onClick = { parity = Course.PARITY_ODD })
                GlassChip("双周", parity == Course.PARITY_EVEN, onClick = { parity = Course.PARITY_EVEN })
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

private fun pairwiseOverlap(a: IntRange, b: IntRange): Boolean =
    maxOf(a.first, b.first) <= minOf(a.last, b.last)

private fun rangesOverlap(a: List<IntRange>, b: List<IntRange>): Boolean =
    a.any { x -> b.any { y -> pairwiseOverlap(x, y) } }
