package com.ufiuikit.util

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import androidx.core.view.WindowCompat

/**
 * UI Kit 内置窗口背景工具。
 * 处理 edge-to-edge 布局和窗口背景色设置。
 */
internal object BackgroundUtil {

    /** 初始化 Activity 窗口（edge-to-edge） */
    fun initActivity(activity: Activity) {
        try {
            WindowCompat.setDecorFitsSystemWindows(activity.window, false)
        } catch (_: Exception) { }
    }

    /** 异步应用窗口背景色 */
    fun applyWindowBackgroundAsync(activity: Activity) {
        try {
            val bgColor = ThemeKit.pageBg(activity)
            activity.window.setBackgroundDrawable(ColorDrawable(bgColor))
        } catch (_: Exception) { }
    }
}
