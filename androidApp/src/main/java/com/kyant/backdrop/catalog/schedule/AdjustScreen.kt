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
import androidx.compose.runtime.mutableLongStateOf
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
fun AdjustScreen(store: ScheduleStore) {
    var showHolidayDialog by remember { mutableStateOf(false) }
    var showMakeupDialog by remember { mutableStateOf(false) }
    var pendingMakeup by remember { mutableStateOf<Makeup?>(null) }
    var pendingMakeupDay by remember { mutableLongStateOf(0L) }

    ScreenScaffold(
        title = "调休管理",
        subtitle = "设置节假日放假与调休补课安排"
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GlassSection {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    GlassTitle("如何使用", fontSize = 15)
                    GlassLabel(
                        "节假日：该日期整天不上课。",
                        fontSize = 12
                    )
                    GlassLabel(
                        "调休补课：该日期按指定星期的课表上课，例如周六补周一的课。",
                        fontSize = 12
                    )
                }
            }

            GlassSection {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    GlassTitle("节假日", fontSize = 16)
                    FlexSpacer()
                    GlassPillButton(
                        "添加",
                        onClick = { showHolidayDialog = true },
                        tint = Color(0xFFFF3B30),
                        contentColorOverride = Color.White
                    )
                }
                if (store.holidays.isEmpty()) {
                    CenteredLabel("暂无节假日安排")
                } else {
                    store.holidays.forEach { holiday ->
                        AdjustItem(
                            color = Color(0xFFFF3B30),
                            title = holiday.name,
                            subtitle = "${formatFullDate(holiday.epochDay)}（第 ${store.weekOf(holiday.epochDay)} 周）",
                            onDelete = { store.removeHoliday(holiday.epochDay) }
                        )
                    }
                }
            }

            GlassSection {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    GlassTitle("调休补课", fontSize = 16)
                    FlexSpacer()
                    GlassPillButton(
                        "添加",
                        onClick = { showMakeupDialog = true },
                        tint = Color(0xFFFF9500),
                        contentColorOverride = Color.White
                    )
                }
                if (store.makeups.isEmpty()) {
                    CenteredLabel("暂无调休补课安排")
                } else {
                    store.makeups.forEach { makeup ->
                        AdjustItem(
                            color = Color(0xFFFF9500),
                            title = makeup.name,
                            subtitle = "${formatFullDate(makeup.epochDay)} 按${weekdayShortName(makeup.targetDayOfWeek)}课表上课",
                            onDelete = { store.removeMakeup(makeup.epochDay) }
                        )
                    }
                }
            }
        }
    }

    if (showHolidayDialog) {
        HolidayDialog(
            onDismiss = { showHolidayDialog = false },
            onSave = { day, name ->
                store.addHoliday(Holiday(day, name.ifBlank { "节假日" }))
                showHolidayDialog = false
            }
        )
    }

    if (showMakeupDialog) {
        MakeupDialog(
            onDismiss = { showMakeupDialog = false },
            onSave = { day, target, name ->
                if (store.holidays.any { it.epochDay == day }) {
                    // 同日已设节假日，先弹出冲突确认，避免静默失效。
                    pendingMakeup = Makeup(day, target, name.ifBlank { "调休补课" })
                    pendingMakeupDay = day
                    showMakeupDialog = false
                } else {
                    store.addMakeup(Makeup(day, target, name.ifBlank { "调休补课" }))
                    showMakeupDialog = false
                }
            }
        )
    }

    pendingMakeup?.let { makeup ->
        GlassDialog(onDismiss = { pendingMakeup = null }) {
            GlassTitle("提醒：与节假日冲突")
            GlassLabel(
                "${formatFullDate(pendingMakeupDay)} 已设为「${
                    store.holidays.firstOrNull { it.epochDay == pendingMakeupDay }?.name ?: "节假日"
                }」，节假日会优先于调休，该调休安排可能不生效。仍要添加吗？",
                fontSize = 13
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FlexSpacer()
                GlassPillButton("取消", onClick = { pendingMakeup = null })
                GlassPillButton(
                    "仍要添加",
                    onClick = {
                        store.addMakeup(makeup)
                        pendingMakeup = null
                    },
                    tint = Color(0xFFFF9500),
                    contentColorOverride = Color.White
                )
            }
        }
    }
}

@Composable
private fun AdjustItem(
    color: Color,
    title: String,
    subtitle: String,
    onDelete: () -> Unit
) {
    GlassSurface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        tint = color,
        tintAlpha = 0.2f,
        contentPadding = PaddingValues(14.dp),
        shadow = false
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(width = 4.dp, height = 34.dp)
                    .background(color, RoundedCornerShape(2.dp))
            )
            Column(
                Modifier.weight(1f).padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                BasicText(title, style = TextStyle(contentColor(), 15.sp, FontWeight.Medium))
                BasicText(subtitle, style = TextStyle(secondaryContentColor(), 12.sp))
            }
            GlassPillButton(
                "删除",
                onClick = onDelete,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun HolidayDialog(
    onDismiss: () -> Unit,
    onSave: (Long, String) -> Unit
) {
    var date by remember { mutableLongStateOf(todayEpochDay()) }
    var name by remember { mutableStateOf("") }

    GlassDialog(onDismiss = onDismiss) {
        GlassTitle("添加节假日")
        GlassLabel("日期", fontSize = 12)
        GlassDateField(
            value = date,
            onChange = { date = it },
            modifier = Modifier.fillMaxWidth()
        )
        GlassLabel("名称", fontSize = 12)
        GlassTextField(value = name, onValueChange = { name = it }, placeholder = "例如：国庆节")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FlexSpacer()
            GlassPillButton("取消", onClick = onDismiss)
            GlassPillButton(
                "保存",
                onClick = { onSave(date, name) },
                tint = Color(0xFFFF3B30),
                contentColorOverride = Color.White
            )
        }
    }
}

@Composable
private fun MakeupDialog(
    onDismiss: () -> Unit,
    onSave: (Long, Int, String) -> Unit
) {
    var date by remember { mutableLongStateOf(todayEpochDay()) }
    var target by remember { mutableIntStateOf(1) }
    var name by remember { mutableStateOf("") }

    GlassDialog(onDismiss = onDismiss) {
        GlassTitle("添加调休补课")
        GlassLabel("补课日期", fontSize = 12)
        GlassDateField(
            value = date,
            onChange = { date = it },
            modifier = Modifier.fillMaxWidth()
        )
        GlassLabel("按哪天的课表上课", fontSize = 12)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (d in 1..7) {
                GlassChip(
                    weekdayShortName(d).removePrefix("周"),
                    selected = target == d,
                    onClick = { target = d },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        GlassLabel("备注", fontSize = 12)
        GlassTextField(value = name, onValueChange = { name = it }, placeholder = "例如：国庆调休")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FlexSpacer()
            GlassPillButton("取消", onClick = onDismiss)
            GlassPillButton(
                "保存",
                onClick = { onSave(date, target, name) },
                tint = Color(0xFFFF9500),
                contentColorOverride = Color.White
            )
        }
    }
}
