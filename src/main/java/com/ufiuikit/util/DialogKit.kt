package com.ufiuikit.util

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.ufiuikit.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ══════════════════════════════════════════════════════════════
// DialogKit — 统一弹窗工具（原 CommonDialogHelper）
// ══════════════════════════════════════════════════════════════

/**
 * 统一弹窗工具类 — 从 MainActivity 抽离。
 *
 * 提供：
 * - 弹窗创建（透明背景、模糊、动画）
 * - 弹窗窗口设置（背景、模糊、高度适配）
 * - 弹窗主题着色（根布局 + 递归视图树）
 * - 通用弹窗组装与显示
 */
object DialogKit {

    private const val TAG = "DialogKit"

    // ── 弹窗创建 ──

    /**
     * 创建带自动退场动画的 Dialog（全局统一）。
     *
     * 重写 [Dialog.dismiss]：
     * - API 31+：先清理模糊标志 → 执行 [AnimKit.applyDialogBlurOut] 渐退动画 → 回调 [onDismissed] → super.dismiss()
     * - API <31：清理模糊标志 → 直接 super.dismiss() → 回调 [onDismissed]
     *
     * 调用方无需手动管理动画状态或调用 [AnimKit.applyDialogBlurOut]。
     *
     * @param context 上下文
     * @param onDismissed 弹窗完全关闭后的回调（可选），用于重置调用方的引用/状态
     */
    fun createAnimatedDialog(context: Context, onDismissed: () -> Unit = {}): Dialog {
        val dialog = object : Dialog(context, R.style.Theme_UFITOOLSWidget_Transparent) {
            @Volatile private var isAnimatingOut = false

            private fun realDismiss() {
                isAnimatingOut = false
                super.dismiss()
                onDismissed()
            }

            override fun dismiss() {
                if (isAnimatingOut) return  // 防止重入
                val win = window
                if (win == null) {
                    realDismiss()
                    return
                }
                isAnimatingOut = true
                // 全版本统一走 applyDialogBlurOut 退场动画：
                // - API 31+：缩放淡出 + 模糊/遮罩同步消退
                // - API <31：缩放淡出（内部 setWindowAnimations(0) 覆盖 XML 动画）
                AnimKit.applyDialogBlurOut(this) { realDismiss() }
            }
        }
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    /**
     * 一次性完成弹窗窗口的标准化设置：
     * 透明背景、低遮罩、缩放动画、模糊背景（API 31+）、高度适配。
     */
    fun setupDialogWindow(context: Context, dialog: Dialog, widthRatio: Float = 0.88f) {
        dialog.setCanceledOnTouchOutside(true)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setDimAmount(0.08f)
            setWindowAnimations(R.style.DialogAnimationTheme)
        }
        applyDialogBlur(context, dialog)
        PopupKit.autoAdjustDialogHeight(context, dialog, widthRatio)
    }

    // ── 主题着色 ──

    /**
     * 为弹窗根布局应用主题背景 + 描边，并递归着色全部子视图。
     */
    fun applyThemeToDialogRoot(context: Context, dialog: Dialog) {
        val root = dialog.findViewById<ViewGroup>(android.R.id.content)
            ?.let { if (it.childCount > 0) it.getChildAt(0) as? ViewGroup else it } ?: return
        val cardBg = ThemeKit.cardBg(context)
        val textPrimary = ThemeKit.textPrimary(context)

        root.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(cardBg)
            cornerRadius = dp2px(context, 16).toFloat()
            val borderColor = if (ThemeKit.isDark(context))
                (ThemeKit.textSecondary(context) and 0x00FFFFFF) or 0x60000000 else 0x35000000
            setStroke(dp2px(context, 2), borderColor)
        }
        root.elevation = 24f

        // 标题 + 图标着色
        dialog.findViewById<TextView>(R.id.common_dialog_title)?.setTextColor(textPrimary)
        dialog.findViewById<ImageView>(R.id.common_dialog_icon)?.setColorFilter(ThemeKit.iconTint(context))

