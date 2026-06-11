# UFI UI Kit

**[English](README.md)**

一个轻量级 Android UI 组件库，提取自 [UFI-TOOLS Widget](https://github.com/Asunano/UFITOOLSWidget)，提供统一的主题系统、对话框框架、动画工具集和通用控件。

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-100%25-blue?logo=kotlin" alt="Kotlin">
  <img src="https://img.shields.io/badge/Min%20SDK-26%20(Android%208.0)-green" alt="Min SDK">
  <img src="https://img.shields.io/badge/Target%20SDK-34-orange" alt="Target SDK">
  <img src="https://img.shields.io/badge/License-MIT-yellow" alt="License">
  <img src="https://img.shields.io/badge/Version-0.1-lightgrey" alt="Version">
</p>

---

## 功能特性

### ThemeKit — 5 套预设主题 + Material You 动态取色

完整的主题系统，支持亮色/暗色模式。内置 5 套精心调配的色彩方案，支持自定义颜色，并可在 Android 12+ 设备上通过 Material You 从壁纸自动提取主题色。

| 默认 | 科技蓝 | 薄荷绿 | 梦幻紫 | 活力橙 |
|------|--------|--------|--------|--------|
| `#222222` | `#1677FF` | `#34C799` | `#7B61FF` | `#FF7D34` |

```kotlin
// 获取当前主题颜色
val accent = ThemeKit.accent(context)
val cardBg = ThemeKit.cardBg(context)

// 切换主题
UfiUIKit.setColorTheme(context, 1)  // 科技蓝
UfiUIKit.setAppTheme(context, "dark")

// 将主题应用到 Activity
UfiUIKit.applyTheme(activity)
```

### DialogKit — 统一对话框框架

统一的对话框样式，自带退场动画、背景模糊（API 31+）和智能高度适配。包含标准对话框、警告确认框、选择列表、预设标签和输入面板。

```kotlin
DialogKit.showCommonDialog(
    context = this,
    title = "设置",
    iconRes = R.drawable.ic_settings,
    onFill = { content -> /* 添加自定义视图 */ },
    primaryBtnText = "保存",
    onPrimaryClick = { dialog -> dialog.dismiss() }
)

DialogKit.showWarningConfirmDialog(context, "删除", "此操作不可撤销") {
    // 确认操作
}
```

### AnimKit — 动画引擎

流畅的文字更新配合模糊过渡（API 31+ 使用 RenderEffect）、对话框模糊进出、主题切换时的圆形揭示动画、交叉淡入淡出以及按压缩放触摸反馈。

```kotlin
AnimKit.smoothUpdateText(textView, "新内容")

AnimKit.applyCircleRevealPulse(activity) {
    UfiUIKit.applyThemeToViewTree(root)
}

button.setOnTouchListener(AnimKit.scaleTouchListener())
```

### ToastKit — 水滴动画 Toast

优雅的下落动画 Toast，支持三种样式（信息/成功/警告），以及加载指示器和确认对话框。

```kotlin
ToastKit.showDropToast(activity, ToastStyle.SUCCESS, "保存成功")
ToastKit.showLoadingToast(activity, "加载中...")
```

### WidgetKit — 通用控件

设置项、自定义开关、主题化输入框、下拉菜单、分页栏和图片裁剪视图 —— 全部遵循统一的设计语言。

```kotlin
WidgetKit.setupSwitchItem(view, iconRes, "通知", "接收提醒", true) { checked -> }

val bar = PaginationKit.create(context) { action -> /* 处理分页 */ }
PaginationKit.update(bar, currentPage = 2, totalPages = 10)
```

---

## 项目结构

```
ufi-ui-kit/
├── src/main/java/com/ufiuikit/
│   ├── UfiUIKit.kt          # 公开入口
│   ├── util/
│   │   ├── ThemeKit.kt      # 主题系统（ThemeKit + ThemeApplier + ThemeNotifier）
│   │   ├── DialogKit.kt     # 对话框（DialogKit + PopupKit + ToastKit）
│   │   ├── AnimKit.kt       # 动画 + 触摸反馈
│   │   ├── WidgetKit.kt     # 控件（WidgetKit + PaginationKit + CropView）
│   │   ├── ThemedSliderUtil.kt
│   │   ├── SPUtil.kt        # [内部] SharedPreferences
│   │   ├── DebugLogger.kt   # [内部] 日志
│   │   └── BackgroundUtil.kt # [内部] 窗口背景
│   └── view/
│       ├── ThemeSlider.kt
│       └── LoadingAnimationView.kt
└── src/main/res/
    ├── layout/    （7 个可复用布局）
    ├── drawable/  （38 个背景/形状/图标）
    ├── anim/      （3 个动画）
    └── values/    （颜色、主题、字符串）
```

**4 个核心 Kit 类：**

| Kit | 职责 | 包含 |
|-----|------|------|
| `ThemeKit` | 主题 + 配色 + 通知 | ThemeKit, ThemeApplier, ThemeNotifier |
| `DialogKit` | 对话框 + 菜单 + Toast | DialogKit, PopupKit, ToastKit |
| `AnimKit` | 动画 + 触摸反馈 | AnimKit |
| `WidgetKit` | 设置项 + 分页 + 裁剪 | WidgetKit, PaginationKit, CropView |

---

## 快速开始

### 1. 作为模块引入

将 `ufi-ui-kit` 目录复制到你的项目根目录。

**settings.gradle.kts：**
```kotlin
include(":ufi-ui-kit")
```

**app/build.gradle.kts：**
```kotlin
dependencies {
    implementation(project(":ufi-ui-kit"))
}
```

确保根目录的 `build.gradle.kts` 已声明 Android Library 插件：
```kotlin
plugins {
    alias(libs.plugins.android.library) apply false
}
```

### 2. 使用

```kotlin
import com.ufiuikit.UfiUIKit
import com.ufiuikit.util.ThemeKit
import com.ufiuikit.util.DialogKit
import com.ufiuikit.util.ToastKit

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 应用主题
        UfiUIKit.applyTheme(this)

        // 显示 Toast
        ToastKit.showDropToast(this, ToastStyle.INFO, "Hello UFI UI Kit!")
    }
}
```

---

## 环境要求

- **最低 SDK：** 26（Android 8.0）
- **目标 SDK：** 34
- **Kotlin：** 2.0.21+
- **Java：** 21

### 依赖（自动引入）

- `androidx.core:core-ktx`
- `androidx.appcompat:appcompat`
- `com.google.android.material:material`
- `androidx.cardview:cardview`
- `androidx.lifecycle:lifecycle-runtime-ktx`

---

## 构建 AAR

```bash
./gradlew :ufi-ui-kit:assembleRelease
```

输出路径：`ufi-ui-kit/build/outputs/aar/ufi-ui-kit-release.aar`

在其他项目中使用 AAR，将其复制到 `libs/` 目录并添加：
```kotlin
dependencies {
    implementation(files("libs/ufi-ui-kit-release.aar"))
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.cardview:cardview:1.0.0")
}
```

---

## 详细文档

完整的 API 文档和使用示例请参阅 [docs/API.md](docs/API.md)。

---

## 许可证

本项目基于 MIT 许可证开源 —— 详见 [LICENSE](LICENSE) 文件。

---

## 关联项目

- [UFI-TOOLS Widget](https://github.com/Asunano/UFITOOLSWidget) —— 本库的源项目，用于监控 UFI 随身 WiFi 设备的桌面小组件。
