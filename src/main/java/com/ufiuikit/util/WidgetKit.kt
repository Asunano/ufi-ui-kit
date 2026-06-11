package com.ufiuikit.util

import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.ufiuikit.R

// ═══════════════════════════════════════════════════════════════
// 1. WidgetKit — 公共设置项组件库
// ═══════════════════════════════════════════════════════════════

/**
 * 公共设置项组件库
 *
 * 提供与 [layout_common_switch_item]、[layout_common_setting_item] 样式一致的
 * View 工厂方法，避免在各 Activity 中手写重复的 findViewById + 样式代码。
 */
object WidgetKit {

    // ── 尺寸常量（与 XML 布局保持一致）──
    private const val SWITCH_TRACK_W = 42   // dp
    private const val SWITCH_TRACK_H = 24   // dp
    private const val SWITCH_THUMB_W = 18   // dp
    private const val SWITCH_THUMB_H = 18   // dp
    private const val SWITCH_THUMB_MARGIN_START = 3 // dp

    // ═══════════════════════════════════════════
    // 1. 开关项配置（inflate 的 layout_common_switch_item）
    // ═══════════════════════════════════════════

    /**
     * 配置已 inflate 的 [R.layout.layout_common_switch_item]（或 include 该布局的根 View）。
     *
     * 自动设置图标、标题、副标题（可选），并绑定自定义 FrameLayout 开关。
     *
     * 用法示例：
     * ```kotlin
     * WidgetKit.setupSwitchItem(
     *     findViewById(R.id.item_widget_clip_to_outline),
     *     iconRes = R.drawable.ic_rounded_corners,
     *     label = "兼容性小组件圆角",
     *     subtitle = "如果桌面小组件没有圆角效果，可开启此项强制圆角",
     *     initialChecked = SPUtil.getWidgetClipToOutline(this),
     *     onToggle = { checked -> SPUtil.setWidgetClipToOutline(this, checked) }
     * )
     * ```
     */
    fun setupSwitchItem(
        itemView: View,
        iconRes: Int,
        label: String,
        subtitle: String? = null,
        initialChecked: Boolean,
        onToggle: (Boolean) -> Unit
    ) {
        itemView.findViewById<ImageView>(R.id.common_item_icon)
            ?.setImageResource(iconRes)
        itemView.findViewById<TextView>(R.id.common_switch_label)?.text = label
        itemView.findViewById<TextView>(R.id.common_switch_subtitle)?.apply {
            if (subtitle != null) {
                text = subtitle
                visibility = View.VISIBLE
            } else {
                visibility = View.GONE
            }
        }
        ThemeApplier.setupSwitch(itemView, initialChecked, onToggle)
    }

    // ═══════════════════════════════════════════
    // 2. 代码构建开关行（弹窗/动态布局用）
    // ═══════════════════════════════════════════