        // 递归着色视图树
        applyThemeToViewTree(root, context)
    }

    /**
     * 递归为弹窗视图树着色：MaterialButton、Button、TextView、ImageView。
     * 跳过 common_dialog_content 动态容器（由调用方自行填充）。
     */
    fun applyThemeToViewTree(view: View?, context: Context) {
        if (view == null) return
        val textPrimary = ThemeKit.textPrimary(context)
        val textSecondary = ThemeKit.textSecondary(context)
        val iconTint = ThemeKit.iconTint(context)
        val btnBg = ThemeKit.btnBg(context)

        if (view is ViewGroup && view.id != R.id.common_dialog_content) {
            for (i in 0 until view.childCount) {
                applyThemeToViewTree(view.getChildAt(i), context)
            }
        }
        when (view) {
            is MaterialButton -> {
                if ((view.strokeWidth ?: 0) > 0) {
                    // 描边按钮（次要操作）
                    view.setTextColor(textPrimary)
                    view.strokeColor = ColorStateList.valueOf(textSecondary)
                    view.iconTint = ColorStateList.valueOf(iconTint)
                } else {
                    // 实色按钮（主要操作）
                    view.backgroundTintList = ColorStateList.valueOf(btnBg)
                    view.setTextColor(0xFFFFFFFF.toInt())
                    view.iconTint = ColorStateList.valueOf(0xFFFFFFFF.toInt())
                }
                view.textSize = 14f
                view.insetTop = 0
                view.insetBottom = 0
            }
            is Button -> {
                view.backgroundTintList = ColorStateList.valueOf(btnBg)
                view.setTextColor(0xFFFFFFFF.toInt())
            }
            is TextView -> {
                if (view.id == R.id.common_dialog_btn_primary) return
                // switch label / 标题类文字统一取主色（粗体由 XML style 或代码设置）
                if (view.id == R.id.common_switch_label || view.id == R.id.common_item_title) {
                    view.setTextColor(textPrimary)
                    return
                }
                if (view.textSize <= 13f) view.setTextColor(textSecondary)
                else view.setTextColor(textPrimary)
            }
            is ImageView -> {
                view.setColorFilter(iconTint)
            }
        }
    }

    // ── 背景模糊 ──

    /**
     * 应用背景模糊：API 31+ 原生模糊，API 26-30 bitmap 缩放模糊。
     */
    fun applyDialogBlur(context: Context, dialog: Dialog) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AnimKit.applyDialogBlurIn(dialog)
        } else {
            applyLegacyBlur(context, dialog)
        }
    }

    /** API 26-30：截屏 + 多级缩放模拟毛玻璃效果（异步版本，避免主线程阻塞） */
    private fun applyLegacyBlur(context: Context, dialog: Dialog) {
        try {
            val decorView = dialog.window?.decorView?.rootView ?: return
            val vw = decorView.width
            val vh = decorView.height
            if (vw <= 0 || vh <= 0) return

            // 步骤 1-2（必须主线程）：截取 decorView 当前画面
            val capture = Bitmap.createBitmap(vw, vh, Bitmap.Config.ARGB_8888)
            decorView.draw(Canvas(capture))

            val smallW = (vw * 0.06f).toInt().coerceAtLeast(4)
            val smallH = (vh * 0.06f).toInt().coerceAtLeast(4)
            val windowRef = java.lang.ref.WeakReference(dialog.window)
            val res = context.resources

            // 步骤 3-5（CPU 密集）：缩放模糊在后台线程完成，结果回主线程设置
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    val small = Bitmap.createScaledBitmap(capture, smallW, smallH, true)
                    capture.recycle()
                    val blurred = Bitmap.createScaledBitmap(small, vw, vh, true)
                    small.recycle()
                    withContext(Dispatchers.Main) {
                        windowRef.get()?.setBackgroundDrawable(BitmapDrawable(res, blurred))
                    }
                } catch (e: Exception) {
                    capture.recycle()
                    Log.w(TAG, "Legacy blur async failed: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Legacy blur failed: ${e.message}")
            DebugLogger.w(TAG, "Legacy blur failed: ${e.message}")
        }
    }

    // ── 通用弹窗组装 ──

    /**
     * 配置并显示通用弹窗（使用已创建的 Dialog）。
     * 适合调用方自行管理 Dialog 生命周期（防抖/复用）的场景。
     */
    fun configureAndShow(
        context: Context,
        dialog: Dialog,
        title: String,
        iconRes: Int,
        onFill: (LinearLayout) -> Unit,
        primaryBtnText: String = "关闭",
        onPrimaryClick: ((Dialog) -> Unit)? = null,
        secondaryBtnText: String? = null,
        onSecondaryClick: ((Dialog) -> Unit)? = null,
        widthRatio: Float = 0.88f
    ) {
        dialog.findViewById<TextView>(R.id.common_dialog_title).text = title
        dialog.findViewById<ImageView>(R.id.common_dialog_icon).setImageResource(iconRes)

        val content = dialog.findViewById<LinearLayout>(R.id.common_dialog_content)
        content.removeAllViews()
        onFill(content)

        val btnPrimary = dialog.findViewById<MaterialButton>(R.id.common_dialog_btn_primary)
        btnPrimary.text = primaryBtnText
        AnimKit.applyScaleClickAnimation(btnPrimary) {
            if (onPrimaryClick != null) {
                onPrimaryClick(dialog)
            } else {
                dialog.dismiss()
            }
        }

        val btnSecondary = dialog.findViewById<MaterialButton>(R.id.common_dialog_btn_secondary)
        if (secondaryBtnText != null) {
            btnSecondary.visibility = View.VISIBLE
            btnSecondary.text = secondaryBtnText
            AnimKit.applyScaleClickAnimation(btnSecondary) {
                onSecondaryClick?.invoke(dialog) ?: dialog.dismiss()
            }
            (btnPrimary.layoutParams as? LinearLayout.LayoutParams)?.marginStart =
                dp2px(context, 12)
        } else {
            btnSecondary.visibility = View.GONE
            (btnPrimary.layoutParams as? LinearLayout.LayoutParams)?.marginStart = 0
        }

        setupDialogWindow(context, dialog, widthRatio)
        dialog.show()
    }

    // ── 通用弹窗一步到位 ──

    /**
     * 一步到位：创建弹窗（带动画退场）→ 主题着色 → 填充内容 → 配置按钮 → 显示。
     *
     * 适用于不需要持有 Dialog 引用的场景。如需自己管理生命周期，用 createDialog + configureAndShow。
     *
     * @return 已显示的 [Dialog] 实例
     */
    fun showCommonDialog(
        context: Context,
        title: String,
        iconRes: Int,
        onFill: (LinearLayout) -> Unit,
        primaryBtnText: String = "关闭",
        onPrimaryClick: ((Dialog) -> Unit)? = null,
        secondaryBtnText: String? = null,
        onSecondaryClick: ((Dialog) -> Unit)? = null,
        widthRatio: Float = 0.88f
    ): Dialog {
        // 防御性检查：Activity 已销毁或正在销毁时不显示弹窗，避免 BadTokenException
        val activity = context as? android.app.Activity
        if (activity != null && (activity.isFinishing || activity.isDestroyed)) {
            return createAnimatedDialog(context) // 返回一个不会 show 的空 Dialog
        }
        val dialog = createAnimatedDialog(context)
        dialog.setContentView(R.layout.layout_common_dialog)
        applyThemeToDialogRoot(context, dialog)
        configureAndShow(context, dialog, title, iconRes, onFill,
            primaryBtnText, onPrimaryClick, secondaryBtnText, onSecondaryClick, widthRatio)
        return dialog
    }

    // ── 选择列表弹窗（无按钮，点击选项即触发） ──

    /**
     * 显示选择列表弹窗（无按钮，点击选项即触发回调并关闭）。
     *
     * 适用于"从列表中选择一项"的场景，如对比度选择、色源选择。
     * 自动创建 AnimatedDialog、设置通用布局、应用主题、配置窗口并显示。
     *
     * @param context  上下文
     * @param title    弹窗标题
     * @param iconRes  图标资源
     * @param onFill   填充内容到 content LinearLayout，接收 (content, dialog) 便于添加点击关闭事件
     * @param widthRatio 弹窗宽度比，默认 0.88
     * @return 已显示的 [Dialog] 实例
     */
    fun showSelectionDialog(
        context: Context,
        title: String,
        iconRes: Int,
        onFill: (LinearLayout, Dialog) -> Unit,
        widthRatio: Float = 0.88f
    ): Dialog {
        val dialog = createAnimatedDialog(context)
        dialog.setContentView(R.layout.layout_common_dialog)

        dialog.findViewById<TextView>(R.id.common_dialog_title)?.text = title
        dialog.findViewById<ImageView>(R.id.common_dialog_icon)?.setImageResource(iconRes)
        dialog.findViewById<View>(R.id.common_dialog_button_container)?.visibility = View.GONE

        applyThemeToDialogRoot(context, dialog)

        val content = dialog.findViewById<LinearLayout>(R.id.common_dialog_content)
        onFill(content, dialog)

        setupDialogWindow(context, dialog, widthRatio)
        dialog.show()
        return dialog
    }

    // ── 背景 Drawable 工厂（选项列表项用） ──

    /**
     * 创建选中态选项背景（实色填充 + 圆角）。
     * @param accentColor 强调色（选中态背景色）
     * @param cornerRadius 圆角半径（px）
     */
    fun createSelectedBg(accentColor: Int, cornerRadius: Float): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(accentColor)
            this.cornerRadius = cornerRadius
        }
    }

    /**
     * 创建未选中态选项背景（cardBg 底色 + 描边 + 圆角）。
     * @param context 上下文
     * @param cornerRadius 圆角半径（px）
     */
    fun createUnselectedBg(context: Context, cornerRadius: Float): GradientDrawable {
        val cardBg = ThemeKit.cardBg(context)
        val isDark = ThemeKit.isDark(context)
        val borderColor = if (isDark) 0x30FFFFFF.toInt() else 0x20000000
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(cardBg)
            this.cornerRadius = cornerRadius
            setStroke(dp2px(context, 1), borderColor)
        }
    }

    // ── 红色警告弹窗 ──

    private const val WARN_COLOR_LIGHT = 0xFFE53935.toInt()  // 浅色模式红
    private const val WARN_COLOR_DARK  = 0xFFEF5350.toInt()  // 深色模式红（稍亮，提高对比度）

    /**
     * 显示红色警告确认弹窗 —— 专属样式，醒目警示用户。
     *
     * 视觉特征：
     * - 基于 cardBg 的淡红底色 + 红色半透描边
     * - 标题/图标/分隔线均为红色（自动适配明暗模式）
     * - 主按钮红色实色 + 白字白图标，次按钮红色描边
     * - Dim 遮罩 0.15f（比普通弹窗更暗）
     *
     * @return 已显示的 [Dialog] 实例，调用方可持有以控制生命周期
     */
    fun showWarningConfirmDialog(
        context: Context,
        title: String,
        message: String,
        confirmText: String = "确定",
        cancelText: String = "取消",
        onConfirm: () -> Unit
    ): Dialog {
        val dialog = createAnimatedDialog(context)
        dialog.setContentView(R.layout.layout_common_dialog)

        val isDark = ThemeKit.isDark(context)
        val warnColor = if (isDark) WARN_COLOR_DARK else WARN_COLOR_LIGHT
        val textPrimary = ThemeKit.textPrimary(context)
        val cardBg = ThemeKit.cardBg(context)
        val density = context.resources.displayMetrics.density

        // 淡红底色：cardBg 上覆盖 8%(浅色) / 12%(深色) 不透明度的红色
        val warnBgAlpha = if (isDark) 0x1F else 0x14  // 深色 12% / 浅色 8%
        val warnBg = blendColorOn(cardBg, warnColor, warnBgAlpha)
        // 描边：红色 35%(浅色) / 50%(深色) 不透明度
        val warnBorderAlpha = if (isDark) 0x80 else 0x59  // 深色 50% / 浅色 35%
        val warnBorder = (warnColor and 0x00FFFFFF) or (warnBorderAlpha shl 24)
        // 分隔线：红色 18%(浅色) / 25%(深色) 不透明度
        val warnDividerAlpha = if (isDark) 0x40 else 0x2E
        val warnDivider = (warnColor and 0x00FFFFFF) or (warnDividerAlpha shl 24)

        // ── 1. 根布局：红色描边 + 淡红底色 ──
        val root = dialog.findViewById<ViewGroup>(android.R.id.content)
            ?.let { if (it.childCount > 0) it.getChildAt(0) as? ViewGroup else it }
        root?.apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(warnBg)
                cornerRadius = 16f * density
                setStroke((2 * density).toInt(), warnBorder)
            }
            elevation = 24f
        }

        // ── 2. 图标 + 标题：红色 ──
        dialog.findViewById<ImageView>(R.id.common_dialog_icon).apply {
            setImageResource(android.R.drawable.ic_dialog_alert)
            setColorFilter(warnColor)
        }
        dialog.findViewById<TextView>(R.id.common_dialog_title).apply {
            text = title
            setTextColor(warnColor)
        }

        // ── 3. 警告消息 ──
        dialog.findViewById<LinearLayout>(R.id.common_dialog_content).apply {
            addView(TextView(context).apply {
                text = message
                textSize = 15f
                setTextColor(textPrimary)
                setLineSpacing(0f, 1.3f)
                gravity = Gravity.CENTER
            })
        }

        // ── 4. 底部红色分隔线 ──
        dialog.findViewById<View>(R.id.common_dialog_button_divider)?.apply {
            setBackgroundColor(warnDivider)
        }

        // ── 5+6. 按钮（dismiss 由 createAnimatedDialog 自动处理模糊清理+退场动画）──
        dialog.findViewById<MaterialButton>(R.id.common_dialog_btn_primary).apply {
            text = confirmText
            backgroundTintList = ColorStateList.valueOf(warnColor)
            setTextColor(Color.WHITE)
            setOnClickListener {
                dialog.dismiss()
                onConfirm()
            }
        }

        dialog.findViewById<MaterialButton>(R.id.common_dialog_btn_secondary).apply {
            visibility = View.VISIBLE
            text = cancelText
            setTextColor(warnColor)
            strokeColor = ColorStateList.valueOf(warnColor)
            strokeWidth = (1 * density).toInt()
            setOnClickListener {
                dialog.dismiss()
            }
        }

        // ── 7. 窗口设置 ──
        dialog.setCanceledOnTouchOutside(true)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setDimAmount(0.15f)
            setWindowAnimations(R.style.DialogAnimationTheme)
        }
        applyDialogBlur(context, dialog)
        PopupKit.autoAdjustDialogHeight(context, dialog, 0.88f)

        dialog.show()
        return dialog
    }

    /**
     * 在 [base] 颜色上以 [alpha] 不透明度叠加 [overlay] 颜色。
     * alpha 范围 0x00~0xFF。
     */
    private fun blendColorOn(base: Int, overlay: Int, alpha: Int): Int {
        val a = alpha.coerceIn(0, 255)
        val invA = 255 - a
        val r = (((base shr 16) and 0xFF) * invA + ((overlay shr 16) and 0xFF) * a) / 255
        val g = (((base shr 8) and 0xFF) * invA + ((overlay shr 8) and 0xFF) * a) / 255
        val b = ((base and 0xFF) * invA + (overlay and 0xFF) * a) / 255
        return 0xFF000000.toInt() or (r shl 16) or (g shl 8) or b
    }

    // ── 公共输入面板 ──

    /**
     * 创建带主题配色的输入面板（EditText + 确定按钮），适用于自定义输入场景。
     * @param context 上下文
     * @param hint 输入框 hint 文本
     * @param validate 验证回调，返回 null=合法，返回 String=错误提示
     * @param onConfirm 确认回调
     */
    fun createInputPanel(
        context: Context,
        hint: String = "输入数值",
        validate: (String) -> String? = { null },
        onConfirm: (String) -> Unit
    ): LinearLayout {
        val accent = ThemeKit.accent(context)
        val cardBg = ThemeKit.cardBg(context)
        val textPrimary = ThemeKit.textPrimary(context)

        val panel = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            visibility = android.view.View.GONE
            alpha = 0f
            tag = "custom_input_panel"
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val density = context.resources.displayMetrics.density
        val et = EditText(context).apply {
            tag = "custom_input_field"
            layoutParams = LinearLayout.LayoutParams(0, (40 * density).toInt(), 1f)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(cardBg)
                cornerRadius = 8f * density
                setStroke(1, if (ThemeKit.isDark(context))
                    0x30FFFFFF.toInt() else 0x20000000)
            }
            gravity = android.view.Gravity.CENTER
            this.hint = hint
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            maxLines = 1
            setTextColor(textPrimary)
            setHintTextColor(ThemeKit.textSecondary(context))
            textSize = 13f
            setPadding((12 * density).toInt(), 0, (12 * density).toInt(), 0)
        }
        panel.addView(et)

        val btnConfirm = MaterialButton(context).apply {
            text = "确定"
            backgroundTintList = android.content.res.ColorStateList.valueOf(ThemeKit.btnBg(context))
            setTextColor(0xFFFFFFFF.toInt())
            setCornerRadius((12f * density).toInt())
            insetTop = 0
            insetBottom = 0
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, (48 * density).toInt()
            ).apply { marginStart = (8 * density).toInt() }
        }
        AnimKit.applyScaleClickAnimation(btnConfirm) {
            val text = et.text.toString()
            val error = validate(text)
            if (error != null) {
                ToastKit.showDropToast(context, ToastStyle.WARNING, error)
            } else {
                onConfirm(text)
            }
        }
        panel.addView(btnConfirm)

        return panel
    }

    /**
     * 面板渐入/渐出动画
     * @param panel 目标面板
     * @param show true=显示, false=隐藏
     * @param onEnd 动画结束回调
     */
    fun animatePanelVisibility(panel: View, show: Boolean, onEnd: () -> Unit = {}) {
        panel.animate().cancel()
        if (show) {
            panel.visibility = android.view.View.VISIBLE
            panel.alpha = 0f
            panel.translationY = 10f
            panel.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(200)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .withEndAction(onEnd)
                .start()
        } else {
            panel.animate()
                .alpha(0f)
                .translationY(10f)
                .setDuration(150)
                .setInterpolator(android.view.animation.AccelerateInterpolator())
                .withEndAction {
                    panel.visibility = android.view.View.GONE
                    panel.translationY = 0f
                    onEnd()
                }
                .start()
        }
    }

    /**
     * 预设芯片动画与高亮更新
     */
    private fun updateChipHighlight(chip: View, active: Boolean, accent: Int, textSecondary: Int) {
        val tv = chip as? TextView ?: return
        tv.setTextColor(if (active) 0xFFFFFFFF.toInt() else textSecondary)
        val chipBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            if (active) {
                setColor(accent)
            } else {
                setColor(android.graphics.Color.TRANSPARENT)
                setStroke(dp2px(chip.context, 1), (textSecondary and 0x00FFFFFF) or 0x60000000)
            }
            cornerRadius = 12f * dp2px(chip.context, 1)
        }
        tv.background = chipBg
    }

    /**
     * 创建字符串标签预设快捷按钮行，自动高亮当前值。
     * 适用于筛选类型、状态等文本选项。
     *
     * @param context 上下文
     * @param options 选项列表（id, label）
     * @param currentValue 当前选中 id
     * @param onSelect 选中回调
     * @return Pair(行View, 更新函数)
     */
    fun createStringPresetRow(
        context: Context,
        options: List<Pair<String, String>>,
        currentValue: String,
        onSelect: (String) -> Unit
    ): Pair<LinearLayout, (String) -> Unit> {
        val accent = ThemeKit.accent(context)
        val textSecondary = ThemeKit.textSecondary(context)
        val chips = mutableListOf<TextView>()

        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        options.forEach { (id, label) ->
            val chip = TextView(context).apply {
                text = label
                textSize = 12f
                gravity = android.view.Gravity.CENTER
                setPadding(dp2px(context, 10), dp2px(context, 4), dp2px(context, 10), dp2px(context, 4))
                isClickable = true
                isFocusable = true
                setOnClickListener { onSelect(id) }
            }
            chips.add(chip)
            val params = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = dp2px(context, 6) }
            row.addView(chip, params)
        }

        val update: (String) -> Unit = { selected ->
            chips.forEachIndexed { i, chip ->
                updateChipHighlight(chip, options[i].first == selected, accent, textSecondary)
            }
        }
        update(currentValue)

        return row to update
    }

    /**
     * 创建双栏网格字符串选项行（适用于较多选项，如筛选类型）。
     */
    fun createStringPresetGrid(
        context: Context,
        options: List<Pair<String, String>>,
        currentValue: String,
        columns: Int = 2,
        onSelect: (String) -> Unit
    ): Pair<android.widget.GridLayout, (String) -> Unit> {
        val accent = ThemeKit.accent(context)
        val textSecondary = ThemeKit.textSecondary(context)
        val chips = mutableListOf<TextView>()

        val grid = android.widget.GridLayout(context).apply {
            columnCount = columns
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        options.forEach { (id, label) ->
            val chip = TextView(context).apply {
                text = label
                textSize = 12f
                gravity = android.view.Gravity.CENTER
                setPadding(dp2px(context, 10), dp2px(context, 6), dp2px(context, 10), dp2px(context, 6))
                isClickable = true
                isFocusable = true
                setOnClickListener { onSelect(id) }
            }
            chips.add(chip)
            val params = android.widget.GridLayout.LayoutParams().apply {
                width = 0
                height = ViewGroup.LayoutParams.WRAP_CONTENT
                columnSpec = android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1f)
                setMargins(dp2px(context, 3), dp2px(context, 3), dp2px(context, 3), dp2px(context, 3))
            }
            grid.addView(chip, params)
        }

        val update: (String) -> Unit = { selected ->
            chips.forEachIndexed { i, chip ->
                updateChipHighlight(chip, options[i].first == selected, accent, textSecondary)
            }
        }
        update(currentValue)

        return grid to update
    }

    /**
     * 创建常用值预设快捷按钮行，自动高亮当前值
     * @param context 上下文
     * @param values 预设值列表
     * @param formatLabel 格式化标签 (value) -> String
     * @param currentValue 当前选中值
     * @param onSelect 选中回调
     * @return Pair(行View, 更新函数) — 调用 update(value) 刷新高亮
     */
    fun createPresetRow(
        context: Context,
        values: List<Int>,
        formatLabel: (Int) -> String,
        currentValue: Int,
        onSelect: (Int) -> Unit
    ): Pair<LinearLayout, (Int) -> Unit> {
        val accent = ThemeKit.accent(context)
        val textSecondary = ThemeKit.textSecondary(context)
        val chips = mutableListOf<TextView>()

        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp2px(context, 12) }
        }

        values.forEach { value ->
            val chip = TextView(context).apply {
                text = formatLabel(value)
                textSize = 12f
                gravity = android.view.Gravity.CENTER
                setPadding(dp2px(context, 10), dp2px(context, 4), dp2px(context, 10), dp2px(context, 4))
                isClickable = true
                isFocusable = true
                setOnClickListener { onSelect(value) }
            }
            chips.add(chip)
            val params = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = dp2px(context, 6) }
            row.addView(chip, params)
        }

        val update: (Int) -> Unit = { selected ->
            chips.forEachIndexed { i, chip ->
                updateChipHighlight(chip, values[i] == selected, accent, textSecondary)
            }
        }
        update(currentValue)

        return row to update
    }

    private fun dp2px(context: Context, dp: Int): Int =
        (dp * context.resources.displayMetrics.density).toInt()
}

