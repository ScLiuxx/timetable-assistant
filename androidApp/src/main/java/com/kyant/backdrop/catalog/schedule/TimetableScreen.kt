package com.kyant.backdrop.catalog.schedule

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.floor

@Composable
fun TimetableScreen(
    store: ScheduleStore,
    onAdd: (day: Int, period: Int) -> Unit,
    onEdit: (Course) -> Unit
) {
    val today = todayEpochDay()
    val semester = store.semester
    val currentWeek = store.weekOf(today).coerceIn(1, semester.totalWeeks)
    val pagerState = rememberPagerState(initialPage = currentWeek - 1) { semester.totalWeeks }
    val week = pagerState.currentPage + 1
    val scope = rememberCoroutineScope()
    LaunchedEffect(currentWeek) {
        if (pagerState.currentPage != currentWeek - 1) {
            pagerState.scrollToPage(currentWeek - 1)
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Spacer(Modifier.size(10.dp))

        Column(
            Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    GlassTitle(semester.name, fontSize = 24)
                    GlassLabel("共 ${semester.totalWeeks} 周 · 每天 ${semester.periodsPerDay} 节", fontSize = 12)
                }
                GlassPillButton("回到本周", onClick = {
                    scope.launch { pagerState.animateScrollToPage(currentWeek - 1) }
                })
            }

            GlassSurface(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                shadow = false
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassPillButton(
                        "上一周",
                        onClick = {
                            if (week > 1) scope.launch { pagerState.animateScrollToPage(week - 2) }
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    )
                    Spacer(Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        GlassTitle("第 $week 周", fontSize = 17)
                        GlassLabel(
                            if (week == store.weekOf(today)) "本周" else {
                                val diff = week - store.weekOf(today)
                                if (diff > 0) "${diff} 周后" else "${-diff} 周前"
                            },
                            fontSize = 11
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    GlassPillButton(
                        "下一周",
                        onClick = {
                            if (week < semester.totalWeeks) scope.launch { pagerState.animateScrollToPage(week) }
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        HorizontalPager(
            state = pagerState,
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .navigationBarsPadding()
                .padding(horizontal = 8.dp)
                .padding(bottom = 84.dp)
        ) { page ->
            val pageWeek = page + 1
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val density = LocalDensity.current
                val timeColumnWidth = 40.dp
                val rowHeight = 58.dp
                val headerHeight = 50.dp
                val dayWidth = (maxWidth - timeColumnWidth) / 7

                val weekCourses = store.coursesForWeek(pageWeek)
                val gridHeight = rowHeight * semester.periodsPerDay

                Column(Modifier.fillMaxSize()) {
                    DayHeaderRow(
                        store = store,
                        week = pageWeek,
                        timeColumnWidth = timeColumnWidth,
                        dayWidth = dayWidth,
                        today = today,
                        height = headerHeight
                    )
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(gridHeight)
                                .pointerInput(pageWeek, semester) {
                                    detectTapGestures { position ->
                                        val px = with(density) { timeColumnWidth.toPx() }
                                        val dayPx = with(density) { dayWidth.toPx() }
                                        val rowPx = with(density) { rowHeight.toPx() }
                                        if (position.x >= px) {
                                            val day = (floor((position.x - px) / dayPx).toInt() + 1)
                                                .coerceIn(1, 7)
                                            val period = (floor(position.y / rowPx).toInt() + 1)
                                                .coerceIn(1, semester.periodsPerDay)
                                            onAdd(day, period)
                                        }
                                    }
                                }
                        ) {
                            GridLines(semester.periodsPerDay, rowHeight, timeColumnWidth, dayWidth)

                            Column(Modifier.width(timeColumnWidth)) {
                                repeat(semester.periodsPerDay) { index ->
                                    val period = store.periods.getOrNull(index)
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .height(rowHeight),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            BasicText(
                                                "${index + 1}",
                                                style = TextStyle(contentColor(), 13.sp, FontWeight.SemiBold)
                                            )
                                            if (period != null) {
                                                BasicText(
                                                    period.start,
                                                    style = TextStyle(secondaryContentColor(), 9.sp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            weekCourses.forEach { (day, courses) ->
                                courses.forEach { course ->
                                    val x = timeColumnWidth + dayWidth * (day - 1)
                                    val y = rowHeight * (course.startPeriod - 1)
                                    CourseCard(
                                        course = course,
                                        modifier = Modifier
                                            .offset(x = x + 1.dp, y = y + 2.dp)
                                            .width(dayWidth - 2.dp)
                                            .height(rowHeight * (course.endPeriod - course.startPeriod + 1) - 4.dp),
                                        onClick = { onEdit(course) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GridLines(
    periods: Int,
    rowHeight: Dp,
    timeColumnWidth: Dp,
    dayWidth: Dp
) {
    val lineColor = contentColor().copy(alpha = 0.08f)
    Canvas(Modifier.fillMaxSize()) {
        val rowPx = rowHeight.toPx()
        val timePx = timeColumnWidth.toPx()
        val dayPx = dayWidth.toPx()
        for (i in 0..periods) {
            val y = rowPx * i
            drawLine(lineColor, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(size.width, y), 1f)
        }
        for (i in 0..7) {
            val x = timePx + dayPx * i
            drawLine(lineColor, androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Offset(x, size.height), 1f)
        }
    }
}

@Composable
private fun DayHeaderRow(
    store: ScheduleStore,
    week: Int,
    timeColumnWidth: Dp,
    dayWidth: Dp,
    today: Long,
    height: Dp
) {
    Row(Modifier.fillMaxWidth().height(height)) {
        Box(Modifier.width(timeColumnWidth), contentAlignment = Alignment.BottomCenter) {
            BasicText(
                "节次",
                Modifier.padding(bottom = 8.dp),
                style = TextStyle(secondaryContentColor(), 10.sp)
            )
        }
        for (day in 1..7) {
            val epochDay = store.dateOf(week, day)
            val resolution = store.resolveDay(epochDay)
            val isToday = epochDay == today
            val headerColor = when {
                resolution.type == DayType.HOLIDAY -> Color(0xFFFF3B30)
                resolution.type == DayType.MAKEUP -> Color(0xFFFF9500)
                isToday -> accentColor()
                else -> contentColor()
            }
            Box(
                Modifier
                    .width(dayWidth)
                    .height(height),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    BasicText(
                        weekdayShortName(day),
                        style = TextStyle(headerColor, 12.sp, FontWeight.Medium)
                    )
                    BasicText(
                        when (resolution.type) {
                            DayType.HOLIDAY -> "休"
                            DayType.MAKEUP -> "补${weekdayShortName(resolution.courseDayOfWeek).removePrefix("周")}"
                            DayType.NORMAL -> formatMonthDay(epochDay)
                        },
                        style = TextStyle(
                            if (resolution.type == DayType.NORMAL) secondaryContentColor() else headerColor,
                            10.sp,
                            if (resolution.type == DayType.NORMAL) FontWeight.Normal else FontWeight.SemiBold
                        )
                    )
                }
                if (isToday) {
                    Box(
                        Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 2.dp)
                            .size(4.dp)
                            .background(accentColor(), androidx.compose.foundation.shape.CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
fun CourseCard(
    course: Course,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val color = courseColor(course.colorIndex)
    GlassSurface(
        modifier = modifier,
        shape = RoundedCornerShape(11.dp),
        tint = color,
        tintAlpha = 0.55f,
        contentPadding = PaddingValues(5.dp),
        onClick = onClick,
        shadow = false
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            BasicText(
                course.name,
                style = TextStyle(Color.White, 11.sp, FontWeight.SemiBold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (course.location.isNotBlank()) {
                BasicText(
                    "@${course.location}",
                    style = TextStyle(Color.White.copy(alpha = 0.9f), 9.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