    /**
     * 纯代码构建一个开关行，样式与 [R.layout.layout_common_switch_item]
     * 的开关区域一致（自定义 FrameLayout 滑块开关 + 标题/副标题）。
     *
     * 适用于弹窗 content 区等无法 `<include>` XML 的场景。
     *
     * @param context 上下文
     * @param label   开关标题（对应 common_switch_label）
     * @param subtitle 副标题，传 null 则不显示
     * @return 已绑定开关逻辑的根 View，可直接 addView 到容器中
     */
    fun createSwitchRow(
        context: Context,
        label: String,
        subtitle: String? = null,
        initialChecked: Boolean,
        onToggle: (Boolean) -> Unit
    ): View {
        val density = context.resources.displayMetrics.density
        val dp = { v: Int -> (v * density).toInt() }

        // ── 根容器 ──
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, dp(4), 0, dp(4))
        }

        // ── 左侧文字区域 ──
        val textCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
            )
        }

        // 标题（取色使用动态 ThemeKit，确保跟随主题切换）
        val tvLabel = TextView(context).apply {
            id = R.id.common_switch_label
            setTextAppearance(R.style.AppText_Title)
            setTextColor(ThemeKit.textPrimary(context))
            text = label
        }
        textCol.addView(tvLabel)

        // 副标题（可选，取色使用动态 ThemeKit）
        val tvSubtitle = TextView(context).apply {
            id = R.id.common_switch_subtitle
            setTextAppearance(R.style.AppText_Subtitle)
            setTextColor(ThemeKit.textSecondary(context))
            if (subtitle != null) {
                text = subtitle
            } else {
                visibility = View.GONE
            }
        }
        textCol.addView(tvSubtitle)
        root.addView(textCol)

        // ── 右侧自定义开关 ──
        val track = FrameLayout(context).apply {
            id = R.id.common_switch_track
            layoutParams = LinearLayout.LayoutParams(dp(SWITCH_TRACK_W), dp(SWITCH_TRACK_H))
            // 初始 off 背景（ThemeApplier.setupSwitch 会在初始化时根据主题替换）
            setBackgroundResource(R.drawable.bg_common_switch_track_off)
            isClickable = true
            isFocusable = true
        }

        val thumb = View(context).apply {
            id = R.id.common_switch_thumb
            val lp = FrameLayout.LayoutParams(dp(SWITCH_THUMB_W), dp(SWITCH_THUMB_H))
            lp.gravity = Gravity.CENTER_VERTICAL or Gravity.START
            lp.marginStart = dp(SWITCH_THUMB_MARGIN_START)
            layoutParams = lp
            setBackgroundResource(R.drawable.bg_widget_mask)
            elevation = 1f
        }
        track.addView(thumb)
        root.addView(track)

        // ── 委托 ThemeApplier.setupSwitch 绑定动画 + 交互逻辑 ──
        ThemeApplier.setupSwitch(root, initialChecked, onToggle)

        return root
    }

    // ═══════════════════════════════════════════
    // 3. 分隔线
    // ═══════════════════════════════════════════

    /**
     * 创建一条主题色分隔线（1dp 高，宽度填满父容器）。
     */
    fun createDivider(context: Context): View {
        return View(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 1
            )
            setBackgroundColor(ThemeKit.divider(context))
        }
    }

    // ═══════════════════════════════════════════
    // 4. 设置项配置（inflate 的 layout_common_setting_item）
    // ═══════════════════════════════════════════

    /**
     * 配置已 inflate 的 [R.layout.layout_common_setting_item]（或 include 该布局的根 View）。
     *
     * 自动设置图标、标题、副标题（可选），并绑定点击事件。
     *
     * 用法示例：
     * ```kotlin
     * WidgetKit.setupSettingItem(
     *     findViewById(R.id.item_basic_config),
     *     iconRes = R.drawable.ic_router,
     *     title = "基础连接",
     *     showSubtitle = false,
     *     onClick = ::showBasicConfigDialog
     * )
     * ```
     */
    fun setupSettingItem(
        itemView: View,
        iconRes: Int,
        title: String,
        subtitle: String? = null,
        showSubtitle: Boolean = true,
        onClick: () -> Unit
    ) {
        itemView.findViewById<ImageView>(R.id.common_item_icon)
            ?.setImageResource(iconRes)
        itemView.findViewById<TextView>(R.id.common_item_title)?.text = title
        itemView.findViewById<TextView>(R.id.common_item_subtitle)?.apply {
            if (showSubtitle && subtitle != null) {
                text = subtitle
                visibility = View.VISIBLE
            } else {
                visibility = View.GONE
            }
        }
        itemView.setOnClickListener { onClick() }
    }

    // ═══════════════════════════════════════════
    // 5. 主题 EditText（弹窗内表单输入）
    // ═══════════════════════════════════════════

    /**
     * 创建带主题样式的单行 [EditText]（卡片背景 + 圆角 + 描边）。
     *
     * 适用于弹窗 content 区的多字段表单。
     *
     * @param context   上下文
     * @param hint      占位提示
     * @param text      当前值，默认空字符串
     * @param inputType 输入类型，默认 [InputType.TYPE_CLASS_TEXT]
     */
    fun createThemedEditText(
        context: Context,
        hint: String,
        text: String = "",
        inputType: Int = InputType.TYPE_CLASS_TEXT
    ): EditText {
        val density = context.resources.displayMetrics.density
        val dp = { v: Int -> (v * density).toInt() }
        val cardBg = ThemeKit.cardBg(context)
        val textPrimary = ThemeKit.textPrimary(context)
        val textSecondary = ThemeKit.textSecondary(context)
        return EditText(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setHint(hint)
            setText(text)
            this.inputType = inputType
            maxLines = 1
            textSize = 14f
            setTextColor(textPrimary)
            setHintTextColor(textSecondary)
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(cardBg)
                cornerRadius = 10f * density
                setStroke(1, if (ThemeKit.isDark(context)) 0x30FFFFFF.toInt() else 0x20000000)
            }
        }
    }

    // ═══════════════════════════════════════════
    // 6. EditText 转下拉选择器
    // ═══════════════════════════════════════════

    /**
     * 将 [EditText] 改造为"点击弹出下拉菜单"的选择器（禁止手动输入）。
     *
     * 下拉菜单样式由 [PopupKit.showDropDownMenu] 提供。
     *
     * @param editText     目标 EditText
     * @param options      展示选项数组（如 "auto (自动探测)"）
     * @param values       选中后写入 EditText 的实际值数组（如 "auto"）
     * @param currentValue 当前值，用于定位默认选中项
     */
    fun setupDropdownOnEditText(
        editText: EditText,
        options: Array<String>,
        values: Array<String>,
        currentValue: String
    ) {
        editText.apply {
            isFocusable = false
            isClickable = true
            isCursorVisible = false
            setOnClickListener {
                val currentIdx = values.indexOf(currentValue.lowercase()).coerceAtLeast(0)
                PopupKit.showDropDownMenu(
                    it,
                    options = options,
                    currentIndex = currentIdx,
                    onSelect = { which -> setText(values[which]) }
                )
            }
        }
    }

    // ═══════════════════════════════════════════
    // 7. "恢复默认"按钮（弹窗底部）
    // ═══════════════════════════════════════════

    /**
     * 创建"恢复默认" [MaterialButton]（Outlined 样式），用于弹窗内一键重置表单字段。
     *
     * @param context            上下文
     * @param onRestoreDefaults  点击回调
     */
    fun createRestoreDefaultsButton(
        context: Context,
        onRestoreDefaults: () -> Unit
    ): MaterialButton {
        val density = context.resources.displayMetrics.density
        val dp = { v: Int -> (v * density).toInt() }
        val secondaryColor = ThemeKit.textSecondary(context)
        val textPrimary = ThemeKit.textPrimary(context)

        return MaterialButton(context, null,
            com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(48)
            ).apply {
                topMargin = dp(10)
            }
            text = "恢复默认"
            textSize = 14f
            insetTop = 0
            insetBottom = 0
            setTextColor(textPrimary)
            strokeColor = ColorStateList.valueOf(secondaryColor)
            strokeWidth = dp(1)
            @Suppress("DEPRECATION")
            setCornerRadius(dp(12))
            setOnClickListener { onRestoreDefaults() }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// 2. PaginationKit — 翻页栏公共组件
// ═══════════════════════════════════════════════════════════════

/**
 * 翻页栏公共组件 — 胶囊形式，固定页面底部，支持首页/上一页/页码/下一页/末页 + 点击跳转。
 *
 * 使用方式：
 * ```kotlin
 * val bar = PaginationKit.create(context) { action -> ... }
 * rootLayout.addView(bar, FrameLayout.LayoutParams(...))
 * PaginationKit.update(bar, currentPage, totalPages)
 * PaginationKit.fadeVisibility(bar, totalPages > 1)
 * ```
 */
object PaginationKit {

    sealed class Action {
        object FIRST : Action()
        object PREV : Action()
        object NEXT : Action()
        object LAST : Action()
        data class Jump(val page: Int) : Action()
    }

    // ── 尺寸常量 ──
    private const val BTN_SIZE_DP      = 28f
    private const val ICON_SIZE_DP     = 16f
    private const val PAGE_TEXT_SP     = 11f
    private const val BAR_HPAD_DP      = 6f       // 缩小水平内边距，首末页按钮更贴近边框
    private const val BAR_VPAD_DP      = 5f
    private const val BTN_MARGIN_DP    = 0.5f
    private const val PAGE_MARGIN_DP   = 6f
    private const val PAGE_HPAD_DP     = 10f
    private const val PAGE_VPAD_DP     = 3f
    private const val BAR_CORNER_DP    = 30f      // 胶囊圆角加强
    private const val PAGE_CORNER_DP   = 7f
    private const val STROKE_WIDTH_DP  = 1.5f

    /**
     * 创建翻页栏（返回 LinearLayout，添加到布局即可）。
     */
    fun create(context: Context, onAction: (Action) -> Unit): LinearLayout {
        val d = context.resources.displayMetrics.density
        val accent = ThemeKit.accent(context)
        val textSec = ThemeKit.textSecondary(context)
        val cardBg = ThemeKit.cardBg(context)

        val bar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(
                (BAR_HPAD_DP * d).toInt(), (BAR_VPAD_DP * d).toInt(),
                (BAR_HPAD_DP * d).toInt(), (BAR_VPAD_DP * d).toInt()
            )
            tag = "pagination_bar"

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.CENTER_HORIZONTAL }

            val strokeColor = if (ThemeKit.isDark(context)) 0x30FFFFFF.toInt() else 0x28000000
            background = GradientDrawable().apply {
                setColor(cardBg)
                setStroke((STROKE_WIDTH_DP * d).toInt(), strokeColor)
                cornerRadius = BAR_CORNER_DP * d
            }
        }

        // 首页 (|<)
        bar.addView(iconBtn(context, R.drawable.ic_chevron_left_pipe, textSec) { onAction(Action.FIRST) }.apply { tag = "btn_first" }, btnLp(d))
        // 上一页 (<)
        bar.addView(iconBtn(context, R.drawable.ic_chevron_left, textSec) { onAction(Action.PREV) }.apply { tag = "btn_prev" }, btnLp(d))

        // 页码信息（可点击跳转）
        val tvPage = TextView(context).apply {
            text = "1 / 1"
            textSize = PAGE_TEXT_SP
            setTextColor(ThemeKit.textPrimary(context))
            gravity = Gravity.CENTER
            tag = "tv_page_info"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = (PAGE_MARGIN_DP * d).toInt()
                marginEnd = (PAGE_MARGIN_DP * d).toInt()
            }
            setPadding(
                (PAGE_HPAD_DP * d).toInt(), (PAGE_VPAD_DP * d).toInt(),
                (PAGE_HPAD_DP * d).toInt(), (PAGE_VPAD_DP * d).toInt()
            )
            background = GradientDrawable().apply {
                setColor((accent and 0x00FFFFFF) or 0x15000000)
                cornerRadius = PAGE_CORNER_DP * d
            }
            isClickable = true
            isFocusable = true
        }
        applyPressEffect(tvPage, scale = 0.9f) { bg ->
            bg.setColor((accent and 0x00FFFFFF) or 0x25000000)
        }
        tvPage.setOnClickListener { showJumpDialog(context, onAction) }
        bar.addView(tvPage)

        // 下一页 (>)
        bar.addView(iconBtn(context, R.drawable.ic_chevron_right, textSec) { onAction(Action.NEXT) }.apply { tag = "btn_next" }, btnLp(d))
        // 末页 (>|)
        bar.addView(iconBtn(context, R.drawable.ic_chevron_right_pipe, textSec) { onAction(Action.LAST) }.apply { tag = "btn_last" }, btnLp(d))

        return bar
    }

    /**
     * 更新翻页栏状态（页码 + 按钮可用性 + 动画过渡）。
     */
    fun update(bar: LinearLayout, currentPage: Int, totalPages: Int) {
        val tvPage = bar.findViewWithTag<TextView>("tv_page_info")
        tvPage?.text = "$currentPage / $totalPages"

        val canPrev = currentPage > 1
        val canNext = currentPage < totalPages

        bar.findViewWithTag<View>("btn_first")?.isEnabled = canPrev
        bar.findViewWithTag<View>("btn_prev")?.isEnabled = canPrev
        bar.findViewWithTag<View>("btn_next")?.isEnabled = canNext
        bar.findViewWithTag<View>("btn_last")?.isEnabled = canNext

        // 禁用态动画过渡（平滑 alpha 变化）
        listOf("btn_first", "btn_prev").forEach { t ->
            bar.findViewWithTag<View>(t)?.animate()
                ?.alpha(if (canPrev) 1f else 0.25f)
                ?.setDuration(150)?.start()
        }
        listOf("btn_next", "btn_last").forEach { t ->
            bar.findViewWithTag<View>(t)?.animate()
                ?.alpha(if (canNext) 1f else 0.25f)
                ?.setDuration(150)?.start()
        }
    }

    /**
     * 渐入渐出控制翻页栏可见性。
     */
    fun fadeVisibility(bar: View, visible: Boolean) {
        val targetAlpha = if (visible) 1f else 0f
        if (bar.alpha == targetAlpha && (visible == (bar.visibility == View.VISIBLE))) return

        bar.animate().cancel()

        if (visible && bar.visibility != View.VISIBLE) {
            bar.alpha = 0f
            bar.visibility = View.VISIBLE
        }

        bar.animate()
            .alpha(targetAlpha)
            .setDuration(200)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .setListener(if (!visible) object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    bar.visibility = View.GONE
                }
            } else null)
            .start()
    }

    /**
     * 页码跳转弹窗。
     */
    private fun showJumpDialog(context: Context, onAction: (Action) -> Unit) {
        val d = context.resources.displayMetrics.density
        val cardBg = ThemeKit.cardBg(context)
        val textPrimary = ThemeKit.textPrimary(context)

        DialogKit.showCommonDialog(
            context = context,
            title = "跳转到页码",
            iconRes = R.drawable.ic_notification,
            onFill = { content ->
                content.addView(TextView(context).apply {
                    text = "请输入目标页码"
                    textSize = 13f
                    setTextColor(ThemeKit.textSecondary(context))
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = (8 * d).toInt() }
                })
                val et = EditText(context).apply {
                    hint = "页码"
                    textSize = 16f
                    setTextColor(textPrimary)
                    setHintTextColor(ThemeKit.textSecondary(context))
                    inputType = android.text.InputType.TYPE_CLASS_NUMBER
                    gravity = Gravity.CENTER
                    setPadding((12 * d).toInt(), (8 * d).toInt(), (12 * d).toInt(), (8 * d).toInt())
                    background = GradientDrawable().apply {
                        setColor(cardBg)
                        cornerRadius = 10 * d
                        setStroke((1 * d).toInt(), (ThemeKit.textSecondary(context) and 0x00FFFFFF) or 0x40000000)
                    }
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, (44 * d).toInt()
                    )
                    tag = "jump_input"
                }
                content.addView(et)
            },
            primaryBtnText = "跳转",
            onPrimaryClick = { dialog ->
                val et = dialog.findViewById<EditText>(R.id.common_dialog_content)
                    ?.findViewWithTag<EditText>("jump_input")
                val input = et?.text?.toString()?.toIntOrNull()
                if (input != null && input > 0) {
                    onAction(Action.Jump(input))
                }
                dialog.dismiss()
            },
            secondaryBtnText = "取消",
            onSecondaryClick = { d -> d.dismiss() }
        )
    }

    // ── 内部工具 ──

    /**
     * 创建带按压动画反馈的图标按钮（ImageView + 缩放 + 背景色变化）。
     */
    private fun iconBtn(context: Context, iconRes: Int, color: Int, onClick: () -> Unit): ImageView {
        val d = context.resources.displayMetrics.density
        val size = (BTN_SIZE_DP * d).toInt()
        val iconSize = (ICON_SIZE_DP * d).toInt()

        val iv = ImageView(context).apply {
            setImageResource(iconRes)
            imageTintList = ColorStateList.valueOf(color)
            // 图标居中
            setPadding(
                (size - iconSize) / 2, (size - iconSize) / 2,
                (size - iconSize) / 2, (size - iconSize) / 2
            )
            layoutParams = LinearLayout.LayoutParams(size, size)
            isClickable = true
            isFocusable = true
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.TRANSPARENT)
            }
        }

        val pressColor = (color and 0x00FFFFFF) or 0x18000000

        iv.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate().scaleX(0.85f).scaleY(0.85f).setDuration(50).start()
                    (v.background as? GradientDrawable)?.setColor(pressColor)
                }
                MotionEvent.ACTION_UP -> {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                    (v.background as? GradientDrawable)?.setColor(Color.TRANSPARENT)
                    if (event.x >= 0 && event.x <= v.width && event.y >= 0 && event.y <= v.height) {
                        v.playSoundEffect(android.view.SoundEffectConstants.CLICK)
                        onClick()
                    }
                }
                MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                    (v.background as? GradientDrawable)?.setColor(Color.TRANSPARENT)
                }
            }
            true
        }
        return iv
    }

    /**
     * 为 View 添加通用按压效果（缩放 + 自定义背景回调）。
     */
    private fun applyPressEffect(view: View, scale: Float = 0.88f, onDown: ((GradientDrawable) -> Unit)? = null) {
        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate().scaleX(scale).scaleY(scale).setDuration(50).start()
                    (v.background as? GradientDrawable)?.let { onDown?.invoke(it) }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                }
            }
            false
        }
    }

    private fun btnLp(d: Float) = LinearLayout.LayoutParams(
        (BTN_SIZE_DP * d).toInt(), (BTN_SIZE_DP * d).toInt()
    ).apply {
        marginStart = (BTN_MARGIN_DP * d).toInt()
        marginEnd = (BTN_MARGIN_DP * d).toInt()
    }
}