// ══════════════════════════════════════════════════════════════
// PopupKit — 统一弹窗与菜单工具（原 PopupViewUtil）
// ══════════════════════════════════════════════════════════════

/**
 * 统一弹窗与菜单工具类。
 *
 * 弹窗创建统一委托给 [DialogKit]，确保样式一致。
 */
object PopupKit {

    /**
     * 显示精致的下拉菜单（基于 PopupWindow）
     */
    fun showDropDownMenu(
        anchor: View,
        options: Array<String>,
        currentIndex: Int = -1,
        onSelect: (Int) -> Unit
    ) {
        val context = anchor.context
        val inflater = LayoutInflater.from(context)

        // 1. 创建滚动容器和内容列表
        val scroll = ScrollView(context).apply {
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        val contentLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val p = dp2px(context, 4)
            setPadding(p, p, p, p)
        }
        scroll.addView(contentLayout)

        val textPrimary = ThemeKit.textPrimary(context)
        val accent = ThemeKit.accent(context)
        val cardBg = ThemeKit.cardBg(context)
        val borderColor = if (ThemeKit.isDark(context))
            0x60FFFFFF.toInt() else 0x35000000

        // 2. 预创建弹窗对象
        val popup = android.widget.PopupWindow(scroll,
            anchor.width, // 宽度与锚点对齐
            WindowManager.LayoutParams.WRAP_CONTENT,
            true
        )

        // 3. 填充选项
        options.forEachIndexed { index, option ->
            val itemView = inflater.inflate(R.layout.layout_dialog_list_item, contentLayout, false)

            // 移除外层 Margin，实现紧凑排列
            (itemView.layoutParams as? ViewGroup.MarginLayoutParams)?.apply {
                topMargin = 0
                bottomMargin = 0
            }

            val tv = itemView.findViewById<TextView>(R.id.dialog_item_text)
            tv.apply {
                text = option
                setTextColor(textPrimary)
                textSize = 14f
            }

            // 选中态显示逻辑
            if (index == currentIndex) {
                val bgSelected = itemView.findViewById<View>(R.id.dialog_item_bg)
                val ivCheck = itemView.findViewById<ImageView>(R.id.dialog_item_check)

                bgSelected.visibility = View.VISIBLE
                ivCheck.visibility = View.VISIBLE
                ivCheck.setColorFilter(accent)
                tv.setTextColor(accent)
                tv.paint.isFakeBoldText = true

                // 绘制选中背景
                val alphaAccent = (accent and 0x00FFFFFF) or 0x26000000 // 15% alpha
                bgSelected.background = GradientDrawable().apply {
                    setColor(alphaAccent)
                    cornerRadius = dp2px(context, 8).toFloat()
                }
            } else {
                itemView.findViewById<View>(R.id.dialog_item_bg).visibility = View.GONE
                itemView.findViewById<View>(R.id.dialog_item_check).visibility = View.GONE
            }

            // 菜单项高度：显著减小间隙 (14dp -> 8dp)
            val innerContent = tv.parent as View
            innerContent.setPadding(dp2px(context, 14), dp2px(context, 8), dp2px(context, 14), dp2px(context, 8))

            itemView.setOnClickListener {
                onSelect(index)
                popup.dismiss()
            }
            contentLayout.addView(itemView)
        }

        // 4. 智能高度识别：计算最大允许高度 (屏幕高度的 40%)
        val dm = context.resources.displayMetrics
        val maxAvailableHeight = (dm.heightPixels * 0.4f).toInt()

        // 测量实际所需高度
        scroll.measure(
            View.MeasureSpec.makeMeasureSpec(anchor.width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )

        if (scroll.measuredHeight > maxAvailableHeight) {
            popup.height = maxAvailableHeight
        }

        // 5. 应用主题外观
        scroll.background = GradientDrawable().apply {
            setColor(cardBg)
            cornerRadius = dp2px(context, 12).toFloat()
            setStroke(2, borderColor)
        }

        popup.elevation = dp2px(context, 12).toFloat()
        popup.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        popup.animationStyle = R.style.DialogAnimationTheme
        popup.showAsDropDown(anchor, 0, dp2px(context, 2))
    }

    /**
     * 显示通用确认/警告弹窗
     */
    fun showConfirmDialog(
        context: Context,
        title: String,
        message: String,
        iconRes: Int = android.R.drawable.ic_dialog_alert,
        primaryBtnText: String = "确定",
        secondaryBtnText: String = "取消",
        isWarning: Boolean = false,
        onConfirm: () -> Unit
    ): Dialog {
        val dialog = DialogKit.createAnimatedDialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.layout_common_dialog)

        DialogKit.applyThemeToDialogRoot(context, dialog)

        val textPrimary = ThemeKit.textPrimary(context)
        val warnColor = 0xFFE53935.toInt()

        dialog.findViewById<TextView>(R.id.common_dialog_title).apply {
            text = title
            if (isWarning) setTextColor(warnColor)
        }
        dialog.findViewById<ImageView>(R.id.common_dialog_icon).apply {
            setImageResource(iconRes)
            if (isWarning) setColorFilter(warnColor)
        }

        val content = dialog.findViewById<LinearLayout>(R.id.common_dialog_content)
        content.addView(TextView(context).apply {
            text = message
            textSize = 15f
            setTextColor(textPrimary)
            setLineSpacing(0f, 1.2f)
        })

        dialog.findViewById<MaterialButton>(R.id.common_dialog_btn_primary).apply {
            text = primaryBtnText
            if (isWarning) {
                backgroundTintList = ColorStateList.valueOf(warnColor)
                setTextColor(0xFFFFFFFF.toInt())
            }
            setOnClickListener {
                onConfirm()
                dialog.dismiss()
            }
        }

        dialog.findViewById<MaterialButton>(R.id.common_dialog_btn_secondary).apply {
            visibility = View.VISIBLE
            text = secondaryBtnText
            setOnClickListener { dialog.dismiss() }
        }

        DialogKit.setupDialogWindow(context, dialog)
        dialog.show()
        return dialog
    }

