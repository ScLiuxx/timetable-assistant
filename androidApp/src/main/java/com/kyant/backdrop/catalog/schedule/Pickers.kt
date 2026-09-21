package com.kyant.backdrop.catalog.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.ceil

private val WEEK_DAYS = listOf("一", "二", "三", "四", "五", "六", "日")

/**
 * 统一的玻璃风格日期选择输入框（与其它 Glass UI 风格一致）。
 * 点击后弹出玻璃日历，选中后通过 [onChange] 回调。
 */
@Composable
fun GlassDateField(
    value: Long,
    onChange: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var open by rememberSaveable { mutableStateOf(false) }
    GlassPillButton(
        formatFullDate(value),
        onClick = { open = true },
        modifier = modifier
    )
    if (open) {
        GlassDatePickerDialog(
            initial = value,
            onConfirm = { selected ->
                onChange(selected)
                open = false
            },
            onDismiss = { open = false }
        )
    }
}

/**
 * 统一的玻璃态时间选择器。点击后弹出时间选择，选中后回调 [onChange]。
 */
@Composable
fun GlassTimeField(
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var open by rememberSaveable { mutableStateOf(false) }
    GlassPillButton(
        value,
        onClick = { open = true },
        modifier = modifier
    )
    if (open) {
        GlassTimePickerDialog(
            initial = value,
            onConfirm = { selected -> onChange(selected); open = false },
            onDismiss = { open = false }
        )
    }
}

@Composable
fun GlassDatePickerDialog(
    initial: Long,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val (iniY, iniM, iniD) = epochDayToCivil(initial)
    var year by rememberSaveable { mutableIntStateOf(iniY) }
    var month by rememberSaveable { mutableIntStateOf(iniM) }
    var selected by rememberSaveable { mutableLongStateOf(initial) }

    GlassDialog(onDismiss = onDismiss) {
        GlassTitle("选择日期", fontSize = 17)

        // 年月导航
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassPillButton(
                "‹",
                onClick = {
                    if (month <= 1) { year--; month = 12 } else month--
                },
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            )
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                GlassTitle("$year 年 $month 月", fontSize = 15)
            }
            GlassPillButton(
                "›",
                onClick = {
                    if (month == 12) { year++; month = 1 } else month++
                },
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // 星期表头
        Row(Modifier.fillMaxWidth()) {
            WEEK_DAYS.forEach { label ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    BasicText(
                        label,
                        style = TextStyle(secondaryContentColor(), 12.sp)
                    )
                }
            }
        }

        // 日期网格（周一开头）
        val dayCount = daysInMonthOf(year, month)
        val firstOffset = (isoDayOfWeek(civilToEpochDay(year, month, 1)) + 6) % 7
        val totalCells = ceil((firstOffset + dayCount) / 7.0).toInt() * 7
        for (r in 0 until totalCells / 7) {
            Row(Modifier.fillMaxWidth()) {
                for (c in 0..6) {
                    val index = r * 7 + c
                    val dayNumber = index - firstOffset + 1
                    val dayOfMonth = if (dayNumber in 1..dayCount) dayNumber else 0
                    if (dayOfMonth == 0) {
                        Spacer(Modifier.weight(1f).padding(2.dp))
                    } else {
                        val epoch = civilToEpochDay(year, month, dayOfMonth)
                        val isSelected = epoch == selected
                        val isToday = epoch == todayEpochDay()
                        Box(
                            Modifier
                                .weight(1f)
                                .padding(2.dp)
                                .height(36.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isSelected -> accentColor()
                                        isToday -> accentColor().copy(alpha = 0.22f)
                                        else -> Color.Transparent
                                    }
                                )
                                .clickable {
                                    selected = epoch
                                    val (sy, sm, sd) = epochDayToCivil(epoch)
                                    year = sy; month = sm
                                    // 保持选中日不回跳
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            BasicText(
                                dayOfMonth.toString(),
                                style = TextStyle(
                                    if (isSelected) Color.White else contentColor(),
                                    13.sp,
                                    if (isSelected || isToday) FontWeight.SemiBold else FontWeight.Normal
                                )
                            )
                        }
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                BasicText(
                    formatFullDate(selected),
                    style = TextStyle(secondaryContentColor(), 12.sp)
                )
            }
            GlassPillButton("取消", onClick = onDismiss)
            GlassPillButton(
                "确定",
                onClick = { onConfirm(selected) },
                tint = accentColor(),
                contentColorOverride = Color.White
            )
        }
    }
}

@Composable
fun GlassTimePickerDialog(
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val base = minutesOf(initial)?.let { it / 60 to it % 60 } ?: (8 to 0)
    var hour by rememberSaveable { mutableIntStateOf(base.first) }
    var minute by rememberSaveable { mutableIntStateOf(base.second) }

    GlassDialog(onDismiss = onDismiss) {
        GlassTitle("选择时间", fontSize = 16)
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BasicText(
                "%02d:%02d".format(hour, minute),
                style = TextStyle(accentColor(), 44.sp, FontWeight.SemiBold)
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 24 小时制小时列
                GlassNumberColumn(
                    modifier = Modifier.weight(1f),
                    label = "时",
                    range = 0..23,
                    value = hour,
                    onValueChange = { hour = it }
                )
                GlassNumberColumn(
                    modifier = Modifier.weight(1f),
                    label = "分",
                    range = 0..59 step 5,
                    value = minute,
                    onValueChange = { minute = it }
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f))
                GlassPillButton("取消", onClick = onDismiss)
                GlassPillButton(
                    "确定",
                    onClick = { onConfirm("%02d:%02d".format(hour, minute)) },
                    tint = accentColor(),
                    contentColorOverride = Color.White
                )
            }
        }
    }
}

/**
 * 简单的加减式数字选择列，用于玻璃态时间选择的小时/分钟调整。
 */
@Composable
private fun GlassNumberColumn(
    modifier: Modifier,
    label: String,
    range: IntProgression,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    val first = range.first
    val last = range.last
    val step = range.step
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BasicText(label, style = TextStyle(secondaryContentColor(), 12.sp))
        GlassPillButton(
            "▲",
            onClick = { onValueChange(nextInRange(value, first, last, step, 1)) },
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp)
        )
        GlassSurface(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
            shadow = false
        ) {
            BasicText(
                "%02d".format(value),
                Modifier.fillMaxWidth().align(Alignment.Center),
                style = TextStyle(contentColor(), 30.sp, FontWeight.SemiBold, textAlign = TextAlign.Center)
            )
        }
        GlassPillButton(
            "▼",
            onClick = { onValueChange(nextInRange(value, first, last, step, -1)) },
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp)
        )
    }
}

private fun nextInRange(
    value: Int,
    first: Int,
    last: Int,
    step: Int,
    direction: Int
): Int {
    val candidate = value + direction * step
    return when {
        direction > 0 && candidate > last -> first
        direction < 0 && candidate < first -> last
        else -> candidate
    }
}

private fun daysInMonthOf(year: Int, month: Int): Int = when (month) {
    1, 3, 5, 7, 8, 10, 12 -> 31
    4, 6, 9, 11 -> 30
    else -> if (isLeapYear(year)) 29 else 28
}

private fun isLeapYear(year: Int): Boolean =
    year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)