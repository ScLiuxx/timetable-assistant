package com.kyant.backdrop.catalog.schedule

import android.content.ComponentCallbacks2
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

/**
 * 应用级内存压力跟踪，对应金标联盟「公平运行内存机制」中的应用侧优化。
 *
 * 系统通过 [MainActivity.onTrimMemory] 上报内存等级后，将 [level] 更新为组合状态，
 * 界面层据此在内存紧张时释放大对象（如壁纸位图缓存），从而把不必要的内存交还系统，
 * 避免后台被杀 / 卡顿 / 闪退等共性问题。
 */
@Suppress("DEPRECATION") // TRIM_MEMORY_RUNNING_* 在新 API 中标记废弃但语义清晰，兼容多版本。
object MemoryTracker {

    /** 当前内存压力等级，取 [ComponentCallbacks2] 的 TRIM_MEMORY_* 常量；0 表示无压力。 */
    var level by mutableIntStateOf(0)

    /** 是否达到需要释放资源的高压档（RUNNING_MODERATE 及以上）。 */
    fun shouldRelease(): Boolean =
        level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE

    /** 由 Activity.onTrimMemory 调用，注入系统压力等级。 */
    fun onTrimMemory(newLevel: Int) {
        if (newLevel > level) {
            level = newLevel
        } else if (newLevel == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            level = newLevel
        }
    }

    /**
     * 回到前台时调用：把内存压力等级复位到 0。
     *
     * onTrimMemory 只会把 [level] 推高，若不做复位，一旦触发过一次高压释放
     * （如壁纸位图被清空）就永远不会恢复。回到前台通常意味着应用重回焦点、
     * 系统压力缓解，此时复位等级并允许界面层重新加载被释放的资源。
     */
    fun recover() {
        level = 0
    }
}