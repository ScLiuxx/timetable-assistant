package com.kyant.backdrop.catalog.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CourseListScreen(
    store: ScheduleStore,
    onAdd: (day: Int, period: Int) -> Unit,
    onEdit: (Course) -> Unit
) {
    var confirmClear by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var parityFilter by remember { mutableIntStateOf(Course.PARITY_ALL) }

    val filtered = store.courses.filter { course ->
        val keyword = query.trim()
        val matchKeyword = keyword.isEmpty() ||
            course.name.contains(keyword, ignoreCase = true) ||
            course.teacher.contains(keyword, ignoreCase = true) ||
            course.location.contains(keyword, ignoreCase = true) ||
            course.note.contains(keyword, ignoreCase = true)
        val matchParity = parityFilter == Course.PARITY_ALL || course.effectiveParity == parityFilter
        matchKeyword && matchParity
    }

    ScreenScaffold(
        title = "课程管理",
        subtitle = "共 ${store.courses.size} 门课程"
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GlassTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = "搜索课程 / 教师 / 地点 / 备注",
                singleLine = true
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlassChip(
                    "全部",
                    selected = parityFilter == Course.PARITY_ALL,
                    onClick = { parityFilter = Course.PARITY_ALL },
                    modifier = Modifier.weight(1f)
                )
                GlassChip(
                    "单周",
                    selected = parityFilter == Course.PARITY_ODD,
                    onClick = { parityFilter = Course.PARITY_ODD },
                    modifier = Modifier.weight(1f)
                )
                GlassChip(
                    "双周",
                    selected = parityFilter == Course.PARITY_EVEN,
                    onClick = { parityFilter = Course.PARITY_EVEN },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassPillButton(
                    "添加课程",
                    onClick = { onAdd(1, 1) },
                    tint = accentColor(),
                    contentColorOverride = Color.White
                )
                if (store.courses.isNotEmpty()) {
                    GlassPillButton("清空课程", onClick = { confirmClear = true })
                }
            }

            when {
                store.courses.isEmpty() -> {
                    GlassSection {
                        Column(
                            Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            GlassTitle("还没有课程", fontSize = 16)
                            GlassLabel("点击上方按钮添加第一门课程", fontSize = 13)
                        }
                    }
                }
                filtered.isEmpty() && query.isNotBlank() -> {
                    GlassSection {
                        Column(
                            Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            GlassTitle("没有匹配的课程", fontSize = 16)
                            GlassLabel("换个关键词或筛选条件试试", fontSize = 13)
                        }
                    }
                }
                else -> {
                    for (day in 1..7) {
                        val dayCourses = filtered
                            .filter { it.dayOfWeek == day }
                            .sortedBy { it.startPeriod }
                        if (dayCourses.isEmpty()) continue
                        SectionHeader("${weekdayShortName(day)} · ${dayCourses.size} 门")
                        dayCourses.forEach { course ->
                            CourseListItem(course = course, onClick = { onEdit(course) })
                        }
                    }
                }
            }
        }
    }

    if (confirmClear) {
        GlassDialog(onDismiss = { confirmClear = false }) {
            GlassTitle("清空课程")
            GlassLabel("确定要删除全部 ${store.courses.size} 门课程吗？此操作不可撤销。", fontSize = 14)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FlexSpacer()
                GlassPillButton("取消", onClick = { confirmClear = false })
                GlassPillButton(
                    "清空",
                    onClick = {
                        store.clearCourses()
                        confirmClear = false
                    },
                    tint = Color(0xFFFF3B30),
                    contentColorOverride = Color.White
                )
            }
        }
    }
}

@Composable
private fun CourseListItem(course: Course, onClick: () -> Unit) {
    val color = courseColor(course.colorIndex)
    GlassSurface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        tint = color,
        tintAlpha = 0.2f,
        contentPadding = PaddingValues(14.dp),
        onClick = onClick,
        shadow = false
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(width = 4.dp, height = 42.dp)
                    .background(color, RoundedCornerShape(2.dp))
            )
            Column(
                Modifier.weight(1f).padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                BasicText(course.name, style = TextStyle(contentColor(), 16.sp, FontWeight.SemiBold))
                BasicText(
                    listOfNotNull(
                        course.periodLabel(),
                        course.location.takeIf { it.isNotBlank() }?.let { "@$it" },
                        course.teacher.takeIf { it.isNotBlank() }
                    ).joinToString(" · "),
                    style = TextStyle(secondaryContentColor(), 12.sp)
                )
                BasicText(course.weeksLabel(), style = TextStyle(secondaryContentColor(), 11.sp))
                if (course.note.isNotBlank()) {
                    BasicText(
                        course.note,
                        style = TextStyle(secondaryContentColor().copy(alpha = 0.8f), 11.sp)
                    )
                }
            }
            GlassPillButton(
                "编辑",
                onClick = onClick,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}
