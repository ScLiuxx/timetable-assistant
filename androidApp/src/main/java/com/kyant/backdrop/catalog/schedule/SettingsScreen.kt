package com.kyant.backdrop.catalog.schedule

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

@Composable
fun SettingsScreen(
    store: ScheduleStore,
    hasCustomWallpaper: Boolean,
    onPickWallpaper: () -> Unit,
    onResetWallpaper: () -> Unit
) {
    val context = LocalContext.current
    var showExport by remember { mutableStateOf(false) }
    var exportText by remember { mutableStateOf("") }
    var showImport by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    var importMessage by remember { mutableStateOf<String?>(null) }
    var confirmReset by remember { mutableStateOf(false) }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> store.updateReminderEnabled(granted) }
    val toggleReminder: () -> Unit = {
        if (store.reminderEnabled) {
            store.updateReminderEnabled(false)
        } else if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            store.updateReminderEnabled(true)
        }
    }

    ScreenScaffold(
        title = "设置",
        subtitle = "学期信息、作息时间与数据管理"
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeader("学期信息")
            GlassSection {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    GlassLabel("课表名称", fontSize = 12)
                    GlassTextField(
                        value = store.semester.name,
                        onValueChange = { store.updateSemester(store.semester.copy(name = it)) },
                        placeholder = "例如：2026 春季学期"
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    GlassLabel("学期开始日期（第 1 周周一）", fontSize = 12)
                    GlassPillButton(
                        formatFullDate(store.semester.startEpochDay),
                        onClick = {
                            showDatePicker(context, store.semester.startEpochDay) {
                                store.updateSemester(store.semester.copy(startEpochDay = mondayOfWeek(it)))
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    val currentWeek = store.weekOf(todayEpochDay())
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicText(
                            if (currentWeek >= 1) {
                                "今天是第 $currentWeek 周"
                            } else {
                                "尚未开学"
                            },
                            style = TextStyle(secondaryContentColor(), 12.sp)
                        )
                        FlexSpacer()
                        GlassPillButton(
                            "设为本周一",
                            onClick = {
                                store.updateSemester(
                                    store.semester.copy(startEpochDay = mondayOfWeek(todayEpochDay()))
                                )
                            },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 12.dp,
                                vertical = 6.dp
                            )
                        )
                    }
                }
                StepperSetting(
                    label = "总周数",
                    value = store.semester.totalWeeks,
                    range = 1..60
                ) { store.updateSemester(store.semester.copy(totalWeeks = it)) }
                StepperSetting(
                    label = "每天节数",
                    value = store.semester.periodsPerDay,
                    range = 1..20
                ) { store.updateSemester(store.semester.copy(periodsPerDay = it)) }
            }

            SectionHeader("作息时间")
            GlassSection {
                for (index in 0 until store.semester.periodsPerDay) {
                    val period = store.periods.getOrNull(index) ?: PeriodTime()
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicText(
                            "第 ${index + 1} 节",
                            style = TextStyle(contentColor(), 14.sp),
                            modifier = Modifier.padding(end = 10.dp)
                        )
                        FlexSpacer()
                        GlassPillButton(
                            period.start,
                            onClick = {
                                showTimePicker(context, period.start) { newStart ->
                                    store.updatePeriods(
                                        store.periods.toMutableList().also {
                                            it[index] = it[index].copy(start = newStart)
                                        }
                                    )
                                }
                            },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 12.dp,
                                vertical = 6.dp
                            )
                        )
                        BasicText(
                            " ~ ",
                            style = TextStyle(secondaryContentColor(), 13.sp)
                        )
                        GlassPillButton(
                            period.end,
                            onClick = {
                                showTimePicker(context, period.end) { newEnd ->
                                    store.updatePeriods(
                                        store.periods.toMutableList().also {
                                            it[index] = it[index].copy(end = newEnd)
                                        }
                                    )
                                }
                            },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 12.dp,
                                vertical = 6.dp
                            )
                        )
                    }
                }
            }

            SectionHeader("上课提醒")
            GlassSection {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        BasicText("上课提醒", style = TextStyle(contentColor(), 15.sp))
                        BasicText(
                            "课程开始前发送通知",
                            style = TextStyle(secondaryContentColor(), 12.sp)
                        )
                    }
                    GlassPillButton(
                        if (store.reminderEnabled) "已开启" else "已关闭",
                        onClick = toggleReminder,
                        tint = if (store.reminderEnabled) accentColor() else Color.Unspecified,
                        contentColorOverride = if (store.reminderEnabled) Color.White else null
                    )
                }
                if (store.reminderEnabled) {
                    GlassLabel("提前提醒", fontSize = 12)
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (minutes in listOf(5, 10, 15, 30)) {
                            GlassChip(
                                "$minutes 分钟",
                                selected = store.reminderLeadMinutes == minutes,
                                onClick = { store.updateReminderLeadMinutes(minutes) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    if (!canScheduleExactAlarms(context)) {
                        GlassLabel("系统未授予精确闹钟权限，提醒时间可能有偏差。", fontSize = 11)
                        GlassPillButton(
                            "去授权",
                            onClick = { openExactAlarmSettings(context) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    GlassLabel("开启后可在课程开始前收到通知提醒。", fontSize = 12)
                }
            }

            SectionHeader("外观")
            GlassSection {
                SettingsRowButton("更换背景图片", onPickWallpaper)
                if (hasCustomWallpaper) {
                    SettingsRowButton("恢复默认背景", onClick = onResetWallpaper)
                }
            }

            SectionHeader("数据管理")
            GlassSection {
                SettingsRowButton("导出课表", onClick = {
                    exportText = store.exportJson()
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("course_schedule", exportText))
                    showExport = true
                })
                SettingsRowButton("导入课表", onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val text = clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()
                    importText = if (!text.isNullOrBlank() && text.trimStart().startsWith("{")) text else ""
                    importMessage = null
                    showImport = true
                })
                SettingsRowButton("恢复默认设置", onClick = { confirmReset = true }, danger = true)
            }

            SectionHeader("关于")
            GlassSection {
                GlassLabel("课表助手 · 基于 Liquid Glass 液态玻璃界面", fontSize = 13)
                GlassLabel("支持多周课表、单双周、节假日与调休补课。", fontSize = 12)
            }
        }
    }

    if (showExport) {
        GlassDialog(onDismiss = { showExport = false }) {
            GlassTitle("导出成功")
            GlassLabel("课表数据已复制到剪贴板，可以粘贴保存或分享。", fontSize = 13)
            BasicText(
                exportText.take(600),
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 180.dp)
                    .verticalScroll(rememberScrollState()),
                style = TextStyle(secondaryContentColor(), 11.sp)
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FlexSpacer()
                GlassPillButton("完成", onClick = { showExport = false })
            }
        }
    }

    if (showImport) {
        GlassDialog(onDismiss = { showImport = false }) {
            GlassTitle("导入课表")
            GlassLabel("请粘贴导出的课表 JSON 数据。", fontSize = 13)
            GlassTextField(
                value = importText,
                onValueChange = {
                    importText = it
                    importMessage = null
                },
                placeholder = "在此粘贴 JSON",
                singleLine = false,
                minLines = 5
            )
            importMessage?.let {
                BasicText(
                    it,
                    style = TextStyle(
                        if (it.startsWith("导入成功")) accentColor() else Color(0xFFFF3B30),
                        12.sp
                    )
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FlexSpacer()
                GlassPillButton("取消", onClick = { showImport = false })
                GlassPillButton(
                    "导入",
                    onClick = {
                        importMessage = if (store.importJson(importText.trim())) {
                            "导入成功"
                        } else {
                            "导入失败：数据格式不正确"
                        }
                    },
                    tint = accentColor(),
                    contentColorOverride = Color.White
                )
            }
        }
    }

    if (confirmReset) {
        GlassDialog(onDismiss = { confirmReset = false }) {
            GlassTitle("恢复默认设置")
            GlassLabel("将清空所有课程、调休与学期设置，且不可撤销。", fontSize = 13)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FlexSpacer()
                GlassPillButton("取消", onClick = { confirmReset = false })
                GlassPillButton(
                    "恢复",
                    onClick = {
                        store.resetAll()
                        confirmReset = false
                    },
                    tint = Color(0xFFFF3B30),
                    contentColorOverride = Color.White
                )
            }
        }
    }
}

@Composable
private fun SettingsRowButton(
    label: String,
    onClick: () -> Unit,
    danger: Boolean = false
) {
    GlassSurface(
        Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        onClick = onClick,
        shadow = false
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            BasicText(
                label,
                style = TextStyle(if (danger) Color(0xFFFF3B30) else contentColor(), 15.sp)
            )
            FlexSpacer()
            IconView(IconChevronRight, secondaryContentColor(), size = 18.dp)
        }
    }
}

@Composable
private fun StepperSetting(
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
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 4.dp)
        )
        BasicText(
            value.toString(),
            Modifier.padding(horizontal = 14.dp),
            style = TextStyle(contentColor(), 16.sp, FontWeight.SemiBold)
        )
        GlassPillButton(
            "+",
            onClick = { if (value < range.last) onChange(value + 1) },
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 4.dp)
        )
    }
}
