package com.kyant.backdrop.catalog.schedule

import android.content.Context
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import kotlinx.coroutines.delay
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.catalog.R
import com.kyant.backdrop.catalog.components.LocalGlassOpacity
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
    var showOnboarding by remember { mutableStateOf(!store.onboardingDone) }

    // 平板 / 大屏检测：宽度 ≥ 600dp 时切换为侧边导航栏布局。
    val isWide = LocalConfiguration.current.screenWidthDp >= 600

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

    // 周期刷新 Live Updates：进行中的课程进度滚动更新，下课后自动撤销。
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            LiveUpdateManager.sync(context, store)
        }
    }

    var editorVisible by remember { mutableStateOf(false) }
    var editingCourse by remember { mutableStateOf<Course?>(null) }
    var editorDay by remember { mutableIntStateOf(1) }
    var editorPeriod by remember { mutableIntStateOf(1) }

    var customWallpaper by remember { mutableStateOf(loadWallpaperPainter(context)) }
    // 公平运行内存机制：系统内存紧张时释放壁纸位图，把内存交还系统，避免卡顿/被杀。
    LaunchedEffect(MemoryTracker.level) {
        if (MemoryTracker.shouldRelease()) {
            customWallpaper = null
        }
    }
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
            LocalAccentColor provides accentColorPreset(store.accentColorIndex),
            LocalGlassOpacity provides store.glassOpacity
        ) {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(
                            start = if (isWide) 84.dp else 0.dp,
                            bottom = if (isWide) 0.dp else 66.dp
                        )
                ) {
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
                            },
                            onShowOnboarding = {
                                showOnboarding = true
                                selectedTab = 0
                            }
                        )
                    }
                }

                if (isWide) {
                    GlassSurface(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 12.dp, top = 56.dp, bottom = 24.dp)
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .width(64.dp)
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(28.dp),
                        contentPadding = PaddingValues(vertical = 14.dp, horizontal = 8.dp),
                        shadow = false
                    ) {
                        Column(
                            Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Tabs.forEachIndexed { index, item ->
                                val selected = selectedTab == index
                                Column(
                                    Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable(
                                            interactionSource = null,
                                            indication = null,
                                            onClick = { selectedTab = index }
                                        ),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    IconView(
                                        item.icon,
                                        if (selected) accentColor() else secondaryContentColor(),
                                        size = 22.dp
                                    )
                                    androidx.compose.foundation.layout.Spacer(Modifier.size(3.dp))
                                    BasicText(
                                        item.label,
                                        style = TextStyle(
                                            if (selected) accentColor() else secondaryContentColor(),
                                            9.sp,
                                            if (selected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                                        )
                                    )
                                }
                            }
                        }
                    }
                } else {
                    LiquidBottomTabs(
                        selectedTabIndex = { selectedTab },
                        onTabSelected = { selectedTab = it },
                        backdrop = backdrop,
                        tabsCount = Tabs.size,
                        glassOpacity = store.glassOpacity,
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

                if (showOnboarding) {
                    OnboardingScreen(
                        onFinish = {
                            store.completeOnboarding()
                            showOnboarding = false
                        },
                        onSkip = {
                            store.completeOnboarding()
                            showOnboarding = false
                        }
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