    /**
     * 智能识别弹窗内容高度。
     * 若超过屏幕 82%，则锁定窗口高度并启用内部滚动。
     */
    fun autoAdjustDialogHeight(context: Context, dialog: Dialog, widthRatio: Float = 0.88f) {
        val window = dialog.window ?: return
        val root = dialog.findViewById<View>(R.id.common_dialog_root) ?: return
        val scroll = dialog.findViewById<View>(R.id.common_dialog_scroll_view) ?: return

        val dm = context.resources.displayMetrics
        val width = (dm.widthPixels * widthRatio).toInt()
        val maxHeight = (dm.heightPixels * 0.82f).toInt()

        // 测量前重置为自适应，以获取真实内容高度
        val rootLp = root.layoutParams
        rootLp.height = ViewGroup.LayoutParams.WRAP_CONTENT
        root.layoutParams = rootLp

        val scrollLp = scroll.layoutParams as LinearLayout.LayoutParams
        scrollLp.height = ViewGroup.LayoutParams.WRAP_CONTENT
        scrollLp.weight = 0f
        scroll.layoutParams = scrollLp

        root.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )

        if (root.measuredHeight > maxHeight) {
            // 内容过多 → 限制窗口总高度，并让 ScrollView 填充剩余空间
            window.setLayout(width, maxHeight)

            // 确保根布局也填满窗口，否则 weight 失效
            rootLp.height = ViewGroup.LayoutParams.MATCH_PARENT
            root.layoutParams = rootLp

            val lp = scroll.layoutParams as LinearLayout.LayoutParams
            lp.height = 0
            lp.weight = 1f
            scroll.layoutParams = lp

            // ── 主题统一与唤醒：应用当前主题色并闪烁提示 ──
            val themeColor = ThemeKit.textSecondary(context)
            val thumbColor = (themeColor and 0x00FFFFFF) or 0x4D000000 // 30% 不透明度

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    val scrollV = scroll as? ScrollView
                    scrollV?.verticalScrollbarThumbDrawable?.let {
                        val wrapped = androidx.core.graphics.drawable.DrawableCompat.wrap(it).mutate()
                        androidx.core.graphics.drawable.DrawableCompat.setTint(wrapped, thumbColor)
                        scrollV.verticalScrollbarThumbDrawable = wrapped
                    }
                } catch (e: Exception) { DebugLogger.w("PopupKit", "scrollbar tint failed: ${e.message}") }
            }

            // 强制唤醒滚动条
            scroll.postDelayed({
                try {
                    val method = View::class.java.getDeclaredMethod("awakenScrollBars")
                    method.isAccessible = true
                    method.invoke(scroll)
                } catch (_: Exception) {
                    scroll.isVerticalScrollBarEnabled = false
                    scroll.isVerticalScrollBarEnabled = true
                }
            }, 350)
        } else {
            // 内容较少 → 保持自适应
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)

            rootLp.height = ViewGroup.LayoutParams.WRAP_CONTENT
            root.layoutParams = rootLp

            val lp = scroll.layoutParams as LinearLayout.LayoutParams
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT
            lp.weight = 0f
            scroll.layoutParams = lp
        }
    }

    private fun dp2px(context: Context, dp: Int): Int = (dp * context.resources.displayMetrics.density).toInt()
}

