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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TodayScreen(
    store: ScheduleStore,
    onEdit: (Course) -> Unit
) {
    val today = todayEpochDay()
    val resolution = store.resolveDay(today)
    val week = store.weekOf(today)
    val courses = store.coursesFor(today)
    val (nowHour, nowMinute) = currentHourMinute()
    val nowMinutes = nowHour * 60 + nowMinute

    ScreenScaffold(
        title = "今日课程",
        subtitle = "${formatFullDate(today)} · 第 $week 周"
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (resolution.type) {
                DayType.HOLIDAY -> InfoBanner(
                    Color(0xFFFF3B30),
                    "今天放假",
                    resolution.note
                )
                DayType.MAKEUP -> InfoBanner(
                    Color(0xFFFF9500),
                    "调休上课",
                    "按${weekdayShortName(resolution.courseDayOfWeek)}的课表上课 · ${resolution.note}"
                )
                DayType.NORMAL -> Unit
            }

            if (courses.isEmpty()) {
                GlassSection {
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        GlassTitle("今天没有课", fontSize = 16)
                        GlassLabel("好好休息，或者去添加课程吧", fontSize = 13)
                    }
                }
            } else {
                courses.forEach { course ->
                    val startMinutes = store.periods.getOrNull(course.startPeriod - 1)?.let { minutesOf(it.start) }
                    val endMinutes = store.periods.getOrNull(course.endPeriod - 1)?.let { minutesOf(it.end) }
                    val isCurrent = startMinutes != null && endMinutes != null &&
                        nowMinutes in startMinutes..endMinutes
                    val isPast = endMinutes != null && nowMinutes > endMinutes
                    TodayCourseCard(
                        course = course,
                        start = store.periods.getOrNull(course.startPeriod - 1)?.start ?: "",
                        end = store.periods.getOrNull(course.endPeriod - 1)?.end ?: "",
                        isCurrent = isCurrent,
                        isPast = isPast,
                        onClick = { onEdit(course) }
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoBanner(color: Color, title: String, subtitle: String) {
    GlassSurface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        tint = color,
        tintAlpha = 0.3f,
        contentPadding = PaddingValues(16.dp),
        shadow = false
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            BasicText(title, style = TextStyle(contentColor(), 16.sp, FontWeight.SemiBold))
            BasicText(subtitle, style = TextStyle(secondaryContentColor(), 12.sp))
        }
    }
}

@Composable
private fun TodayCourseCard(
    course: Course,
    start: String,
    end: String,
    isCurrent: Boolean,
    isPast: Boolean,
    onClick: () -> Unit
) {
    val color = courseColor(course.colorIndex)
    GlassSurface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        tint = color,
        tintAlpha = if (isCurrent) 0.42f else 0.22f,
        contentPadding = PaddingValues(16.dp),
        onClick = onClick,
        shadow = false
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                Modifier.width(58.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                BasicText(start, style = TextStyle(contentColor(), 14.sp, FontWeight.SemiBold))
                BasicText(end, style = TextStyle(secondaryContentColor(), 11.sp))
            }
            Box(
                Modifier
                    .width(4.dp)
                    .size(width = 4.dp, height = 40.dp)
                    .background(color, CircleShape)
            )
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BasicText(
                        course.name,
                        style = TextStyle(
                            contentColor().copy(alpha = if (isPast) 0.5f else 1f),
                            16.sp,
                            FontWeight.SemiBold
                        )
                    )
                    if (isCurrent) {
                        BasicText(
                            "进行中",
                            style = TextStyle(accentColor(), 11.sp, FontWeight.SemiBold)
                        )
                    }
                }
                BasicText(
                    listOfNotNull(
                        course.periodLabel(),
                        course.location.takeIf { it.isNotBlank() }?.let { "@$it" },
                        course.teacher.takeIf { it.isNotBlank() }
                    ).joinToString(" · "),
                    style = TextStyle(secondaryContentColor(), 12.sp)
                )
                BasicText(
                    course.weeksLabel(),
                    style = TextStyle(secondaryContentColor(), 11.sp)
                )
            }
        }
    }
}
