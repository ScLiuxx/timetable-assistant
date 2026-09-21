package com.kyant.backdrop.catalog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.kyant.backdrop.catalog.schedule.MemoryTracker
import com.kyant.backdrop.catalog.schedule.ScheduleApp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            ScheduleApp()
        }
    }

    // 金标联盟「公平运行内存机制」应用侧适配：把系统内存压力等级传递给界面层，
    // 由 MemoryTracker 触发大对象（壁纸位图等）的按需释放，把内存交还系统。
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        MemoryTracker.onTrimMemory(level)
    }
}