// ══════════════════════════════════════════════════════════════
// ToastStyle 枚举
// ══════════════════════════════════════════════════════════════

/** Toast 样式 */
enum class ToastStyle(val iconRes: Int) {
    INFO(R.drawable.ic_info),
    SUCCESS(R.drawable.ic_check),
    WARNING(android.R.drawable.ic_dialog_alert)
}

// ══════════════════════════════════════════════════════════════
// ToastKit — 统一 Toast 工具（原 ToastUtil）
// ══════════════════════════════════════════════════════════════

/**
 * 统一 Toast 工具类 — 水滴下落动画效果。
 *
 * 从屏幕中上方自然滴落，带有弹性弹跳和涟漪脉冲动画，
 * 全程使用硬件加速 + translationY 避免布局抖动，
 * 自动跟随当前主题配色（通过 ThemeKit 取色）。
 *
 * 支持三种样式：
 * - [ToastStyle.INFO]：普通提示，灰色图标
 * - [ToastStyle.SUCCESS]：成功提示，✓ 图标
 * - [ToastStyle.WARNING]：警告提示，红色图标 + 淡红底色
 *
 * 用法示例：
 * ```kotlin
 * // 加载中（需手动关闭或由 showDropToast 自动替换）
 * ToastKit.showLoadingToast(activity, "正在检查更新...")
 *
 * // 成功提示
 * ToastKit.showDropToast(activity, ToastStyle.SUCCESS, "已是最新版本")
 *
 * // 警告提示
 * ToastKit.showDropToast(activity, ToastStyle.WARNING, "网络连接失败")
 *
 * // 确认弹窗（需用户点击确认关闭）
 * ToastKit.showConfirmDialog(context, "流量提醒", "本月流量已用完")
 * ```
 */
