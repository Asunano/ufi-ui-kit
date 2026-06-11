package com.ufiuikit

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import com.ufiuikit.util.ThemeKit
import com.ufiuikit.util.ThemeApplier
import com.ufiuikit.util.ThemeNotifier
import com.ufiuikit.util.SPUtil
import com.ufiuikit.util.BackgroundUtil

/**
 * UFI UI Kit 公开入口。
 *
 * 提供快捷方法获取配色、应用主题、管理主题设置等。
 *
 * 用法示例：
 * ```kotlin
 * // 获取当前主题色
 * val accent = UfiUIKit.accent(context)
 * val cardBg = UfiUIKit.cardBg(context)
 *
 * // 对整个视图树应用主题着色
 * val root = findViewById<ViewGroup>(android.R.id.content)
 * UfiUIKit.applyThemeToViewTree(root)
 *
 * // 设置主题
 * UfiUIKit.setColorTheme(context, 1)  // 科技蓝
 * UfiUIKit.setAppTheme(context, "system")
 * ```
 */
object UfiUIKit {

    // ── 配色快捷方法 ──
    fun accent(ctx: Context): Int = ThemeKit.accent(ctx)
    fun cardBg(ctx: Context): Int = ThemeKit.cardBg(ctx)
    fun pageBg(ctx: Context): Int = ThemeKit.pageBg(ctx)
    fun textPrimary(ctx: Context): Int = ThemeKit.textPrimary(ctx)
    fun textSecondary(ctx: Context): Int = ThemeKit.textSecondary(ctx)
    fun btnBg(ctx: Context): Int = ThemeKit.btnBg(ctx)
    fun iconTint(ctx: Context): Int = ThemeKit.iconTint(ctx)
    fun divider(ctx: Context): Int = ThemeKit.divider(ctx)
    fun dataHighlight(ctx: Context): Int = ThemeKit.dataHighlight(ctx)
    fun isDark(ctx: Context): Boolean = ThemeKit.isDark(ctx)

    // ── 主题应用 ──

    /**
     * 对 Activity 的视图树递归应用主题着色。
     * 自动处理：窗口背景 + 递归着色 TextView、ImageView、MaterialButton、卡片背景。
     *
     * 在 setContentView 之后调用。
     */
    fun applyTheme(activity: Activity) {
        try {
            BackgroundUtil.initActivity(activity)
            BackgroundUtil.applyWindowBackgroundAsync(activity)
            activity.window.decorView.post {
                if (activity.isFinishing || activity.isDestroyed) return@post
                val root = activity.findViewById<ViewGroup>(android.R.id.content)
                ThemeApplier.applyThemeToViewTree(root)
            }
        } catch (_: Exception) { }
    }

    /**
     * 对指定 ViewGroup 递归应用主题着色（同步版本）。
     * 适用于弹窗内容区、动态添加的视图等局部刷新场景。
     */
    fun applyThemeToViewTree(root: ViewGroup?) {
        ThemeApplier.applyThemeToViewTree(root)
    }

    // ── 主题管理 ──

    /** 设置主题 ID（0=默认, 1=科技蓝, 2=薄荷绿, 3=梦幻紫, 4=活力橙） */
    fun setColorTheme(ctx: Context, themeId: Int) = SPUtil.setColorTheme(ctx, themeId)

    /** 设置应用明暗模式（"light" / "dark" / "system"） */
    fun setAppTheme(ctx: Context, mode: String) = SPUtil.setAppTheme(ctx, mode)

    /** 获取当前主题 Palette */
    fun currentPalette(ctx: Context): ThemeKit.Palette = ThemeKit.current(ctx)

    /** 获取所有预设主题 */
    fun allPalettes(): List<ThemeKit.Palette> = ThemeKit.ALL

    /** 通知所有 Activity 主题已变更 */
    fun notifyThemeChanged(ctx: Context) = ThemeNotifier.notifyThemeChanged(ctx)
}
