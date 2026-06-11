package com.ufiuikit.util

import android.content.Context
import android.content.SharedPreferences

/**
 * UI Kit 内置 SharedPreferences 工具。
 *
 * 管理主题配色、暗色模式、动态配色等设置。
 * 宿主项目可通过 [ThemeConfig] 扩展自定义存储逻辑。
 */
internal object SPUtil {

    private const val SP_NAME = "ufi_ui_kit_prefs"

    fun getSp(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)

    // ── 主题配色 ──
    fun getColorTheme(ctx: Context): Int = getSp(ctx).getInt("color_theme", 0)

    fun setColorTheme(ctx: Context, id: Int) =
        getSp(ctx).edit().putInt("color_theme", id).apply()

    fun getAppTheme(ctx: Context): String =
        getSp(ctx).getString("app_theme", "system") ?: "system"

    fun setAppTheme(ctx: Context, mode: String) =
        getSp(ctx).edit().putString("app_theme", mode).apply()

    // ── 动态配色 (Material You) ──
    fun getWidgetDynamicColor(ctx: Context): Boolean =
        getSp(ctx).getBoolean("widget_dynamic_color", false)

    fun getWidgetBgImageUri(ctx: Context): String =
        getSp(ctx).getString("widget_bg_image_uri", "") ?: ""

    fun getWidgetDynamicColorSource(ctx: Context): Int =
        getSp(ctx).getInt("widget_dynamic_color_source", 0)

    fun getWidgetDynamicContrast(ctx: Context): Int =
        getSp(ctx).getInt("widget_dynamic_contrast", 1)

    fun getWidgetDynamicAdvanced(ctx: Context): Boolean =
        getSp(ctx).getBoolean("widget_dynamic_advanced", false)

    fun getDynAdvSatBoost(ctx: Context): Int =
        getSp(ctx).getInt("dyn_adv_sat_boost", 100)

    fun getDynAdvLightBg(ctx: Context): Int =
        getSp(ctx).getInt("dyn_adv_light_bg", 97)

    fun getDynAdvLightTxt(ctx: Context): Int =
        getSp(ctx).getInt("dyn_adv_light_txt", 12)

    fun getDynAdvDarkBg(ctx: Context): Int =
        getSp(ctx).getInt("dyn_adv_dark_bg", 8)

    fun getDynAdvDarkTxt(ctx: Context): Int =
        getSp(ctx).getInt("dyn_adv_dark_txt", 90)

    // ── 自定义配色 ──
    fun getCustomAccentLight(ctx: Context): Int =
        getSp(ctx).getInt("custom_accent_light", 0xFF222222.toInt())

    fun getCustomAccentDark(ctx: Context): Int =
        getSp(ctx).getInt("custom_accent_dark", 0xFFCCCCCC.toInt())
}