object ToastKit {

    /** 当前显示的装饰视图 Toast（用于自动移除） */
    @Volatile private var activeToast: View? = null
    /** 待执行的自动移除任务 */
    @Volatile private var pendingRemoveRunnable: Runnable? = null
    /** 加载中 Toast 的视图（仍使用 decorView 方式） */
    @Volatile private var activeLoadingView: View? = null

    // ── 警告色（与 DialogKit 保持一致）──
    private const val WARN_COLOR_LIGHT = 0xFFE53935.toInt()
    private const val WARN_COLOR_DARK = 0xFFEF5350.toInt()

    // ── 常量 ──
    private const val TOP_MARGIN_DP = 48
    private const val MAX_WIDTH_RATIO = 0.82f
    private const val DROP_DURATION = 600L       // 下落时长(ms)，稍长更流畅
    private const val EXIT_DURATION = 250L       // 退出时长(ms)
    private const val RIPPLE_PUSH_DP = 5f        // 涟漪下落幅度(dp)
    private const val RIPPLE_REBOUND_DP = 6f     // 涟漪回弹幅度(dp)

    // ══════════════════════════════════════════════════════════════
    // 1. 加载中 Toast（持久显示，需手动关闭或被 showDropToast 替换）
    // ══════════════════════════════════════════════════════════════

