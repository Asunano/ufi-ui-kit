# UFI UI Kit 使用文档

> **版本**: 0.1 | **最低 SDK**: API 26 (Android 8.0) | **目标 SDK**: 34
>
> 从 UFI-TOOLS Widget 项目提取的通用 Android UI 组件库。4 个核心 Kit 类覆盖主题、弹窗、动画和控件。

---

## 目录

1. [快速接入](#1-快速接入)
2. [库结构总览](#2-库结构总览)
3. [ThemeKit — 主题系统](#3-themekit--主题系统)
4. [DialogKit — 弹窗与通知](#4-dialogkit--弹窗与通知)
5. [AnimKit — 动画与触摸反馈](#5-animkit--动画与触摸反馈)
6. [WidgetKit — 通用控件](#6-widgetkit--通用控件)
7. [辅助组件](#7-辅助组件)
8. [XML 资源清单](#8-xml-资源清单)
9. [在别的项目中使用](#9-在别的项目中使用)

---

## 1. 快速接入

### 作为模块引入（推荐）

将 `ufi-ui-kit` 目录复制到目标项目根目录，然后：

**settings.gradle.kts**：
```kotlin
include(":ufi-ui-kit")
```

**app/build.gradle.kts**：
```kotlin
dependencies {
    implementation(project(":ufi-ui-kit"))
}
```

确保根项目 `build.gradle.kts` 有 Android Library 插件：
```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false  // 新增
    alias(libs.plugins.kotlin.android) apply false
}
```

### 使用 AAR

```bash
./gradlew :ufi-ui-kit:assembleRelease
# 生成: ufi-ui-kit/build/outputs/aar/ufi-ui-kit-release.aar
```

复制到目标项目 `libs/` 目录，然后：
```kotlin
dependencies {
    implementation(files("libs/ufi-ui-kit-release.aar"))
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.cardview:cardview:1.0.0")
}
```

### 初始化

```kotlin
import com.ufiuikit.UfiUIKit

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    UfiUIKit.applyTheme(this)  // 自动处理背景色、文字色、图标色
}
```

---

## 2. 库结构总览

```
ufi-ui-kit/src/main/
├── java/com/ufiuikit/
│   ├── UfiUIKit.kt          # 公开入口（配色快捷方法 + 主题应用）
│   ├── util/
│   │   ├── ThemeKit.kt      # 主题系统（ThemeKit + ThemeApplier + ThemeNotifier）
│   │   ├── DialogKit.kt     # 弹窗框架（DialogKit + PopupKit + ToastKit）
│   │   ├── AnimKit.kt       # 动画引擎（模糊、揭露、文本渐变、触摸反馈）
│   │   ├── WidgetKit.kt     # 通用控件（WidgetKit + PaginationKit + CropView）
│   │   ├── ThemedSliderUtil.kt  # 主题滑块工厂
│   │   ├── SPUtil.kt        # [内部] SharedPreferences
│   │   ├── DebugLogger.kt   # [内部] 日志
│   │   └── BackgroundUtil.kt # [内部] 窗口背景
│   └── view/
│       ├── ThemeSlider.kt   # 主题滑块自定义 View
│       └── LoadingAnimationView.kt # 加载动画 View
└── res/
    ├── layout/   (7 个通用布局)
    ├── drawable/ (38 个背景/形状/图标)
    ├── anim/     (3 个动画)
    └── values/   (colors, themes, strings)
```

**4 个核心 Kit 类**：

| Kit | 职责 | 包含的类 |
|-----|------|----------|
| `ThemeKit` | 主题配色 + 视图着色 + 主题通知 | ThemeKit, ThemeApplier, ThemeNotifier |
| `DialogKit` | 弹窗 + 下拉菜单 + Toast | DialogKit, PopupKit, ToastKit |
| `AnimKit` | 所有动画 + 触摸反馈 | AnimKit |
| `WidgetKit` | 设置项 + 翻页栏 + 裁切 | WidgetKit, PaginationKit, CropView |

---

## 3. ThemeKit — 主题系统

### 预设主题

| ID | 名称 | 强调色 |
|----|------|--------|
| 0 | 默认 | 黑色 #222222 |
| 1 | 科技蓝 | 蓝色 #1677FF |
| 2 | 薄荷绿 | 绿色 #34C799 |
| 3 | 梦幻紫 | 紫色 #7B61FF |
| 4 | 活力橙 | 橙色 #FF7D34 |

### 获取配色

```kotlin
val accent = ThemeKit.accent(context)         // 强调色
val cardBg = ThemeKit.cardBg(context)         // 卡片背景
val pageBg = ThemeKit.pageBg(context)         // 页面背景
val textPrimary = ThemeKit.textPrimary(context)   // 主文字色
val textSecondary = ThemeKit.textSecondary(context) // 辅助文字色
val btnBg = ThemeKit.btnBg(context)           // 按钮背景色
val iconTint = ThemeKit.iconTint(context)     // 图标着色
val divider = ThemeKit.divider(context)       // 分隔线色
val dataHighlight = ThemeKit.dataHighlight(context) // 数据高亮色
val isDark = ThemeKit.isDark(context)         // 是否暗色模式
```

### 切换主题

```kotlin
UfiUIKit.setColorTheme(context, 1)     // 科技蓝
UfiUIKit.setAppTheme(context, "dark")  // 暗色模式
UfiUIKit.setAppTheme(context, "system") // 跟随系统
```

### 对 Activity 应用主题

```kotlin
// 自动处理窗口背景 + 递归着色视图树
UfiUIKit.applyTheme(activity)

// 或对局部视图树着色（弹窗内容、动态视图）
UfiUIKit.applyThemeToViewTree(myViewGroup)
```

### ThemeApplier — 通用着色方法

```kotlin
// 创建圆角卡片背景
val bg = ThemeApplier.makeCardBg(ThemeKit.cardBg(context))
myView.background = bg

// 设置自定义开关
ThemeApplier.setupSwitch(view, initialChecked = true) { checked ->
    // 处理开关变化
}

// 配置设置项卡片
ThemeApplier.setupSettingItem(view, iconRes, title, subtitle)
```

### ThemeNotifier — 主题变更广播

```kotlin
// 发送通知（设置保存后调用）
ThemeNotifier.notifyThemeChanged(context)

// 接收通知（在 Activity 中注册）
private var receiver: BroadcastReceiver? = null
override fun onResume() {
    super.onResume()
    receiver = ThemeNotifier.register(this, Runnable {
        UfiUIKit.applyTheme(this)
    })
}
override fun onPause() {
    super.onPause()
    ThemeNotifier.unregister(this, receiver)
}
```

---

## 4. DialogKit — 弹窗与通知

### DialogKit — 通用弹窗

```kotlin
// 一步到位弹窗
DialogKit.showCommonDialog(
    context = this,
    title = "连接设置",
    iconRes = R.drawable.ic_settings,
    onFill = { content ->
        // content 是 LinearLayout，往里面添加任意视图
        content.addView(TextView(this).apply { text = "弹窗内容" })
    },
    primaryBtnText = "确定",
    onPrimaryClick = { dialog -> dialog.dismiss() },
    secondaryBtnText = "取消",
    onSecondaryClick = { dialog -> dialog.dismiss() }
)

// 红色警告弹窗
DialogKit.showWarningConfirmDialog(
    context = this,
    title = "删除确认",
    message = "此操作不可撤销",
    onConfirm = { /* 执行删除 */ }
)

// 选择列表弹窗（无按钮，点击选项即关闭）
DialogKit.showSelectionDialog(
    context = this,
    title = "选择间隔",
    iconRes = R.drawable.ic_sync,
    onFill = { content, dialog ->
        listOf("5秒", "10秒", "30秒").forEach { option ->
            val tv = TextView(this).apply { text = option; textSize = 15f }
            tv.setOnClickListener { /* 处理选择 */; dialog.dismiss() }
            content.addView(tv)
        }
    }
)
```

### 预设值按钮和输入面板

```kotlin
// 整数预设行
val (row, update) = DialogKit.createPresetRow(
    context = this,
    values = listOf(5, 10, 30, 60),
    formatLabel = { "$it秒" },
    currentValue = 10,
    onSelect = { value -> /* 处理选择 */ }
)
content.addView(row)

// 字符串预设行
val (strRow, updateStr) = DialogKit.createStringPresetRow(
    context, listOf("auto" to "自动", "off" to "关闭"), "auto"
) { id -> /* 处理 */ }

// 输入面板
val panel = DialogKit.createInputPanel(context, hint = "输入数值",
    validate = { if (it.toIntOrNull() == null) "请输入数字" else null },
    onConfirm = { value -> /* 处理输入 */ }
)
DialogKit.animatePanelVisibility(panel, show = true)
```

### PopupKit — 下拉菜单

```kotlin
PopupKit.showDropDownMenu(
    anchor = editText,
    options = arrayOf("自动", "手动", "关闭"),
    currentIndex = 0,
    onSelect = { index -> /* 处理选择 */ }
)

PopupKit.showConfirmDialog(
    context = this,
    title = "退出确认",
    message = "确定要退出吗？",
    onConfirm = { finish() }
)
```

### ToastKit — 水滴下落 Toast

```kotlin
ToastKit.showDropToast(activity, ToastStyle.INFO, "已复制")
ToastKit.showDropToast(activity, ToastStyle.SUCCESS, "保存成功")
ToastKit.showDropToast(activity, ToastStyle.WARNING, "网络失败")

// 带副标题
ToastKit.showDropToast(activity, ToastStyle.SUCCESS, "下载完成", "文件已保存", 3000L)

// 加载中（不自动消失）
ToastKit.showLoadingToast(activity, "正在加载...")
ToastKit.dismissActiveToast()  // 手动关闭

// 确认弹窗
ToastKit.showConfirmDialog(context, "流量提醒", "本月流量已超出")
ToastKit.showWarningConfirmDialog(context, "危险", "将清除所有数据") { /* 确认 */ }
```

---

## 5. AnimKit — 动画与触摸反馈

### 文本平滑更新

```kotlin
// API 31+ 原生模糊渐变，低版本 alpha+scale 回退
AnimKit.smoothUpdateText(myTextView, "新内容")
```

### 弹窗模糊动画

```kotlin
AnimKit.applyDialogBlurIn(dialog)     // 入场模糊
AnimKit.applyDialogBlurOut(dialog) { /* 退场完成回调 */ }
```

### 主题切换圆形揭露

```kotlin
AnimKit.applyCircleRevealPulse(activity, duration = 1000L) {
    // 在此闭包中执行主题切换（内容在下层静默更新，圆形揭露扩散展示）
    UfiUIKit.applyThemeToViewTree(rootViewGroup)
}
```

### Activity 过渡

```kotlin
AnimKit.applyCrossfadeEnterFromRecreate(activity)
```

### 按压缩放

```kotlin
// 简单版（0.92f 缩放）
AnimKit.applyScaleClickAnimation(button) { /* 点击处理 */ }

// 高级版 — 自定义参数（OvershootInterpolator 弹性回弹）
myCard.setOnTouchListener(AnimKit.scaleTouchListener())  // 默认参数
myCard.setOnTouchListener(AnimKit.scaleTouchListener(
    pressScale = 0.94f, tension = 1.5f
))
```

---

## 6. WidgetKit — 通用控件

### WidgetKit — 设置项组件

```kotlin
// 配置开关项（从 XML include）
WidgetKit.setupSwitchItem(
    itemView = findViewById(R.id.item_switch),
    iconRes = R.drawable.ic_notification,
    label = "开启通知",
    subtitle = "接收设备状态通知",
    initialChecked = true,
    onToggle = { checked -> /* 保存 */ }
)

// 代码构建开关行（弹窗/动态布局用）
val switchRow = WidgetKit.createSwitchRow(context, "深色模式", "跟随系统", false) { checked -> }
dialogContent.addView(switchRow)

// 配置设置项卡片
WidgetKit.setupSettingItem(view, R.drawable.ic_settings, "连接设置") { /* 打开 */ }

// 主题化 EditText
val et = WidgetKit.createThemedEditText(context, hint = "设备地址", text = "192.168.0.1")

// 将 EditText 改为下拉选择器
WidgetKit.setupDropdownOnEditText(et,
    options = arrayOf("自动", "手动"),
    values = arrayOf("auto", "manual"),
    currentValue = "auto"
)

// 分隔线
container.addView(WidgetKit.createDivider(context))

// "恢复默认"按钮
container.addView(WidgetKit.createRestoreDefaultsButton(context) { /* 重置字段 */ })
```

### PaginationKit — 翻页栏

```kotlin
val bar = PaginationKit.create(context) { action ->
    when (action) {
        is PaginationKit.Action.FIRST -> goToPage(1)
        is PaginationKit.Action.PREV  -> goToPage(current - 1)
        is PaginationKit.Action.NEXT  -> goToPage(current + 1)
        is PaginationKit.Action.LAST  -> goToPage(total)
        is PaginationKit.Action.Jump  -> goToPage(action.page)
    }
}
rootLayout.addView(bar, FrameLayout.LayoutParams(
    WRAP_CONTENT, WRAP_CONTENT, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
))

PaginationKit.update(bar, currentPage = 2, totalPages = 10)
PaginationKit.fadeVisibility(bar, totalPages > 1)
```

### CropView — 图片裁切

```xml
<com.ufiuikit.util.CropView
    android:id="@+id/crop_view"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

```kotlin
cropView.setImageBitmap(bitmap, targetW = 1080, targetH = 1920)
val cropped = cropView.getCroppedBitmap(1080, 1920)
```

---

## 7. 辅助组件

### ThemedSliderUtil — 主题滑块

```kotlin
val (slider, label) = ThemedSliderUtil.createSlider(
    context = this,
    config = ThemedSliderUtil.SliderConfig(
        minValue = 5f, maxValue = 120f,
        defaultValue = 30f, valueSuffix = " 秒"
    ),
    onValueChange = { value -> /* 处理滑动 */ }
)
content.addView(label)
content.addView(slider)
```

### 自定义日志实现

```kotlin
DebugLogger.logger = object : DebugLogger.Logger {
    override fun w(tag: String, msg: String) { Timber.w("[UIKit] $tag: $msg") }
    override fun e(tag: String, msg: String) { Timber.e("[UIKit] $tag: $msg") }
}
```

---

## 8. XML 资源清单

### 通用布局

| 文件 | 用途 |
|------|------|
| `layout_common_dialog.xml` | 通用弹窗（图标+标题+滚动内容+双按钮） |
| `layout_common_action_button.xml` | 可复用操作按钮 |
| `layout_common_input_field.xml` | 输入框（标题+副标题+EditText） |
| `layout_common_setting_item.xml` | 设置项卡片（图标+标题+箭头） |
| `layout_common_switch.xml` | 自定义开关（滑块轨道+拇指） |
| `layout_common_switch_item.xml` | 开关卡片（图标+标题+开关） |
| `layout_dialog_list_item.xml` | 弹窗列表项（选中背景+勾选图标） |

### 背景 Drawable

| 文件 | 用途 |
|------|------|
| `bg_widget_card.xml` | 圆角卡片（12dp，跟随 card_bg） |
| `bg_btn_primary.xml` | 主按钮（12dp，跟随 accent） |
| `bg_chip_selected.xml` / `bg_chip_unselected.xml` | 芯片选中/未选中 |
| `bg_common_switch_track_off.xml` | 开关轨道关闭态 |
| `bg_input_field.xml` | 输入框背景（10dp，浅灰） |
| `bg_window_scrim.xml` / `bg_window_scrim_dark.xml` | 窗口遮罩渐变 |
| `widget_bg_light.xml` / `widget_bg_dark.xml` | 小组件亮/暗色背景 |

### 主题样式

| 样式名 | 用途 |
|--------|------|
| `Theme.UFITOOLSWidget` | 基础主题（Material3 DayNight） |
| `Theme.UFITOOLSWidget.Transparent` | 透明弹窗 |
| `DialogAnimationTheme` | 弹窗弹性动画 |
| `AppButton.Primary` / `AppButton.Secondary` | 按钮样式 |
| `AppCard` | 通用卡片 |
| `AppText.Title` / `AppText.Subtitle` | 文字样式 |

---

## 9. 在别的项目中使用

### 最小集成

```kotlin
// 设置主题
UfiUIKit.setColorTheme(context, 1)

// 在 Activity 中
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    UfiUIKit.applyTheme(this)
}
```

### 弹窗 + Toast 组合

```kotlin
DialogKit.showCommonDialog(
    context = this,
    title = "刷新间隔",
    iconRes = R.drawable.ic_sync,
    onFill = { content ->
        val (slider, label) = ThemedSliderUtil.createSlider(
            this, ThemedSliderUtil.SliderConfig(5f, 60f, 1f, 15f, " 秒")
        ) { interval = it.toInt() }
        content.addView(label)
        content.addView(slider)
    },
    primaryBtnText = "保存",
    onPrimaryClick = { dialog ->
        dialog.dismiss()
        ToastKit.showDropToast(this, ToastStyle.SUCCESS, "已保存")
    }
)
```

### 主题切换完整流程

```kotlin
// 设置页保存主题
private fun saveTheme(id: Int) {
    UfiUIKit.setColorTheme(this, id)
    UfiUIKit.notifyThemeChanged(this)  // 通知所有 Activity
    UfiUIKit.applyTheme(this)          // 当前页立即刷新
}

// 其他页面接收
override fun onResume() {
    super.onResume()
    themeReceiver = ThemeNotifier.register(this, Runnable { UfiUIKit.applyTheme(this) })
}
override fun onPause() {
    super.onPause()
    ThemeNotifier.unregister(this, themeReceiver)
}
```

### 依赖清单

UI Kit 自动引入：`core-ktx`, `appcompat`, `material`, `cardview`, `lifecycle-runtime-ktx`
