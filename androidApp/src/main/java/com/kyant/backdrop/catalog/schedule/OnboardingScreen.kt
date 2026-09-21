package com.kyant.backdrop.catalog.schedule

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class OnboardPage(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val desc: String
)

private val OnboardPages = listOf(
    OnboardPage(
        "欢迎使用课表助手",
        "让每周的课程与安排井然有序",
        IconCalendar,
        "基于液态玻璃界面的多周课程表。清晰罗列每一周、每一天、每一节的课程安排。"
    ),
    OnboardPage(
        "多周与单双周",
        "灵活适配你的课表模式",
        IconRefresh,
        "支持多周循环、单周/双周（奇偶周）课程，配合自定义作息时间与总周数，自动切换。"
    ),
    OnboardPage(
        "今日与调休补课",
        "重要的日子绝不错过",
        IconBell,
        "「今日」页汇总当天课程；节假日、调休、补课一键管理，周次自动换算。"
    ),
    OnboardPage(
        "上课提醒与 Live Updates",
        "上课前，稳稳提醒",
        IconBell,
        "桌面小组件展示今日课程；课程开始时状态栏 Live Updates 实时显示当前课程进度与倒计时。"
    ),
    OnboardPage(
        "个性化与数据",
        "属于你的课表",
        IconSettings,
        "自定义主题色、背景壁纸与玻璃透明度；课表数据可导出分享，也能一键导入恢复。"
    )
)

/**
 * 全屏新手指引。在玻璃背景之上展示分页引导，支持「跳过」与本页切换。
 */
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    onSkip: () -> Unit
) {
    var page by remember { mutableIntStateOf(0) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.18f))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 顶部 app 标识
            Box(
                Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(contentColor().copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                IconView(OnboardPages[0].icon, accentColor(), size = 34.dp)
            }
            Spacer(Modifier.size(20.dp))

            // 玻璃内容卡片
            GlassSurface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 24.dp, vertical = 28.dp
                ),
                shadow = false
            ) {
                AnimatedContent(
                    targetState = page,
                    transitionSpec = {
                        (slideInVertically { it / 4 } + fadeIn()) togetherWith
                            (slideOutVertically { -it / 4 } + fadeOut())
                    },
                    label = "onboard"
                ) { index ->
                    OnboardPageContent(OnboardPages[index])
                }
            }

            Spacer(Modifier.size(24.dp))

            // 分页指示点
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(OnboardPages.size) { index ->
                    val active = index == page
                    Box(
                        Modifier
                            .size(if (active) 18.dp else 8.dp, 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (active) accentColor() else contentColor().copy(alpha = 0.25f)
                            )
                    )
                }
            }

            Spacer(Modifier.size(24.dp))

            // 底部按钮
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassPillButton(
                    "跳过",
                    onClick = onSkip,
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        vertical = 13.dp
                    )
                )
                GlassPillButton(
                    if (page == OnboardPages.lastIndex) "开始使用" else "下一步",
                    onClick = {
                        if (page == OnboardPages.lastIndex) onFinish() else page++
                    },
                    modifier = Modifier.weight(1f),
                    tint = accentColor(),
                    contentColorOverride = Color.White,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        vertical = 13.dp
                    )
                )
            }
        }
    }
}

@Composable
private fun OnboardPageContent(page: OnboardPage) {
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(accentColor().copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            IconView(page.icon, accentColor(), size = 38.dp)
        }
        Spacer(Modifier.size(20.dp))
        androidx.compose.foundation.text.BasicText(
            page.title,
            style = TextStyle(contentColor(), 22.sp, FontWeight.SemiBold)
        )
        Spacer(Modifier.size(8.dp))
        androidx.compose.foundation.text.BasicText(
            page.subtitle,
            style = TextStyle(secondaryContentColor(), 13.sp)
        )
        Spacer(Modifier.size(16.dp))
        androidx.compose.foundation.text.BasicText(
            page.desc,
            Modifier.fillMaxWidth(),
            style = TextStyle(
                secondaryContentColor(),
                15.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
        )
    }
}