    /**
     * 显示加载中 Toast。
     *
     * 不会自动消失，直到调用 [dismissActiveToast] 或下一个 [showDropToast]。
     *
     * @param activity 当前 Activity
     * @param message  加载提示文字
     */
    @JvmStatic
    fun showLoadingToast(activity: Activity, message: String = "正在检查更新...") {
        val decorView = activity.window.decorView as? ViewGroup ?: return
        dismissActiveToast()

        val density = activity.resources.displayMetrics.density
        val dp = { v: Int -> (v * density).toInt() }

        // 构建视图（无图标，纯文字居中）
        val toast = createLoadingView(activity, message, dp)
        val maxWidth = (activity.resources.displayMetrics.widthPixels * MAX_WIDTH_RATIO).toInt()

        // 先测量确定实际宽度
        toast.measure(
            View.MeasureSpec.makeMeasureSpec(maxWidth, View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val toastWidth = minOf(toast.measuredWidth, maxWidth)
        val toastHeight = toast.measuredHeight

        // 用实测宽度添加到 DecorView，确保不超出屏幕
        decorView.addView(toast, android.widget.FrameLayout.LayoutParams(
            toastWidth,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL
        })
        activeLoadingView = toast

        // 启用硬件加速图层
        toast.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        // 初始状态：隐藏在屏幕上方
        val topTarget = dp(TOP_MARGIN_DP).toFloat()
        toast.translationY = -toastHeight - topTarget - dp(40).toFloat()
        toast.alpha = 0f

        // 下落入场（带弹性弹跳）
        toast.animate()
            .translationY(topTarget)
            .alpha(1f)
            .setDuration(DROP_DURATION)
            .setInterpolator(OvershootInterpolator(1.25f))
            .start()
    }

    /** 构建加载中视图（纯文字 + 居中） */
    private fun createLoadingView(
        activity: Activity,
        message: String,
        dp: (Int) -> Int
    ): LinearLayout {
        val cardBg = ThemeKit.cardBg(activity)
        val textPrimary = ThemeKit.textPrimary(activity)
        val isDark = ThemeKit.isDark(activity)
        val density = activity.resources.displayMetrics.density

        return LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
            setPadding(dp(24), dp(14), dp(24), dp(14))

            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(cardBg)
                cornerRadius = 14f * density
                val borderColor = if (isDark) 0x50FFFFFF.toInt() else 0x28000000
                setStroke(dp(1), borderColor)
            }
            elevation = 16f

            addView(TextView(activity).apply {
                text = message
                textSize = 14f
                setTextColor(textPrimary)
            })
        }
    }

    // ══════════════════════════════════════════════════════════════
    // 2. 标准 Toast（带图标 + 水滴动画 + 自动消失）
    // ══════════════════════════════════════════════════════════════