// ═══════════════════════════════════════════════════════════════
// 3. CropView — 图片裁切视图
// ═══════════════════════════════════════════════════════════════

/**
 * 图片裁切视图。
 * 根据目标宽高比在视图中绘制裁切框蒙层，用户拖动图片选择裁切区域。
 */
class CropView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var sourceBitmap: Bitmap? = null
    private val drawMatrix = Matrix()
    private val inverseMatrix = Matrix()

    private var lastX = 0f
    private var lastY = 0f

    // 缩放手势识别
    private val scaleDetector: ScaleGestureDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val sf = detector.scaleFactor
                val currentScale = getCurrentScale()
                val newScale = currentScale * sf
                val minAllowed = baseScale
                val maxAllowed = baseScale * maxScaleMultiplier
                if (newScale < minAllowed || newScale > maxAllowed) return false

                drawMatrix.postScale(sf, sf, detector.focusX, detector.focusY)
                checkBounds()
                invalidate()
                return true
            }
        })
    /** 初始缩放基底（刚好填满裁切框的缩放值） */
    private var baseScale = 1f
    private val maxScaleMultiplier = 5f

    /** 目标裁切宽高比 = targetW / targetH */
    private var targetAspectRatio = 1f
    /** 裁切框在视图中的位置 */
    private val cropFrame = RectF()
    /** 最终输出尺寸 */
    private var outputW = 1080
    private var outputH = 1920

    // 蒙层绘制
    private val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x88000000.toInt()
        style = Paint.Style.FILL
    }
    private val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x44FFFFFF
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }

    fun setImageBitmap(bitmap: Bitmap, targetW: Int, targetH: Int) {
        this.sourceBitmap = bitmap
        this.targetAspectRatio = targetW.toFloat() / targetH.toFloat()
        this.outputW = targetW
        this.outputH = targetH

        post {
            calcCropFrame()
            initMatrix()
            invalidate()
        }
    }

    private fun getCurrentScale(): Float {
        val values = FloatArray(9)
        drawMatrix.getValues(values)
        return values[Matrix.MSCALE_X]
    }

    /** 计算裁切框：在视图内居中，保持目标宽高比，尽量填满 */
    private fun calcCropFrame() {
        val vw = width.toFloat()
        val vh = height.toFloat()
        if (vw <= 0 || vh <= 0) return

        val viewRatio = vw / vh

        if (viewRatio > targetAspectRatio) {
            // 视图比目标更宽 → 高度撑满，宽度按比例缩
            val fh = vh
            val fw = fh * targetAspectRatio
            cropFrame.set((vw - fw) / 2f, 0f, (vw + fw) / 2f, fh)
        } else {
            // 视图比目标更窄/等高 → 宽度撑满，高度按比例缩
            val fw = vw
            val fh = fw / targetAspectRatio
            cropFrame.set(0f, (vh - fh) / 2f, fw, (vh + fh) / 2f)
        }
    }

    private fun initMatrix() {
        val bmp = sourceBitmap ?: return
        if (cropFrame.width() <= 0 || cropFrame.height() <= 0) return

        val imgW = bmp.width.toFloat()
        val imgH = bmp.height.toFloat()

        // 缩放图片使其至少填满裁切框（不留空白）
        baseScale = Math.max(cropFrame.width() / imgW, cropFrame.height() / imgH)
        drawMatrix.setScale(baseScale, baseScale)

        // 居中到裁切框
        val dx = cropFrame.centerX() - imgW * baseScale / 2f
        val dy = cropFrame.centerY() - imgH * baseScale / 2f
        drawMatrix.postTranslate(dx, dy)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)

        if (scaleDetector.isInProgress) {
            return true
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x
                lastY = event.y
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - lastX
                val dy = event.y - lastY
                drawMatrix.postTranslate(dx, dy)
                checkBounds()
                lastX = event.x
                lastY = event.y
                invalidate()
                return true
            }
            MotionEvent.ACTION_POINTER_UP -> {
                // 双指缩放结束，更新为剩余手指的位置
                val idx = if (event.actionIndex == 0) 1 else 0
                lastX = event.getX(idx)
                lastY = event.getY(idx)
                return true
            }
        }
        return true
    }

    /** 确保图片始终覆盖裁切框，不留空白 */
    private fun checkBounds() {
        val bmp = sourceBitmap ?: return
        val rect = RectF(0f, 0f, bmp.width.toFloat(), bmp.height.toFloat())
        drawMatrix.mapRect(rect)

        var dx = 0f
        var dy = 0f

        if (rect.left > cropFrame.left) dx = cropFrame.left - rect.left
        if (rect.right < cropFrame.right) dx = cropFrame.right - rect.right
        if (rect.top > cropFrame.top) dy = cropFrame.top - rect.top
        if (rect.bottom < cropFrame.bottom) dy = cropFrame.bottom - rect.bottom

        drawMatrix.postTranslate(dx, dy)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0 && (w != oldw || h != oldh)) {
            calcCropFrame()
            initMatrix()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (cropFrame.isEmpty) return

        sourceBitmap?.let { bmp ->
            // 1. 绘制图片
            canvas.drawBitmap(bmp, drawMatrix, null)

            // 2. 绘制蒙层（裁切框外半透明遮挡）
            // 上部蒙层
            if (cropFrame.top > 0) {
                canvas.drawRect(0f, 0f, width.toFloat(), cropFrame.top, dimPaint)
            }
            // 下部蒙层
            if (cropFrame.bottom < height) {
                canvas.drawRect(0f, cropFrame.bottom, width.toFloat(), height.toFloat(), dimPaint)
            }
            // 左部蒙层
            if (cropFrame.left > 0) {
                canvas.drawRect(0f, cropFrame.top, cropFrame.left, cropFrame.bottom, dimPaint)
            }
            // 右部蒙层
            if (cropFrame.right < width) {
                canvas.drawRect(cropFrame.right, cropFrame.top, width.toFloat(), cropFrame.bottom, dimPaint)
            }

            // 3. 裁切框边框
            canvas.drawRect(cropFrame, framePaint)

            // 4. 九宫格辅助线
            val gw = cropFrame.width() / 3f
            val gh = cropFrame.height() / 3f
            for (i in 1..2) {
                val x = cropFrame.left + gw * i
                canvas.drawLine(x, cropFrame.top, x, cropFrame.bottom, gridPaint)
                val y = cropFrame.top + gh * i
                canvas.drawLine(cropFrame.left, y, cropFrame.right, y, gridPaint)
            }
        }
    }

    /**
     * 执行裁切：将裁切框内的图片区域提取为 Bitmap，输出到目标尺寸。
     */
    fun getCroppedBitmap(targetW: Int, targetH: Int): Bitmap? {
        val src = sourceBitmap ?: return null
        if (cropFrame.width() <= 0 || cropFrame.height() <= 0) return null

        // 逆推裁切框在原始图片上的对应区域
        drawMatrix.invert(inverseMatrix)
        val srcRect = RectF()
        inverseMatrix.mapRect(srcRect, cropFrame)

        val left = Math.max(0, srcRect.left.toInt())
        val top = Math.max(0, srcRect.top.toInt())
        val w = Math.min(src.width - left, srcRect.width().toInt())
        val h = Math.min(src.height - top, srcRect.height().toInt())

        if (w <= 0 || h <= 0) return null

        return try {
            val cropped = Bitmap.createBitmap(src, left, top, w, h)
            if (w != targetW || h != targetH) {
                val scaled = Bitmap.createScaledBitmap(cropped, targetW, targetH, true)
                if (scaled != cropped) cropped.recycle()
                scaled
            } else {
                cropped
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
