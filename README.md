# 课表助手（Timetable Assistant）

基于 Compose Multiplatform 与 **Liquid Glass 液态玻璃**界面打造的课程表应用，强调通透、可实时调节的玻璃质感与流畅交互。

## 功能

- **多课程表管理**：支持多周循环课表、单周 / 双周（奇偶周）、每日多节自定义作息时间。
- **假期与调休**：节假日、调休补课编辑，自动换算当周周次。
- **上课提醒**：基于系统机台通知 + 精确闹钟权限，课程开始前按提前量提醒。
- **桌面小组件**：2x2 紧凑 / 4x2 宽图两种模板，跟随应用主题色渲染。
- **Android 16 Live Updates**：上课过程中状态栏实时展示当前课程与倒计时。
- **液态玻璃 UI**：基于本仓库 `backdrop` 渲染库的液态玻璃材质，支持全局玻璃透明度调节（0.2 ~ 0.8），并支持自定义背景壁纸。
- **主题与可访问性**：主题色自定义、浅色/深色自适应。
- **数据管理**：JSON / iCal（.ics）导出、JSON 导入，本地持久化。

## 技术栈

- Kotlin / Kotlin Multiplatform + Compose Multiplatform
- `backdrop`：自研液态玻璃（Liquid Glass）效果渲染库（`blur`、`vibrancy`、`lens`、`highlight`、`shadow`）
- Android Widget（RemoteViews）+ Android 16 Live Updates（`NotificationCompat.ProgressStyle` + 置顶服务通知）
- Gradle 构建，JDK 17

## 目录结构

```
androidApp/    课表助手 Android 应用（界面、状态、小组件、Live Updates）
app/           液态玻璃交互组件库（LiquidButton / LiquidToggle / LiquidSlider / LiquidBottomTabs 等）
backdrop/      Liquid Glass 效果渲染库（drawBackdrop / blur / lens / highlight / shadow）
```

## 构建与运行

```bash
# 构建 Debug APK
./gradlew :androidApp:assembleDebug
# 产物：androidApp/build/outputs/apk/debug/androidApp-debug.apk

# 构建 Release APK
./gradlew :androidApp:assembleRelease
```

要求：JDK 17+，Android SDK 37（compileSdk），应用 `targetSdk 36` 以兼容 Android 16。

## 截图

> 待补充截图

## 开源许可

[MIT](./LICENSE)