    /**
     * 显示水滴下落 Toast（Activity 版本）。
     *
     * 将 Toast 添加到 DecorView 并执行下落动画，
     * 在 [duration] 毫秒后自动消失。
     *
     * @param activity 当前 Activity
     * @param style    [ToastStyle] 样式（INFO / SUCCESS / WARNING）
     * @param title    标题（必填）
     * @param message  消息正文（可选）
     * @param duration 停留时长（ms），默认 2200
     */
    @JvmStatic
    fun showDropToast(
        activity: Activity,
        style: ToastStyle = ToastStyle.INFO,
        title: String,
        message: String = "",
        duration: Long = 2200L
    ) {
        val decorView = activity.window.decorView as? ViewGroup ?: return
        dismissActiveToast()

        val density = activity.resources.displayMetrics.density
        val dp = { v: Int -> (v * density).toInt() }
        val screenWidth = activity.resources.displayMetrics.widthPixels
        val maxWidth = (screenWidth * MAX_WIDTH_RATIO).toInt()
        val isDark = ThemeKit.isDark(activity)
        val warnColor = if (isDark) WARN_COLOR_DARK else WARN_COLOR_LIGHT
        val isWarning = style == ToastStyle.WARNING

        // 构建视图
        val toastView = createToastView(activity, style.iconRes, title, message, dp, isWarning, warnColor)

        // 测量并限定宽度
        toastView.measure(
            View.MeasureSpec.makeMeasureSpec(maxWidth, View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val toastWidth = minOf(toastView.measuredWidth, maxWidth)
        val toastHeight = toastView.measuredHeight

        // 添加到 DecorView
        decorView.addView(toastView, android.widget.FrameLayout.LayoutParams(
            toastWidth,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL
        })
        activeToast = toastView

        // 硬件加速
        toastView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        // 初始状态：隐藏在屏幕上方
        val topTarget = dp(TOP_MARGIN_DP).toFloat()
        toastView.translationY = -toastHeight - topTarget - dp(40).toFloat()
        toastView.alpha = 0f

        // 下落入场（带弹性弹跳）
        toastView.animate()
            .translationY(topTarget)
            .alpha(1f)
            .setDuration(DROP_DURATION)
            .setInterpolator(OvershootInterpolator(1.25f))
            .withEndAction {
                // 落地后涟漪脉冲
                startRipplePulse(toastView)
            }
            .start()

        // 自动消失
        pendingRemoveRunnable = Runnable {
            exitAndRemove(toastView, decorView)
            if (activeToast == toastView) activeToast = null
        }
        toastView.postDelayed(pendingRemoveRunnable, duration)
    }

    /**
     * 显示标准 Toast（Context 版本）。
     *
     * 尝试将 [context] 转为 Activity，成功则使用装饰视图动画 Toast；
     * 非 Activity 上下文（如后台 Worker）不会显示 Toast，
     * 后台通知应使用系统通知栏。
     */
    @JvmStatic
    fun showDropToast(
        context: Context,
        style: ToastStyle = ToastStyle.INFO,
        title: String,
        message: String = "",
        duration: Long = 2200L
    ) {
        val activity = context as? Activity
        if (activity != null) {
            showDropToast(activity, style, title, message, duration)
        }
        // 非 Activity 上下文：静默忽略（后台 Worker 使用系统通知栏）
    }

    /** 构建带图标的 Toast 视图 */
    private fun createToastView(
        activity: Activity,
        iconRes: Int,
        title: String,
        message: String,
        dp: (Int) -> Int,
        isWarning: Boolean,
        warnColor: Int
    ): LinearLayout {
        val cardBg = ThemeKit.cardBg(activity)
        val textPrimary = ThemeKit.textPrimary(activity)
        val iconTint = ThemeKit.iconTint(activity)
        val isDark = ThemeKit.isDark(activity)
        val density = activity.resources.displayMetrics.density

        return LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(14))

            // ── Card 背景 ──
            val bgColor = if (isWarning) {
                // 警告：在 cardBg 上叠加 8%~12% 红色
                val warnAlpha = if (isDark) 0x1F else 0x14
                blendColorOn(cardBg, warnColor, warnAlpha)
            } else {
                cardBg
            }
            val borderColor = if (isWarning) {
                (warnColor and 0x00FFFFFF) or 0x50000000.toInt()
            } else if (isDark) {
                0x50FFFFFF.toInt()
            } else {
                0x28000000
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(bgColor)
                cornerRadius = 14f * density
                setStroke(dp(1), borderColor)
            }
            elevation = 16f

            // ── 图标 ──
            addView(ImageView(activity).apply {
                layoutParams = LinearLayout.LayoutParams(dp(22), dp(22))
                setImageResource(iconRes)
                setColorFilter(if (isWarning) warnColor else iconTint)
            })

            // ── 文字区域 ──
            addView(LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
                ).apply { marginStart = dp(12) }

                addView(TextView(activity).apply {
                    text = title
                    textSize = 15f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setTextColor(textPrimary)
                })

                if (message.isNotBlank()) {
                    addView(TextView(activity).apply {
                        text = message
                        textSize = 13f
                        setTextColor(textPrimary)
                        alpha = 0.55f
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply { topMargin = dp(3) }
                    })
                }
            })
        }
    }

    // ══════════════════════════════════════════════════════════════
    // 3. 确认弹窗（需用户点击确认关闭）
    // ══════════════════════════════════════════════════════════════

    /**
     * 显示确认通知弹窗。
     *
     * 适用于需要用户确认的重要通知（如流量限制提醒），
     * 不会自动关闭，需用户点击确认按钮。
     * 内部委托给 [DialogKit.showCommonDialog] 确保样式统一。
     *
     * @param context     上下文
     * @param title       标题
     * @param message     消息正文
     * @param iconRes     图标资源，默认 ic_info
     * @param confirmText 确认按钮文字，默认"我知道了"
     * @param onConfirm   确认回调
     */
    @JvmStatic
    fun showConfirmDialog(
        context: Context,
        title: String,
        message: String,
        iconRes: Int = R.drawable.ic_info,
        confirmText: String = "我知道了",
        onConfirm: () -> Unit = {}
    ) {
        DialogKit.showCommonDialog(
            context,
            title = title,
            iconRes = iconRes,
            onFill = { content ->
                content.addView(TextView(context).apply {
                    text = message
                    textSize = 15f
                    setTextColor(ThemeKit.textPrimary(context))
                    setLineSpacing(0f, 1.3f)
                })
            },
            primaryBtnText = confirmText,
            onPrimaryClick = { dialog ->
                dialog.dismiss()
                onConfirm()
            }
        )
    }

    /**
     * 显示警告确认弹窗（醒目红色样式）。
     * 适用于需要用户特别注意的确认场景。
     * 委托给 [DialogKit.showWarningConfirmDialog]。
     */
    @JvmStatic
    fun showWarningConfirmDialog(
        context: Context,
        title: String,
        message: String,
        confirmText: String = "确定",
        cancelText: String = "取消",
        onConfirm: () -> Unit
    ) {
        DialogKit.showWarningConfirmDialog(
            context,
            title = title,
            message = message,
            confirmText = confirmText,
            cancelText = cancelText,
            onConfirm = onConfirm
        )
    }

    // ══════════════════════════════════════════════════════════════
    // 4. 内部工具方法
    // ══════════════════════════════════════════════════════════════

    /** 涟漪脉冲动画：水滴落下后单次水波扩散，弹动次数减半，仅保留主波 */
    private fun startRipplePulse(toast: View) {
        if (toast.parent == null) return
        val density = toast.resources.displayMetrics.density
        val pushPx = RIPPLE_PUSH_DP * density
        val reboundPx = RIPPLE_REBOUND_DP * density

        // 单波涟漪：下落冲击 → 回弹 → 稳定
        toast.animate()
            .translationYBy(pushPx)
            .setDuration(180)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .withEndAction {
                if (toast.parent == null) return@withEndAction
                toast.animate()
                    .translationYBy(-(pushPx + reboundPx))
                    .setDuration(250)
                    .setInterpolator(android.view.animation.DecelerateInterpolator(1.8f))
                    .withEndAction {
                        if (toast.parent == null) return@withEndAction
                        toast.animate()
                            .translationYBy(reboundPx * 0.4f)
                            .setDuration(180)
                            .setInterpolator(android.view.animation.DecelerateInterpolator())
                            .start()
                    }
                    .start()
            }
            .start()
    }

    private fun exitAndRemove(toast: View, parent: ViewGroup) {
        if (toast.parent == null) return
        toast.animate().cancel()
        toast.animate()
            .alpha(0f)
            .translationYBy(-16f)
            .setDuration(EXIT_DURATION)
            .setInterpolator(AccelerateInterpolator(1.5f))
            .withEndAction {
                if (toast.parent != null) parent.removeView(toast)
                if (activeLoadingView == toast) activeLoadingView = null
                if (activeToast == toast) activeToast = null
            }
            .start()
    }

    /**
     * 立即移除当前活跃 Toast。
     * 在 [showDropToast] 和 [showLoadingToast] 中会自动调用。
     */
    @JvmStatic
    fun dismissActiveToast() {
        // 取消待执行的自动移除任务
        pendingRemoveRunnable?.let { runnable ->
            activeToast?.removeCallbacks(runnable)
        }
        pendingRemoveRunnable = null

        // 移除装饰视图 Toast
        activeToast?.let { toast ->
            toast.animate().cancel()
            (toast.parent as? ViewGroup)?.removeView(toast)
            activeToast = null
        }

        // 清除 Activity 内嵌的加载中 Toast
        activeLoadingView?.let { toast ->
            toast.animate().cancel()
            (toast.parent as? ViewGroup)?.removeView(toast)
            activeLoadingView = null
        }
    }

    /** 在 [base] 上叠加 [overlay] 颜色（指定 alpha 不透明度 0x00~0xFF） */
    private fun blendColorOn(base: Int, overlay: Int, alpha: Int): Int {
        val a = alpha.coerceIn(0, 255)
        val invA = 255 - a
        val r = (((base shr 16) and 0xFF) * invA + ((overlay shr 16) and 0xFF) * a) / 255
        val g = (((base shr 8) and 0xFF) * invA + ((overlay shr 8) and 0xFF) * a) / 255
        val b = ((base and 0xFF) * invA + (overlay and 0xFF) * a) / 255
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }
}
