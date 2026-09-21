package com.kyant.backdrop.catalog.schedule

import android.content.Context
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.catalog.R
import com.kyant.backdrop.catalog.components.LiquidBottomTab
import com.kyant.backdrop.catalog.components.LiquidBottomTabs
import java.io.File

private data class TabItem(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private val Tabs = listOf(
    TabItem("课表", IconCalendar),
    TabItem("今日", IconToday),
    TabItem("调休", IconSwap),
    TabItem("课程", IconList),
    TabItem("设置", IconSettings)
)

@Composable
fun ScheduleApp() {
    val context = LocalContext.current
    val store = remember { ScheduleStore(context) }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        ReminderScheduler.ensureChannel(context)
    }
    LaunchedEffect(
        store.reminderEnabled,
        store.reminderLeadMinutes,
        store.courses.toList(),
        store.periods,
        store.semester,
        store.holidays.toList(),
        store.makeups.toList()
    ) {
        ReminderScheduler.reschedule(context, store)
    }

    var editorVisible by remember { mutableStateOf(false) }
    var editingCourse by remember { mutableStateOf<Course?>(null) }
    var editorDay by remember { mutableIntStateOf(1) }
    var editorPeriod by remember { mutableIntStateOf(1) }

    var customWallpaper by remember { mutableStateOf(loadWallpaperPainter(context)) }
    val pickWallpaper = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    wallpaperFile(context).outputStream().use { output -> input.copyTo(output) }
                }
            }.onSuccess {
                customWallpaper = loadWallpaperPainter(context)
            }
        }
    }

    val openAdd: (Int, Int) -> Unit = { day, period ->
        editingCourse = null
        editorDay = day
        editorPeriod = period
        editorVisible = true
    }
    val openEdit: (Course) -> Unit = { course ->
        editingCourse = course
        editorDay = course.dayOfWeek
        editorPeriod = course.startPeriod
        editorVisible = true
    }

    ScheduleBackdrop(customWallpaper) { backdrop ->
        CompositionLocalProvider(
            LocalGlassBackdrop provides backdrop,
            LocalAccentColor provides accentColorPreset(store.accentColorIndex)
        ) {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize()) {
                    when (selectedTab) {
                        0 -> TimetableScreen(store, onAdd = openAdd, onEdit = openEdit)
                        1 -> TodayScreen(store, onEdit = openEdit)
                        2 -> AdjustScreen(store)
                        3 -> CourseListScreen(store, onAdd = openAdd, onEdit = openEdit)
                        4 -> SettingsScreen(
                            store = store,
                            hasCustomWallpaper = customWallpaper != null,
                            onPickWallpaper = {
                                pickWallpaper.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onResetWallpaper = {
                                wallpaperFile(context).delete()
                                customWallpaper = null
                            }
                        )
                    }
                }

                LiquidBottomTabs(
                    selectedTabIndex = { selectedTab },
                    onTabSelected = { selectedTab = it },
                    backdrop = backdrop,
                    tabsCount = Tabs.size,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Tabs.forEachIndexed { index, item ->
                        LiquidBottomTab({ selectedTab = index }) {
                            IconView(item.icon, contentColor(), size = 21.dp)
                            BasicText(
                                item.label,
                                style = TextStyle(contentColor(), 10.sp)
                            )
                        }
                    }
                }

                if (editorVisible) {
                    CourseEditorDialog(
                        store = store,
                        existing = editingCourse,
                        defaultDay = editorDay,
                        defaultPeriod = editorPeriod,
                        onDismiss = { editorVisible = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduleBackdrop(
    customWallpaper: Painter?,
    content: @Composable BoxScope.(LayerBackdrop) -> Unit
) {
    val backdrop = rememberLayerBackdrop()
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Image(
            customWallpaper ?: painterResource(R.drawable.wallpaper_light),
            contentDescription = null,
            modifier = Modifier
                .layerBackdrop(backdrop)
                .fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        if (isSystemInDarkTheme()) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
        }
        content(backdrop)
    }
}

@Composable
fun ScreenScaffold(
    title: String,
    subtitle: String?,
    content: @Composable BoxScope.() -> Unit
) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        androidx.compose.foundation.layout.Spacer(Modifier.size(12.dp))
        Column(
            Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            BasicText(
                title,
                style = TextStyle(
                    contentColor(),
                    28.sp,
                    androidx.compose.ui.text.font.FontWeight.Bold
                )
            )
            if (subtitle != null) {
                GlassLabel(subtitle, fontSize = 13)
            }
        }
        androidx.compose.foundation.layout.Spacer(Modifier.size(12.dp))
        Box(
            Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(bottom = 88.dp),
            content = content
        )
    }
}

private fun wallpaperFile(context: Context): File = File(context.filesDir, "wallpaper.img")

private fun loadWallpaperPainter(context: Context): Painter? {
    val file = wallpaperFile(context)
    if (!file.exists() || file.length() == 0L) return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    return runCatching {
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, 2048)
        }
        BitmapFactory.decodeFile(file.absolutePath, options)
            ?.let { BitmapPainter(it.asImageBitmap()) }
    }.getOrNull()
}

private fun sampleSizeFor(width: Int, height: Int, maxSize: Int): Int {
    var sample = 1
    var w = width
    var h = height
    while (w / 2 >= maxSize || h / 2 >= maxSize) {
        w /= 2
        h /= 2
        sample *= 2
    }
    return sample
}
