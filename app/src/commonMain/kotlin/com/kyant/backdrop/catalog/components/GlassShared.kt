package com.kyant.backdrop.catalog.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * 全局玻璃表面透明度（0.2 ~ 0.8），值越大玻璃越“实”、底层越透不过。
 * 与液态玻璃相关的组件（LiquidButton/Toggle/Slider/BottomTabs 及自建 Glass 组件）
 * 统一读取该值，使“玻璃透明度”调节实时作用到所有玻璃组件上。
 */
val LocalGlassOpacity = staticCompositionLocalOf<Float> { 0.42f }

/**
 * 依据当前局部玻璃透明度与明暗主题，计算玻璃表面基底色。
 * 供所有玻璃组件的 onDrawSurface 用透明度的基底填充，从而响应透明度调节。
 */
@Composable
fun glassSurfaceColor(alpha: Float = LocalGlassOpacity.current): Color =
    if (!isSystemInDarkTheme()) Color.White.copy(alpha = alpha)
    else Color(0xFF16161A).copy(alpha = alpha)