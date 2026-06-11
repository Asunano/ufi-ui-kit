# UFI UI Kit

**[中文文档](README_zh-CN.md)**

A lightweight Android UI component library extracted from [UFI-TOOLS Widget](https://github.com/Asunano/UFITOOLSWidget), providing a unified theming system, dialog framework, animation utilities, and common widgets.

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-100%25-blue?logo=kotlin" alt="Kotlin">
  <img src="https://img.shields.io/badge/Min%20SDK-26%20(Android%208.0)-green" alt="Min SDK">
  <img src="https://img.shields.io/badge/Target%20SDK-34-orange" alt="Target SDK">
  <img src="https://img.shields.io/badge/License-MIT-yellow" alt="License">
  <img src="https://img.shields.io/badge/Version-0.1-lightgrey" alt="Version">
</p>

---

## Features

### ThemeKit — 5 Preset Themes + Material You Dynamic Colors

A complete theming system with light/dark mode support. Includes 5 carefully crafted color palettes, custom color support, and automatic color extraction from wallpapers using Material You (Android 12+).

| Default | Tech Blue | Mint Green | Dream Purple | Vibrant Orange |
|---------|-----------|------------|--------------|----------------|
| `#222222` | `#1677FF` | `#34C799` | `#7B61FF` | `#FF7D34` |

```kotlin
// Get current theme colors
val accent = ThemeKit.accent(context)
val cardBg = ThemeKit.cardBg(context)

// Switch theme
UfiUIKit.setColorTheme(context, 1)  // Tech Blue
UfiUIKit.setAppTheme(context, "dark")

// Apply theme to Activity
UfiUIKit.applyTheme(activity)
```

### DialogKit — Unified Dialog Framework

Consistent dialog styling with automatic dismiss animations, background blur (API 31+), and smart height adaptation. Includes standard dialogs, warning dialogs, selection lists, preset chips, and input panels.

```kotlin
DialogKit.showCommonDialog(
    context = this,
    title = "Settings",
    iconRes = R.drawable.ic_settings,
    onFill = { content -> /* add your views */ },
    primaryBtnText = "Save",
    onPrimaryClick = { dialog -> dialog.dismiss() }
)

DialogKit.showWarningConfirmDialog(context, "Delete", "This cannot be undone") {
    // confirm action
}
```

### AnimKit — Animation Engine

Smooth text updates with blur transitions (RenderEffect on API 31+), dialog blur in/out, circular reveal for theme switching, crossfade transitions, and press-scale touch feedback.

```kotlin
AnimKit.smoothUpdateText(textView, "New value")

AnimKit.applyCircleRevealPulse(activity) {
    UfiUIKit.applyThemeToViewTree(root)
}

button.setOnTouchListener(AnimKit.scaleTouchListener())
```

### ToastKit — Water-Drop Toast

Elegant drop-animation toasts with three styles (info/success/warning), loading indicators, and confirmation dialogs.

```kotlin
ToastKit.showDropToast(activity, ToastStyle.SUCCESS, "Saved successfully")
ToastKit.showLoadingToast(activity, "Loading...")
```

### WidgetKit — Common Controls

Settings items, custom switches, themed inputs, dropdowns, pagination bars, and image crop views — all following the same design language.

```kotlin
WidgetKit.setupSwitchItem(view, iconRes, "Notifications", "Receive alerts", true) { checked -> }

val bar = PaginationKit.create(context) { action -> /* handle pagination */ }
PaginationKit.update(bar, currentPage = 2, totalPages = 10)
```

---

## Architecture

```
ufi-ui-kit/
├── src/main/java/com/ufiuikit/
│   ├── UfiUIKit.kt          # Public entry point
│   ├── util/
│   │   ├── ThemeKit.kt      # Theme system (ThemeKit + ThemeApplier + ThemeNotifier)
│   │   ├── DialogKit.kt     # Dialogs (DialogKit + PopupKit + ToastKit)
│   │   ├── AnimKit.kt       # Animations + touch feedback
│   │   ├── WidgetKit.kt     # Widgets (WidgetKit + PaginationKit + CropView)
│   │   ├── ThemedSliderUtil.kt
│   │   ├── SPUtil.kt        # [internal] SharedPreferences
│   │   ├── DebugLogger.kt   # [internal] Logging
│   │   └── BackgroundUtil.kt # [internal] Window background
│   └── view/
│       ├── ThemeSlider.kt
│       └── LoadingAnimationView.kt
└── src/main/res/
    ├── layout/    (7 reusable layouts)
    ├── drawable/  (38 backgrounds/shapes/icons)
    ├── anim/      (3 animations)
    └── values/    (colors, themes, strings)
```

**4 core Kit classes:**

| Kit | Responsibility | Contains |
|-----|----------------|----------|
| `ThemeKit` | Theming + coloring + notifications | ThemeKit, ThemeApplier, ThemeNotifier |
| `DialogKit` | Dialogs + menus + toasts | DialogKit, PopupKit, ToastKit |
| `AnimKit` | Animations + touch feedback | AnimKit |
| `WidgetKit` | Settings items + pagination + crop | WidgetKit, PaginationKit, CropView |

---

## Quick Start

### 1. Add as a module

Copy the `ufi-ui-kit` directory into your project root.

**settings.gradle.kts:**
```kotlin
include(":ufi-ui-kit")
```

**app/build.gradle.kts:**
```kotlin
dependencies {
    implementation(project(":ufi-ui-kit"))
}
```

Ensure your root `build.gradle.kts` has the Android Library plugin:
```kotlin
plugins {
    alias(libs.plugins.android.library) apply false
}
```

### 2. Use it

```kotlin
import com.ufiuikit.UfiUIKit
import com.ufiuikit.util.ThemeKit
import com.ufiuikit.util.DialogKit
import com.ufiuikit.util.ToastKit

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Apply theme
        UfiUIKit.applyTheme(this)

        // Show a toast
        ToastKit.showDropToast(this, ToastStyle.INFO, "Hello UFI UI Kit!")
    }
}
```

---

## Requirements

- **Min SDK:** 26 (Android 8.0)
- **Target SDK:** 34
- **Kotlin:** 2.0.21+
- **Java:** 21

### Dependencies (automatically included)

- `androidx.core:core-ktx`
- `androidx.appcompat:appcompat`
- `com.google.android.material:material`
- `androidx.cardview:cardview`
- `androidx.lifecycle:lifecycle-runtime-ktx`

---

## Building the AAR

```bash
./gradlew :ufi-ui-kit:assembleRelease
```

Output: `ufi-ui-kit/build/outputs/aar/ufi-ui-kit-release.aar`

To use the AAR in another project, copy it to `libs/` and add:
```kotlin
dependencies {
    implementation(files("libs/ufi-ui-kit-release.aar"))
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.cardview:cardview:1.0.0")
}
```

---

## Documentation

See [docs/API.md](docs/API.md) for detailed API documentation and usage examples.

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## Related Projects

- [UFI-TOOLS Widget](https://github.com/Asunano/UFITOOLSWidget) — The original project this library was extracted from. A desktop widget for monitoring UFI portable WiFi devices.
